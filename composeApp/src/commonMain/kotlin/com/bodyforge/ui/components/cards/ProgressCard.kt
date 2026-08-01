package com.bodyforge.ui.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.bodyforge.ui.components.pagerSafeHorizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bodyforge.ui.theme.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.data.Weights
import com.bodyforge.presentation.state.SettingsState
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.util.formatThousands
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Distinct colours for up to five simultaneous series. A getter so it re-reads the active theme.
private val seriesPalette: List<Color> get() = listOf(AccentBlue, AccentOrange, AccentGreen, AccentPurple, AccentRed)
private const val MAX_SUBJECTS = 5

// A palette color for the given index that is never the line/series color it's paired with, so a
// node can never coincidentally match — cycles through the palette with that one color removed.
private fun pickNodeColor(index: Int, avoid: Color): Color {
    val candidates = seriesPalette.filter { it != avoid }
    if (candidates.isEmpty()) return avoid
    return candidates[index % candidates.size]
}

private enum class Metric(val label: String) {
    EST_1RM("Est. 1RM"),
    TOP_WEIGHT("Top weight"),
    VOLUME("Volume")
}

private enum class Scope(val label: String) {
    SPLIT("By split"),
    ROUTINE("By routine"),
    PHASE("By phase"),
    ALL("All time")
}

// routineLabel/variationLabel identify which routine and which variation within it the point's
// workout came from (e.g. "Upper" / "A"), populated in every scope so nodes can always be colored
// by that: one hue per routine, one shade of that hue per variation. workoutId links the node back
// to its workout so tapping it can open the workout detail.
private data class Point(
    val value: Double,
    val date: LocalDate,
    val exerciseNote: String,
    val workoutNote: String,
    val sets: Int,
    // "8 × 78 · 7 × 78 · 6 × 78 kg" for a single tracked exercise; blank for Total Volume, so the
    // point card can show the sets without opening the whole workout.
    val setsDetail: String = "",
    val routineLabel: String = "",
    val variationLabel: String = "",
    val workoutId: String = ""
)
private data class Series(val label: String, val color: Color, val points: List<Point>)
private data class RoutineGroup(val key: String, val label: String, val templateIds: Set<String>)

private fun epley(weightKg: Double, reps: Int): Double =
    if (reps <= 0 || weightKg <= 0.0) 0.0 else weightKg * (1.0 + reps / 30.0)

// A tracked subject is "exerciseId" (all variations together) or "exerciseId\u001Fvariation"
// (only sessions logged under that variation label).
private const val SUBJECT_VARIATION_SEP = '\u001F'

private fun subjectExerciseId(subject: String): String {
    val i = subject.indexOf(SUBJECT_VARIATION_SEP)
    return if (i >= 0) subject.substring(0, i) else subject
}

private fun subjectVariation(subject: String): String? {
    val i = subject.indexOf(SUBJECT_VARIATION_SEP)
    return if (i >= 0) subject.substring(i + 1) else null
}

private fun findSubject(workout: Workout, subject: String): com.bodyforge.domain.models.ExerciseInWorkout? {
    val exId = subjectExerciseId(subject)
    val variation = subjectVariation(subject)
    return workout.exercises.firstOrNull {
        it.exercise.id == exId && (variation == null || it.variation == variation)
    }
}

