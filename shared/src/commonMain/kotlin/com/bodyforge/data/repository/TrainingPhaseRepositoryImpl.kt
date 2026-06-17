package com.bodyforge.data.repository

import com.bodyforge.data.DatabaseFactory
import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.database.TrainingPhase as TrainingPhaseEntity
import com.bodyforge.domain.models.PhaseType
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.repository.TrainingPhaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class TrainingPhaseRepositoryImpl(
    database: BodyForgeDatabase = DatabaseFactory.create()
) : TrainingPhaseRepository {

    private val queries = database.bodyForgeDatabaseQueries

    private fun TrainingPhaseEntity.toDomain(): TrainingPhase = TrainingPhase(
        id = id,
        name = name,
        phaseType = PhaseType.entries.firstOrNull { it.name == phase_type } ?: PhaseType.CUSTOM,
        startDate = LocalDate.fromEpochDays(start_date.toInt()),
        endDate = end_date?.let { LocalDate.fromEpochDays(it.toInt()) },
        goals = Json.decodeFromString<List<String>>(goals),
        description = description,
        isActive = is_active == 1L,
        createdAt = Instant.fromEpochSeconds(created_at)
    )

    override suspend fun getAllPhases(): List<TrainingPhase> = withContext(Dispatchers.IO) {
        queries.selectAllPhases().executeAsList().map { it.toDomain() }
    }

    override suspend fun getActivePhase(): TrainingPhase? = withContext(Dispatchers.IO) {
        queries.selectActivePhase().executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getPhaseById(id: String): TrainingPhase? = withContext(Dispatchers.IO) {
        queries.selectPhaseById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun savePhase(phase: TrainingPhase): TrainingPhase = withContext(Dispatchers.IO) {
        queries.insertPhase(
            id = phase.id,
            name = phase.name,
            phase_type = phase.phaseType.name,
            start_date = phase.startDate.toEpochDays().toLong(),
            end_date = phase.endDate?.toEpochDays()?.toLong(),
            goals = Json.encodeToString(phase.goals),
            description = phase.description,
            is_active = if (phase.isActive) 1L else 0L,
            created_at = phase.createdAt.epochSeconds
        )
        phase
    }

    override suspend fun updatePhase(phase: TrainingPhase): TrainingPhase = withContext(Dispatchers.IO) {
        queries.updatePhase(
            name = phase.name,
            phase_type = phase.phaseType.name,
            start_date = phase.startDate.toEpochDays().toLong(),
            end_date = phase.endDate?.toEpochDays()?.toLong(),
            goals = Json.encodeToString(phase.goals),
            description = phase.description,
            is_active = if (phase.isActive) 1L else 0L,
            id = phase.id
        )
        phase
    }

    override suspend fun deactivateActivePhases(endDate: LocalDate) {
        withContext(Dispatchers.IO) {
            queries.deactivateActivePhases(endDate.toEpochDays().toLong())
        }
    }

    override suspend fun deletePhase(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.deletePhase(id)
            true
        } catch (e: Exception) {
            false
        }
    }
}
