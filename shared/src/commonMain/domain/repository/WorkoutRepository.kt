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