package com.bodyforge.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyforge.data.repository.ExerciseRepositoryImpl
import com.bodyforge.data.repository.WorkoutRepositoryImpl
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
    val error: String? = null,
    val activeTab: String = "create" // create, active, history
)

class WorkoutViewModel : ViewModel() {

    private val exerciseRepo = ExerciseRepositoryImpl()
    private val workoutRepo = WorkoutRepositoryImpl()

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
                val exercises = exerciseRepo.getAllExercises()
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
                val activeWorkout = workoutRepo.getActiveWorkout()
                _uiState.value = _uiState.value.copy(
                    currentWorkout = activeWorkout,
                    activeTab = if (activeWorkout != null) "active" else "create"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load active workout: ${e.message}"
                )
            }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
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
                val savedWorkout = workoutRepo.saveWorkout(workout)

                _uiState.value = _uiState.value.copy(
                    currentWorkout = savedWorkout,
                    selectedExercises = emptyList(),
                    activeTab = "active",
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
                workoutRepo.updateWorkout(updatedWorkout)

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
                workoutRepo.updateWorkout(finishedWorkout)

                _uiState.value = _uiState.value.copy(
                    currentWorkout = null,
                    activeTab = "create",
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