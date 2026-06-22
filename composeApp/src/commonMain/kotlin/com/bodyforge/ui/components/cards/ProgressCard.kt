package com.bodyforge.ui.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.data.Weights
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

private val AccentOrange = Color(0xFFFF6B35)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF10B981)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

// Distinct colours for up to five simultaneous series.
private val seriesPalette = listOf(AccentBlue, AccentOrange, AccentGreen, AccentPurple, AccentRed)
private const val MAX_SUBJECTS = 5

private enum class Metric(val label: String) {
    EST_1RM("Est. 1RM"),
    TOP_WEIGHT("Top weight"),
    VOLUME("Volume")
}

private enum class Scope(val label: String) {
    ALL("All time"),
    PHASE("By phase"),
    ROUTINE("By routine"),
    SPLIT("By split")
}

private data class Point(val value: Double, val date: LocalDate, val exerciseNote: String, val workoutNote: String, val sets: Int)
private data class Series(val label: String, val color: Color, val points: List<Point>)
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

// One graph for every progress view: pick what to track (several exercises and/or total volume at
// once), the metric, and the scope (all time / a phase / a routine / a split).
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

    // Selected subjects: null = total volume, otherwise an exercise id. Up to MAX_SUBJECTS.
    var selectedSubjects by remember { mutableStateOf<List<String?>>(listOf(null)) }
    var metric by remember { mutableStateOf(Metric.EST_1RM) }
    var scope by remember { mutableStateOf(Scope.ALL) }
    var selectedPhaseId by remember(phases) { mutableStateOf(phases.firstOrNull()?.id) }
    var selectedGroupKey by remember(groups) { mutableStateOf(groups.firstOrNull()?.key) }
    var selectedSplitKey by remember(splits) { mutableStateOf(splits.firstOrNull()?.key) }
    var selectedVariationId by remember(selectedGroupKey) { mutableStateOf<String?>(null) } // null = across variations
    var showTrackDialog by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (seriesIndex, pointIndex)

    // Variations (templates) of the currently selected grouped routine, for the drill-down.
    val routineVariations = remember(selectedGroupKey, templates) {
        val key = selectedGroupKey
        if (key != null && key.startsWith("r:")) {
            val rid = key.removePrefix("r:")
            templates.filter { it.routineId == rid }.sortedBy { it.variationLabel }
        } else emptyList()
    }

    val anyExercise = selectedSubjects.any { it != null }

    val scopedWorkouts = remember(workouts, scope, selectedPhaseId, selectedGroupKey, selectedSplitKey, selectedVariationId, phases, groups, splits) {
        val base = when (scope) {
            Scope.ALL -> workouts
            Scope.PHASE -> {
                val p = phases.firstOrNull { it.id == selectedPhaseId }
                if (p == null) emptyList()
                else workouts.filter { it.startDate >= p.startDate && (p.endDate?.let { e -> it.startDate <= e } ?: true) }
            }
            Scope.ROUTINE -> {
                val g = groups.firstOrNull { it.key == selectedGroupKey }
                val ids = if (selectedVariationId != null) setOf(selectedVariationId) else g?.templateIds ?: emptySet()
                workouts.filter { it.templateId != null && it.templateId in ids }
            }
            Scope.SPLIT -> {
                val g = splits.firstOrNull { it.key == selectedSplitKey }
                if (g == null) emptyList()
                else workouts.filter { it.templateId != null && it.templateId in g.templateIds }
            }
        }
        base.sortedBy { it.startedAt }
    }

    val series = remember(scopedWorkouts, selectedSubjects, metric, exercises) {
        selectedSubjects.mapIndexed { i, subj ->
            val effMetric = if (subj == null) Metric.VOLUME else metric
            val pts = scopedWorkouts.mapNotNull { w ->
                val v = subjectValue(w, subj, effMetric) ?: return@mapNotNull null
                val eiw = w.exercises.firstOrNull { it.exercise.id == subj }
                val setCount = if (subj == null) w.performedSets else (eiw?.performedSets ?: 0)
                // Per-set notes for the day, joined with semicolons, after any exercise-level note.
                val setNotes = eiw?.sets?.mapNotNull { it.notes.trim().ifBlank { null } }?.distinct().orEmpty()
                val combinedNote = (listOf(eiw?.notes?.trim().orEmpty()) + setNotes).filter { it.isNotBlank() }.joinToString("; ")
                Point(v, w.startDate, combinedNote, w.notes, setCount)
            }
            val label = subj?.let { id -> exercises.firstOrNull { it.id == id }?.name ?: "Exercise" } ?: "Total Volume"
            Series(label, seriesPalette[i % seriesPalette.size], pts)
        }
    }

    LaunchedEffect(selectedSubjects, metric, scope, selectedPhaseId, selectedGroupKey, selectedSplitKey, selectedVariationId) { selected = null }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Track", fontSize = 11.sp, color = TextSecondary)
            Button(
                onClick = { showTrackDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    series.joinToString(", ") { it.label },
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        if (anyExercise) {
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
                    groups.forEach { g -> SelectChip(g.label, g.key == selectedGroupKey) { selectedGroupKey = g.key; selectedVariationId = null } }
                }
                if (routineVariations.size > 1) {
                    ChipRow("Variation") {
                        SelectChip("Across variations", selectedVariationId == null) { selectedVariationId = null }
                        routineVariations.forEach { t ->
                            SelectChip(t.variationLabel.ifBlank { t.name }, selectedVariationId == t.id) { selectedVariationId = t.id }
                        }
                    }
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

        val totalPoints = series.sumOf { it.points.size }
        if (totalPoints < 2) {
            Text(
                "Not enough data for this selection yet — log at least two matching workouts.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        } else {
            MultiLineChart(series, selected) { selected = it }
            Spacer(Modifier.height(8.dp))
            if (series.size > 1) {
                Legend(series)
                Spacer(Modifier.height(8.dp))
            }
            val sel = selected?.let { (s, p) -> series.getOrNull(s)?.let { ser -> ser.points.getOrNull(p)?.let { ser to it } } }
            if (sel != null) {
                SelectedPointCard(sel.first.label, sel.first.color, sel.second)
            } else {
                Text("Tap a point to read that day's notes.", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f))
            }
        }
    }

    if (showTrackDialog) {
        TrackFilterDialog(
            exercises = exercises,
            selected = selectedSubjects,
            onDone = { selectedSubjects = it; showTrackDialog = false },
            onDismiss = { showTrackDialog = false }
        )
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
private fun Legend(series: List<Series>) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).background(s.color, CircleShape))
                Text(s.label, fontSize = 11.sp, color = TextSecondary, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun SelectedPointCard(label: String, color: Color, point: Point) {
    Card(backgroundColor = SurfaceColor, shape = RoundedCornerShape(8.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$label · ${formatDate(point.date)} · ${Weights.formatRounded(point.value)} ${Weights.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            Text("${point.sets} ${if (point.sets == 1) "set" else "sets"} this day", fontSize = 12.sp, color = TextSecondary)
            if (point.exerciseNote.isNotBlank()) Text(point.exerciseNote, fontSize = 13.sp, color = TextPrimary)
            if (point.workoutNote.isNotBlank()) Text("Workout: ${point.workoutNote}", fontSize = 12.sp, color = TextSecondary)
            if (point.exerciseNote.isBlank() && point.workoutNote.isBlank()) Text("No notes recorded for this day.", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TrackFilterDialog(
    exercises: List<Exercise>,
    selected: List<String?>,
    onDone: (List<String?>) -> Unit,
    onDismiss: () -> Unit
) {
    val current = remember { mutableStateListOf<String?>().also { it.addAll(selected) } }
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    fun toggle(id: String?) {
        if (current.contains(id)) current.remove(id)
        else if (current.size < MAX_SUBJECTS) current.add(id)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Track (up to $MAX_SUBJECTS)", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Text("${current.size} selected", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search exercises") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (query.isBlank() || "total volume".contains(query, ignoreCase = true)) {
                        SelectRow("Total Volume", current.contains(null)) { toggle(null) }
                    }
                    filtered.forEach { ex -> SelectRow(ex.name, current.contains(ex.id)) { toggle(ex.id) } }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            val result = if (current.isEmpty()) listOf<String?>(null) else current.toList()
                            onDone(result)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) { Text("Done", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun SelectRow(text: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) AccentBlue.copy(alpha = 0.15f) else SurfaceColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = if (checked) TextPrimary else TextSecondary, fontSize = 14.sp, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
        if (checked) Text("✓", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MultiLineChart(series: List<Series>, selected: Pair<Int, Int>?, onSelect: (Pair<Int, Int>?) -> Unit) {
    val flat = remember(series) { series.flatMapIndexed { si, s -> s.points.mapIndexed { pi, p -> Triple(si, pi, p) } } }
    if (flat.isEmpty()) return
    val minDay = flat.minOf { it.third.date.toEpochDays() }
    val maxDay = flat.maxOf { it.third.date.toEpochDays() }
    val dayRange = (maxDay - minDay).let { if (it > 0) it else 1 }
    val maxV = flat.maxOf { it.third.value }
    val minV = flat.minOf { it.third.value }
    val vRange = (maxV - minV).let { if (it > 0.0) it else 1.0 }

    fun nearest(x: Float, y: Float, leftPad: Float, topPad: Float, chartW: Float, chartH: Float): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestD = Float.MAX_VALUE
        flat.forEach { (si, pi, p) ->
            val px = leftPad + chartW * (p.date.toEpochDays() - minDay).toFloat() / dayRange.toFloat()
            val py = topPad + chartH * (1f - ((p.value - minV) / vRange).toFloat())
            val d = (x - px) * (x - px) + (y - py) * (y - py)
            if (d < bestD) { bestD = d; best = si to pi }
        }
        return best
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(series) {
                val pad = 8.dp.toPx()
                val top = 12.dp.toPx()
                detectTapGestures { o -> onSelect(nearest(o.x, o.y, pad, top, size.width - 2 * pad, size.height - 2 * top)) }
            }
            .pointerInput(series) {
                val pad = 8.dp.toPx()
                val top = 12.dp.toPx()
                detectDragGestures(
                    onDragStart = { o -> onSelect(nearest(o.x, o.y, pad, top, size.width - 2 * pad, size.height - 2 * top)) },
                    onDrag = { change, _ -> onSelect(nearest(change.position.x, change.position.y, pad, top, size.width - 2 * pad, size.height - 2 * top)) }
                )
            }
    ) {
        val leftPad = 8.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 12.dp.toPx()
        val botPad = 12.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - botPad

        fun xAt(day: Int): Float = leftPad + chartW * (day - minDay).toFloat() / dayRange.toFloat()
        fun yAt(v: Double): Float = topPad + chartH * (1f - ((v - minV) / vRange).toFloat())

        drawLine(SurfaceColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), strokeWidth = 1.dp.toPx())
        series.forEach { s ->
            val sorted = s.points.sortedBy { it.date.toEpochDays() }
            for (i in 0 until sorted.size - 1) {
                drawLine(s.color, Offset(xAt(sorted[i].date.toEpochDays()), yAt(sorted[i].value)), Offset(xAt(sorted[i + 1].date.toEpochDays()), yAt(sorted[i + 1].value)), strokeWidth = 3.dp.toPx())
            }
            s.points.forEach { p -> drawCircle(s.color, radius = 5.dp.toPx(), center = Offset(xAt(p.date.toEpochDays()), yAt(p.value))) }
        }
        selected?.let { (si, pi) ->
            series.getOrNull(si)?.points?.getOrNull(pi)?.let { p ->
                val cx = xAt(p.date.toEpochDays())
                val cy = yAt(p.value)
                drawLine(TextSecondary, Offset(cx, topPad), Offset(cx, topPad + chartH), strokeWidth = 1.dp.toPx())
                drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(cx, cy))
                drawCircle(series[si].color, radius = 5.dp.toPx(), center = Offset(cx, cy))
            }
        }
    }
}
