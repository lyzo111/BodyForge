package com.bodyforge.ui.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutTemplate
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private val AccentOrange = Color(0xFFFF6B35)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

private enum class Metric(val label: String, val unit: String) {
    EST_1RM("Est. 1RM", "kg"),
    TOP_WEIGHT("Top weight", "kg"),
    VOLUME("Volume", "kg")
}

// Which slice of workouts the graph is drawn from.
private enum class Scope(val label: String) {
    ALL("All time"),
    PHASE("By phase"),
    ROUTINE("By routine"),
    SPLIT("By split")
}

private data class Point(val value: Double, val date: LocalDate, val exerciseNote: String, val workoutNote: String)

// A selectable routine slice: a grouped routine (all its variations) or a single ungrouped template.
private data class RoutineGroup(val key: String, val label: String, val templateIds: Set<String>)

private fun epley(weightKg: Double, reps: Int): Double =
    if (reps <= 0 || weightKg <= 0.0) 0.0 else weightKg * (1.0 + reps / 30.0)

// subjectExerciseId == null means the whole-workout total volume.
private fun subjectValue(workout: Workout, subjectExerciseId: String?, metric: Metric): Double? {
    if (subjectExerciseId == null) return workout.totalVolumePerformed.takeIf { it > 0.0 }
    val eiw = workout.exercises.firstOrNull { it.exercise.id == subjectExerciseId } ?: return null
    val sets = eiw.sets.filter { !it.isSkipped && it.reps > 0 }
    if (sets.isEmpty()) return null
    return when (metric) {
        Metric.EST_1RM -> sets.filter { it.weightKg > 0.0 }.maxOfOrNull { epley(it.weightKg, it.reps) }
        Metric.TOP_WEIGHT -> sets.maxOfOrNull { it.weightKg }?.takeIf { it > 0.0 }
        Metric.VOLUME -> eiw.totalVolumePerformed.takeIf { it > 0.0 }
    }
}

private fun routineGroups(templates: List<WorkoutTemplate>): List<RoutineGroup> {
    val grouped = templates.filter { it.routineId.isNotBlank() }
        .groupBy { it.routineId }
        .map { (rid, ts) -> RoutineGroup("r:$rid", ts.first().routineName.ifBlank { "Routine" }, ts.map { it.id }.toSet()) }
    // Ungrouped templates each become their own selectable group so their workouts are tracked too.
    val ungrouped = templates.filter { it.routineId.isBlank() }
        .map { RoutineGroup("t:${it.id}", it.name, setOf(it.id)) }
    return (grouped + ungrouped).sortedBy { it.label }
}

private fun splitGroups(templates: List<WorkoutTemplate>, assignments: Map<String, String>): List<RoutineGroup> {
    return templates.mapNotNull { t -> assignments[t.id]?.takeIf { it.isNotBlank() }?.let { it to t.id } }
        .groupBy({ it.first }, { it.second })
        .map { (split, ids) -> RoutineGroup("s:$split", split, ids.toSet()) }
        .sortedBy { it.label }
}

private fun formatDate(d: LocalDate): String =
    "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthNumber.toString().padStart(2, '0')}.${d.year}"

// One graph for every progress view: pick what to track (an exercise or total volume), the metric,
// and the scope (all time / a phase / a routine). Replaces the separate exercise, variation and
// volume cards.
@Composable
fun ProgressCard(
    workouts: List<Workout>,
    templates: List<WorkoutTemplate>,
    phases: List<TrainingPhase>,
    splitAssignments: Map<String, String>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(if (expanded) "▾" else "▸", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                ProgressContent(workouts, templates, phases, splitAssignments)
            }
        }
    }
}

