package com.bodyforge.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyforge.data.DatabaseFactory
import com.bodyforge.data.repository.SimpleWorkoutRepository
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkoutUiState(
    val availableExercises: List<Exercise> = emptyList(),
    val selectedExercises: List<Exercise> = emptyList(),
    val currentWorkout: Workout? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WorkoutViewModel : ViewModel() {

    private val workoutRepo = WorkoutRepositoryImpl(DatabaseFactory.create())
    private val exerciseRepo = ExerciseRepositoryImpl(DatabaseFactory.create())

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
        loadActiveWorkout()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val exercises = repository.getAllExercises()
                _uiState.value = _uiState.value.copy(
                    availableExercises = exercises,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load exercises: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun loadActiveWorkout() {
        viewModelScope.launch {
            try {
                val activeWorkout = repository.getActiveWorkout()
                _uiState.value = _uiState.value.copy(currentWorkout = activeWorkout)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load active workout: ${e.message}"
                )
            }
        }
    }

    fun addExerciseToSelection(exercise: Exercise) {
        val currentSelected = _uiState.value.selectedExercises
        if (!currentSelected.contains(exercise)) {
            _uiState.value = _uiState.value.copy(
                selectedExercises = currentSelected + exercise
            )
        }
    }

    fun removeExerciseFromSelection(exercise: Exercise) {
        val currentSelected = _uiState.value.selectedExercises
        _uiState.value = _uiState.value.copy(
            selectedExercises = currentSelected - exercise
        )
    }

    fun startWorkout(workoutName: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val selectedExercises = _uiState.value.selectedExercises
                if (selectedExercises.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Please select at least one exercise",
                        isLoading = false
                    )
                    return@launch
                }

                val workout = Workout.create(workoutName, selectedExercises)
                val savedWorkout = repository.saveWorkout(workout)

                _uiState.value = _uiState.value.copy(
                    currentWorkout = savedWorkout,
                    selectedExercises = emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start workout: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun updateSet(exerciseId: String, setId: String, reps: Int?, weight: Double?, completed: Boolean?) {
        val currentWorkout = _uiState.value.currentWorkout ?: return

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

                // Save to database
                repository.updateWorkout(updatedWorkout)

                _uiState.value = _uiState.value.copy(currentWorkout = updatedWorkout)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update set: ${e.message}"
                )
            }
        }
    }

    fun completeWorkout() {
        val currentWorkout = _uiState.value.currentWorkout ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val finishedWorkout = currentWorkout.finish()
                repository.updateWorkout(finishedWorkout)

                _uiState.value = _uiState.value.copy(
                    currentWorkout = null,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to finish workout: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}