package com.bodyforge.data.repository

import com.bodyforge.domain.repository.WorkoutTemplateRepository
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.data.DatabaseFactory
import com.bodyforge.data.mappers.WorkoutMapper.toDomain
import com.bodyforge.database.BodyForgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class WorkoutTemplateRepositoryImpl(
    database: BodyForgeDatabase = DatabaseFactory.create()
) : WorkoutTemplateRepository {

    private val queries = database.bodyForgeDatabaseQueries

    override suspend fun getAllTemplates(): List<WorkoutTemplate> = withContext(Dispatchers.IO) {
        queries.selectAllTemplates().executeAsList().map { it.toDomain() }
    }

    override suspend fun getTemplateById(id: String): WorkoutTemplate? = withContext(Dispatchers.IO) {
        queries.selectTemplateById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getTemplatesByRoutine(routineId: String): List<WorkoutTemplate> = withContext(Dispatchers.IO) {
        queries.selectTemplatesByRoutine(routineId).executeAsList().map { it.toDomain() }
    }

    override suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate = withContext(Dispatchers.IO) {
        queries.insertTemplate(
            id = template.id,
            name = template.name,
            exercise_ids = Json.encodeToString(template.exerciseIds),
            created_at = template.createdAt.epochSeconds,
            description = template.description,
            routine_id = template.routineId,
            routine_name = template.routineName,
            variation_label = template.variationLabel,
            exercise_targets = Json.encodeToString(template.targets)
        )
        template
    }

    override suspend fun updateTemplate(template: WorkoutTemplate): WorkoutTemplate = withContext(Dispatchers.IO) {
        queries.updateTemplate(
            name = template.name,
            exercise_ids = Json.encodeToString(template.exerciseIds),
            description = template.description,
            routine_id = template.routineId,
            routine_name = template.routineName,
            variation_label = template.variationLabel,
            exercise_targets = Json.encodeToString(template.targets),
            id = template.id
        )
        template
    }

    override suspend fun deleteTemplate(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.deleteTemplate(id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun searchTemplates(query: String): List<WorkoutTemplate> = withContext(Dispatchers.IO) {
        queries.searchTemplates(query, query).executeAsList().map { it.toDomain() }
    }
}
