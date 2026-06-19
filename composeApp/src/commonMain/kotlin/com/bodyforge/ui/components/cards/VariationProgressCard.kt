package com.bodyforge.ui.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutTemplate
import kotlin.math.roundToInt

private val AccentOrange = Color(0xFFFF6B35)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

private val variationPalette = listOf(AccentBlue, AccentOrange, AccentGreen, AccentPurple, AccentRed)

private enum class ProgressMetric(val label: String) { VOLUME("Volume"), ONE_REP_MAX("Est. 1RM") }
private enum class ProgressView(val label: String) { ACROSS_VARIATIONS("Across variations"), SAME_VARIATION("Same variation") }

private data class RoutineRef(val id: String, val name: String)
private data class ProgressPoint(val variationLabel: String, val value: Double)

// Epley estimated one-rep-max for a single set.
private fun epley(weightKg: Double, reps: Int): Double =
    if (reps <= 0 || weightKg <= 0.0) 0.0 else weightKg * (1.0 + reps / 30.0)

// Metric for one workout. exerciseId == null means whole-workout volume.
// Returns null when there is no usable data (e.g. the exercise was skipped) so the line shows a gap.
private fun workoutMetric(workout: Workout, exerciseId: String?, metric: ProgressMetric): Double? {
    if (exerciseId == null) return workout.totalVolumePerformed.takeIf { it > 0.0 }
    val exerciseInWorkout = workout.exercises.firstOrNull { it.exercise.id == exerciseId } ?: return null
    return when (metric) {
        ProgressMetric.VOLUME -> exerciseInWorkout.totalVolumePerformed.takeIf { it > 0.0 }
        ProgressMetric.ONE_REP_MAX -> exerciseInWorkout.sets
            .filter { !it.isSkipped && it.weightKg > 0.0 && it.reps > 0 }
            .maxOfOrNull { epley(it.weightKg, it.reps) }
    }
}

@Composable
fun VariationProgressCard(
    workouts: List<Workout>,
    templates: List<WorkoutTemplate>
) {
    val routines = remember(templates) {
        templates.filter { it.routineId.isNotBlank() }
            .groupBy { it.routineId }
            .map { (id, group) -> RoutineRef(id, group.first().routineName.ifBlank { "Routine" }) }
            .sortedBy { it.name }
    }

    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Variation Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            if (routines.isEmpty()) {
                Text(
                    "Group templates into a routine with variations (e.g. Upper A / Upper B) and train them to compare progress here.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            } else {
                VariationProgressContent(workouts, templates, routines)
            }
        }
    }
}

