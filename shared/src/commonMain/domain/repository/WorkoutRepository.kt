package com.bodyforge.data.repository

import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.repository.WorkoutRepository
import com.bodyforge.data.mappers.WorkoutMapper
import com.bodyforge.data.mappers.WorkoutMapper.toDomain
import com.bodyforge.data.mappers.WorkoutMapper.toEntity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class WorkoutRepositoryImpl(
    private val database: BodyForgeDatabase
) : WorkoutRepository {

    private val workoutQueries = database.workoutDatabaseQueries

    override suspend fun saveWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
        // Save workout
        val workoutEntity = workout.toEntity()
        workoutQueries.insertWorkout(
            id = workoutEntity.id,
            name = workoutEntity.name,
            started_at = workoutEntity.started_at,
            finished_at = workoutEntity.finished_at,
            notes = workoutEntity.notes
        )

        // Save exercises and sets
        workout.exercises.forEachIndexed { exerciseIndex, exerciseInWorkout ->
            exerciseInWorkout.sets.forEachIndexed { setIndex, set ->
                val setEntity = set.toEntity(
                    workoutId = workout.id,
                    exerciseId = exerciseInWorkout.exercise.id,
                    orderInWorkout = exerciseIndex,
                    setNumber = setIndex + 1
                )

                workoutQueries.insertWorkoutSet(
                    id = setEntity.id,
                    workout_id = setEntity.workout_id,
                    exercise_id = setEntity.exercise_id,
                    order_in_workout = setEntity.order_in_workout,
                    set_number = setEntity.set_number,
                    reps = setEntity.reps,
                    weight_kg = setEntity.weight_kg,
                    rest_time_seconds = setEntity.rest_time_seconds,
                    completed = setEntity.completed,
                    completed_at = setEntity.completed_at,
                    notes = setEntity.notes
                )
            }
        }

        workout
    }

    override suspend fun getWorkout(id: String): Workout? = withContext(Dispatchers.IO) {
        val workoutEntity = workoutQueries.selectWorkoutById(id).executeAsOneOrNull()
            ?: return@withContext null

        val setsForWorkout = workoutQueries.selectSetsForWorkout(id).executeAsList()

        // Group sets by exercise and order
        val exerciseGroups = setsForWorkout
            .groupBy { it.exercise_id }
            .toList()
            .sortedBy { (_, sets) -> sets.firstOrNull()?.order_in_workout ?: 0 }

        val exercisesWithSets = exerciseGroups.map { (exerciseId, setEntities) ->
            val exerciseEntity = workoutQueries.selectExerciseById(exerciseId).executeAsOne()
            exerciseEntity to setEntities.sortedBy { it.set_number }
        }

        WorkoutMapper.combineWorkoutWithExercises(workoutEntity, exercisesWithSets)
    }

    override suspend fun getAllWorkouts(): List<Workout> = withContext(Dispatchers.IO) {
        workoutQueries.selectAllWorkouts().executeAsList().mapNotNull { workoutEntity ->
            getWorkout(workoutEntity.id)
        }
    }

    override suspend fun getWorkoutsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Workout> = withContext(Dispatchers.IO) {
        val startTimestamp = startDate.atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds
        val endTimestamp = endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds

        workoutQueries.selectWorkoutsByDateRange(startTimestamp, endTimestamp)
            .executeAsList()
            .mapNotNull { getWorkout(it.id) }
    }

    override suspend fun getActiveWorkout(): Workout? = withContext(Dispatchers.IO) {
        val activeWorkoutEntity = workoutQueries.selectActiveWorkout().executeAsOneOrNull()
            ?: return@withContext null

        getWorkout(activeWorkoutEntity.id)
    }

    override suspend fun updateWorkout(workout: Workout): Workout = withContext(Dispatchers.IO) {
        val workoutEntity = workout.toEntity()

        // Update workout
        workoutQueries.updateWorkout(
            name = workoutEntity.name,
            started_at = workoutEntity.started_at,
            finished_at = workoutEntity.finished_at,
            notes = workoutEntity.notes,
            id = workoutEntity.id
        )

        // Delete old sets and insert new ones
        workoutQueries.deleteSetsForWorkout(workout.id)

        // Re-insert all sets
        workout.exercises.forEachIndexed { exerciseIndex, exerciseInWorkout ->
            exerciseInWorkout.sets.forEachIndexed { setIndex, set ->
                val setEntity = set.toEntity(
                    workoutId = workout.id,
                    exerciseId = exerciseInWorkout.exercise.id,
                    orderInWorkout = exerciseIndex,
                    setNumber = setIndex + 1
                )

                workoutQueries.insertWorkoutSet(
                    id = setEntity.id,
                    workout_id = setEntity.workout_id,
                    exercise_id = setEntity.exercise_id,
                    order_in_workout = setEntity.order_in_workout,
                    set_number = setEntity.set_number,
                    reps = setEntity.reps,
                    weight_kg = setEntity.weight_kg,
                    rest_time_seconds = setEntity.rest_time_seconds,
                    completed = setEntity.completed,
                    completed_at = setEntity.completed_at,
                    notes = setEntity.notes
                )
            }
        }

        workout
    }

    override suspend fun deleteWorkout(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            workoutQueries.deleteSetsForWorkout(id)
            workoutQueries.deleteWorkout(id)
            true
        } catch (e: Exception) {
            false
        }
    }
}

// shared/src/commonMain/data/repository/ExerciseRepositoryImpl.kt
package com.bodyforge.data.repository

import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.repository.ExerciseRepository
import com.bodyforge.data.mappers.WorkoutMapper.toDomain
import com.bodyforge.data.mappers.WorkoutMapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class ExerciseRepositoryImpl(
    private val database: BodyForgeDatabase
) : ExerciseRepository {

    private val workoutQueries = database.workoutDatabaseQueries

    override suspend fun getAllExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        workoutQueries.selectAllExercises().executeAsList().map { it.toDomain() }
    }

    override suspend fun getExerciseById(id: String): Exercise? = withContext(Dispatchers.IO) {
        workoutQueries.selectExerciseById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise> = withContext(Dispatchers.IO) {
        workoutQueries.selectExercisesByMuscleGroup(muscleGroup).executeAsList().map { it.toDomain() }
    }

    override suspend fun searchExercises(query: String): List<Exercise> = withContext(Dispatchers.IO) {
        workoutQueries.searchExercises(query).executeAsList().map { it.toDomain() }
    }

    override suspend fun saveCustomExercise(exercise: Exercise): Exercise = withContext(Dispatchers.IO) {
        val exerciseEntity = exercise.copy(isCustom = true).toEntity()

        workoutQueries.insertExercise(
            id = exerciseEntity.id,
            name = exerciseEntity.name,
            muscle_groups = exerciseEntity.muscle_groups,
            instructions = exerciseEntity.instructions,
            equipment_needed = exerciseEntity.equipment_needed,
            is_custom = exerciseEntity.is_custom,
            default_rest_time_seconds = exerciseEntity.default_rest_time_seconds
        )

        exercise.copy(isCustom = true)
    }

    override suspend fun deleteCustomExercise(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            workoutQueries.deleteCustomExercise(id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCustomExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        workoutQueries.selectCustomExercises().executeAsList().map { it.toDomain() }
    }
}