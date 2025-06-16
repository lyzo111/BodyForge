package com.bodyforge.domain.models

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val instructions: String = "",
    val equipmentNeeded: String = "",
    val isCustom: Boolean = false,
    val defaultRestTimeSeconds: Int = 90
) {
    companion object {
        fun getStandardExercises() = listOf(
            Exercise("bench_press", "Bench Press", listOf("Chest", "Triceps"), equipmentNeeded = "Barbell", defaultRestTimeSeconds = 120),
            Exercise("squat", "Squat", listOf("Quadriceps", "Glutes"), equipmentNeeded = "Barbell", defaultRestTimeSeconds = 180),
            Exercise("deadlift", "Deadlift", listOf("Back", "Hamstrings"), equipmentNeeded = "Barbell", defaultRestTimeSeconds = 180),
            Exercise("pull_ups", "Pull-ups", listOf("Back", "Biceps"), equipmentNeeded = "Pull-up Bar", defaultRestTimeSeconds = 120),
            Exercise("overhead_press", "Overhead Press", listOf("Shoulders", "Triceps"), equipmentNeeded = "Barbell", defaultRestTimeSeconds = 120),
            Exercise("barbell_row", "Barbell Row", listOf("Back", "Biceps"), equipmentNeeded = "Barbell", defaultRestTimeSeconds = 120),
            Exercise("dips", "Dips", listOf("Chest", "Triceps"), equipmentNeeded = "Dip Bar", defaultRestTimeSeconds = 90),
            Exercise("chin_ups", "Chin-ups", listOf("Back", "Biceps"), equipmentNeeded = "Pull-up Bar", defaultRestTimeSeconds = 120)
        )
    }
}