package com.bodyforge.data.repository

import com.bodyforge.domain.repository.ExerciseRepository
import com.bodyforge.domain.models.Exercise
import com.bodyforge.data.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ExerciseRepositoryImpl : ExerciseRepository {

    private val database = DatabaseFactory.create()
    private val queries = database.workoutDatabaseQueries

    override suspend fun getAllExercises(): List<Exercise> = withContext(Dispatchers.IO) {
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

    override suspend fun getExerciseById(id: String): Exercise? = withContext(Dispatchers.IO) {
        queries.selectExerciseById(id).executeAsOneOrNull()?.let { entity ->
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

    override suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise> = withContext(Dispatchers.IO) {
        queries.selectExercisesByMuscleGroup(muscleGroup).executeAsList().map { entity ->
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

    override suspend fun searchExercises(query: String): List<Exercise> = withContext(Dispatchers.IO) {
        queries.searchExercises(query).executeAsList().map { entity ->
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

    override suspend fun saveCustomExercise(exercise: Exercise): Exercise = withContext(Dispatchers.IO) {
        val customExercise = exercise.copy(isCustom = true)
        queries.insertExercise(
            id = customExercise.id,
            name = customExercise.name,
            muscle_groups = Json.encodeToString(customExercise.muscleGroups),
            instructions = customExercise.instructions,
            equipment_needed = customExercise.equipmentNeeded,
            is_custom = 1L,
            default_rest_time_seconds = customExercise.defaultRestTimeSeconds.toLong()
        )
        customExercise
    }

    override suspend fun deleteCustomExercise(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.deleteCustomExercise(id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCustomExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        queries.selectCustomExercises().executeAsList().map { entity ->
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
}