// subjectExerciseId == null means the whole-workout total volume.
private fun subjectValue(workout: Workout, subject: String?, metric: Metric): Double? {
    if (subject == null) return workout.totalVolumePerformed.takeIf { it > 0.0 }
    val eiw = findSubject(workout, subject) ?: return null
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
    var scope by remember { mutableStateOf(Scope.SPLIT) }
    var selectedPhaseId by remember(phases) { mutableStateOf(phases.firstOrNull()?.id) }
    var selectedGroupKey by remember(groups) { mutableStateOf(groups.firstOrNull()?.key) }
    var selectedSplitKey by remember(splits) { mutableStateOf(splits.firstOrNull()?.key) }
    // Variation filter by LABEL, not template id: two templates sharing a routine and label
    // (e.g. an old "Lower A" and its copied "Legs A") filter together. null = across variations.
    var selectedVariationLabel by remember(selectedGroupKey) { mutableStateOf<String?>(null) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (seriesIndex, pointIndex)
    var detailWorkoutId by remember { mutableStateOf<String?>(null) }
    var editingWorkout by remember { mutableStateOf<Workout?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Variations (templates) of the currently selected grouped routine, for the drill-down.
    val routineVariations = remember(selectedGroupKey, templates) {
        val key = selectedGroupKey
        if (key != null && key.startsWith("r:")) {
            val rid = key.removePrefix("r:")
            templates.filter { it.routineId == rid }.sortedBy { it.variationLabel }
        } else emptyList()
    }

    val anyExercise = selectedSubjects.any { it != null }

    // Which exercises were actually performed in each template's logged workouts, so the routine /
    // variation / split chips can hide any where the tracked exercise never appeared.
    val exercisesByTemplate = remember(workouts) {
        val map = HashMap<String, MutableSet<String>>()
        workouts.forEach { w ->
            val tid = w.templateId ?: return@forEach
            val set = map.getOrPut(tid) { mutableSetOf() }
            w.exercises.forEach { eiw ->
                if (eiw.sets.any { it.reps > 0 && !it.isSkipped }) set.add(eiw.exercise.id)
            }
        }
        map
    }
    val trackedExerciseIds = remember(selectedSubjects) {
        selectedSubjects.filterNotNull().map { subjectExerciseId(it) }.toSet()
    }
    // Only filter when tracking exercises alone; Total Volume spans everything, so it shows all.
    val filterByExercise = !selectedSubjects.contains(null) && trackedExerciseIds.isNotEmpty()
    val visibleGroups = remember(groups, filterByExercise, trackedExerciseIds, exercisesByTemplate) {
        if (!filterByExercise) groups
        else groups.filter { g -> g.templateIds.any { tid -> exercisesByTemplate[tid]?.any { it in trackedExerciseIds } == true } }
    }
    val visibleSplits = remember(splits, filterByExercise, trackedExerciseIds, exercisesByTemplate) {
        if (!filterByExercise) splits
        else splits.filter { g -> g.templateIds.any { tid -> exercisesByTemplate[tid]?.any { it in trackedExerciseIds } == true } }
    }
    val visibleVariationLabels = remember(routineVariations, filterByExercise, trackedExerciseIds, exercisesByTemplate) {
        val relevant = if (!filterByExercise) routineVariations
            else routineVariations.filter { t -> exercisesByTemplate[t.id]?.any { it in trackedExerciseIds } == true }
        relevant.map { it.variationLabel.ifBlank { it.name } }.distinct()
    }

    // Keep the current selection valid when filtering hides it: jump to the first visible one.
    LaunchedEffect(visibleGroups) {
        if (visibleGroups.none { it.key == selectedGroupKey }) {
            selectedGroupKey = visibleGroups.firstOrNull()?.key
            selectedVariationLabel = null
        }
    }
    LaunchedEffect(visibleSplits) {
        if (visibleSplits.none { it.key == selectedSplitKey }) selectedSplitKey = visibleSplits.firstOrNull()?.key
    }
    LaunchedEffect(visibleVariationLabels) {
        if (selectedVariationLabel != null && selectedVariationLabel !in visibleVariationLabels) selectedVariationLabel = null
    }

    val scopedWorkouts = remember(workouts, scope, selectedPhaseId, selectedGroupKey, selectedSplitKey, selectedVariationLabel, routineVariations, phases, groups, splits) {
        val base = when (scope) {
            Scope.ALL -> workouts
            Scope.PHASE -> {
                val p = phases.firstOrNull { it.id == selectedPhaseId }
                if (p == null) emptyList()
                else workouts.filter { it.startDate >= p.startDate && (p.endDate?.let { e -> it.startDate <= e } ?: true) }
            }
            Scope.ROUTINE -> {
                val g = groups.firstOrNull { it.key == selectedGroupKey }
                val ids = if (selectedVariationLabel != null) {
                    routineVariations
                        .filter { (it.variationLabel.ifBlank { it.name }) == selectedVariationLabel }
                        .map { it.id }
                        .toSet()
                } else g?.templateIds ?: emptySet()
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

    // Template id -> (routine label, variation label), across ALL templates — not just the
    // currently-selected routine — so every point can be tagged regardless of scope. A standalone
    // template (no routineId) is its own single-member "routine" with a blank variation.
    val templateRoutineInfo = remember(templates) {
        templates.associate { t ->
            t.id to if (t.routineId.isNotBlank()) {
                t.routineName.ifBlank { "Routine" } to t.variationLabel.ifBlank { "?" }
            } else {
                t.name to ""
            }
        }
    }

    val series = remember(scopedWorkouts, selectedSubjects, metric, exercises, templateRoutineInfo) {
        selectedSubjects.mapIndexed { i, subj ->
            val effMetric = if (subj == null) Metric.VOLUME else metric
            val pts = scopedWorkouts.mapNotNull { w ->
                val v = subjectValue(w, subj, effMetric) ?: return@mapNotNull null
                val eiw = subj?.let { findSubject(w, it) }
                val setCount = if (subj == null) w.performedSets else (eiw?.performedSets ?: 0)
                // Notes for the day: for a single exercise, its note plus its set notes; for Total
                // Volume, every exercise and set note that day so the aggregate point isn't noteless.
                val combinedNote = if (subj == null) {
                    w.exercises.flatMap { e -> listOf(e.notes.trim()) + e.sets.map { it.notes.trim() } }
                        .filter { it.isNotBlank() }.distinct().joinToString("; ")
                } else {
                    val setNotes = eiw?.sets?.mapNotNull { it.notes.trim().ifBlank { null } }?.distinct().orEmpty()
                    (listOf(eiw?.notes?.trim().orEmpty()) + setNotes).filter { it.isNotBlank() }.joinToString("; ")
                }
                // Reps × weight per performed set, so the point card shows them without a drill-in.
                val setsDetail = if (subj == null || eiw == null) "" else {
                    val performed = eiw.sets.filter { !it.isSkipped && it.reps > 0 }
                    if (performed.isEmpty()) "" else {
                        val bw = eiw.exercise.isBodyweight
                        performed.joinToString("  ·  ") { s ->
                            val w2 = if (bw) { if (s.weightKg > 0) "BW+${Weights.format(s.weightKg)}" else "BW" } else Weights.format(s.weightKg)
                            "${s.reps} × $w2"
                        } + if (bw) "" else " ${Weights.unit}"
                    }
                }
                val (routineLabel, variationLabel) = w.templateId?.let { templateRoutineInfo[it] } ?: ("" to "")
                Point(v, w.startDate, combinedNote, w.notes, setCount, setsDetail, routineLabel, variationLabel, w.id)
            }
            val label = subj?.let { s ->
                val name = exercises.firstOrNull { it.id == subjectExerciseId(s) }?.name ?: "Exercise"
                subjectVariation(s)?.let { "$name ($it)" } ?: name
            } ?: "Total Volume"
            Series(label, seriesPalette[i % seriesPalette.size], pts)
        }
    }

    // Group nodes by routine (a hue) and by variation within that routine (a shade of that hue) —
    // in every scope, whenever there's more than one distinct routine/variation contributing points
    // for a single tracked series. Kept off when tracking several exercises at once, since the line
    // color already carries meaning there and a second color dimension would just be confusing.
    val pointGroups = remember(series) {
        if (series.size != 1) emptyList() else series.first().points
            .map { it.routineLabel to it.variationLabel }
            .filter { it.first.isNotBlank() }
            .distinct()
    }
    val colorByGroup = pointGroups.size >= 2
    val routineOrder = remember(pointGroups) { pointGroups.map { it.first }.distinct() }
    val variationsByRoutine = remember(pointGroups) {
        pointGroups.groupBy({ it.first }, { it.second }).mapValues { (_, vs) -> vs.distinct().sorted() }
    }
    // The palette used for routine hues, with the line's own color pushed to the back so it's only
    // reused as a last resort when there are more routines than spare colors.
    val routineHuePalette = remember(series) {
        val lineColor = series.firstOrNull()?.color
        if (lineColor == null) seriesPalette else seriesPalette.sortedBy { it == lineColor }
    }
    val routineHue: Map<String, Color> = remember(routineOrder, routineHuePalette) {
        routineOrder.mapIndexed { i, r -> r to routineHuePalette[i % routineHuePalette.size] }.toMap()
    }
    // Shades a routine's hue by a variation's position among that routine's variations: lighter for
    // earlier ones, darker for later ones, e.g. "Upper A" light green / "Upper B" dark green.
    val groupColorOf: (String, String) -> Color = { routineLabel, variationLabel ->
        val hue = routineHue[routineLabel]
        if (hue == null) TextSecondary else {
            val variations = variationsByRoutine[routineLabel] ?: listOf(variationLabel)
            if (variations.size <= 1) hue else {
                val idx = variations.indexOf(variationLabel).coerceAtLeast(0)
                val t = idx.toFloat() / (variations.size - 1)
                if (t <= 0.5f) lerp(hue, Color.White, (0.5f - t) * 0.7f)
                else lerp(hue, Color.Black, (t - 0.5f) * 0.5f)
            }
        }
    }

    LaunchedEffect(selectedSubjects, metric, scope, selectedPhaseId, selectedGroupKey, selectedSplitKey, selectedVariationLabel) { selected = null }

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
                    phases.forEach { p -> SelectChip(if (SettingsState.emojiMode) "${p.phaseType.emoji} ${p.name}" else p.name, p.id == selectedPhaseId) { selectedPhaseId = p.id } }
                }
            }
        }
        if (scope == Scope.ROUTINE) {
            if (visibleGroups.isEmpty()) {
                Text(
                    if (filterByExercise) "This exercise hasn't been logged in any routine yet."
                    else "No templates yet — create one on the Templates tab.",
                    fontSize = 12.sp, color = TextSecondary
                )
            } else {
                ChipRow("Routine") {
                    visibleGroups.forEach { g -> SelectChip(g.label, g.key == selectedGroupKey) { selectedGroupKey = g.key; selectedVariationLabel = null } }
                }
                if (visibleVariationLabels.size > 1) {
                    ChipRow("Variation") {
                        SelectChip("Across variations", selectedVariationLabel == null) { selectedVariationLabel = null }
                        visibleVariationLabels.forEach { label ->
                            SelectChip(label, selectedVariationLabel == label) { selectedVariationLabel = label }
                        }
                    }
                }
            }
        }
        if (scope == Scope.SPLIT) {
            if (visibleSplits.isEmpty()) {
                Text(
                    if (filterByExercise) "This exercise hasn't been logged in any split yet."
                    else "No splits yet — assign templates to a split on the Templates tab.",
                    fontSize = 12.sp, color = TextSecondary
                )
            } else {
                ChipRow("Split") {
                    visibleSplits.forEach { g -> SelectChip(g.label, g.key == selectedSplitKey) { selectedSplitKey = g.key } }
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
            MultiLineChart(
                series,
                selected,
                if (colorByGroup) groupColorOf else null,
                onOpen = { (si, pi) ->
                    series.getOrNull(si)?.points?.getOrNull(pi)?.workoutId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { detailWorkoutId = it }
                }
            ) { selected = it }
            Spacer(Modifier.height(8.dp))
            if (colorByGroup) {
                VariationLegend(pointGroups, groupColorOf)
                Spacer(Modifier.height(8.dp))
            } else if (series.size > 1) {
                Legend(series)
                Spacer(Modifier.height(8.dp))
            }
            val selDate = selected?.let { (s, p) -> series.getOrNull(s)?.points?.getOrNull(p)?.date }
            if (selDate != null) {
                // Show every series that has a point on the tapped date, so overlapping nodes are
                // not hidden behind whichever one happened to be nearest.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    series.forEach { ser ->
                        ser.points.firstOrNull { it.date == selDate }?.let { pt ->
                            SelectedPointCard(ser.label, ser.color, pt)
                        }
                    }
                }
            } else {
                Text(
                    "Slide across the chart — it snaps to the nearest session. Tap a node to open that workout.",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showTrackDialog) {
        // Variations logged for each exercise, so they can be tracked as separate subjects.
        val variationsByExercise = remember(workouts) {
            workouts.flatMap { it.exercises }
                .filter { it.variation.isNotBlank() }
                .groupBy({ it.exercise.id }, { it.variation })
                .mapValues { (_, vs) -> vs.distinct().sorted() }
        }
        TrackFilterDialog(
            exercises = exercises,
            variationsByExercise = variationsByExercise,
            selected = selectedSubjects,
            onDone = { selectedSubjects = it; showTrackDialog = false },
            onDismiss = { showTrackDialog = false }
        )
    }

    detailWorkoutId?.let { id ->
        workouts.firstOrNull { it.id == id }?.let { w ->
            WorkoutDetailDialog(
                workout = w,
                allWorkouts = workouts,
                info = w.templateId?.let { templateRoutineInfo[it] },
                onDismiss = { detailWorkoutId = null },
                onEdit = {
                    editingWorkout = w
                    detailWorkoutId = null
                }
            )
        }
    }

    editingWorkout?.let { w ->
        com.bodyforge.ui.components.WorkoutEditorDialog(
            workout = w,
            onDismiss = { editingWorkout = null },
            onSave = { updated ->
                coroutineScope.launch {
                    SharedWorkoutState.workoutRepo.updateWorkout(updated)
                    SharedWorkoutState.loadCompletedWorkouts()
                }
                editingWorkout = null
            }
        )
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable RowScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
        com.bodyforge.ui.components.HScrollIndicator(scrollState)
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
    val scrollState = rememberScrollState()
    Column {
        Row(modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(s.color, CircleShape))
                    Text(s.label, fontSize = 11.sp, color = TextSecondary, maxLines = 1, softWrap = false)
                }
            }
        }
        com.bodyforge.ui.components.HScrollIndicator(scrollState)
    }
}

// Explains the per-node colors when a single line's points span several routines/variations —
// one swatch per (routine, variation) combo, e.g. Upper A / Upper B / Lower A / Lower B.
@Composable
private fun VariationLegend(groups: List<Pair<String, String>>, groupColorOf: (String, String) -> Color) {
    val scrollState = rememberScrollState()
    Column {
        Row(modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            groups.forEach { (routineLabel, variationLabel) ->
                val label = if (variationLabel.isBlank()) routineLabel else "$routineLabel $variationLabel"
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(groupColorOf(routineLabel, variationLabel), CircleShape))
                    Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 1, softWrap = false)
                }
            }
        }
        com.bodyforge.ui.components.HScrollIndicator(scrollState)
    }
}

