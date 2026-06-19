package com.bodyforge.presentation.state

import com.bodyforge.data.SharedExercise
import com.bodyforge.data.SharedTemplate
import com.bodyforge.data.TemplateSharing
import com.bodyforge.data.repository.ExerciseRepositoryImpl
import com.bodyforge.data.repository.TrainingPhaseRepositoryImpl
import com.bodyforge.data.repository.WorkoutRepositoryImpl
import com.bodyforge.data.repository.WorkoutTemplateRepositoryImpl
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.PhaseType
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.WorkoutTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

object SharedWorkoutState {
    // Repositories - Single instances for entire app
    val exerciseRepo = ExerciseRepositoryImpl()
    val workoutRepo = WorkoutRepositoryImpl()
    val templateRepo = WorkoutTemplateRepositoryImpl()
    val phaseRepo = TrainingPhaseRepositoryImpl()

    // Shared State Flows - Single source of truth
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _completedWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _completedWorkouts.asStateFlow()

    private val _templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val templates: StateFlow<List<WorkoutTemplate>> = _templates.asStateFlow()

    private val _phases = MutableStateFlow<List<TrainingPhase>>(emptyList())
    val phases: StateFlow<List<TrainingPhase>> = _phases.asStateFlow()

    private val _activePhase = MutableStateFlow<TrainingPhase?>(null)
    val activePhase: StateFlow<TrainingPhase?> = _activePhase.asStateFlow()

    private val _bodyweight = MutableStateFlow(75.0)
    val bodyweight: StateFlow<Double> = _bodyweight.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Rest timer lives here (not in a screen) so it keeps running and stays visible when the
    // user leaves the Workout tab and comes back.
    private val timerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restJob: Job? = null

    private val _restTotalSeconds = MutableStateFlow(0)
    val restTotalSeconds: StateFlow<Int> = _restTotalSeconds.asStateFlow()

    private val _restRemainingSeconds = MutableStateFlow(0)
    val restRemainingSeconds: StateFlow<Int> = _restRemainingSeconds.asStateFlow()

    // Goes true the moment a rest timer counts down to zero on its own. Drives a closeable
    // "break is over" banner that shows even when the user is on another tab.
    private val _restJustEnded = MutableStateFlow(false)
    val restJustEnded: StateFlow<Boolean> = _restJustEnded.asStateFlow()

    fun startRest(seconds: Int) {
        if (seconds <= 0) return
        restJob?.cancel()
        _restJustEnded.value = false
        _restTotalSeconds.value = seconds
        _restRemainingSeconds.value = seconds
        restJob = timerScope.launch {
            while (isActive && _restRemainingSeconds.value > 0) {
                delay(1000)
                _restRemainingSeconds.value = (_restRemainingSeconds.value - 1).coerceAtLeast(0)
            }
            // Reached zero on its own (not skipped/cancelled) -> buzz and raise the banner.
            if (isActive && _restRemainingSeconds.value == 0) {
                _restJustEnded.value = true
                if (com.bodyforge.data.AppSettings.vibrateOnTimerEnd) {
                    com.bodyforge.data.vibrateDevice()
                }
            }
        }
    }

    fun addRestTime(seconds: Int) {
        if (_restRemainingSeconds.value <= 0) return
        _restTotalSeconds.value += seconds
        _restRemainingSeconds.value += seconds
    }

    fun skipRest() {
        restJob?.cancel()
        _restRemainingSeconds.value = 0
        _restJustEnded.value = false
    }

    fun dismissRestEndedNotice() {
        _restJustEnded.value = false
    }

    // Update functions
    suspend fun loadExercises() {
        _isLoading.value = true
        try {
            val exerciseList = exerciseRepo.getAllExercises()
            _exercises.value = exerciseList
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load active workout: ${e.message}"
        }
    }

    suspend fun loadCompletedWorkouts() {
        try {
            val workouts = workoutRepo.getCompletedWorkouts()
            _completedWorkouts.value = workouts
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load workout history: ${e.message}"
        }
    }

    suspend fun loadTemplates() {
        try {
            val templateList = templateRepo.getAllTemplates()
            _templates.value = templateList
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load templates: ${e.message}"
        }
    }

    suspend fun loadPhases() {
        try {
            _phases.value = phaseRepo.getAllPhases()
            _activePhase.value = phaseRepo.getActivePhase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load training phases: ${e.message}"
        }
    }

    private fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Starts a new phase: ends the currently active one (today) so phases form a continuous
    // timeline, then inserts the new active phase starting today.
    suspend fun startPhase(name: String, phaseType: PhaseType, description: String = "", goals: List<String> = emptyList()): TrainingPhase {
        val today = today()
        phaseRepo.deactivateActivePhases(today)
        val phase = TrainingPhase(
            id = "phase_${Clock.System.now().epochSeconds}",
            name = name,
            phaseType = phaseType,
            startDate = today,
            goals = goals,
            description = description,
            isActive = true
        )
        phaseRepo.savePhase(phase)
        loadPhases()
        return phase
    }

