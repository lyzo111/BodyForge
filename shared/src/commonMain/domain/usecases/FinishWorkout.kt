package com.bodyforge.domain.usecases

import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.repository.WorkoutRepository

class FinishWorkout(
    private val workoutRepository: WorkoutRepository
) {

    suspend operator fun invoke(workoutId: String): Result<Workout> {
        return try {
            val workout = workoutRepository.getWorkout(workoutId)
                ?: return Result.failure(IllegalArgumentException("Workout not found"))

            if (workout.isCompleted) {
                return Result.failure(IllegalStateException("Workout is already completed"))
            }

            // Finish the workout
            val finishedWorkout = workout.finish()

            // Save finished workout
            val savedWorkout = workoutRepository.updateWorkout(finishedWorkout)

            Result.success(savedWorkout)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}