@Composable
private fun SelectedPointCard(label: String, color: Color, point: Point) {
    var showNotes by remember(point) { mutableStateOf(false) }
    val hasNotes = point.exerciseNote.isNotBlank() || point.workoutNote.isNotBlank()
    Card(backgroundColor = SurfaceColor, shape = RoundedCornerShape(8.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$label · ${formatDate(point.date)} · ${Weights.formatRounded(point.value)} ${Weights.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            if (point.setsDetail.isNotBlank()) {
                Text(point.setsDetail, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            } else {
                Text("${point.sets} ${if (point.sets == 1) "set" else "sets"} this day", fontSize = 12.sp, color = TextSecondary)
            }
            if (hasNotes) {
                Text(
                    if (showNotes) "Hide notes" else "Show notes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.clickable { showNotes = !showNotes }
                )
                if (showNotes) {
                    if (point.exerciseNote.isNotBlank()) Text(point.exerciseNote, fontSize = 13.sp, color = TextPrimary)
                    if (point.workoutNote.isNotBlank()) Text("Workout: ${point.workoutNote}", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// Detail view for one workout, opened by tapping its node in the chart — stats, per-exercise
// volumes with deltas vs the previous comparable session, notes, and a jump into the editor.
@Composable
private fun WorkoutDetailDialog(
    workout: Workout,
    allWorkouts: List<Workout>,
    info: Pair<String, String>?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault()) }
    val volume = workout.totalVolumePerformed
    // Previous session of the same template (or same name, for template-less workouts) for the
    // whole-workout delta.
    val prevSame = remember(workout.id, allWorkouts) {
        allWorkouts.filter { w ->
            w.id != workout.id && w.startedAt < workout.startedAt &&
                (if (workout.templateId != null) w.templateId == workout.templateId else w.name == workout.name)
        }.maxByOrNull { it.startedAt }
    }
    val delta = prevSame?.let { volume - it.totalVolumePerformed }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(18.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                workout.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val variationLabel = info?.second.orEmpty()
                            if (variationLabel.isNotBlank()) {
                                Text(
                                    "Variation $variationLabel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            "${dateFormatter.format(Date(workout.startedAt.epochSeconds * 1000))} · ${workout.durationMinutes ?: 0} min",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        "✕",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .background(SurfaceColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStat("${formatThousands(Weights.toDisplay(volume))} ${Weights.unit}", "Volume", Modifier.weight(1f))
                    DetailStat("${workout.performedSets}", "Sets", Modifier.weight(1f))
                    DetailStat("${workout.durationMinutes ?: 0} min", "Duration", Modifier.weight(1f))
                }
                if (delta != null && prevSame != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        (if (delta >= 0) "▲ +" else "▼ −") +
                            "${formatThousands(Weights.toDisplay(abs(delta)))} ${Weights.unit} total vs ${formatDate(prevSame.startDate)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (delta >= 0) AccentGreen else AccentRed
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workout.exercises.forEach { eiw -> DetailExerciseRow(eiw, workout, allWorkouts) }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange.copy(alpha = 0.14f), contentColor = AccentOrange),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevation(0.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("✎ Edit this workout", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentOrange)
                }
            }
        }
    }
}

@Composable
private fun DetailStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SurfaceColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, softWrap = false)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun DetailExerciseRow(eiw: ExerciseInWorkout, workout: Workout, allWorkouts: List<Workout>) {
    val skipped = eiw.sets.isNotEmpty() && eiw.sets.all { it.isSkipped }
    val swappedFromId = eiw.sets.firstOrNull { it.originalExerciseId != null }?.originalExerciseId
    val swappedFromName = swappedFromId?.let { id ->
        allWorkouts.asSequence().flatMap { it.exercises.asSequence() }
            .firstOrNull { it.exercise.id == id }?.exercise?.name
    }
    val performed = eiw.sets.filter { !it.isSkipped && it.reps > 0 }
    val volume = eiw.totalVolumePerformed
    // Previous session containing this exercise, for the per-exercise delta.
    val prev = remember(workout.id, eiw.exercise.id, allWorkouts) {
        allWorkouts.filter { it.id != workout.id && it.startedAt < workout.startedAt }
            .sortedByDescending { it.startedAt }
            .firstNotNullOfOrNull { w -> w.exercises.firstOrNull { it.exercise.id == eiw.exercise.id && it.totalVolumePerformed > 0.0 } }
    }
    val noteParts = buildList {
        if (eiw.notes.isNotBlank()) add(eiw.notes.trim())
        eiw.sets.forEachIndexed { i, s -> if (s.notes.isNotBlank()) add("Set ${i + 1}: ${s.notes.trim()}") }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                eiw.exercise.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            when {
                skipped -> DetailTag("Skipped", TextSecondary)
                swappedFromId != null -> DetailTag("↔ was ${swappedFromName ?: "another exercise"}", AccentBlue)
            }
        }
        if (skipped) {
            Text(
                "Skipped in this session — shows as a gap in progress charts.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (performed.isNotEmpty()) {
            val setsLine = performed.joinToString("  ·  ") { s ->
                val w = if (eiw.exercise.isBodyweight) {
                    if (s.weightKg > 0) "BW+${Weights.format(s.weightKg)}" else "BW"
                } else Weights.format(s.weightKg)
                "${s.reps} × $w"
            } + if (eiw.exercise.isBodyweight) "" else " ${Weights.unit}"
            Text(setsLine, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            if (volume > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        "${formatThousands(Weights.toDisplay(volume))} ${Weights.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (prev != null) {
                        val d = volume - prev.totalVolumePerformed
                        Text(
                            (if (d > 0) "▲ +" else if (d < 0) "▼ −" else "± ") +
                                "${formatThousands(Weights.toDisplay(abs(d)))} ${Weights.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (d > 0) AccentGreen else if (d < 0) AccentRed else TextSecondary
                        )
                    } else {
                        Text("first time", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
        if (noteParts.isNotEmpty()) {
            Text(
                "✎ " + noteParts.joinToString("  ·  "),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun DetailTag(text: String, color: Color) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
private fun TrackFilterDialog(
    exercises: List<Exercise>,
    variationsByExercise: Map<String, List<String>>,
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
                    filtered.forEach { ex ->
                        SelectRow(ex.name, current.contains(ex.id)) { toggle(ex.id) }
                        // Logged variations of this exercise are separate trackable subjects.
                        variationsByExercise[ex.id]?.forEach { variation ->
                            val subjectId = ex.id + SUBJECT_VARIATION_SEP + variation
                            Box(modifier = Modifier.padding(start = 18.dp)) {
                                SelectRow("${ex.name} ($variation)", current.contains(subjectId)) { toggle(subjectId) }
                            }
                        }
                    }
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
private fun MultiLineChart(
    series: List<Series>,
    selected: Pair<Int, Int>?,
    // When non-null, each node is colored by its point's (routineLabel, variationLabel) instead of
    // its series color; the connecting line still uses the series color so the chart stays readable.
    pointColor: ((String, String) -> Color)? = null,
    // Called when a tap lands on (or very near) a node — used to open that workout's detail.
    onOpen: ((Pair<Int, Int>) -> Unit)? = null,
    onSelect: (Pair<Int, Int>?) -> Unit
) {
    val flat = remember(series) { series.flatMapIndexed { si, s -> s.points.mapIndexed { pi, p -> Triple(si, pi, p) } } }
    if (flat.isEmpty()) return
    val minDay = flat.minOf { it.third.date.toEpochDays() }
    val maxDay = flat.maxOf { it.third.date.toEpochDays() }
    val dayRange = (maxDay - minDay).let { if (it > 0) it else 1 }
    // Each series is scaled to its own min/max so a large-magnitude line (e.g. volume) doesn't
    // flatten the others. Lines may cross — that's expected once every line uses the full height.
    val seriesRanges = remember(series) {
        series.map { s ->
            val values = s.points.map { it.value }
            val mn = values.minOrNull() ?: 0.0
            val mx = values.maxOrNull() ?: 1.0
            mn to (mx - mn).let { if (it > 0.0) it else 1.0 }
        }
    }

    // Selection snaps by horizontal distance only, so scrubbing feels like sliding along the
    // timeline no matter where the finger sits vertically.
    fun nearestByX(x: Float, leftPad: Float, chartW: Float): Pair<Pair<Int, Int>?, Float> {
        var best: Pair<Int, Int>? = null
        var bestD = Float.MAX_VALUE
        flat.forEach { (si, pi, p) ->
            val px = leftPad + chartW * (p.date.toEpochDays() - minDay).toFloat() / dayRange.toFloat()
            val d = abs(x - px)
            if (d < bestD) { bestD = d; best = si to pi }
        }
        return best to bestD
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(series) {
                val pad = 8.dp.toPx()
                detectTapGestures { o ->
                    val (best, dist) = nearestByX(o.x, pad, size.width - 2 * pad)
                    onSelect(best)
                    if (best != null && dist <= 26.dp.toPx()) onOpen?.invoke(best)
                }
            }
            // Horizontal-only drags scrub the selection; vertical drags fall through to the page
            // scroll so the chart never traps it.
            .pointerInput(series) {
                val pad = 8.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { o -> onSelect(nearestByX(o.x, pad, size.width - 2 * pad).first) },
                    onHorizontalDrag = { change, _ -> onSelect(nearestByX(change.position.x, pad, size.width - 2 * pad).first) }
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
        fun yAt(si: Int, v: Double): Float {
            val (mn, range) = seriesRanges[si]
            return topPad + chartH * (1f - ((v - mn) / range).toFloat())
        }

        drawLine(SurfaceColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), strokeWidth = 1.dp.toPx())
        series.forEachIndexed { si, s ->
            val sorted = s.points.sortedBy { it.date.toEpochDays() }
            for (i in 0 until sorted.size - 1) {
                drawLine(s.color, Offset(xAt(sorted[i].date.toEpochDays()), yAt(si, sorted[i].value)), Offset(xAt(sorted[i + 1].date.toEpochDays()), yAt(si, sorted[i + 1].value)), strokeWidth = 3.dp.toPx())
            }
            // Nodes must never read as "the line": a background-colored halo breaks the line under
            // each node, and the node itself always uses a palette color other than this series'
            // own line color (variation-colored nodes already exclude it too).
            val defaultNodeColor = pickNodeColor(0, avoid = s.color)
            s.points.forEach { p ->
                val dotColor = pointColor?.invoke(p.routineLabel, p.variationLabel) ?: defaultNodeColor
                val center = Offset(xAt(p.date.toEpochDays()), yAt(si, p.value))
                drawCircle(CardBackground, radius = 6.dp.toPx(), center = center)
                drawCircle(dotColor, radius = 4.dp.toPx(), center = center)
            }
        }
        selected?.let { (si, pi) ->
            series.getOrNull(si)?.points?.getOrNull(pi)?.let { p ->
                val cx = xAt(p.date.toEpochDays())
                val cy = yAt(si, p.value)
                drawLine(TextSecondary, Offset(cx, topPad), Offset(cx, topPad + chartH), strokeWidth = 1.dp.toPx())
                drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(cx, cy))
                drawCircle(pointColor?.invoke(p.routineLabel, p.variationLabel) ?: pickNodeColor(0, avoid = series[si].color), radius = 5.dp.toPx(), center = Offset(cx, cy))
            }
        }
    }
}
