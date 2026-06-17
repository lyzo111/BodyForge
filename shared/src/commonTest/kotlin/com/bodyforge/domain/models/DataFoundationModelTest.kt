package com.bodyforge.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataFoundationModelTest {

    @Test
    fun setStatusFromStorageParsesKnownValues() {
        assertEquals(SetStatus.COMPLETED, SetStatus.fromStorage("COMPLETED"))
        assertEquals(SetStatus.SKIPPED, SetStatus.fromStorage("SKIPPED"))
        assertEquals(SetStatus.SUBSTITUTED, SetStatus.fromStorage("SUBSTITUTED"))
    }

    @Test
    fun setStatusFromStorageFallsBackForUnknownOrEmpty() {
        assertEquals(SetStatus.COMPLETED, SetStatus.fromStorage(""))
        assertEquals(SetStatus.COMPLETED, SetStatus.fromStorage("garbage"))
    }

    @Test
    fun newSetDefaultsToCompletedAndNoOriginalExercise() {
        val set = WorkoutSet.createEmpty("bench_press", 1, 90)
        assertEquals(SetStatus.COMPLETED, set.status)
        assertNull(set.originalExerciseId)
        assertFalse(set.isSkipped)
        assertFalse(set.isSubstituted)
    }

    @Test
    fun skipMarksSetSkippedAndNotCompleted() {
        val skipped = WorkoutSet.createEmpty("bench_press", 1, 90).complete().skip()
        assertTrue(skipped.isSkipped)
        assertEquals(SetStatus.SKIPPED, skipped.status)
        assertFalse(skipped.completed)
        assertNull(skipped.completedAt)
    }

    @Test
    fun substituteRemembersOriginalExercise() {
        val substituted = WorkoutSet.createEmpty("machine_chest_press", 1, 90)
            .substitute("bench_press")
        assertTrue(substituted.isSubstituted)
        assertEquals(SetStatus.SUBSTITUTED, substituted.status)
        assertEquals("bench_press", substituted.originalExerciseId)
    }

    @Test
    fun workoutCreateCarriesTemplateOrigin() {
        val exercises = listOf(Exercise("bench_press", "Bench Press", listOf("Chest")))
        val fromTemplate = Workout.create("Push", exercises, templateId = "template_7")
        assertEquals("template_7", fromTemplate.templateId)

        val adHoc = Workout.create("Push", exercises)
        assertNull(adHoc.templateId)
    }

    @Test
    fun templateCreateCarriesRoutineGrouping() {
        val exercises = listOf(Exercise("bench_press", "Bench Press", listOf("Chest")))
        val upperA = WorkoutTemplate.create(
            name = "Upper A",
            exercises = exercises,
            routineId = "routine_upper",
            routineName = "Upper",
            variationLabel = "A"
        )
        assertTrue(upperA.hasRoutine)
        assertTrue(upperA.hasVariation)
        assertEquals("routine_upper", upperA.routineId)
        assertEquals("Upper", upperA.routineName)
        assertEquals("A", upperA.variationLabel)

        val ungrouped = WorkoutTemplate.create("Full Body", exercises)
        assertFalse(ungrouped.hasRoutine)
        assertFalse(ungrouped.hasVariation)
    }
}
