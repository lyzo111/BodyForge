package com.bodyforge.domain.repository

import com.bodyforge.domain.models.Workout

interface WorkoutRepository {
    suspend fun saveWorkout(workout: Workout): Workout
    suspend fun getActiveWorkout(): Workout?
    suspend fun updateWorkout(workout: Workout): Workout
}