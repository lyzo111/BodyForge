package com.bodyforge.data.repository

import com.bodyforge.domain.repository.WorkoutRepository
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.data.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.json.Json

class WorkoutRepositoryImpl : WorkoutRepository {

    private val database = DatabaseFactory.create()
    private val queries = database.bodyForgeDatabaseQueries

    override suspend fun saveWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
        // Save workout
        queries.insertWorkout(
            id = workout.id,
            name = workout.name,
            started_at = workout.startedAt.epochSeconds,
            finished_at = workout.finishedAt?.epochSeconds,
            notes = workout.notes
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
                    notes = set.notes
                )
            }
        }

        workout
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
                    notes = setEntity.notes
                )
            }

            ExerciseInWorkout(
                exercise = exercise,
                sets = sets,
                orderInWorkout = setEntities.firstOrNull()?.order_in_workout?.toInt() ?: 0
            )
        }

        Workout(
            id = workoutEntity.id,
            name = workoutEntity.name,
            startedAt = Instant.fromEpochSeconds(workoutEntity.started_at),
            finishedAt = workoutEntity.finished_at?.let { Instant.fromEpochSeconds(it) },
            exercises = exercisesInWorkout,
            notes = workoutEntity.notes
        )
    }

    override suspend fun getAllWorkouts(): List<Workout> = withContext(Dispatchers.IO) {
        queries.selectAllWorkouts().executeAsList().mapNotNull { workoutEntity ->
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
                    notes = set.notes
                )
            }
        }

        workout
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