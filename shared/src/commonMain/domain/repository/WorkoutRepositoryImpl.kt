package com.bodyforge.data.repository

import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.repository.WorkoutRepository
import com.bodyforge.domain.repository.ExerciseRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class WorkoutRepositoryImpl(database: BodyForgeDatabase) : WorkoutRepository {
    private val queries = database.workoutDatabaseQueries

    override suspend fun saveWorkout(workout: Workout): Workout = workout
    override suspend fun getActiveWorkout(): Workout? = null
    override suspend fun updateWorkout(workout: Workout): Workout = workout
}

class ExerciseRepositoryImpl(database: BodyForgeDatabase) : ExerciseRepository {
    private val queries = database.workoutDatabaseQueries

    override suspend fun getAllExercises(): List<Exercise> = Exercise.getStandardExercises()
}