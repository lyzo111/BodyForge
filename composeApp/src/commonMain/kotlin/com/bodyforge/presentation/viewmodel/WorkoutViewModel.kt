package com.bodyforge.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.SetStatus
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
            sharedState.startWorkoutFromTemplate(template)
        }
    }

    // Marks every set of an exercise as skipped for this workout (drawn as a gap in progress graphs).
    fun skipExercise(exerciseId: String) {
        val currentWorkout = sharedState.activeWorkout.value ?: return
        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId } ?: return@launch
                val updated = exerciseInWorkout.copy(sets = exerciseInWorkout.sets.map { it.skip() })
                val newWorkout = currentWorkout.updateExercise(exerciseId, updated)
                sharedState.workoutRepo.updateWorkout(newWorkout)
                sharedState.updateActiveWorkout(newWorkout)
            } catch (e: Exception) {
                sharedState.setError("Failed to skip exercise: ${e.message ?: "Unknown error"}")
            }
        }
    }

    // Clears the skipped state so the exercise can be logged normally again.
    fun resumeExercise(exerciseId: String) {
        val currentWorkout = sharedState.activeWorkout.value ?: return
        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId } ?: return@launch
                val updated = exerciseInWorkout.copy(sets = exerciseInWorkout.sets.map { it.copy(status = SetStatus.COMPLETED) })
                val newWorkout = currentWorkout.updateExercise(exerciseId, updated)
                sharedState.workoutRepo.updateWorkout(newWorkout)
                sharedState.updateActiveWorkout(newWorkout)
            } catch (e: Exception) {
                sharedState.setError("Failed to resume exercise: ${e.message ?: "Unknown error"}")
            }
        }
    }

    // Replaces an exercise for this workout while remembering the original it stood in for.
    fun substituteExercise(exerciseId: String, newExercise: Exercise) {
        val currentWorkout = sharedState.activeWorkout.value ?: return
        if (newExercise.id == exerciseId) return
        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId } ?: return@launch
                val updatedSets = exerciseInWorkout.sets.map {
                    it.copy(
                        status = SetStatus.SUBSTITUTED,
                        originalExerciseId = it.originalExerciseId ?: exerciseId
                    )
                }
                val updated = exerciseInWorkout.copy(exercise = newExercise, sets = updatedSets)
                val newWorkout = currentWorkout.updateExercise(exerciseId, updated)
                sharedState.workoutRepo.updateWorkout(newWorkout)
                sharedState.updateActiveWorkout(newWorkout)
            } catch (e: Exception) {
                sharedState.setError("Failed to substitute exercise: ${e.message ?: "Unknown error"}")
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