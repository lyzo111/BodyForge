package com.bodyforge.presentation.state

import com.bodyforge.data.repository.ExerciseRepositoryImpl
import com.bodyforge.data.repository.WorkoutRepositoryImpl
import com.bodyforge.data.repository.WorkoutTemplateRepositoryImpl
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SharedWorkoutState {
    // Repositories - Single instances for entire app
    val exerciseRepo = ExerciseRepositoryImpl()
    val workoutRepo = WorkoutRepositoryImpl()
    val templateRepo = WorkoutTemplateRepositoryImpl()

    // Shared State Flows - Single source of truth
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _completedWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _completedWorkouts.asStateFlow()

    private val _templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val templates: StateFlow<List<WorkoutTemplate>> = _templates.asStateFlow()

    private val _bodyweight = MutableStateFlow(75.0)
    val bodyweight: StateFlow<Double> = _bodyweight.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Update functions
    suspend fun loadExercises() {
        _isLoading.value = true
        try {
            val exerciseList = exerciseRepo.getAllExercises()
            _exercises.value = exerciseList
        } catch (e: Exception) {
            _error.value = "Failed to load exercises: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadActiveWorkout() {
        try {
            val workout = workoutRepo.getActiveWorkout()
            _activeWorkout.value = workout
        } catch (e: Exception) {
            _error.value = "Failed to load active workout: ${e.message}"
        }
    }

    suspend fun loadCompletedWorkouts() {
        try {
            val workouts = workoutRepo.getCompletedWorkouts()
            _completedWorkouts.value = workouts
        } catch (e: Exception) {
            _error.value = "Failed to load workout history: ${e.message}"
        }
    }

    suspend fun loadTemplates() {
        try {
            val templateList = templateRepo.getAllTemplates()
            _templates.value = templateList
        } catch (e: Exception) {
            _error.value = "Failed to load templates: ${e.message}"
        }
    }

    fun updateActiveWorkout(workout: Workout?) {
        _activeWorkout.value = workout
    }

    fun updateBodyweight(weight: Double) {
        _bodyweight.value = weight
    }

    fun clearError() {
        _error.value = null
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // Refresh all data
    suspend fun refreshAll() {
        loadExercises()
        loadActiveWorkout()
        loadCompletedWorkouts()
        loadTemplates()
    }
}