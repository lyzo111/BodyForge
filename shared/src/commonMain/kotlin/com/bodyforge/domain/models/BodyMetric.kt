package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

// A single body measurement on a given day. Weight is always stored in kilograms; body fat and
// muscle mass are optional so a quick weight-only log is still valid.
data class BodyMetric(
    val id: String,
    val date: LocalDate,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val muscleMassKg: Double? = null,
    val notes: String = "",
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        fun create(
            date: LocalDate,
            weightKg: Double,
            bodyFatPct: Double? = null,
            muscleMassKg: Double? = null,
            notes: String = ""
        ): BodyMetric = BodyMetric(
            id = "body_${Clock.System.now().toEpochMilliseconds()}",
            date = date,
            weightKg = weightKg,
            bodyFatPct = bodyFatPct,
            muscleMassKg = muscleMassKg,
            notes = notes
        )
    }
}
