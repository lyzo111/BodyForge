package com.bodyforge.data.repository

import com.bodyforge.domain.repository.WorkoutTemplateRepository
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.data.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class WorkoutTemplateRepositoryImpl : WorkoutTemplateRepository {

    private val database = DatabaseFactory.create()
    private val queries = database.bodyForgeDatabaseQueries

    override suspend fun getAllTemplates(): List<WorkoutTemplate> = withContext(Dispatchers.IO) {
        queries.selectAllTemplates().executeAsList().map { entity ->
            WorkoutTemplate(
                id = entity.id,
                name = entity.name,
                exerciseIds = Json.decodeFromString<List<String>>(entity.exercise_ids),
                createdAt = Instant.fromEpochSeconds(entity.created_at),
                description = entity.description
            )
        }
    }

    override suspend fun getTemplateById(id: String): WorkoutTemplate? = withContext(Dispatchers.IO) {
        queries.selectTemplateById(id).executeAsOneOrNull()?.let { entity ->
            WorkoutTemplate(
                id = entity.id,
                name = entity.name,
                exerciseIds = Json.decodeFromString<List<String>>(entity.exercise_ids),
                createdAt = Instant.fromEpochSeconds(entity.created_at),
                description = entity.description
            )
        }
    }

    override suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate = withContext(Dispatchers.IO) {
        queries.insertTemplate(
            id = template.id,
            name = template.name,
            exercise_ids = Json.encodeToString(template.exerciseIds),
            created_at = template.createdAt.epochSeconds,
            description = template.description
        )
        template
    }

    override suspend fun updateTemplate(template: WorkoutTemplate): WorkoutTemplate = withContext(Dispatchers.IO) {
        queries.updateTemplate(
            name = template.name,
            exercise_ids = Json.encodeToString(template.exerciseIds),
            description = template.description,
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
        queries.searchTemplates(query, query).executeAsList().map { entity ->
            WorkoutTemplate(
                id = entity.id,
                name = entity.name,
                exerciseIds = Json.decodeFromString<List<String>>(entity.exercise_ids),
                createdAt = Instant.fromEpochSeconds(entity.created_at),
                description = entity.description
            )
        }
    }
}