@Composable
private fun VariationProgressContent(
    workouts: List<Workout>,
    templates: List<WorkoutTemplate>,
    routines: List<RoutineRef>
) {
    var selectedRoutineId by remember(routines) { mutableStateOf(routines.first().id) }
    var selectedExerciseId by remember(selectedRoutineId) { mutableStateOf<String?>(null) }
    var metric by remember { mutableStateOf(ProgressMetric.VOLUME) }
    var view by remember { mutableStateOf(ProgressView.ACROSS_VARIATIONS) }

    val variationTemplates = remember(templates, selectedRoutineId) {
        templates.filter { it.routineId == selectedRoutineId }.sortedBy { it.variationLabel }
    }
    val templateIds = remember(variationTemplates) { variationTemplates.map { it.id }.toSet() }
    val idToVariation = remember(variationTemplates) { variationTemplates.associate { it.id to it.variationLabel } }
    val routineWorkouts = remember(workouts, templateIds) {
        workouts.filter { it.templateId != null && it.templateId in templateIds }.sortedBy { it.startedAt }
    }
    val exercisesInRoutine = remember(routineWorkouts) {
        routineWorkouts.flatMap { w -> w.exercises.map { it.exercise } }.distinctBy { it.id }
    }
    var selectedVariationId by remember(variationTemplates) { mutableStateOf(variationTemplates.firstOrNull()?.id) }

    val effectiveMetric = if (selectedExerciseId == null) ProgressMetric.VOLUME else metric

    val points = remember(routineWorkouts, selectedExerciseId, effectiveMetric, view, selectedVariationId, idToVariation) {
        val source = if (view == ProgressView.SAME_VARIATION && selectedVariationId != null) {
            routineWorkouts.filter { it.templateId == selectedVariationId }
        } else {
            routineWorkouts
        }
        source.mapNotNull { w ->
            val value = workoutMetric(w, selectedExerciseId, effectiveMetric) ?: return@mapNotNull null
            ProgressPoint(w.templateId?.let { idToVariation[it] } ?: "", value)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (routines.size > 1) {
            ChipRow(label = "Routine") {
                routines.forEach { routine ->
                    SelectChip(routine.name, routine.id == selectedRoutineId) { selectedRoutineId = routine.id }
                }
            }
        }

        ChipRow(label = "Exercise") {
            SelectChip("Total volume", selectedExerciseId == null) { selectedExerciseId = null }
            exercisesInRoutine.forEach { exercise ->
                SelectChip(exercise.name, selectedExerciseId == exercise.id) { selectedExerciseId = exercise.id }
            }
        }

        if (selectedExerciseId != null) {
            ChipRow(label = "Metric") {
                ProgressMetric.values().forEach { option ->
                    SelectChip(option.label, option == metric) { metric = option }
                }
            }
        }

        ChipRow(label = "View") {
            ProgressView.values().forEach { option ->
                SelectChip(option.label, option == view) { view = option }
            }
        }

        if (view == ProgressView.SAME_VARIATION) {
            ChipRow(label = "Variation") {
                variationTemplates.forEach { template ->
                    val label = template.variationLabel.ifBlank { template.name }
                    SelectChip(label, template.id == selectedVariationId) { selectedVariationId = template.id }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (points.isEmpty()) {
            Text(
                "No data for this selection yet. Complete a workout from one of these variations.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        } else {
            val showVariationColors = view == ProgressView.ACROSS_VARIATIONS
            val distinctVariations = points.map { it.variationLabel }.distinct()
            val variationColor: (String) -> Color = { label ->
                if (!showVariationColors) AccentBlue
                else variationPalette[distinctVariations.indexOf(label).coerceAtLeast(0) % variationPalette.size]
            }

            ProgressLineChart(points, variationColor)
            Spacer(Modifier.height(8.dp))
            Text(
                "${points.size} sessions · latest ${points.last().value.roundToInt()} kg · best ${points.maxOf { it.value }.roundToInt()} kg",
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (showVariationColors && distinctVariations.size > 1) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    distinctVariations.forEach { variation ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(variationColor(variation), RoundedCornerShape(50)))
                            Text(variation.ifBlank { "—" }, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) AccentBlue else SurfaceColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ProgressLineChart(points: List<ProgressPoint>, variationColor: (String) -> Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val n = points.size
        val leftPad = 8.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 12.dp.toPx()
        val botPad = 12.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - botPad
        val values = points.map { it.value }
        val maxV = values.maxOrNull() ?: 0.0
        val minV = values.minOrNull() ?: 0.0
        val range = (maxV - minV).let { if (it > 0.0) it else 1.0 }

        fun xAt(i: Int): Float = leftPad + if (n == 1) chartW / 2f else chartW * i / (n - 1)
        fun yAt(value: Double): Float = topPad + chartH * (1f - ((value - minV) / range).toFloat())

        drawLine(
            color = SurfaceColor,
            start = Offset(leftPad, topPad + chartH),
            end = Offset(leftPad + chartW, topPad + chartH),
            strokeWidth = 1.dp.toPx()
        )

        for (i in 0 until n - 1) {
            drawLine(
                color = AccentBlue,
                start = Offset(xAt(i), yAt(points[i].value)),
                end = Offset(xAt(i + 1), yAt(points[i + 1].value)),
                strokeWidth = 3.dp.toPx()
            )
        }

        points.forEachIndexed { i, point ->
            drawCircle(
                color = variationColor(point.variationLabel),
                radius = 5.dp.toPx(),
                center = Offset(xAt(i), yAt(point.value))
            )
        }
    }
}
