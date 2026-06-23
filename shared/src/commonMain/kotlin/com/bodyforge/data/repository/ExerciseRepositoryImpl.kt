package com.bodyforge.data.repository

import com.bodyforge.domain.repository.ExerciseRepository
import com.bodyforge.domain.models.Exercise
import com.bodyforge.data.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class ExerciseRepositoryImpl : ExerciseRepository {

    private val database = DatabaseFactory.create()
    private val queries = database.bodyForgeDatabaseQueries

    override suspend fun getAllExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        queries.selectAllExercisesActive().executeAsList().map { entity ->
            Exercise(
                id = entity.id,
                name = entity.name,
                muscleGroups = Json.decodeFromString<List<String>>(entity.muscle_groups),
                instructions = entity.instructions,
                equipmentNeeded = entity.equipment_needed,
                isCustom = entity.is_custom == 1L,
                isBodyweight = entity.is_bodyweight == 1L,
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
                isBodyweight = entity.is_bodyweight == 1L,
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
                isBodyweight = entity.is_bodyweight == 1L,
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
                isBodyweight = entity.is_bodyweight == 1L,
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
            is_bodyweight = if (customExercise.isBodyweight) 1L else 0L,
            default_rest_time_seconds = customExercise.defaultRestTimeSeconds.toLong(),
            deleted = 0L
        )
        customExercise
    }

    override suspend fun updateCustomExercise(exercise: Exercise): Exercise = withContext(Dispatchers.IO) {
        queries.updateCustomExercise(
            name = exercise.name,
            muscle_groups = Json.encodeToString(exercise.muscleGroups),
            instructions = exercise.instructions,
            equipment_needed = exercise.equipmentNeeded,
            is_bodyweight = if (exercise.isBodyweight) 1L else 0L,
            default_rest_time_seconds = exercise.defaultRestTimeSeconds.toLong(),
            id = exercise.id
        )
        exercise
    }

    override suspend fun deleteCustomExercise(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.softDeleteCustomExercise(id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun ensureStockExercises() = withContext(Dispatchers.IO) {
        fun add(id: String, name: String, groups: List<String>, equipment: String, bodyweight: Boolean, rest: Int) {
            queries.insertOrIgnoreExercise(id, name, Json.encodeToString(groups), "", equipment, if (bodyweight) 1L else 0L, rest.toLong())
        }
        add("cable_pullovers", "Cable Pullovers", listOf("Back"), "Cable Machine", false, 90)
        add("db_pullovers", "DB Pullovers", listOf("Back", "Chest"), "Dumbbells", false, 90)
        add("hammer_preacher_curls", "Hammer Preacher Curls", listOf("Biceps", "Forearms"), "Dumbbells", false, 60)
        add("db_chest_flyes", "Dumbbell Chest Flyes", listOf("Chest"), "Dumbbells", false, 60)
        add("spider_curls", "Spider Curls", listOf("Biceps"), "Dumbbells", false, 60)
        add("bench_press_db", "Bench Press (Dumbbell)", listOf("Chest", "Triceps"), "Dumbbells", false, 120)
        add("incline_bench_press_db", "Incline Bench Press (Dumbbell)", listOf("Chest", "Triceps"), "Dumbbells", false, 120)
        add("running", "Running", listOf("Cardio"), "None", false, 60)
        add("cycling", "Cycling", listOf("Cardio"), "Bike", false, 60)
        add("cross_stepper", "Cross-Stepper", listOf("Cardio"), "Cross Trainer", false, 60)
        add("rowing", "Rowing", listOf("Cardio"), "Rowing Machine", false, 60)
        queries.renameStockExercise("Bench Press (Barbell)", "bench_press")
        queries.renameStockExercise("Incline Bench Press (Barbell)", "incline_bench_press")
        queries.renameStockExercise("DB Lateral Raises", "lateral_raises")
    }

    override suspend fun getCustomExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        queries.selectCustomExercisesActive().executeAsList().map { entity ->
            Exercise(
                id = entity.id,
                name = entity.name,
                muscleGroups = Json.decodeFromString<List<String>>(entity.muscle_groups),
                instructions = entity.instructions,
                equipmentNeeded = entity.equipment_needed,
                isCustom = entity.is_custom == 1L,
                isBodyweight = entity.is_bodyweight == 1L,
                defaultRestTimeSeconds = entity.default_rest_time_seconds.toInt()
            )
        }
    }
}