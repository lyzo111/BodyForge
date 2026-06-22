package com.bodyforge.data

import kotlinx.datetime.LocalDate

// One imported set/exercise/workout, parsed from a "date,workout,exercise,reps,weight" CSV
// (one row per set).
data class CsvSet(val reps: Int, val weight: Double)
data class CsvExercise(val name: String, val sets: List<CsvSet>)
data class CsvWorkout(val date: LocalDate, val name: String, val exercises: List<CsvExercise>)
data class CsvParseResult(val workouts: List<CsvWorkout>, val skippedRows: Int)

private data class CsvRow(val date: LocalDate, val workout: String, val exercise: String, val reps: Int, val weight: Double)

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
        val weight = cols[4].toDoubleOrNull()
        val exercise = cols[2]
        if (date == null || reps == null || weight == null || exercise.isBlank()) { skipped++; return@forEachIndexed }
        rows.add(CsvRow(date, cols[1].ifBlank { "Imported workout" }, exercise, reps, weight))
    }

    // Group rows into workouts (by date + workout name), then into exercises, preserving order.
    val workouts = rows.groupBy { it.date to it.workout }.map { (key, workoutRows) ->
        val exercises = workoutRows.groupBy { it.exercise }.map { (exName, exRows) ->
            CsvExercise(exName, exRows.map { CsvSet(it.reps, it.weight) })
        }
        CsvWorkout(key.first, key.second, exercises)
    }
    return CsvParseResult(workouts, skipped)
}
