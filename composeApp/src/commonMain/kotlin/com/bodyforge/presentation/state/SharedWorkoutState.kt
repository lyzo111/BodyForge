package com.bodyforge.presentation.state

import com.bodyforge.data.SharedExercise
import com.bodyforge.data.SharedTemplate
import com.bodyforge.data.TemplateSharing
import com.bodyforge.data.repository.ExerciseRepositoryImpl
import com.bodyforge.data.repository.TrainingPhaseRepositoryImpl
import com.bodyforge.data.repository.BodyMetricRepositoryImpl
import com.bodyforge.data.repository.WorkoutRepositoryImpl
import com.bodyforge.data.repository.WorkoutTemplateRepositoryImpl
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.PhaseType
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.models.BodyMetric
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.SetStatus
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn

object SharedWorkoutState {
    // Repositories - Single instances for entire app
    val exerciseRepo = ExerciseRepositoryImpl()
    val workoutRepo = WorkoutRepositoryImpl()
    val templateRepo = WorkoutTemplateRepositoryImpl()
    val phaseRepo = TrainingPhaseRepositoryImpl()
    val bodyMetricRepo = BodyMetricRepositoryImpl()

    // Shared State Flows - Single source of truth
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _completedWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _completedWorkouts.asStateFlow()

    private val _templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val templates: StateFlow<List<WorkoutTemplate>> = _templates.asStateFlow()

    // templateId -> split name (e.g. "PPL"). Persisted in settings, not the database.
    private val _splitAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val splitAssignments: StateFlow<Map<String, String>> = _splitAssignments.asStateFlow()

    private val _phases = MutableStateFlow<List<TrainingPhase>>(emptyList())
    val phases: StateFlow<List<TrainingPhase>> = _phases.asStateFlow()

    private val _activePhase = MutableStateFlow<TrainingPhase?>(null)
    val activePhase: StateFlow<TrainingPhase?> = _activePhase.asStateFlow()

    // phaseId -> split name run during that phase (e.g. "PPL"). Mirror of AppSettings.phaseSplits.
    private val _phaseSplits = MutableStateFlow<Map<String, String>>(emptyMap())
    val phaseSplits: StateFlow<Map<String, String>> = _phaseSplits.asStateFlow()

    private val _bodyMetrics = MutableStateFlow<List<BodyMetric>>(emptyList())
    val bodyMetrics: StateFlow<List<BodyMetric>> = _bodyMetrics.asStateFlow()

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

