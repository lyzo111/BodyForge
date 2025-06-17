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
        fun createEmpty(exerciseId: String, setNumber: Int, defaultRestTime: Int, workoutId: String? = null): WorkoutSet {
            // FIXED: Always use timestamp to ensure unique IDs
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
}