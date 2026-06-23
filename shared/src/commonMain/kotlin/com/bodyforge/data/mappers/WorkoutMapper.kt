package com.bodyforge.data.mappers

import com.bodyforge.database.Exercise as ExerciseEntity
import com.bodyforge.database.Workout as WorkoutEntity
import com.bodyforge.database.WorkoutSet as WorkoutSetEntity
import com.bodyforge.database.WorkoutTemplate as WorkoutTemplateEntity
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.domain.models.SetStatus
import com.bodyforge.domain.models.ExerciseInWorkout
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object WorkoutMapper {

    // Cardio metrics are stored as a JSON map of metric key -> value; blank means none.
    fun serializeMetrics(metrics: Map<String, Double>): String =
        if (metrics.isEmpty()) "" else Json.encodeToString(metrics)

    fun parseMetrics(raw: String): Map<String, Double> =
        if (raw.isBlank()) emptyMap() else runCatching { Json.decodeFromString<Map<String, Double>>(raw) }.getOrElse { emptyMap() }

    // Exercise Mappings
    fun ExerciseEntity.toDomain(): Exercise {
        return Exercise(
            id = id,
            name = name,
            muscleGroups = Json.decodeFromString<List<String>>(muscle_groups),
            instructions = instructions,
            equipmentNeeded = equipment_needed,
            isCustom = is_custom == 1L,
            isBodyweight = is_bodyweight == 1L,  // Map bodyweight flag
            defaultRestTimeSeconds = default_rest_time_seconds.toInt()
        )
    }

    fun Exercise.toEntity(): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = name,
            muscle_groups = Json.encodeToString(muscleGroups),
            instructions = instructions,
            equipment_needed = equipmentNeeded,
            is_custom = if (isCustom) 1L else 0L,
            is_bodyweight = if (isBodyweight) 1L else 0L,  // Map bodyweight flag
            default_rest_time_seconds = defaultRestTimeSeconds.toLong(),
            deleted = 0L
        )
    }

    // WorkoutSet Mappings
    fun WorkoutSetEntity.toDomain(): WorkoutSet {
        return WorkoutSet(
            id = id,
            reps = reps.toInt(),
            weightKg = weight_kg,
            restTimeSeconds = rest_time_seconds.toInt(),
            completed = completed == 1L,
            completedAt = completed_at?.let { Instant.fromEpochSeconds(it) },
            notes = notes,
            status = SetStatus.fromStorage(status),
            originalExerciseId = original_exercise_id,
            metrics = parseMetrics(metrics)
        )
    }

    fun WorkoutSet.toEntity(workoutId: String, exerciseId: String, orderInWorkout: Int, setNumber: Int): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = id,
            workout_id = workoutId,
            exercise_id = exerciseId,
            order_in_workout = orderInWorkout.toLong(),
            set_number = setNumber.toLong(),
            reps = reps.toLong(),
            weight_kg = weightKg,
            rest_time_seconds = restTimeSeconds.toLong(),
            completed = if (completed) 1L else 0L,
            completed_at = completedAt?.epochSeconds,
            notes = notes,
            status = status.name,
            original_exercise_id = originalExerciseId,
            metrics = serializeMetrics(metrics)
        )
    }

    // Workout Mappings
    fun WorkoutEntity.toDomain(): Workout {
        return Workout(
            id = id,
            name = name,
            startedAt = Instant.fromEpochSeconds(started_at),
            finishedAt = finished_at?.let { Instant.fromEpochSeconds(it) },
            exercises = emptyList(), // Will be populated separately
            notes = notes,
            templateId = template_id
        )
    }

    fun Workout.toEntity(): WorkoutEntity {
        return WorkoutEntity(
            id = id,
            name = name,
            started_at = startedAt.epochSeconds,
            finished_at = finishedAt?.epochSeconds,
            notes = notes,
            template_id = templateId
        )
    }

    // WorkoutTemplate Mappings
    fun WorkoutTemplateEntity.toDomain(): WorkoutTemplate {
        return WorkoutTemplate(
            id = id,
            name = name,
            exerciseIds = Json.decodeFromString<List<String>>(exercise_ids),
            createdAt = Instant.fromEpochSeconds(created_at),
            description = description,
            routineId = routine_id,
            routineName = routine_name,
            variationLabel = variation_label
        )
    }

    fun WorkoutTemplate.toEntity(): WorkoutTemplateEntity {
        return WorkoutTemplateEntity(
            id = id,
            name = name,
            exercise_ids = Json.encodeToString(exerciseIds),
            created_at = createdAt.epochSeconds,
            description = description,
            routine_id = routineId,
            routine_name = routineName,
            variation_label = variationLabel
        )
    }

    // Combined mapping for full workout with exercises and sets
    fun combineWorkoutWithExercises(
        workout: WorkoutEntity,
        exercisesWithSets: List<Pair<ExerciseEntity, List<WorkoutSetEntity>>>
    ): Workout {
        val exercisesInWorkout = exercisesWithSets.mapIndexed { index, (exerciseEntity, setEntities) ->
            val exercise = exerciseEntity.toDomain()
            val sets = setEntities.map { it.toDomain() }

            ExerciseInWorkout(
                exercise = exercise,
                sets = sets,
                orderInWorkout = index
            )
        }

        return workout.toDomain().copy(exercises = exercisesInWorkout)
    }
}

// Convenience extension for JSON handling
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}