    // Wall-clock end of the current rest (epoch millis, 0 = none). Driving the bar from this rather
    // than a per-second decrement keeps it smooth and correct even after the screen locks.
    private val _restEndsAtMillis = MutableStateFlow(0L)
    val restEndsAtMillis: StateFlow<Long> = _restEndsAtMillis.asStateFlow()

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
        val endMs = Clock.System.now().toEpochMilliseconds() + seconds * 1000L
        _restEndsAtMillis.value = endMs
        // Exact alarm so the end buzz fires even if the app is backgrounded or the phone is locked.
        // The alarm (not this in-app loop) does the vibrating, so there is never a double buzz.
        com.bodyforge.scheduleRestAlarm(endMs)
        com.bodyforge.showRestNotification(endMs)
        restJob = timerScope.launch {
            // Poll the wall clock instead of counting down, so a throttled coroutine (e.g. while the
            // phone is locked) can't make the timer drift — remaining is always (end - now).
            while (isActive) {
                val end = _restEndsAtMillis.value
                if (end == 0L) break
                val remMs = end - Clock.System.now().toEpochMilliseconds()
                if (remMs <= 0L) {
                    _restRemainingSeconds.value = 0
                    _restEndsAtMillis.value = 0L
                    _restJustEnded.value = true
                    break
                }
                _restRemainingSeconds.value = ((remMs + 999L) / 1000L).toInt()
                delay(250)
            }
        }
    }

    fun addRestTime(seconds: Int) {
        if (_restEndsAtMillis.value == 0L) return
        _restEndsAtMillis.value += seconds * 1000L
        _restTotalSeconds.value += seconds
        _restRemainingSeconds.value += seconds
        com.bodyforge.scheduleRestAlarm(_restEndsAtMillis.value)
        // Re-post so the notification's chronometer counts down from the new end time.
        com.bodyforge.showRestNotification(_restEndsAtMillis.value)
    }

    fun skipRest() {
        restJob?.cancel()
        com.bodyforge.cancelRestAlarm()
        com.bodyforge.cancelRestNotification()
        _restEndsAtMillis.value = 0L
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

    // Reactivates a finished workout from history (e.g. one that the orphan cleanup auto-finished).
    // Any other active workout is finished first so the single-active-workout rule holds; sets are
    // preserved as-is.
    suspend fun resumeWorkout(workout: Workout) {
        try {
            _activeWorkout.value?.let { active ->
                if (active.id != workout.id) {
                    workoutRepo.updateWorkout(active.copy(finishedAt = Clock.System.now()))
                }
            }
            // Exclude the break between finishing and resuming: shift startedAt forward by the pause
            // so only actively-trained time counts toward the workout's duration.
            val now = Clock.System.now()
            val pauseSeconds = workout.finishedAt?.let { (now.epochSeconds - it.epochSeconds).coerceAtLeast(0L) } ?: 0L
            val resumed = workout.copy(
                finishedAt = null,
                startedAt = kotlinx.datetime.Instant.fromEpochSeconds(workout.startedAt.epochSeconds + pauseSeconds)
            )
            workoutRepo.updateWorkout(resumed)
            loadActiveWorkout()
            loadCompletedWorkouts()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to resume workout: ${e.message}"
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

    fun loadSplitAssignments() {
        _splitAssignments.value = com.bodyforge.data.AppSettings.splitAssignments
        _phaseSplits.value = com.bodyforge.data.AppSettings.phaseSplits
    }

    // Assigns a template to a split (blank removes it). Persists to settings and updates state.
    // Split rotation: the app cycles through the chosen split's templates and suggests what to
    // train next. Starting the suggested template advances the pointer; anything else leaves it.
    private val _rotationSplit = MutableStateFlow("")
    val rotationSplit: StateFlow<String> = _rotationSplit.asStateFlow()

    private val _rotationOrder = MutableStateFlow<List<String>>(emptyList())
    val rotationOrder: StateFlow<List<String>> = _rotationOrder.asStateFlow()

    private val _rotationIndex = MutableStateFlow(0)
    val rotationIndex: StateFlow<Int> = _rotationIndex.asStateFlow()

    fun loadRotation() {
        _rotationSplit.value = com.bodyforge.data.AppSettings.rotationSplit
        _rotationOrder.value = com.bodyforge.data.AppSettings.rotationOrder
        _rotationIndex.value = com.bodyforge.data.AppSettings.rotationIndex
    }

    // Default cycle: one round per variation — every "A" template in creation order, then every
    // "B", and so on; the order stays user-editable afterwards.
    fun setRotationSplit(split: String) {
        com.bodyforge.data.AppSettings.rotationSplit = split
        _rotationSplit.value = split
        val ids = if (split.isBlank()) emptyList() else _templates.value
            .filter { _splitAssignments.value[it.id] == split }
            .sortedWith(compareBy({ it.variationLabel }, { it.createdAt }))
            .map { it.id }
        com.bodyforge.data.AppSettings.rotationOrder = ids
        _rotationOrder.value = ids
        com.bodyforge.data.AppSettings.rotationIndex = 0
        _rotationIndex.value = 0
    }

    fun setRotationOrder(order: List<String>) {
        com.bodyforge.data.AppSettings.rotationOrder = order
        _rotationOrder.value = order
    }

    fun advanceRotation() {
        val next = _rotationIndex.value + 1
        com.bodyforge.data.AppSettings.rotationIndex = next
        _rotationIndex.value = next
    }

    fun assignSplit(templateId: String, splitName: String) {
        val updated = com.bodyforge.data.AppSettings.splitAssignments.toMutableMap()
        val trimmed = splitName.trim()
        if (trimmed.isBlank()) updated.remove(templateId) else updated[templateId] = trimmed
        com.bodyforge.data.AppSettings.splitAssignments = updated
        _splitAssignments.value = updated
    }

    // Records which split (e.g. "PPL") was run during a phase. Blank removes the link.
    fun setPhaseSplit(phaseId: String, splitName: String) {
        val updated = com.bodyforge.data.AppSettings.phaseSplits.toMutableMap()
        val trimmed = splitName.trim()
        if (trimmed.isBlank()) updated.remove(phaseId) else updated[phaseId] = trimmed
        com.bodyforge.data.AppSettings.phaseSplits = updated
        _phaseSplits.value = updated
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
    // startDate defaults to today; a non-null endDate creates an already-completed (back-dated)
    // phase, which leaves the currently active phase untouched.
    suspend fun startPhase(
        name: String,
        phaseType: PhaseType,
        description: String = "",
        split: String = "",
        goals: List<String> = emptyList(),
        startDate: kotlinx.datetime.LocalDate? = null,
        endDate: kotlinx.datetime.LocalDate? = null
    ): TrainingPhase {
        val today = today()
        if (endDate == null) phaseRepo.deactivateActivePhases(today)
        val phase = TrainingPhase(
            id = "phase_${Clock.System.now().epochSeconds}",
            name = name,
            phaseType = phaseType,
            startDate = startDate ?: today,
            endDate = endDate,
            goals = goals,
            description = description,
            isActive = endDate == null
        )
        phaseRepo.savePhase(phase)
        setPhaseSplit(phase.id, split)
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

    suspend fun loadBodyMetrics() {
        try {
            _bodyMetrics.value = bodyMetricRepo.getAllMetrics()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load body metrics: ${e.message}"
        }
    }

    suspend fun addBodyMetric(metric: BodyMetric) {
        bodyMetricRepo.saveMetric(metric)
        loadBodyMetrics()
    }

    suspend fun deleteBodyMetric(id: String) {
        bodyMetricRepo.deleteMetric(id)
        loadBodyMetrics()
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

    // Pre-fills each exercise's sets from history, so a template-started workout opens with last
    // session's numbers instead of blanks. Two settings choose the source independently for the set
    // count and for the reps/weight: the last session of the SAME template variation (matched by
    // templateId) or the most recent session in ANY variation. Same-variation falls back to any
    // when there's no same-variation history yet (e.g. first time running this template).
    private suspend fun prefillFromHistory(workout: Workout): Workout {
        val history = workoutRepo.getCompletedWorkouts()
        if (history.isEmpty()) return workout
        val templateId = workout.templateId
        val setsSame = com.bodyforge.data.AppSettings.prefillSetsSameVariation
        val weightsSame = com.bodyforge.data.AppSettings.prefillWeightsSameVariation

        val updatedExercises = workout.exercises.map { exerciseInWorkout ->
            val exId = exerciseInWorkout.exercise.id
            fun lastPerformed(sameVariationOnly: Boolean): ExerciseInWorkout? =
                history.firstNotNullOfOrNull { completed ->
                    if (sameVariationOnly && (templateId == null || completed.templateId != templateId)) return@firstNotNullOfOrNull null
                    completed.exercises.firstOrNull { it.exercise.id == exId }
                        ?.takeIf { previous -> previous.sets.any { it.reps > 0 } }
                }
            val countSource = (if (setsSame) lastPerformed(true) else null) ?: lastPerformed(false)
            val valueSource = (if (weightsSame) lastPerformed(true) else null) ?: lastPerformed(false)
            if (countSource == null && valueSource == null) return@map exerciseInWorkout

            val valueSets = valueSource?.sets?.filter { it.reps > 0 }.orEmpty()
            val count = countSource?.sets?.count { it.reps > 0 }
                ?: valueSets.size.takeIf { it > 0 }
                ?: return@map exerciseInWorkout
            val prefilledSets = (0 until count).map { index ->
                // Fewer reps/weight sources than sets: repeat the last one for the extra sets.
                val source = valueSets.getOrNull(index) ?: valueSets.lastOrNull()
                WorkoutSet.createEmpty(
                    exerciseId = exId,
                    setNumber = index + 1,
                    defaultRestTime = exerciseInWorkout.exercise.defaultRestTimeSeconds,
                    workoutId = workout.id
                ).copy(reps = source?.reps ?: 0, weightKg = source?.weightKg ?: 0.0)
            }
            // Carry over the exercise-level note and per-exercise variation label from whichever
            // source we used, but never set notes — those are tied to that day's performance.
            val metaSource = valueSource ?: countSource
            exerciseInWorkout.copy(
                sets = prefilledSets,
                notes = metaSource?.notes ?: "",
                variation = metaSource?.variation ?: ""
            )
        }
        return workout.copy(exercises = updatedExercises)
    }

    // A template target defines only the SET COUNT here — the reps and weight per set stay the
    // history-prefilled values so you always see (and aim to beat) what you actually lifted last
    // time. The target rep range is shown separately under the exercise as a goal; its low end is
    // only used to seed reps when there is no history for that set yet.
    private fun applyTemplateTargets(workout: Workout, template: WorkoutTemplate): Workout {
        if (template.targets.isEmpty()) return workout
        val updated = workout.exercises.map { eiw ->
            val target = template.targets[eiw.exercise.id] ?: return@map eiw
            val count = target.sets.coerceIn(1, 20)
            val historySets = eiw.sets
            val sets = (1..count).map { n ->
                val hist = historySets.getOrNull(n - 1) ?: historySets.lastOrNull()
                val reps = hist?.reps?.takeIf { it > 0 } ?: target.minReps.coerceAtLeast(0)
                val weight = hist?.weightKg ?: 0.0
                WorkoutSet.createEmpty(
                    exerciseId = eiw.exercise.id,
                    setNumber = n,
                    defaultRestTime = eiw.exercise.defaultRestTimeSeconds,
                    workoutId = workout.id
                ).copy(reps = reps, weightKg = weight)
            }
            eiw.copy(sets = sets)
        }
        return workout.copy(exercises = updated)
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
                val workout = applyTemplateTargets(
                    prefillFromHistory(
                        Workout.create(
                            template.name.ifEmpty { "Template Workout" },
                            exercises,
                            templateId = template.id
                        )
                    ),
                    template
                )
                val saved = workoutRepo.saveWorkout(workout)
                updateActiveWorkout(saved)
                // Starting the currently suggested template advances the split rotation; starting
                // anything else leaves the pointer alone so the planned day stays up next.
                val order = _rotationOrder.value.filter { id -> _templates.value.any { it.id == id } }
                if (_rotationSplit.value.isNotBlank() && order.isNotEmpty() &&
                    order[_rotationIndex.value.mod(order.size)] == template.id
                ) {
                    advanceRotation()
                }
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

    // Imports completed workouts from a "date,workout,exercise,reps,weight" CSV (one row per set),
    // back-dated into history. Returns (workoutsImported, rowsSkipped).
    // Normalizes an exercise name for fuzzy matching: lowercase, letters and digits only, so spacing
    // and punctuation differences ("Bench Press" vs "bench-press") collapse to the same key.
    private fun normalizeExerciseName(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

    suspend fun importWorkoutsFromCsv(csv: String): Pair<Int, Int> {
        val parsed = com.bodyforge.data.parseWorkoutCsv(csv)
        if (parsed.workouts.isEmpty()) {
            loadCompletedWorkouts()
            return 0 to parsed.skippedRows
        }
        val allExercises = exerciseRepo.getAllExercises()
        val byName = allExercises.associateBy { it.name.trim().lowercase() }.toMutableMap()
        // Fuzzy index over exercises that already carry muscle groups, keyed by a space/punctuation-
        // insensitive form, so an imported "bench press" inherits the tags of a stock "Bench Press".
        val byNorm = allExercises.filter { it.muscleGroups.isNotEmpty() }
            .associateBy { normalizeExerciseName(it.name) }
        val tz = TimeZone.currentSystemDefault()
        val base = Clock.System.now().epochSeconds
        var seq = 0
        var imported = 0
        try {
            for (cw in parsed.workouts) {
                val start = cw.date.atTime(12, 0).toInstant(tz)
                val finish = Instant.fromEpochSeconds(start.epochSeconds + 1800)
                val exercisesInWorkout = cw.exercises.mapIndexed { index, ce ->
                    val key = ce.name.trim().lowercase()
                    val exercise = byName[key] ?: byNorm[normalizeExerciseName(ce.name)] ?: run {
                        val created = createCustomExercise(ce.name.trim(), emptyList(), "", false)
                        byName[key] = created
                        created
                    }
                    val sets = ce.sets.map { s ->
                        WorkoutSet(
                            id = "set_imp_${base}_${seq++}",
                            reps = s.reps,
                            weightKg = s.weight,
                            restTimeSeconds = exercise.defaultRestTimeSeconds,
                            completed = true,
                            completedAt = finish,
                            notes = s.notes,
                            status = SetStatus.COMPLETED
                        )
                    }
                    ExerciseInWorkout(exercise = exercise, sets = sets, orderInWorkout = index)
                }
                val workout = Workout(
                    id = "workout_imp_${base}_${seq++}",
                    name = cw.name,
                    startedAt = start,
                    finishedAt = finish,
                    exercises = exercisesInWorkout,
                    notes = ""
                )
                workoutRepo.importWorkout(workout)
                imported++
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "CSV import failed: ${e.message}"
        }
        loadCompletedWorkouts()
        return imported to parsed.skippedRows
    }

    // A same-named exercise pair the user hasn't decided about yet: `keep` is the better-tagged
    // one (stock first, then the one with muscle groups), `duplicate` the candidate to fold in.
    data class DuplicateExercises(val keep: Exercise, val duplicate: Exercise)

    private val _duplicatePrompt = MutableStateFlow<DuplicateExercises?>(null)
    val duplicatePrompt: StateFlow<DuplicateExercises?> = _duplicatePrompt.asStateFlow()

    private fun duplicateKey(a: String, b: String) = listOf(a, b).sorted().joinToString("|")

    // Surfaces one not-yet-asked duplicate pair (same name, case-insensitive) at a time.
    fun checkForDuplicateExercises() {
        val asked = com.bodyforge.data.AppSettings.duplicateMergeAsked
        val groups = _exercises.value.groupBy { it.name.trim().lowercase() }.values.filter { it.size > 1 }
        for (group in groups) {
            val sorted = group.sortedWith(
                compareBy({ it.isCustom }, { it.muscleGroups.isEmpty() }, { it.id })
            )
            val keep = sorted.first()
            for (dup in sorted.drop(1)) {
                if (duplicateKey(keep.id, dup.id) !in asked) {
                    _duplicatePrompt.value = DuplicateExercises(keep, dup)
                    return
                }
            }
        }
        _duplicatePrompt.value = null
    }

    fun dismissDuplicatePrompt() {
        val pair = _duplicatePrompt.value ?: return
        com.bodyforge.data.AppSettings.duplicateMergeAsked =
            com.bodyforge.data.AppSettings.duplicateMergeAsked + duplicateKey(pair.keep.id, pair.duplicate.id)
        _duplicatePrompt.value = null
        checkForDuplicateExercises()
    }

    // Folds the duplicate into the kept exercise everywhere — logged sets, templates (ids and
    // targets) — then soft-deletes the duplicate and reloads everything that referenced it.
    suspend fun mergeDuplicateExercises() {
        val pair = _duplicatePrompt.value ?: return
        try {
            exerciseRepo.mergeExercises(pair.keep.id, pair.duplicate.id)
            _templates.value
                .filter { pair.duplicate.id in it.exerciseIds || pair.duplicate.id in it.targets.keys }
                .forEach { template ->
                    templateRepo.updateTemplate(
                        template.copy(
                            exerciseIds = template.exerciseIds.map { if (it == pair.duplicate.id) pair.keep.id else it }.distinct(),
                            targets = template.targets.mapKeys { (k, _) -> if (k == pair.duplicate.id) pair.keep.id else k }
                        )
                    )
                }
            com.bodyforge.data.AppSettings.duplicateMergeAsked =
                com.bodyforge.data.AppSettings.duplicateMergeAsked + duplicateKey(pair.keep.id, pair.duplicate.id)
            _duplicatePrompt.value = null
            loadExercises()
            loadActiveWorkout()
            loadCompletedWorkouts()
            loadTemplates()
            checkForDuplicateExercises()
        } catch (e: Exception) {
            _error.value = "Merge failed: ${e.message}"
        }
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
        loadSplitAssignments()
        loadRotation()
        loadBodyMetrics()
        checkForDuplicateExercises()
    }
}