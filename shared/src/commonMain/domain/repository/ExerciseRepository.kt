package com.bodyforge.domain.repository

import com.bodyforge.domain.models.Exercise

interface ExerciseRepository {
    suspend fun getAllExercises(): List<Exercise>
}