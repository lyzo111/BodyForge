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

// shared/src/commonMain/domain/usecases/UpdateWorkoutSet.kt
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

// shared/src/commonMain/domain/usecases/FinishWorkout.kt
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