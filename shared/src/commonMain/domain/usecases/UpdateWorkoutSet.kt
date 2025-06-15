package com.bodyforge.domain.usecases

import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.repository.WorkoutRepository

class UpdateWorkoutSet(
    private val workoutRepository: WorkoutRepository
) {

    suspend operator fun invoke(
        workoutId: String,
        exerciseId: String,
        setId: String,
        reps: Int? = null,
        weight: Double? = null,
        completed: Boolean? = null
    ): Result<Workout> {
        return try {
            val workout = workoutRepository.getWorkout(workoutId)
                ?: return Result.failure(IllegalArgumentException("Workout not found"))

            val exerciseInWorkout = workout.exercises.find { it.exercise.id == exerciseId }
                ?: return Result.failure(IllegalArgumentException("Exercise not found in workout"))

            val set = exerciseInWorkout.sets.find { it.id == setId }
                ?: return Result.failure(IllegalArgumentException("Set not found"))

            // Update set with provided values
            val updatedSet = set.copy(
                reps = reps ?: set.reps,
                weightKg = weight ?: set.weightKg,
                completed = completed ?: set.completed
            ).let {
                // If marking as completed, set completion time
                if (completed == true && !set.completed) it.complete() else it
            }

            // Update exercise with new set
            val updatedExercise = exerciseInWorkout.updateSet(setId, updatedSet)

            // Update workout with new exercise
            val updatedWorkout = workout.updateExercise(exerciseId, updatedExercise)

            // Save updated workout
            val savedWorkout = workoutRepository.updateWorkout(updatedWorkout)

            Result.success(savedWorkout)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
