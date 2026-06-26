package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// Optional planned target for one exercise in a template: how many sets, the rep goal and a
// starting weight (kg). Used to pre-fill the sets when a workout is started from the template.
@Serializable
data class ExerciseTarget(
    val sets: Int = 3,
    val reps: Int = 8,
    val weightKg: Double = 0.0
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exerciseIds: List<String>,
    val createdAt: Instant,
    val description: String = "",
    val routineId: String = "",
    val routineName: String = "",
    val variationLabel: String = "",
    // exerciseId -> planned target; empty when the template has no explicit targets.
    val targets: Map<String, ExerciseTarget> = emptyMap()
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