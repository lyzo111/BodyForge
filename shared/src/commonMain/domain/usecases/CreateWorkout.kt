package com.bodyforge.domain.usecases

import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.repository.WorkoutRepository

class CreateWorkout(
    private val workoutRepository: WorkoutRepository
) {

    suspend operator fun invoke(
        workoutName: String = "",
        selectedExercises: List<Exercise>
    ): Result<Workout> {
        return try {
            // Validation
            if (selectedExercises.isEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Workout must contain at least one exercise")
                )
            }

            // Check if there's already an active workout
            val activeWorkout = workoutRepository.getActiveWorkout()
            if (activeWorkout != null) {
                return Result.failure(
                    IllegalStateException("There's already an active workout. Finish it first.")
                )
            }

            // Create new workout
            val workout = Workout.create(
                name = workoutName,
                exercises = selectedExercises
            )

            // Save workout
            val savedWorkout = workoutRepository.saveWorkout(workout)

            Result.success(savedWorkout)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