    suspend fun updatePhase(phase: TrainingPhase) {
        phaseRepo.updatePhase(phase)
        loadPhases()
    }

    // Ends a phase as of today and clears its active flag.
    suspend fun completePhase(id: String) {
        val phase = phaseRepo.getPhaseById(id) ?: return
        phaseRepo.updatePhase(phase.copy(endDate = today(), isActive = false))
        loadPhases()
    }

    // Re-opens a completed phase (e.g. after accidentally completing it): clears its end date,
    // makes it active and ends any other currently active phase.
    suspend fun resumePhase(id: String) {
        val phase = phaseRepo.getPhaseById(id) ?: return
        phaseRepo.deactivateActivePhases(today())
        phaseRepo.updatePhase(phase.copy(endDate = null, isActive = true))
        loadPhases()
    }

    suspend fun deletePhase(id: String) {
        phaseRepo.deletePhase(id)
        loadPhases()
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

    suspend fun updateCustomExercise(exercise: Exercise) {
        exerciseRepo.updateCustomExercise(exercise)
        loadExercises()
    }

    suspend fun deleteCustomExercise(id: String) {
        exerciseRepo.deleteCustomExercise(id)
        loadExercises()
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

    // Pre-fills each exercise's sets with the reps/weight logged the last time that exercise was
    // performed, so a template-started workout opens with last session's numbers instead of blanks.
    private suspend fun prefillFromHistory(workout: Workout): Workout {
        val history = workoutRepo.getCompletedWorkouts()
        if (history.isEmpty()) return workout
        val updatedExercises = workout.exercises.map { exerciseInWorkout ->
            val lastPerformed = history.firstNotNullOfOrNull { completed ->
                completed.exercises.firstOrNull { it.exercise.id == exerciseInWorkout.exercise.id }
                    ?.takeIf { previous -> previous.sets.any { it.reps > 0 } }
            } ?: return@map exerciseInWorkout
            val previousSets = lastPerformed.sets.filter { it.reps > 0 }
            val prefilledSets = previousSets.mapIndexed { index, source ->
                WorkoutSet.createEmpty(
                    exerciseId = exerciseInWorkout.exercise.id,
                    setNumber = index + 1,
                    defaultRestTime = exerciseInWorkout.exercise.defaultRestTimeSeconds,
                    workoutId = workout.id
                ).copy(reps = source.reps, weightKg = source.weightKg)
            }
            exerciseInWorkout.copy(sets = prefilledSets)
        }
        return workout.copy(exercises = updatedExercises)
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
                val workout = prefillFromHistory(
                    Workout.create(
                        template.name.ifEmpty { "Template Workout" },
                        exercises,
                        templateId = template.id
                    )
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

    // Builds a portable representation of a template and opens the system share sheet.
    fun shareTemplate(template: WorkoutTemplate) {
        val resolved = template.exerciseIds.mapNotNull { id -> _exercises.value.firstOrNull { it.id == id } }
        val shared = SharedTemplate(
            name = template.name,
            description = template.description,
            exercises = resolved.map { SharedExercise(it.name, it.muscleGroups, it.equipmentNeeded, it.isBodyweight) }
        )
        TemplateSharing.share(TemplateSharing.encode(shared), "BodyForge: ${template.name}")
    }

    // Rebuilds a shared template locally: reuses matching exercises (by id, then name) and creates
    // custom ones for anything the user doesn't have yet, then saves the template.
    suspend fun importSharedTemplate(shared: SharedTemplate): Boolean {
        return try {
            val existing = exerciseRepo.getAllExercises()
            val resolved = shared.exercises.map { se ->
                existing.firstOrNull { it.name.equals(se.name, ignoreCase = true) }
                    ?: exerciseRepo.saveCustomExercise(
                        Exercise(
                            id = generateCustomExerciseId(se.name),
                            name = se.name,
                            muscleGroups = se.muscleGroups,
                            equipmentNeeded = se.equipment,
                            isCustom = true,
                            isBodyweight = se.isBodyweight
                        )
                    )
            }
            val template = WorkoutTemplate(
                id = "template_${Clock.System.now().epochSeconds}",
                name = shared.name,
                exerciseIds = resolved.map { it.id },
                createdAt = Clock.System.now(),
                description = shared.description
            )
            templateRepo.saveTemplate(template)
            loadExercises()
            loadTemplates()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setError("Failed to import template: ${e.message}")
            false
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
        try {
            exerciseRepo.ensureStockExercises()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Non-fatal: keep startup going even if the stock-exercise sync fails.
        }
        loadExercises()
        loadActiveWorkout()
        loadCompletedWorkouts()
        loadTemplates()
        loadPhases()
    }
}