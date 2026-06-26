package com.bodyforge.data.repository

import com.bodyforge.data.DatabaseFactory
import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.database.BodyMetric as BodyMetricEntity
import com.bodyforge.domain.models.BodyMetric
import com.bodyforge.domain.repository.BodyMetricRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class BodyMetricRepositoryImpl(
    database: BodyForgeDatabase = DatabaseFactory.create()
) : BodyMetricRepository {

    private val queries = database.bodyForgeDatabaseQueries

    private fun BodyMetricEntity.toDomain(): BodyMetric = BodyMetric(
        id = id,
        date = LocalDate.fromEpochDays(date.toInt()),
        weightKg = weight_kg,
        bodyFatPct = body_fat_pct,
        muscleMassKg = muscle_mass_kg,
        notes = notes,
        createdAt = Instant.fromEpochSeconds(created_at)
    )

    override suspend fun getAllMetrics(): List<BodyMetric> = withContext(Dispatchers.IO) {
        queries.selectAllBodyMetrics().executeAsList().map { it.toDomain() }
    }

    override suspend fun saveMetric(metric: BodyMetric): BodyMetric = withContext(Dispatchers.IO) {
        queries.insertBodyMetric(
            id = metric.id,
            date = metric.date.toEpochDays().toLong(),
            weight_kg = metric.weightKg,
            body_fat_pct = metric.bodyFatPct,
            muscle_mass_kg = metric.muscleMassKg,
            notes = metric.notes,
            created_at = metric.createdAt.epochSeconds
        )
        metric
    }

    override suspend fun deleteMetric(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.deleteBodyMetric(id)
            true
        } catch (e: Exception) {
            false
        }
    }
}
