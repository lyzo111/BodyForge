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
        // Standard exercises for quick start
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

// shared/src/commonMain/domain/models/WorkoutSet.kt
package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class WorkoutSet(
    val id: String,
    val reps: Int,
    val weightKg: Double,
    val restTimeSeconds: Int,
    val completed: Boolean = false,
    val completedAt: Instant? = null,
    val notes: String = ""
) {
    companion object {
        fun createEmpty(exerciseId: String, setNumber: Int, defaultRestTime: Int): WorkoutSet {
            return WorkoutSet(
                id = "${exerciseId}_set_$setNumber",
                reps = 0,
                weightKg = 0.0,
                restTimeSeconds = defaultRestTime
            )
        }
    }

    fun complete(): WorkoutSet {
        return copy(
            completed = true,
            completedAt = Clock.System.now()
        )
    }
}

// shared/src/commonMain/domain/models/ExerciseInWorkout.kt
package com.bodyforge.domain.models

data class ExerciseInWorkout(
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
    val orderInWorkout: Int,
    val notes: String = ""
) {
    val isCompleted: Boolean get() = sets.isNotEmpty() && sets.all { it.completed }
    val completedSets: Int get() = sets.count { it.completed }
    val totalVolume: Double get() = sets.filter { it.completed }.sumOf { it.reps * it.weightKg }

    fun addSet(): ExerciseInWorkout {
        val newSet = WorkoutSet.createEmpty(
            exerciseId = exercise.id,
            setNumber = sets.size + 1,
            defaultRestTime = exercise.defaultRestTimeSeconds
        )
        return copy(sets = sets + newSet)
    }

    fun updateSet(setId: String, updatedSet: WorkoutSet): ExerciseInWorkout {
        val updatedSets = sets.map { if (it.id == setId) updatedSet else it }
        return copy(sets = updatedSets)
    }
}

// shared/src/commonMain/domain/models/Workout.kt
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
    val notes: String = ""
) {
    val isActive: Boolean get() = finishedAt == null
    val isCompleted: Boolean get() = finishedAt != null
    val durationMinutes: Long? get() = finishedAt?.let {
        (it.epochSeconds - startedAt.epochSeconds) / 60
    }

    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val totalVolume: Double get() = exercises.sumOf { it.totalVolume }

    val startDate get() = startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

    companion object {
        fun create(name: String, exercises: List<Exercise>): Workout {
            val exercisesInWorkout = exercises.mapIndexed { index, exercise ->
                val defaultSets = (1..3).map { setNumber ->
                    WorkoutSet.createEmpty(exercise.id, setNumber, exercise.defaultRestTimeSeconds)
                }
                ExerciseInWorkout(
                    exercise = exercise,
                    sets = defaultSets,
                    orderInWorkout = index
                )
            }

            return Workout(
                id = "workout_${Clock.System.now().epochSeconds}",
                name = name.ifBlank { generateWorkoutName(exercises) },
                startedAt = Clock.System.now(),
                exercises = exercisesInWorkout
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