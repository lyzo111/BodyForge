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