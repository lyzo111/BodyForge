package com.bodyforge.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bodyforge.database.BodyForgeDatabase
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.SetStatus
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.WorkoutTemplate
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the repositories against an in-memory SQLite database to verify the new schema
 * columns and queries round-trip through the real repository code.
 */
class RepositoryDbTest {

    private lateinit var database: BodyForgeDatabase
    private lateinit var workoutRepo: WorkoutRepositoryImpl
    private lateinit var templateRepo: WorkoutTemplateRepositoryImpl

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BodyForgeDatabase.Schema.create(driver)
        database = BodyForgeDatabase(driver)
        workoutRepo = WorkoutRepositoryImpl(database)
        templateRepo = WorkoutTemplateRepositoryImpl(database)
    }

    @Test
    fun workoutPersistsTemplateOriginAndSetStatuses() = runBlocking {
        val bench = Exercise(id = "bench_press", name = "Bench Press", muscleGroups = listOf("Chest"))
        val sets = listOf(
            WorkoutSet("s1", reps = 8, weightKg = 80.0, restTimeSeconds = 90, completed = true),
            WorkoutSet("s2", reps = 0, weightKg = 0.0, restTimeSeconds = 90, status = SetStatus.SKIPPED),
            WorkoutSet(
                "s3", reps = 8, weightKg = 70.0, restTimeSeconds = 90,
                status = SetStatus.SUBSTITUTED, originalExerciseId = "bench_press"
            )
        )
        val workout = Workout(
            id = "w1",
            name = "Push",
            startedAt = Instant.fromEpochSeconds(100),
            finishedAt = Instant.fromEpochSeconds(200),
            exercises = listOf(ExerciseInWorkout(bench, sets, 0)),
            templateId = "template_1"
        )

        workoutRepo.saveWorkout(workout)

        val loaded = workoutRepo.getWorkout("w1")!!
        assertEquals("template_1", loaded.templateId)

        val loadedSets = loaded.exercises.single().sets
        assertEquals(SetStatus.COMPLETED, loadedSets[0].status)
        assertEquals(SetStatus.SKIPPED, loadedSets[1].status)
        assertEquals(SetStatus.SUBSTITUTED, loadedSets[2].status)
        assertEquals("bench_press", loadedSets[2].originalExerciseId)
        assertNull(loadedSets[0].originalExerciseId)
    }

    @Test
    fun getWorkoutsByTemplateFiltersByOrigin() = runBlocking {
        val bench = Exercise(id = "bench_press", name = "Bench Press", muscleGroups = listOf("Chest"))
        fun workout(id: String, templateId: String?) = Workout(
            id = id,
            name = id,
            startedAt = Instant.fromEpochSeconds(100),
            finishedAt = Instant.fromEpochSeconds(200),
            exercises = listOf(
                ExerciseInWorkout(bench, listOf(WorkoutSet("${id}_s", reps = 5, weightKg = 50.0, restTimeSeconds = 90)), 0)
            ),
            templateId = templateId
        )

        workoutRepo.saveWorkout(workout("w_from_template", "template_42"))
        workoutRepo.saveWorkout(workout("w_ad_hoc", null))

        val byTemplate = workoutRepo.getWorkoutsByTemplate("template_42")
        assertEquals(listOf("w_from_template"), byTemplate.map { it.id })
        assertTrue(workoutRepo.getWorkoutsByTemplate("unknown_template").isEmpty())
    }

    @Test
    fun templatesGroupByRoutineAndOrderByVariation() = runBlocking {
        val exerciseIds = listOf("bench_press")
        fun template(id: String, name: String, routineId: String, variation: String) = WorkoutTemplate(
            id = id,
            name = name,
            exerciseIds = exerciseIds,
            createdAt = Instant.fromEpochSeconds(1),
            routineId = routineId,
            routineName = if (routineId.isEmpty()) "" else "Upper",
            variationLabel = variation
        )

        templateRepo.saveTemplate(template("t_upper_b", "Upper B", "routine_upper", "B"))
        templateRepo.saveTemplate(template("t_upper_a", "Upper A", "routine_upper", "A"))
        templateRepo.saveTemplate(template("t_full_body", "Full Body", "", ""))

        val routine = templateRepo.getTemplatesByRoutine("routine_upper")
        assertEquals(listOf("A", "B"), routine.map { it.variationLabel })
        assertEquals(3, templateRepo.getAllTemplates().size)

        val reloaded = templateRepo.getTemplateById("t_upper_a")!!
        assertEquals("routine_upper", reloaded.routineId)
        assertEquals("Upper", reloaded.routineName)
        assertTrue(reloaded.hasVariation)
    }
}
