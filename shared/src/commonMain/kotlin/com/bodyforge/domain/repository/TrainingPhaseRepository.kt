package com.bodyforge.domain.repository

import com.bodyforge.domain.models.TrainingPhase
import kotlinx.datetime.LocalDate

interface TrainingPhaseRepository {
    suspend fun getAllPhases(): List<TrainingPhase>
    suspend fun getActivePhase(): TrainingPhase?
    suspend fun getPhaseById(id: String): TrainingPhase?
    suspend fun savePhase(phase: TrainingPhase): TrainingPhase
    suspend fun updatePhase(phase: TrainingPhase): TrainingPhase
    suspend fun deactivateActivePhases(endDate: LocalDate)
    suspend fun deletePhase(id: String): Boolean
}
