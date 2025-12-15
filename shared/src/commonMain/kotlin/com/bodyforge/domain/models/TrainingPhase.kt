package com.bodyforge.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class TrainingPhase(
    val id: String,
    val name: String,
    val phaseType: PhaseType,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val goals: List<String> = emptyList(),
    val description: String = "",
    val templateFolders: List<TemplateFolder> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        fun create(
            name: String,
            phaseType: PhaseType,
            startDate: LocalDate,
            goals: List<String> = emptyList()
        ): TrainingPhase {
            return TrainingPhase(
                id = "phase_${Clock.System.now().epochSeconds}",
                name = name,
                phaseType = phaseType,
                startDate = startDate,
                goals = goals
            )
        }
    }

    val durationDays: Int? get() = endDate?.let { end ->
        end.toEpochDays() - startDate.toEpochDays()
    }

    val isCompleted: Boolean get() = endDate != null

    fun complete(endDate: LocalDate): TrainingPhase {
        return copy(endDate = endDate, isActive = false)
    }

    fun addTemplateFolder(folder: TemplateFolder): TrainingPhase {
        return copy(templateFolders = templateFolders + folder)
    }

    fun removeTemplateFolder(folderId: String): TrainingPhase {
        return copy(templateFolders = templateFolders.filter { it.id != folderId })
    }
}

enum class PhaseType(val displayName: String, val emoji: String) {
    STRENGTH("Strength", "💪"),
    HYPERTROPHY("Hypertrophy", "📈"),
    CUT("Cut", "🔥"),
    BULK("Bulk", "📊"),
    POWERLIFTING("Powerlifting", "🏋️"),
    DELOAD("Deload", "😌"),
    CONDITIONING("Conditioning", "🏃"),
    SKILL("Skill Focus", "🎯"),
    CUSTOM("Custom", "⚙️")
}

data class TemplateFolder(
    val id: String,
    val name: String,
    val description: String = "",
    val templateIds: List<String> = emptyList(),
    val schedulingType: SchedulingType = SchedulingType.MANUAL,
    val frequency: Int? = null, // Times per week
    val restDaysBetween: Int? = null,
    val color: String = "#3B82F6", // Hex color for UI
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        fun create(
            name: String,
            description: String = "",
            schedulingType: SchedulingType = SchedulingType.MANUAL
        ): TemplateFolder {
            return TemplateFolder(
                id = "folder_${Clock.System.now().epochSeconds}",
                name = name,
                description = description,
                schedulingType = schedulingType
            )
        }

        // Predefined folder types for common splits
        fun createPPLFolders(): List<TemplateFolder> = listOf(
            create("Push Day", "Chest, Shoulders, Triceps"),
            create("Pull Day", "Back, Biceps"),
            create("Leg Day", "Quadriceps, Glutes, Hamstrings")
        )

        fun createUpperLowerFolders(): List<TemplateFolder> = listOf(
            create("Upper Body", "Chest, Back, Shoulders, Arms"),
            create("Lower Body", "Quadriceps, Glutes, Hamstrings, Calves")
        )

        fun createFullBodyFolders(): List<TemplateFolder> = listOf(
            create("Full Body A", "Compound movements focus"),
            create("Full Body B", "Accessory movements focus")
        )
    }

    val templateCount: Int get() = templateIds.size

    fun addTemplate(templateId: String): TemplateFolder {
        return copy(templateIds = templateIds + templateId)
    }

    fun removeTemplate(templateId: String): TemplateFolder {
        return copy(templateIds = templateIds.filter { it != templateId })
    }
}

enum class SchedulingType(val displayName: String) {
    MANUAL("Manual Selection"),
    FREQUENCY("Frequency Based"),  // e.g., 3x per week
    ROTATION("Rotation Cycle"),    // e.g., Push → Pull → Legs → repeat
    REST_BASED("Rest Day Based")   // e.g., every other day
}

// Analytics and Progress Tracking Models
data class PhaseAnalytics(
    val phaseId: String,
    val totalWorkouts: Int,
    val totalVolume: Double,
    val averageIntensity: Double,
    val muscleGroupDistribution: Map<String, Int>,
    val weeklyFrequency: Double,
    val progressMetrics: List<ProgressMetric>
)

data class ProgressMetric(
    val id: String,
    val exerciseId: String,
    val metricType: ProgressType,
    val startValue: Double,
    val currentValue: Double,
    val targetValue: Double? = null,
    val unit: String, // "kg", "reps", "seconds"
    val lastUpdated: Instant
) {
    val progressPercentage: Double get() = when {
        targetValue != null && targetValue != startValue -> {
            ((currentValue - startValue) / (targetValue - startValue) * 100).coerceIn(0.0, 100.0)
        }
        else -> ((currentValue - startValue) / startValue * 100)
    }

    val isImproving: Boolean get() = currentValue > startValue
}

enum class ProgressType(val displayName: String) {
    ONE_REP_MAX("1RM"),
    VOLUME("Volume"),
    ENDURANCE("Endurance"),
    FREQUENCY("Frequency"),
    BODYWEIGHT("Bodyweight")
}

// Extension functions for analytics
fun List<com.bodyforge.domain.models.Workout>.analyzePhase(phase: TrainingPhase): PhaseAnalytics {
    val phaseWorkouts = this.filter { workout ->
        val workoutDate = workout.startDate
        workoutDate >= phase.startDate && (phase.endDate?.let { workoutDate <= it } ?: true)
    }

    val totalVolume = phaseWorkouts.sumOf { it.totalVolumePerformed }
    val muscleGroups = mutableMapOf<String, Int>()

    phaseWorkouts.forEach { workout ->
        workout.exercises.forEach { exerciseInWorkout ->
            exerciseInWorkout.exercise.muscleGroups.forEach { muscleGroup ->
                muscleGroups[muscleGroup] = muscleGroups.getOrDefault(muscleGroup, 0) + 1
            }
        }
    }

    val weeksDuration = phase.durationDays?.let { it / 7.0 } ?: 1.0
    val weeklyFrequency = phaseWorkouts.size / weeksDuration

    return PhaseAnalytics(
        phaseId = phase.id,
        totalWorkouts = phaseWorkouts.size,
        totalVolume = totalVolume,
        averageIntensity = 0.0, // TODO: Calculate based on RPE or %1RM
        muscleGroupDistribution = muscleGroups,
        weeklyFrequency = weeklyFrequency,
        progressMetrics = emptyList() // TODO: Calculate from workout progression
    )
}