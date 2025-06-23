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
import kotlinx.datetime.Clock

data class WorkoutUiState(
    val availableExercises: List<Exercise> = emptyList(),
    val selectedExercises: List<Exercise> = emptyList(),
    val currentWorkout: Workout? = null,
    val completedWorkouts: List<Workout> = emptyList(),
    val bodyweight: Double = 75.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeTab: String = "create"
)

class WorkoutViewModel : ViewModel() {

    private val exerciseRepo = ExerciseRepositoryImpl()
    private val workoutRepo = WorkoutRepositoryImpl()

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
        loadActiveWorkout()
        loadCompletedWorkouts()
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

    private fun loadCompletedWorkouts() {
        viewModelScope.launch {
            try {
                val completedWorkouts = workoutRepo.getCompletedWorkouts()
                _uiState.value = _uiState.value.copy(
                    completedWorkouts = completedWorkouts
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load workout history: ${e.message}"
                )
            }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.value = _uiState.value.copy(activeTab = tab)

        if (tab == "history") {
            loadCompletedWorkouts()
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

                loadCompletedWorkouts()

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

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                val success = workoutRepo.deleteWorkout(workoutId)
                if (success) {
                    loadCompletedWorkouts()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete workout"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete workout: ${e.message}"
                )
            }
        }
    }

    fun addSetToExercise(exerciseId: String) {
        val currentWorkout = _uiState.value.currentWorkout ?: return

        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId }
                    ?: return@launch

                val updatedExercise = exerciseInWorkout.addSet()
                val updatedWorkout = currentWorkout.updateExercise(exerciseId, updatedExercise)

                workoutRepo.updateWorkout(updatedWorkout)

                _uiState.value = _uiState.value.copy(currentWorkout = updatedWorkout)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add set: ${e.message}"
                )
            }
        }
    }

    fun removeSetFromExercise(exerciseId: String, setId: String) {
        val currentWorkout = _uiState.value.currentWorkout ?: return

        viewModelScope.launch {
            try {
                val exerciseInWorkout = currentWorkout.exercises.find { it.exercise.id == exerciseId }
                    ?: return@launch

                val updatedSets = exerciseInWorkout.sets.filter { it.id != setId }
                val updatedExercise = exerciseInWorkout.copy(sets = updatedSets)
                val updatedWorkout = currentWorkout.updateExercise(exerciseId, updatedExercise)

                workoutRepo.updateWorkout(updatedWorkout)

                _uiState.value = _uiState.value.copy(currentWorkout = updatedWorkout)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove set: ${e.message}"
                )
            }
        }
    }

    fun updateBodyweight(newBodyweight: Double) {
        _uiState.value = _uiState.value.copy(bodyweight = newBodyweight)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Create Custom Exercise
    fun createCustomExercise(
        name: String,
        muscleGroups: List<String>,
        equipment: String,
        isBodyweight: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val customExercise = Exercise(
                    id = "custom_${Clock.System.now().epochSeconds}_${name.replace(" ", "_").lowercase()}",
                    name = name,
                    muscleGroups = muscleGroups,
                    instructions = "", // User can add later
                    equipmentNeeded = equipment,
                    isCustom = true,
                    isBodyweight = isBodyweight,
                    defaultRestTimeSeconds = if (isBodyweight) 90 else 120
                )

                exerciseRepo.saveCustomExercise(customExercise)
                loadExercises() // Reload to show new exercise

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create exercise: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // Update existing workout (for history editing)
    fun updateWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                workoutRepo.updateWorkout(workout)
                loadCompletedWorkouts() // Reload history to show changes
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update workout: ${e.message}"
                )
            }
        }
    }
}