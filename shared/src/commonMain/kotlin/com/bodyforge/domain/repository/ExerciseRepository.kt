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