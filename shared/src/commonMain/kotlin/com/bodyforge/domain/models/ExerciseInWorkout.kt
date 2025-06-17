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

    // NEW: Better stats logic for bodyweight vs weighted exercises
    val performedSets: Int get() = sets.count { set ->
        set.reps > 0 && (set.weightKg > 0 || exercise.isBodyweight)
    }

    val totalVolumePerformed: Double get() = sets.filter { set ->
        set.reps > 0 && set.weightKg > 0  // Volume only counts with actual weight
    }.sumOf { it.reps * it.weightKg }

    fun addSet(): ExerciseInWorkout {
        val newSet = WorkoutSet.createEmpty(
            exerciseId = exercise.id,
            setNumber = sets.size + 1,
            defaultRestTime = exercise.defaultRestTimeSeconds,
            workoutId = "current_workout"  // TODO: Pass real workout ID
        )
        return copy(sets = sets + newSet)
    }

    fun updateSet(setId: String, updatedSet: WorkoutSet): ExerciseInWorkout {
        val updatedSets = sets.map { if (it.id == setId) updatedSet else it }
        return copy(sets = updatedSets)
    }
}