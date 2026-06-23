package com.bodyforge.data.repository

import com.bodyforge.domain.repository.WorkoutRepository
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.SetStatus
import com.bodyforge.data.DatabaseFactory
import com.bodyforge.database.BodyForgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class WorkoutRepositoryImpl(
    database: BodyForgeDatabase = DatabaseFactory.create()
) : WorkoutRepository {

    private val queries = database.bodyForgeDatabaseQueries

    // Initialize: Clean up any orphaned "active" workouts from previous sessions
    init {
        cleanupOrphanedActiveWorkouts()
    }

    private fun cleanupOrphanedActiveWorkouts() {
        try {
            // Only auto-finish workouts left open for over a day, so a recent active session survives
            // an app restart and can simply be continued. Older orphans (forgotten sessions) are
            // closed out, and they stay resumable from history.
            val now = Clock.System.now().epochSeconds
            val staleCutoff = now - 24 * 60 * 60
            queries.updateOrphanedWorkouts(now, staleCutoff)
        } catch (e: Exception) {
            // Log error but don't crash the app
            println("Warning: Could not cleanup orphaned workouts: ${e.message}")
        }
    }

    override suspend fun saveWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
        // First, finish any existing active workouts (only one active at a time)
        finishAllActiveWorkouts()

        // Save workout
        queries.insertWorkout(
            id = workout.id,
            name = workout.name,
            started_at = workout.startedAt.epochSeconds,
            finished_at = workout.finishedAt?.epochSeconds,
            notes = workout.notes,
            template_id = workout.templateId
        )

        // Save exercises and sets
        workout.exercises.forEachIndexed { exerciseIndex, exerciseInWorkout ->
            exerciseInWorkout.sets.forEachIndexed { setIndex, set ->
                queries.insertWorkoutSet(
                    id = set.id,
                    workout_id = workout.id,
                    exercise_id = exerciseInWorkout.exercise.id,
                    order_in_workout = exerciseIndex.toLong(),
                    set_number = (setIndex + 1).toLong(),
                    reps = set.reps.toLong(),
                    weight_kg = set.weightKg,
                    rest_time_seconds = set.restTimeSeconds.toLong(),
                    completed = if (set.completed) 1L else 0L,
                    completed_at = set.completedAt?.epochSeconds,
                    notes = if (setIndex == 0) exerciseInWorkout.notes else set.notes,
                    status = set.status.name,
                    original_exercise_id = set.originalExerciseId,
                    metrics = com.bodyforge.data.mappers.WorkoutMapper.serializeMetrics(set.metrics)
                )
            }
        }

        workout
    }

    // Saves an already-completed workout (e.g. a CSV import) without touching active workouts,
    // so a back-dated import never finishes an in-progress session.
    override suspend fun importWorkout(workout: Workout): Unit = withContext(Dispatchers.IO) {
        queries.insertWorkout(
            id = workout.id,
            name = workout.name,
            started_at = workout.startedAt.epochSeconds,
            finished_at = workout.finishedAt?.epochSeconds,
            notes = workout.notes,
            template_id = workout.templateId
        )
        workout.exercises.forEachIndexed { exerciseIndex, exerciseInWorkout ->
            exerciseInWorkout.sets.forEachIndexed { setIndex, set ->
                queries.insertWorkoutSet(
                    id = set.id,
                    workout_id = workout.id,
                    exercise_id = exerciseInWorkout.exercise.id,
                    order_in_workout = exerciseIndex.toLong(),
                    set_number = (setIndex + 1).toLong(),
                    reps = set.reps.toLong(),
                    weight_kg = set.weightKg,
                    rest_time_seconds = set.restTimeSeconds.toLong(),
                    completed = if (set.completed) 1L else 0L,
                    completed_at = set.completedAt?.epochSeconds,
                    notes = set.notes,
                    status = set.status.name,
                    original_exercise_id = set.originalExerciseId,
                    metrics = com.bodyforge.data.mappers.WorkoutMapper.serializeMetrics(set.metrics)
                )
            }
        }
    }

    private fun finishAllActiveWorkouts() {
        try {
            queries.finishAllActiveWorkouts(Clock.System.now().epochSeconds)
        } catch (e: Exception) {
            println("Warning: Could not finish active workouts: ${e.message}")
        }
    }

    override suspend fun getWorkout(id: String): Workout? = withContext(Dispatchers.IO) {
        val workoutEntity = queries.selectWorkoutById(id).executeAsOneOrNull()
            ?: return@withContext null

        val setsForWorkout = queries.selectSetsForWorkout(id).executeAsList()

        // Group sets by exercise and order
        val exerciseGroups = setsForWorkout
            .groupBy { it.exercise_id }
            .toList()
            .sortedBy { (_, sets) -> sets.firstOrNull()?.order_in_workout ?: 0 }

        val exercisesInWorkout = exerciseGroups.map { (exerciseId, setEntities) ->
            val exerciseEntity = queries.selectExerciseById(exerciseId).executeAsOne()
            val exercise = Exercise(
                id = exerciseEntity.id,
                name = exerciseEntity.name,
                muscleGroups = Json.decodeFromString<List<String>>(exerciseEntity.muscle_groups),
                instructions = exerciseEntity.instructions,
                equipmentNeeded = exerciseEntity.equipment_needed,
                isCustom = exerciseEntity.is_custom == 1L,
                isBodyweight = exerciseEntity.is_bodyweight == 1L,  // FIXED: Added here too
                defaultRestTimeSeconds = exerciseEntity.default_rest_time_seconds.toInt()
            )

            val sets = setEntities.sortedBy { it.set_number }.map { setEntity ->
                WorkoutSet(
                    id = setEntity.id,
                    reps = setEntity.reps.toInt(),
                    weightKg = setEntity.weight_kg,
                    restTimeSeconds = setEntity.rest_time_seconds.toInt(),
                    completed = setEntity.completed == 1L,
                    completedAt = setEntity.completed_at?.let { Instant.fromEpochSeconds(it) },
                    notes = setEntity.notes,
                    status = SetStatus.fromStorage(setEntity.status),
                    originalExerciseId = setEntity.original_exercise_id,
                    metrics = com.bodyforge.data.mappers.WorkoutMapper.parseMetrics(setEntity.metrics)
                )
            }

            ExerciseInWorkout(
                exercise = exercise,
                sets = sets,
                orderInWorkout = setEntities.firstOrNull()?.order_in_workout?.toInt() ?: 0,
                notes = sets.firstOrNull()?.notes ?: ""
            )
        }

        Workout(
            id = workoutEntity.id,
            name = workoutEntity.name,
            startedAt = Instant.fromEpochSeconds(workoutEntity.started_at),
            finishedAt = workoutEntity.finished_at?.let { Instant.fromEpochSeconds(it) },
            exercises = exercisesInWorkout,
            notes = workoutEntity.notes,
            templateId = workoutEntity.template_id
        )
    }

    override suspend fun getAllWorkouts(): List<Workout> = withContext(Dispatchers.IO) {
        queries.selectAllWorkouts().executeAsList().mapNotNull { workoutEntity ->
            getWorkout(workoutEntity.id)
        }
    }

    // NEW: Get only completed workouts for history
    override suspend fun getCompletedWorkouts(): List<Workout> = withContext(Dispatchers.IO) {
        queries.selectCompletedWorkouts().executeAsList().mapNotNull { workoutEntity ->
            getWorkout(workoutEntity.id)
        }
    }

    override suspend fun getWorkoutsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Workout> = withContext(Dispatchers.IO) {
        val startTimestamp = startDate.atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds
        val endTimestamp = endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds

        queries.selectWorkoutsByDateRange(startTimestamp, endTimestamp)
            .executeAsList()
            .mapNotNull { getWorkout(it.id) }
    }

    override suspend fun getActiveWorkout(): Workout? = withContext(Dispatchers.IO) {
        val activeWorkoutEntity = queries.selectActiveWorkout().executeAsOneOrNull()
            ?: return@withContext null

        getWorkout(activeWorkoutEntity.id)
    }

    override suspend fun updateWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
        // Update workout
        queries.updateWorkout(
            name = workout.name,
            started_at = workout.startedAt.epochSeconds,
            finished_at = workout.finishedAt?.epochSeconds,
            notes = workout.notes,
            template_id = workout.templateId,
            id = workout.id
        )

        // Delete old sets and insert new ones (simple approach)
        queries.deleteSetsForWorkout(workout.id)

        // Re-insert all sets
        workout.exercises.forEachIndexed { exerciseIndex, exerciseInWorkout ->
            exerciseInWorkout.sets.forEachIndexed { setIndex, set ->
                queries.insertWorkoutSet(
                    id = set.id,
                    workout_id = workout.id,
                    exercise_id = exerciseInWorkout.exercise.id,
                    order_in_workout = exerciseIndex.toLong(),
                    set_number = (setIndex + 1).toLong(),
                    reps = set.reps.toLong(),
                    weight_kg = set.weightKg,
                    rest_time_seconds = set.restTimeSeconds.toLong(),
                    completed = if (set.completed) 1L else 0L,
                    completed_at = set.completedAt?.epochSeconds,
                    notes = if (setIndex == 0) exerciseInWorkout.notes else set.notes,
                    status = set.status.name,
                    original_exercise_id = set.originalExerciseId,
                    metrics = com.bodyforge.data.mappers.WorkoutMapper.serializeMetrics(set.metrics)
                )
            }
        }

        workout
    }

    override suspend fun getWorkoutsByTemplate(templateId: String): List<Workout> = withContext(Dispatchers.IO) {
        queries.selectWorkoutsByTemplate(templateId).executeAsList().mapNotNull { workoutEntity ->
            getWorkout(workoutEntity.id)
        }
    }

    override suspend fun deleteWorkout(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.deleteSetsForWorkout(id)
            queries.deleteWorkout(id)
            true
        } catch (e: Exception) {
            false
        }
    }
}