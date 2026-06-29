package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class Workout(
    val id: String,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val exercises: List<ExerciseInWorkout> = emptyList(),
    val notes: String = "",
    val templateId: String? = null
) {
    val isActive: Boolean get() = finishedAt == null
    val isCompleted: Boolean get() = finishedAt != null
    // Active training time: total elapsed minus long idle gaps. Built from the start, each completed
    // set's time and the finish; only gaps shorter than the pause threshold count, so resuming a
    // workout hours later does not count the break as training.
    val durationMinutes: Long? get() {
        val end = finishedAt ?: return null
        val wallSeconds = (end.epochSeconds - startedAt.epochSeconds).coerceAtLeast(0)
        val setTimes = exercises.flatMap { it.sets }.mapNotNull { it.completedAt?.epochSeconds }
        // Without any completed-set timestamps there is nothing to detect a pause from, so fall back
        // to the plain elapsed time.
        if (setTimes.isEmpty()) return wallSeconds / 60
        val pauseThresholdSeconds = 20 * 60L
        val stamps = (setTimes + listOf(startedAt.epochSeconds, end.epochSeconds)).sorted()
        var active = 0L
        for (i in 1 until stamps.size) {
            val gap = stamps[i] - stamps[i - 1]
            if (gap in 0..pauseThresholdSeconds) active += gap
        }
        return active / 60
    }

    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val totalVolume: Double get() = exercises.sumOf { it.totalVolume }

    val performedSets: Int get() = exercises.sumOf { it.performedSets }
    val totalVolumePerformed: Double get() = exercises.sumOf { it.totalVolumePerformed }

    val startDate get() = startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

    companion object {
        fun create(name: String, exercises: List<Exercise>, templateId: String? = null): Workout {
            val workoutId = "workout_${Clock.System.now().epochSeconds}"

            val exercisesInWorkout = exercises.mapIndexed { index, exercise ->
                val defaultSets = (1..3).map { setNumber ->
                    WorkoutSet.createEmpty(exercise.id, setNumber, exercise.defaultRestTimeSeconds, workoutId)  // Pass workout ID
                }
                ExerciseInWorkout(
                    exercise = exercise,
                    sets = defaultSets,
                    orderInWorkout = index
                )
            }

            return Workout(
                id = workoutId,
                name = name.ifBlank { generateWorkoutName(exercises) },
                startedAt = Clock.System.now(),
                exercises = exercisesInWorkout,
                templateId = templateId
            )
        }

        private fun generateWorkoutName(exercises: List<Exercise>): String {
            val muscleGroups = exercises.flatMap { it.muscleGroups }
            return when {
                muscleGroups.count { it.contains("Chest") } >= 1 &&
                        muscleGroups.count { it.contains("Triceps") } >= 1 -> "Push Day"

                muscleGroups.count { it.contains("Back") } >= 1 &&
                        muscleGroups.count { it.contains("Biceps") } >= 1 -> "Pull Day"

                muscleGroups.count { it.contains("Quadriceps") } >= 1 ||
                        muscleGroups.count { it.contains("Glutes") } >= 1 -> "Leg Day"

                else -> "Upper Body Workout"
            }
        }
    }

    fun finish(): Workout {
        return copy(finishedAt = Clock.System.now())
    }

    fun updateExercise(exerciseId: String, updatedExercise: ExerciseInWorkout): Workout {
        val updatedExercises = exercises.map {
            if (it.exercise.id == exerciseId) updatedExercise else it
        }
        return copy(exercises = updatedExercises)
    }
}