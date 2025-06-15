package com.bodyforge.domain.repository

import com.bodyforge.domain.models.Workout
import kotlinx.datetime.LocalDate

interface WorkoutRepository {
    suspend fun saveWorkout(workout: Workout): Workout
    suspend fun getWorkout(id: String): Workout?
    suspend fun getAllWorkouts(): List<Workout>
    suspend fun getWorkoutsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Workout>
    suspend fun getActiveWorkout(): Workout?
    suspend fun updateWorkout(workout: Workout): Workout
    suspend fun deleteWorkout(id: String): Boolean
}

// shared/src/commonMain/domain/repository/ExerciseRepository.kt
package com.bodyforge.domain.repository

import com.bodyforge.domain.models.Exercise

interface ExerciseRepository {
    suspend fun getAllExercises(): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise>
    suspend fun searchExercises(query: String): List<Exercise>
    suspend fun saveCustomExercise(exercise: Exercise): Exercise
    suspend fun deleteCustomExercise(id: String): Boolean
    suspend fun getCustomExercises(): List<Exercise>
}