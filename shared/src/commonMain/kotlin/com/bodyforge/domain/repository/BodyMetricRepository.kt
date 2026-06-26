package com.bodyforge.domain.repository

import com.bodyforge.domain.models.BodyMetric

interface BodyMetricRepository {
    suspend fun getAllMetrics(): List<BodyMetric>
    suspend fun saveMetric(metric: BodyMetric): BodyMetric
    suspend fun deleteMetric(id: String): Boolean
}