@Composable
private fun ProgressContent(
    workouts: List<Workout>,
    templates: List<WorkoutTemplate>,
    phases: List<TrainingPhase>,
    splitAssignments: Map<String, String>
) {
    val exercises = remember(workouts) {
        workouts.sortedByDescending { it.startedAt }
            .flatMap { w -> w.exercises.map { it.exercise } }
            .distinctBy { it.id }
    }
    val groups = remember(templates) { routineGroups(templates) }
    val splits = remember(templates, splitAssignments) { splitGroups(templates, splitAssignments) }

    var subjectId by remember { mutableStateOf<String?>(null) } // null => total volume
    var metric by remember { mutableStateOf(Metric.EST_1RM) }
    var scope by remember { mutableStateOf(Scope.ALL) }
    var selectedPhaseId by remember(phases) { mutableStateOf(phases.firstOrNull()?.id) }
    var selectedGroupKey by remember(groups) { mutableStateOf(groups.firstOrNull()?.key) }
    var selectedSplitKey by remember(splits) { mutableStateOf(splits.firstOrNull()?.key) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val effectiveMetric = if (subjectId == null) Metric.VOLUME else metric

    val scopedWorkouts = remember(workouts, scope, selectedPhaseId, selectedGroupKey, selectedSplitKey, phases, groups, splits) {
        val base = when (scope) {
            Scope.ALL -> workouts
            Scope.PHASE -> {
                val p = phases.firstOrNull { it.id == selectedPhaseId }
                if (p == null) emptyList()
                else workouts.filter { it.startDate >= p.startDate && (p.endDate?.let { e -> it.startDate <= e } ?: true) }
            }
            Scope.ROUTINE -> {
                val g = groups.firstOrNull { it.key == selectedGroupKey }
                if (g == null) emptyList()
                else workouts.filter { it.templateId != null && it.templateId in g.templateIds }
            }
            Scope.SPLIT -> {
                val g = splits.firstOrNull { it.key == selectedSplitKey }
                if (g == null) emptyList()
                else workouts.filter { it.templateId != null && it.templateId in g.templateIds }
            }
        }
        base.sortedBy { it.startedAt }
    }

    val points = remember(scopedWorkouts, subjectId, effectiveMetric) {
        scopedWorkouts.mapNotNull { w ->
            val v = subjectValue(w, subjectId, effectiveMetric) ?: return@mapNotNull null
            val note = w.exercises.firstOrNull { it.exercise.id == subjectId }?.notes ?: ""
            Point(v, w.startDate, note, w.notes)
        }
    }

    LaunchedEffect(subjectId, effectiveMetric, scope, selectedPhaseId, selectedGroupKey, selectedSplitKey) { selectedIndex = null }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipRow("Track") {
            SelectChip("Total Volume", subjectId == null) { subjectId = null }
            exercises.forEach { ex -> SelectChip(ex.name, subjectId == ex.id) { subjectId = ex.id } }
        }
        if (subjectId != null) {
            ChipRow("Metric") {
                Metric.values().forEach { option -> SelectChip(option.label, option == metric) { metric = option } }
            }
        }
        ChipRow("Scope") {
            Scope.values().forEach { option -> SelectChip(option.label, option == scope) { scope = option } }
        }
        if (scope == Scope.PHASE) {
            if (phases.isEmpty()) {
                Text("No phases yet — start one on the Training Phase card.", fontSize = 12.sp, color = TextSecondary)
            } else {
                ChipRow("Phase") {
                    phases.forEach { p -> SelectChip("${p.phaseType.emoji} ${p.name}", p.id == selectedPhaseId) { selectedPhaseId = p.id } }
                }
            }
        }
        if (scope == Scope.ROUTINE) {
            if (groups.isEmpty()) {
                Text("No templates yet — create one on the Templates tab.", fontSize = 12.sp, color = TextSecondary)
            } else {
                ChipRow("Routine") {
                    groups.forEach { g -> SelectChip(g.label, g.key == selectedGroupKey) { selectedGroupKey = g.key } }
                }
            }
        }
        if (scope == Scope.SPLIT) {
            if (splits.isEmpty()) {
                Text("No splits yet — assign templates to a split on the Templates tab.", fontSize = 12.sp, color = TextSecondary)
            } else {
                ChipRow("Split") {
                    splits.forEach { g -> SelectChip(g.label, g.key == selectedSplitKey) { selectedSplitKey = g.key } }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (points.size < 2) {
            Text(
                "Not enough data for this selection yet — log at least two matching workouts.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        } else {
            LineChart(points, selectedIndex) { selectedIndex = it }
            Spacer(Modifier.height(8.dp))
            val unit = effectiveMetric.unit
            val selected = selectedIndex?.let { points.getOrNull(it) }
            if (selected != null) {
                SelectedPointCard(selected, unit)
            } else {
                val values = points.map { it.value }
                val change = values.last() - values.first()
                val sign = if (change >= 0) "+" else ""
                val best = values.maxOrNull() ?: 0.0
                Text(
                    "${points.size} sessions · latest ${values.last().roundToInt()} $unit · best ${best.roundToInt()} $unit · $sign${change.roundToInt()} $unit overall",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text("Tap a point to read that day's notes.", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f))
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
private fun SelectedPointCard(point: Point, unit: String) {
    Card(backgroundColor = SurfaceColor, shape = RoundedCornerShape(8.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${formatDate(point.date)} · ${point.value.roundToInt()} $unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
            if (point.exerciseNote.isNotBlank()) Text(point.exerciseNote, fontSize = 13.sp, color = TextPrimary)
            if (point.workoutNote.isNotBlank()) Text("Workout: ${point.workoutNote}", fontSize = 12.sp, color = TextSecondary)
            if (point.exerciseNote.isBlank() && point.workoutNote.isBlank()) Text("No notes recorded for this day.", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

private fun nearestNodeIndex(x: Float, n: Int, width: Float, pad: Float): Int {
    if (n <= 1) return 0
    val chartW = width - 2 * pad
    return (0 until n).minByOrNull { abs(x - (pad + chartW * it / (n - 1))) } ?: 0
}

@Composable
private fun LineChart(points: List<Point>, selectedIndex: Int?, onSelect: (Int) -> Unit) {
    val values = points.map { it.value }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(points.size) {
                val pad = 8.dp.toPx()
                detectTapGestures { offset -> onSelect(nearestNodeIndex(offset.x, points.size, size.width.toFloat(), pad)) }
            }
            .pointerInput(points.size) {
                val pad = 8.dp.toPx()
                detectDragGestures(
                    onDragStart = { offset -> onSelect(nearestNodeIndex(offset.x, points.size, size.width.toFloat(), pad)) },
                    onDrag = { change, _ -> onSelect(nearestNodeIndex(change.position.x, points.size, size.width.toFloat(), pad)) }
                )
            }
    ) {
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

        drawLine(SurfaceColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), strokeWidth = 1.dp.toPx())
        for (i in 0 until n - 1) {
            drawLine(AccentBlue, Offset(xAt(i), yAt(values[i])), Offset(xAt(i + 1), yAt(values[i + 1])), strokeWidth = 3.dp.toPx())
        }
        values.forEachIndexed { i, v ->
            if (i == selectedIndex) {
                drawLine(TextSecondary, Offset(xAt(i), topPad), Offset(xAt(i), topPad + chartH), strokeWidth = 1.dp.toPx())
                drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(xAt(i), yAt(v)))
                drawCircle(AccentOrange, radius = 5.dp.toPx(), center = Offset(xAt(i), yAt(v)))
            } else {
                drawCircle(AccentOrange, radius = 5.dp.toPx(), center = Offset(xAt(i), yAt(v)))
            }
        }
    }
}
