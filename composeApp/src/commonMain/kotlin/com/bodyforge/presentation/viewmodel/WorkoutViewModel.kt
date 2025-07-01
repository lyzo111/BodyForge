package com.bodyforge.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.presentation.state.SharedWorkoutState
import kotlinx.coroutines.launch

class WorkoutViewModel : ViewModel() {

    private val sharedState = SharedWorkoutState

    fun initialize() {
        viewModelScope.launch {
            sharedState.refreshAll()
        }
    }

    fun startQuickWorkout(exercises: List<Exercise>, workoutName: String = "") {
        viewModelScope.launch {
            sharedState.setLoading(true)
            try {
                if (exercises.isEmpty()) {
                    sharedState.setError("Please select at least one exercise")
                    return@launch
                }

                val workout = Workout.create(workoutName.ifEmpty { "" }, exercises)
                val savedWorkout = sharedState.workoutRepo.saveWorkout(workout)

                sharedState.updateActiveWorkout(savedWorkout)
                sharedState.clearError()
            } catch (e: Exception) {
                sharedState.setError("Failed to start workout: ${e.message ?: "Unknown error"}")
            } finally {
                sharedState.setLoading(false)
            }
        }
    }

    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            sharedState.setLoading(true)
            try {
                // Load exercises from template
                val exercises = mutableListOf<Exercise>()
                template.exerciseIds.forEach { exerciseId ->
                    sharedState.exerciseRepo.getExerciseById(exerciseId)?.let { exercise ->
                        exercises.add(exercise)
                    }
                }

                if (exercises.isEmpty()) {
                    sharedState.setError("Template contains no valid exercises")
                    return@launch
                }

                if (exercises.size != template.exerciseIds.size) {
                    sharedState.setError("Some exercises from this template are no longer available")
                }

                val workout = Workout.create(template.name.ifEmpty { "Template Workout" }, exercises)
                val savedWorkout = sharedState.workoutRepo.saveWorkout(workout)

                sharedState.updateActiveWorkout(savedWorkout)
                sharedState.clearError()
            } catch (e: Exception) {
                sharedState.setError("Failed to start workout from template: ${e.message ?: "Unknown error"}")
            } finally {
                sharedState.setLoading(false)
            }
        }
    }

    fun updateSet(exerciseId: String, setId: String, reps: Int?, weight: Double?, completed: Boolean?) {
        val currentWorkout = sharedState.activeWorkout.value ?: return

        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId }
                    ?: return@launch

                val set = exerciseInWorkout.sets.find { it.id == setId }
                    ?: return@launch

                val updatedSet = set.copy(
                    reps = reps ?: set.reps,
                    weightKg = weight ?: set.weightKg,
                    completed = completed ?: set.completed
                ).let {
                    if (completed == true && !set.completed) it.complete() else it
                }

                val updatedExercise = exerciseInWorkout.updateSet(setId, updatedSet)
                val updatedWorkout = currentWorkout.updateExercise(exerciseId, updatedExercise)

                sharedState.workoutRepo.updateWorkout(updatedWorkout)
                sharedState.updateActiveWorkout(updatedWorkout)

            } catch (e: Exception) {
                sharedState.setError("Failed to update set: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun addSetToExercise(exerciseId: String) {
        val currentWorkout = sharedState.activeWorkout.value ?: return

        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId }
                    ?: return@launch

                val updatedExercise = exerciseInWorkout.addSet()
                val updatedWorkout = currentWorkout.updateExercise(exerciseId, updatedExercise)

                sharedState.workoutRepo.updateWorkout(updatedWorkout)
                sharedState.updateActiveWorkout(updatedWorkout)

            } catch (e: Exception) {
                sharedState.setError("Failed to add set: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun removeSetFromExercise(exerciseId: String, setId: String) {
        val currentWorkout = sharedState.activeWorkout.value ?: return

        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId }
                    ?: return@launch

                val updatedSets = exerciseInWorkout.sets.filter { it.id != setId }
                val updatedExercise = exerciseInWorkout.copy(sets = updatedSets)
                val updatedWorkout = currentWorkout.updateExercise(exerciseId, updatedExercise)

                sharedState.workoutRepo.updateWorkout(updatedWorkout)
                sharedState.updateActiveWorkout(updatedWorkout)

            } catch (e: Exception) {
                sharedState.setError("Failed to remove set: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun completeWorkout() {
        val currentWorkout = sharedState.activeWorkout.value ?: return

        viewModelScope.launch {
            sharedState.setLoading(true)
            try {
                val finishedWorkout = currentWorkout.finish()
                sharedState.workoutRepo.updateWorkout(finishedWorkout)

                // Clear active workout and refresh completed workouts
                sharedState.updateActiveWorkout(null)
                sharedState.loadCompletedWorkouts()

                sharedState.clearError()
            } catch (e: Exception) {
                sharedState.setError("Failed to finish workout: ${e.message ?: "Unknown error"}")
            } finally {
                sharedState.setLoading(false)
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                val success = sharedState.workoutRepo.deleteWorkout(workoutId)
                if (success) {
                    sharedState.loadCompletedWorkouts()
                } else {
                    sharedState.setError("Failed to delete workout")
                }
            } catch (e: Exception) {
                sharedState.setError("Failed to delete workout: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun updateWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                sharedState.workoutRepo.updateWorkout(workout)
                sharedState.loadCompletedWorkouts()
            } catch (e: Exception) {
                sharedState.setError("Failed to update workout: ${e.message ?: "Unknown error"}")
            }
        }
    }
}