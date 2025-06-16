package com.bodyforge.domain.models

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