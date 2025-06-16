package com.bodyforge.data.mappers

import com.bodyforge.database.Exercise as ExerciseEntity
import com.bodyforge.database.Workout as WorkoutEntity
import com.bodyforge.database.WorkoutSet as WorkoutSetEntity
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.ExerciseInWorkout
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object WorkoutMapper {

    // Exercise Mappings
    fun ExerciseEntity.toDomain(): Exercise {
        return Exercise(
            id = id,
            name = name,
            muscleGroups = Json.decodeFromString<List<String>>(muscle_groups),
            instructions = instructions,
            equipmentNeeded = equipment_needed,
            isCustom = is_custom == 1L,
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
            default_rest_time_seconds = defaultRestTimeSeconds.toLong()
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
            notes = notes
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
            notes = notes
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
            notes = notes
        )
    }

    fun Workout.toEntity(): WorkoutEntity {
        return WorkoutEntity(
            id = id,
            name = name,
            started_at = startedAt.epochSeconds,
            finished_at = finishedAt?.epochSeconds,
            notes = notes
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