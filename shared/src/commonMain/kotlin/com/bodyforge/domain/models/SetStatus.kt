package com.bodyforge.domain.models

/**
 * Lifecycle status of a single set within a workout.
 *
 * COMPLETED is the normal state for any set that was performed or is intended to be performed.
 * SKIPPED marks a set the user explicitly left out for this workout (drawn as a gap in progress graphs).
 * SUBSTITUTED marks a set whose exercise was swapped for another one; the replaced exercise is kept
 * in [WorkoutSet.originalExerciseId] so progress graphs can render it as a tagged node.
 */
enum class SetStatus {
    COMPLETED,
    SKIPPED,
    SUBSTITUTED;

    companion object {
        /** Maps a stored status string back to an enum, falling back to COMPLETED for legacy or unknown values. */
        fun fromStorage(value: String): SetStatus =
            entries.firstOrNull { it.name == value } ?: COMPLETED
    }
}
