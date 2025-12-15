package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exerciseIds: List<String>,
    val createdAt: Instant,
    val description: String = ""
) {
    companion object {
        fun create(name: String, exercises: List<Exercise>): WorkoutTemplate {
            return WorkoutTemplate(
                id = "template_${Clock.System.now().epochSeconds}",
                name = name,
                exerciseIds = exercises.map { it.id },
                createdAt = Clock.System.now()
            )
        }
    }

    // Get exercise count for display
    val exerciseCount: Int get() = exerciseIds.size

    // Check if template is valid (has exercises)
    val isValid: Boolean get() = exerciseIds.isNotEmpty() && name.isNotBlank()
}