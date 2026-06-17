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
import kotlinx.datetime.Clock

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

    // Persists a user-created exercise, refreshes the shared list and returns the saved instance
    // so callers (e.g. the template dialog) can select it without reloading the screen.
    suspend fun createCustomExercise(
        name: String,
        muscleGroups: List<String>,
        equipment: String,
        isBodyweight: Boolean
    ): Exercise {
        val created = exerciseRepo.saveCustomExercise(
            Exercise(
                id = generateCustomExerciseId(name),
                name = name,
                muscleGroups = muscleGroups,
                equipmentNeeded = equipment,
                isCustom = true,
                isBodyweight = isBodyweight
            )
        )
        loadExercises()
        return created
    }

    // Builds a unique, readable id, e.g. "custom_cable_lateral_raises_1718611200000".
    private fun generateCustomExerciseId(name: String): String {
        val slug = buildString {
            var pendingSeparator = false
            for (char in name.lowercase()) {
                if (char.isLetterOrDigit()) {
                    append(char)
                    pendingSeparator = false
                } else if (!pendingSeparator) {
                    append('_')
                    pendingSeparator = true
                }
            }
        }.trim('_').ifBlank { "exercise" }
        return "custom_${slug}_${Clock.System.now().toEpochMilliseconds()}"
    }

    // Starts a workout from a template: resolves its exercises, records the template origin
    // (so workouts can be compared per template / variation) and makes it the active workout.
    // Returns the started workout, or null if the template has no resolvable exercises.
    suspend fun startWorkoutFromTemplate(template: WorkoutTemplate): Workout? {
        setLoading(true)
        return try {
            val exercises = template.exerciseIds.mapNotNull { exerciseRepo.getExerciseById(it) }
            if (exercises.isEmpty()) {
                setError("Template contains no valid exercises")
                null
            } else {
                val workout = Workout.create(
                    template.name.ifEmpty { "Template Workout" },
                    exercises,
                    templateId = template.id
                )
                val saved = workoutRepo.saveWorkout(workout)
                updateActiveWorkout(saved)
                if (exercises.size != template.exerciseIds.size) {
                    setError("Some exercises from this template are no longer available")
                } else {
                    clearError()
                }
                saved
            }
        } catch (e: Exception) {
            setError("Failed to start workout from template: ${e.message ?: "Unknown error"}")
            null
        } finally {
            setLoading(false)
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