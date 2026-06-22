package com.bodyforge.data

import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

// One imported set/exercise/workout, parsed from a
// "date,workout,exercise,reps,weight[,unit][,notes]" CSV (one row per set). weight is stored in kg;
// a "lbs" unit is converted on import. notes is optional free text per set.
data class CsvSet(val reps: Int, val weight: Double, val notes: String = "")
data class CsvExercise(val name: String, val sets: List<CsvSet>)
data class CsvWorkout(val date: LocalDate, val name: String, val exercises: List<CsvExercise>)
data class CsvParseResult(val workouts: List<CsvWorkout>, val skippedRows: Int)

private data class CsvRow(val date: LocalDate, val workout: String, val exercise: String, val reps: Int, val weight: Double, val notes: String)

private const val LB_TO_KG = 0.45359237

// Converts a weight to kg when the row's unit column says pounds; otherwise keeps it as-is.
// Rounded to two decimals so converted values stay tidy.
private fun normalizeWeightToKg(weight: Double, unit: String?): Double {
    val u = unit?.trim()?.lowercase()
    val kg = if (u == "lb" || u == "lbs" || u == "pound" || u == "pounds") weight * LB_TO_KG else weight
    return (kg * 100).roundToInt() / 100.0
}

// Parses dates as ISO (yyyy-MM-dd) or as day-first dd.MM.yyyy / dd/MM/yyyy / dd-MM-yy.
private fun parseFlexibleDate(s: String): LocalDate? {
    runCatching { LocalDate.parse(s) }.getOrNull()?.let { return it }
    val m = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})""").find(s) ?: return null
    val (a, b, c) = m.destructured
    val year = c.toInt().let { if (it < 100) 2000 + it else it }
    return runCatching { LocalDate(year, b.toInt(), a.toInt()) }.getOrNull()
}

fun parseWorkoutCsv(csv: String): CsvParseResult {
    val rows = mutableListOf<CsvRow>()
    var skipped = 0
    val lines = csv.split("\r\n", "\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEachIndexed { index, line ->
        val cols = line.split(",").map { it.trim() }
        // Skip an optional header row.
        if (index == 0 && cols.isNotEmpty() && cols[0].equals("date", ignoreCase = true)) return@forEachIndexed
        if (cols.size < 5) { skipped++; return@forEachIndexed }
        val date = parseFlexibleDate(cols[0])
        val reps = cols[3].toIntOrNull()
        val rawWeight = cols[4].toDoubleOrNull()
        val exercise = cols[2]
        if (date == null || reps == null || rawWeight == null || exercise.isBlank()) { skipped++; return@forEachIndexed }
        val weight = normalizeWeightToKg(rawWeight, cols.getOrNull(5))
        // Everything past the notes column is kept as part of the note, so commas in notes survive.
        val notes = if (cols.size > 6) cols.subList(6, cols.size).joinToString(",").trim() else ""
        rows.add(CsvRow(date, cols[1].ifBlank { "Imported workout" }, exercise, reps, weight, notes))
    }

    // Group rows into workouts (by date + workout name), then into exercises, preserving order.
    val workouts = rows.groupBy { it.date to it.workout }.map { (key, workoutRows) ->
        val exercises = workoutRows.groupBy { it.exercise }.map { (exName, exRows) ->
            CsvExercise(exName, exRows.map { CsvSet(it.reps, it.weight, it.notes) })
        }
        CsvWorkout(key.first, key.second, exercises)
    }
    return CsvParseResult(workouts, skipped)
}
