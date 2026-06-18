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
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.Workout
import kotlin.math.roundToInt

private val AccentOrange = Color(0xFFFF6B35)
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

private enum class ExerciseMetric(val label: String, val unit: String) {
    EST_1RM("Est. 1RM", "kg"),
    TOP_WEIGHT("Top weight", "kg"),
    VOLUME("Volume", "kg")
}

// Epley estimated one-rep-max for a single set.
private fun epley(weightKg: Double, reps: Int): Double =
    if (reps <= 0 || weightKg <= 0.0) 0.0 else weightKg * (1.0 + reps / 30.0)

// Per-workout value for the selected metric, or null when the exercise has no usable data
// (e.g. it was skipped) so that workout simply drops out of the line.
private fun metricValue(exercise: ExerciseInWorkout, metric: ExerciseMetric): Double? {
    val sets = exercise.sets.filter { !it.isSkipped && it.reps > 0 }
    if (sets.isEmpty()) return null
    return when (metric) {
        ExerciseMetric.EST_1RM -> sets.filter { it.weightKg > 0.0 }.maxOfOrNull { epley(it.weightKg, it.reps) }
        ExerciseMetric.TOP_WEIGHT -> sets.maxOfOrNull { it.weightKg }?.takeIf { it > 0.0 }
        ExerciseMetric.VOLUME -> exercise.totalVolumePerformed.takeIf { it > 0.0 }
    }
}

@Composable
fun ExerciseProgressCard(workouts: List<Workout>) {
    // Exercises that appear in completed workouts, most recently trained first.
    val exercises = remember(workouts) {
        workouts.sortedByDescending { it.startedAt }
            .flatMap { w -> w.exercises.map { it.exercise } }
            .distinctBy { it.id }
    }

    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🏋️ Exercise Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            if (exercises.isEmpty()) {
                Text("Complete a workout to track progress per exercise.", fontSize = 13.sp, color = TextSecondary)
            } else {
                ExerciseProgressContent(workouts, exercises)
            }
        }
    }
}

@Composable
private fun ExerciseProgressContent(workouts: List<Workout>, exercises: List<Exercise>) {
    var selectedExerciseId by remember(exercises) { mutableStateOf(exercises.first().id) }
    var metric by remember { mutableStateOf(ExerciseMetric.EST_1RM) }

    val values = remember(workouts, selectedExerciseId, metric) {
        workouts.sortedBy { it.startedAt }.mapNotNull { w ->
            val exercise = w.exercises.firstOrNull { it.exercise.id == selectedExerciseId } ?: return@mapNotNull null
            metricValue(exercise, metric)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipRow(label = "Exercise") {
            exercises.forEach { exercise ->
                SelectChip(exercise.name, exercise.id == selectedExerciseId) { selectedExerciseId = exercise.id }
            }
        }
        ChipRow(label = "Metric") {
            ExerciseMetric.values().forEach { option ->
                SelectChip(option.label, option == metric) { metric = option }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (values.size < 2) {
            Text(
                "Not enough data yet — log this exercise in at least two workouts.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        } else {
            ExerciseLineChart(values)
            Spacer(Modifier.height(8.dp))
            val change = values.last() - values.first()
            val sign = if (change >= 0) "+" else ""
            val best = values.maxOrNull() ?: 0.0
            Text(
                "${values.size} sessions · latest ${values.last().roundToInt()} ${metric.unit} · best ${best.roundToInt()} ${metric.unit} · $sign${change.roundToInt()} ${metric.unit} overall",
                fontSize = 12.sp,
                color = TextSecondary
            )
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
private fun ExerciseLineChart(values: List<Double>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val n = values.size
        val leftPad = 8.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 12.dp.toPx()
        val botPad = 12.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - botPad
        val maxV = values.maxOrNull() ?: 0.0
        val minV = values.minOrNull() ?: 0.0
        val range = (maxV - minV).let { if (it > 0.0) it else 1.0 }

        fun xAt(i: Int): Float = leftPad + if (n == 1) chartW / 2f else chartW * i / (n - 1)
        fun yAt(v: Double): Float = topPad + chartH * (1f - ((v - minV) / range).toFloat())

        drawLine(
            color = SurfaceColor,
            start = Offset(leftPad, topPad + chartH),
            end = Offset(leftPad + chartW, topPad + chartH),
            strokeWidth = 1.dp.toPx()
        )
        for (i in 0 until n - 1) {
            drawLine(
                color = AccentBlue,
                start = Offset(xAt(i), yAt(values[i])),
                end = Offset(xAt(i + 1), yAt(values[i + 1])),
                strokeWidth = 3.dp.toPx()
            )
        }
        values.forEachIndexed { i, v ->
            drawCircle(color = AccentOrange, radius = 5.dp.toPx(), center = Offset(xAt(i), yAt(v)))
        }
    }
}
