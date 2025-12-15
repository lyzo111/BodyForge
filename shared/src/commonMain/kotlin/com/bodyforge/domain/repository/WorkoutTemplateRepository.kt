package com.bodyforge.domain.repository

import com.bodyforge.domain.models.WorkoutTemplate

interface WorkoutTemplateRepository {
    suspend fun getAllTemplates(): List<WorkoutTemplate>
    suspend fun getTemplateById(id: String): WorkoutTemplate?
    suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate
    suspend fun updateTemplate(template: WorkoutTemplate): WorkoutTemplate
    suspend fun deleteTemplate(id: String): Boolean
    suspend fun searchTemplates(query: String): List<WorkoutTemplate>
}