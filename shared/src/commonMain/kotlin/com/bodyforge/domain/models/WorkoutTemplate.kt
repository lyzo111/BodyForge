package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exerciseIds: List<String>,
    val createdAt: Instant,
    val description: String = "",
    val routineId: String = "",
    val routineName: String = "",
    val variationLabel: String = ""
) {
    companion object {
        fun create(
            name: String,
            exercises: List<Exercise>,
            routineId: String = "",
            routineName: String = "",
            variationLabel: String = ""
        ): WorkoutTemplate {
            return WorkoutTemplate(
                id = "template_${Clock.System.now().epochSeconds}",
                name = name,
                exerciseIds = exercises.map { it.id },
                createdAt = Clock.System.now(),
                routineId = routineId,
                routineName = routineName,
                variationLabel = variationLabel
            )
        }
    }

    // Get exercise count for display
    val exerciseCount: Int get() = exerciseIds.size

    // Check if template is valid (has exercises)
    val isValid: Boolean get() = exerciseIds.isNotEmpty() && name.isNotBlank()

    // True when this template is grouped under a parent routine
    val hasRoutine: Boolean get() = routineId.isNotBlank()

    // True when this template carries a variation label such as "A" or "B"
    val hasVariation: Boolean get() = variationLabel.isNotBlank()
}