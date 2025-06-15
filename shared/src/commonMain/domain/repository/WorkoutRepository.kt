package com.bodyforge.data.repository

import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SimpleWorkoutRepository(
    private val database: BodyForgeDatabase
) {

    private val queries = database.workoutDatabaseQueries

    suspend fun getAllExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        queries.selectAllExercises().executeAsList().map { entity ->
            Exercise(
                id = entity.id,
                name = entity.name,
                muscleGroups = Json.decodeFromString<List<String>>(entity.muscle_groups),
                instructions = entity.instructions,
                equipmentNeeded = entity.equipment_needed,
                isCustom = entity.is_custom == 1L,
                defaultRestTimeSeconds = entity.default_rest_time_seconds.toInt()
            )
        }
    }

    suspend fun saveWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
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

    suspend fun updateWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
        // Update workout
        queries.updateWorkout(
            name = workout.name,
            started_at = workout.startedAt.epochSeconds,
            finished_at = workout.finishedAt?.epochSeconds,
            notes = workout.notes,
            id = workout.id
        )

        // Delete and re-insert sets (simple approach)
        queries.deleteSetsForWorkout(workout.id)

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

    suspend fun getActiveWorkout(): Workout? = withContext(Dispatchers.IO) {
        val workoutEntity = queries.selectActiveWorkout().executeAsOneOrNull()
            ?: return@withContext null

        val setsForWorkout = queries.selectSetsForWorkout(workoutEntity.id).executeAsList()

        // Group sets by exercise
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
}