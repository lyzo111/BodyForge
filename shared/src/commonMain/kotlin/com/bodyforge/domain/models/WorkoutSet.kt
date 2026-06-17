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
    val notes: String = "",
    val status: SetStatus = SetStatus.COMPLETED,
    val originalExerciseId: String? = null
) {
    val isSkipped: Boolean get() = status == SetStatus.SKIPPED
    val isSubstituted: Boolean get() = status == SetStatus.SUBSTITUTED

    companion object {
        fun createEmpty(exerciseId: String, setNumber: Int, defaultRestTime: Int, workoutId: String? = null): WorkoutSet {
            // Always use timestamp plus a random suffix to ensure unique IDs
            val timestamp = Clock.System.now().epochSeconds
            val uniqueId = "set_${timestamp}_${exerciseId}_${setNumber}_${(0..9999).random()}"

            return WorkoutSet(
                id = uniqueId,
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

    /** Marks this set as skipped for the current workout without deleting it. */
    fun skip(): WorkoutSet {
        return copy(
            status = SetStatus.SKIPPED,
            completed = false,
            completedAt = null
        )
    }

    /** Marks this set as substituted, remembering the exercise it replaced. */
    fun substitute(originalExerciseId: String): WorkoutSet {
        return copy(
            status = SetStatus.SUBSTITUTED,
            originalExerciseId = originalExerciseId
        )
    }
}