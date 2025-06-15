package com.bodyforge.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyforge.di.AppContainer
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.ExerciseInWorkout
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

    private val createWorkout = AppContainer.appModule.createWorkout
    private val updateWorkoutSet = AppContainer.appModule.updateWorkoutSet
    private val finishWorkout = AppContainer.appModule.finishWorkout
    private val exerciseRepository = AppContainer.appModule.exerciseRepository
    private val workoutRepository = AppContainer.appModule.workoutRepository

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
                val exercises = exerciseRepository.getAllExercises()
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
                val activeWorkout = workoutRepository.getActiveWorkout()
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
                val result = createWorkout(workoutName, _uiState.value.selectedExercises)
                result.fold(
                    onSuccess = { workout ->
                        _uiState.value = _uiState.value.copy(
                            currentWorkout = workout,
                            selectedExercises = emptyList(),
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to start workout",
                            isLoading = false
                        )
                    }
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
                val result = updateWorkoutSet(
                    workoutId = currentWorkout.id,
                    exerciseId = exerciseId,
                    setId = setId,
                    reps = reps,
                    weight = weight,
                    completed = completed
                )

                result.fold(
                    onSuccess = { updatedWorkout ->
                        _uiState.value = _uiState.value.copy(currentWorkout = updatedWorkout)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to update set"
                        )
                    }
                )
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
                val result = finishWorkout(currentWorkout.id)
                result.fold(
                    onSuccess = { finishedWorkout ->
                        _uiState.value = _uiState.value.copy(
                            currentWorkout = null,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to finish workout",
                            isLoading = false
                        )
                    }
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