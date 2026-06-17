package com.bodyforge.data.mappers

import com.bodyforge.data.mappers.WorkoutMapper.toDomain
import com.bodyforge.data.mappers.WorkoutMapper.toEntity
import com.bodyforge.database.Workout as WorkoutEntity
import com.bodyforge.database.WorkoutSet as WorkoutSetEntity
import com.bodyforge.database.WorkoutTemplate as WorkoutTemplateEntity
import com.bodyforge.domain.models.SetStatus
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.WorkoutTemplate
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkoutMapperTest {

    // WorkoutSet status / original exercise

    @Test
    fun substitutedSetMapsToEntityAndBack() {
        val domain = WorkoutSet(
            id = "set1",
            reps = 8,
            weightKg = 80.0,
            restTimeSeconds = 90,
            completed = true,
            completedAt = Instant.fromEpochSeconds(1000),
            notes = "swap",
            status = SetStatus.SUBSTITUTED,
            originalExerciseId = "bench_press"
        )

        val entity = domain.toEntity(
            workoutId = "w1",
            exerciseId = "machine_chest_press",
            orderInWorkout = 0,
            setNumber = 1
        )

        assertEquals("SUBSTITUTED", entity.status)
        assertEquals("bench_press", entity.original_exercise_id)

        val roundTripped = entity.toDomain()
        assertEquals(SetStatus.SUBSTITUTED, roundTripped.status)
        assertEquals("bench_press", roundTripped.originalExerciseId)
    }

    @Test
    fun skippedStatusStringMapsToEnum() {
        val entity = workoutSetEntity(status = "SKIPPED")
        assertEquals(SetStatus.SKIPPED, entity.toDomain().status)
    }

    @Test
    fun unknownStatusFallsBackToCompleted() {
        val entity = workoutSetEntity(status = "NOT_A_REAL_STATUS")
        assertEquals(SetStatus.COMPLETED, entity.toDomain().status)
    }

    @Test
    fun defaultSetHasNullOriginalExercise() {
        val entity = workoutSetEntity(status = "COMPLETED", originalExerciseId = null)
        val domain = entity.toDomain()
        assertEquals(SetStatus.COMPLETED, domain.status)
        assertNull(domain.originalExerciseId)
    }

    // Workout template_id

    @Test
    fun workoutTemplateIdMapsBothWays() {
        val domain = Workout(
            id = "w1",
            name = "Push",
            startedAt = Instant.fromEpochSeconds(500),
            finishedAt = null,
            notes = "",
            templateId = "template_42"
        )

        val entity = domain.toEntity()
        assertEquals("template_42", entity.template_id)
        assertEquals("template_42", entity.toDomain().templateId)
    }

    @Test
    fun workoutWithoutTemplateRoundTripsAsNull() {
        val entity = WorkoutEntity(
            id = "w2",
            name = "Ad hoc",
            started_at = 1L,
            finished_at = null,
            notes = "",
            template_id = null
        )
        assertNull(entity.toDomain().templateId)
    }

    // WorkoutTemplate routine grouping

    @Test
    fun templateRoutineFieldsMapBothWays() {
        val domain = WorkoutTemplate(
            id = "t1",
            name = "Upper A",
            exerciseIds = listOf("bench_press", "barbell_row"),
            createdAt = Instant.fromEpochSeconds(10),
            description = "desc",
            routineId = "routine_upper",
            routineName = "Upper",
            variationLabel = "A"
        )

        val entity = domain.toEntity()
        assertEquals("routine_upper", entity.routine_id)
        assertEquals("Upper", entity.routine_name)
        assertEquals("A", entity.variation_label)

        val roundTripped = entity.toDomain()
        assertEquals(domain.exerciseIds, roundTripped.exerciseIds)
        assertEquals("routine_upper", roundTripped.routineId)
        assertEquals("Upper", roundTripped.routineName)
        assertEquals("A", roundTripped.variationLabel)
    }

    @Test
    fun legacyTemplateEntityWithEmptyRoutineMapsToUngrouped() {
        val entity = WorkoutTemplateEntity(
            id = "t2",
            name = "Full Body",
            exercise_ids = "[\"squat\"]",
            created_at = 5L,
            description = "",
            routine_id = "",
            routine_name = "",
            variation_label = ""
        )
        val domain = entity.toDomain()
        assertEquals(false, domain.hasRoutine)
        assertEquals(false, domain.hasVariation)
    }

    private fun workoutSetEntity(
        status: String,
        originalExerciseId: String? = null
    ) = WorkoutSetEntity(
        id = "set",
        workout_id = "w",
        exercise_id = "e",
        order_in_workout = 0,
        set_number = 1,
        reps = 5,
        weight_kg = 60.0,
        rest_time_seconds = 90,
        completed = 1,
        completed_at = null,
        notes = "",
        status = status,
        original_exercise_id = originalExerciseId
    )
}
