package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.presentation.state.SettingsState
import com.bodyforge.data.Weights
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.domain.models.analyzePhase
import com.bodyforge.ui.components.cards.PhaseSection
import com.bodyforge.ui.components.cards.ProgressCard
import com.bodyforge.ui.components.cards.TagExercisesDialog
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Colors
private val AccentOrange = Color(0xFFFF6B35)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

@Composable
fun AnalyticsScreen(listState: LazyListState) {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val templates by SharedWorkoutState.templates.collectAsState()
    val phases by SharedWorkoutState.phases.collectAsState()
    val splitAssignments by SharedWorkoutState.splitAssignments.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    // Expand state for the analytics dropdowns, hoisted here so "Open all" / "Close all" can drive
    // every section at once while each section keeps its own toggle.
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Completed workouts already stay fresh through app start and workout completion. Re-fetching
    // on every visit swapped the list reference out and bounced the scroll position to the top.
    LaunchedEffect(Unit) {
        if (completedWorkouts.isEmpty()) SharedWorkoutState.loadCompletedWorkouts()
        SharedWorkoutState.loadTemplates()
        SharedWorkoutState.loadPhases()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Training phases (periodization) — independent of whether workouts exist yet
        item {
            PhaseSection()
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }
        } else if (completedWorkouts.isEmpty()) {
            item {
                EmptyAnalyticsCard()
            }
        } else {
            val plateaus = computePlateaus(completedWorkouts)
            val sectionKeys = buildList {
                add("progress")
                if (phases.isNotEmpty()) add("phase")
                add("muscle"); add("achievements")
                if (plateaus.isNotEmpty()) add("plateau")
                add("frequency")
            }
            val allOpen = sectionKeys.all { expandedSections[it] == true }

            // Quick Stats Row
            item {
                QuickStatsRow(completedWorkouts)
            }

            // Open all / Close all for the dropdowns below.
            item {
                Text(
                    text = if (allOpen) "Close all" else "Open all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        val target = !allOpen
                        sectionKeys.forEach { expandedSections[it] = target }
                    }
                )
            }

            item {
                ProgressCard(
                    completedWorkouts,
                    templates,
                    phases,
                    splitAssignments,
                    expandedSections["progress"] == true
                ) { expandedSections["progress"] = !(expandedSections["progress"] == true) }
            }

            if (phases.isNotEmpty()) {
                item {
                    PhaseComparisonCard(
                        completedWorkouts,
                        phases,
                        expandedSections["phase"] == true
                    ) { expandedSections["phase"] = !(expandedSections["phase"] == true) }
                }
            }

            item {
                MuscleGroupBalanceCard(
                    completedWorkouts,
                    expandedSections["muscle"] == true
                ) { expandedSections["muscle"] = !(expandedSections["muscle"] == true) }
            }

            item {
                AchievementsCard(
                    completedWorkouts,
                    expandedSections["achievements"] == true
                ) { expandedSections["achievements"] = !(expandedSections["achievements"] == true) }
            }

            if (plateaus.isNotEmpty()) {
                item {
                    PlateauDetectionCard(
                        plateaus,
                        expandedSections["plateau"] == true
                    ) { expandedSections["plateau"] = !(expandedSections["plateau"] == true) }
                }
            }

            item {
                TrainingFrequencyCard(
                    completedWorkouts,
                    templates,
                    phases,
                    splitAssignments,
                    expandedSections["frequency"] == true
                ) { expandedSections["frequency"] = !(expandedSections["frequency"] == true) }
            }
        }
    }
}

// Section card whose body stays hidden until the header is tapped — turns each graph into a
// dropdown so the Analytics page reads as a short list of sections by default.
@Composable
private fun CollapsibleCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(if (expanded) "▾" else "▸", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

private fun fmtDay(d: LocalDate): String =
    "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthNumber.toString().padStart(2, '0')}.${d.year}"

// Groups completed workouts into each training phase by date (via analyzePhase) so blocks of
// training can be compared — the point of the phase feature.
@Composable
private fun PhaseComparisonCard(
    workouts: List<com.bodyforge.domain.models.Workout>,
    phases: List<TrainingPhase>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    CollapsibleCard("Phase Comparison", expanded, onToggle) {
        Text(
            "Workouts are grouped into your phases by date, so you can compare training blocks.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        val ordered = phases.sortedWith(
            compareByDescending<TrainingPhase> { it.isActive }.thenByDescending { it.startDate }
        )
        ordered.forEach { phase ->
            val a = workouts.analyzePhase(phase)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(if (SettingsState.emojiMode) "${phase.phaseType.emoji} ${phase.name}" else phase.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                val range = phase.endDate?.let { "${fmtDay(phase.startDate)} – ${fmtDay(it)}" } ?: "since ${fmtDay(phase.startDate)}"
                Text("${phase.phaseType.displayName} · $range", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                if (a.totalWorkouts == 0) {
                    Text("No workouts logged in this phase yet.", fontSize = 12.sp, color = TextSecondary)
                } else {
                    val avgPerSession = a.totalVolume / a.totalWorkouts
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PhaseStat("${a.totalWorkouts}", "Workouts")
                        PhaseStat("${Weights.formatRounded(a.totalVolume)} ${Weights.unit}", "Volume")
                        PhaseStat("${Weights.formatRounded(avgPerSession)} ${Weights.unit}", "Avg/session")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, softWrap = false)
        Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun EmptyAnalyticsCard() {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No Data Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Complete a few workouts to see your progress analytics",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CurrentPhaseCard() {
    Card(
        backgroundColor = AccentPurple.copy(alpha = 0.8f),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current Phase",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "No active phase • Create your first training phase",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                TextButton(
                    onClick = { /* TODO: Manage phases */ }
                ) {
                    Text("Manage", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(workouts: List<com.bodyforge.domain.models.Workout>) {
    val totalVolume = workouts.sumOf { it.totalVolumePerformed }
    val avgDuration = workouts.mapNotNull { it.durationMinutes }.average().takeIf { !it.isNaN() } ?: 0.0
    val thisWeekWorkouts = workouts.filter {
        val workoutDate = it.startDate
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysDiff = today.toEpochDays() - workoutDate.toEpochDays()
        daysDiff <= 7
    }.size

    // Wider fixed-width cards in a horizontally scrollable row so labels like "Total Volume" and
    // "Avg Duration" are shown in full instead of being squeezed and clipped.
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatCard(value = "${workouts.size}", label = "Workouts", color = AccentBlue)
        QuickStatCard(value = "${Weights.formatRounded(totalVolume)} ${Weights.unit}", label = "Total Volume", color = AccentGreen)
        QuickStatCard(value = "${avgDuration.roundToInt()}m", label = "Avg Duration", color = AccentOrange)
        QuickStatCard(value = "$thisWeekWorkouts", label = "This Week", color = AccentPurple)
    }
}

@Composable
private fun QuickStatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = color.copy(alpha = 0.1f),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun VolumeProgressionCard(workouts: List<com.bodyforge.domain.models.Workout>, expanded: Boolean, onToggle: () -> Unit) {
    CollapsibleCard("Volume Progression", expanded, onToggle) {
        VolumeChart(workouts)
    }
}

@Composable
private fun VolumeChart(workouts: List<com.bodyforge.domain.models.Workout>) {
    // All-time volume as a line chart (oldest -> newest)
    val series = remember(workouts) {
        workouts.sortedBy { it.startedAt }.map { it.totalVolumePerformed }
    }

    if (series.isEmpty()) {
        Text(
            text = "No volume data available",
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        return
    }

    val maxV = series.maxOrNull() ?: 0.0
    val minV = series.minOrNull() ?: 0.0
    val range = (maxV - minV).let { if (it > 0.0) it else 1.0 }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val n = series.size
            val leftPad = 8.dp.toPx()
            val rightPad = 8.dp.toPx()
            val topPad = 12.dp.toPx()
            val botPad = 12.dp.toPx()
            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - botPad

            fun xAt(i: Int): Float = leftPad + if (n == 1) chartW / 2f else chartW * i / (n - 1)
            fun yAt(v: Double): Float = topPad + chartH * (1f - ((v - minV) / range).toFloat())

            drawLine(SurfaceColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), strokeWidth = 1.dp.toPx())
            for (i in 0 until n - 1) {
                drawLine(AccentBlue, Offset(xAt(i), yAt(series[i])), Offset(xAt(i + 1), yAt(series[i + 1])), strokeWidth = 3.dp.toPx())
            }
            series.forEachIndexed { i, v ->
                drawCircle(AccentBlue, radius = 4.dp.toPx(), center = Offset(xAt(i), yAt(v)))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "${series.size} workouts · latest ${Weights.formatRounded(series.last())} ${Weights.unit} · best ${Weights.formatRounded(maxV)} ${Weights.unit}",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun MuscleGroupBalanceCard(workouts: List<com.bodyforge.domain.models.Workout>, expanded: Boolean, onToggle: () -> Unit) {
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val untaggedCustomCount = exercises.count { it.isCustom && it.muscleGroups.isEmpty() }
    var showTagDialog by remember { mutableStateOf(false) }
    CollapsibleCard("Muscle Group Balance", expanded, onToggle) {
        var bySets by remember { mutableStateOf(false) }
        Text(
            if (bySets) "Number of sets per muscle group" else "Number of exercises per muscle group",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Exercises", !bySets) { bySets = false }
            FilterChip("Sets", bySets) { bySets = true }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Per muscle group, count either how many exercises hit it or how many sets were performed.
        val muscleGroupCounts = mutableMapOf<String, Int>()
        workouts.forEach { workout ->
            workout.exercises.forEach { exerciseInWorkout ->
                val amount = if (bySets) exerciseInWorkout.performedSets else 1
                exerciseInWorkout.exercise.muscleGroups.forEach { muscleGroup ->
                    muscleGroupCounts[muscleGroup] = muscleGroupCounts.getOrDefault(muscleGroup, 0) + amount
                }
            }
        }

        val maxCount = muscleGroupCounts.values.maxOrNull() ?: 1
        val topMuscleGroups = muscleGroupCounts.toList().sortedByDescending { it.second }.take(6)

        if (topMuscleGroups.isEmpty()) {
            Text(
                text = "No muscle group data available",
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            topMuscleGroups.forEach { (muscleGroup, count) ->
                MuscleGroupBar(
                    muscleGroup = muscleGroup,
                    count = count,
                    maxCount = maxCount
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Imported/custom exercises saved without muscle groups never reach the bars above; offer to
        // tag them so they start counting.
        if (untaggedCustomCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showTagDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Tag $untaggedCustomCount exercise${if (untaggedCustomCount == 1) "" else "s"} without muscle groups",
                    color = AccentOrange,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }

    if (showTagDialog) {
        TagExercisesDialog(onDismiss = { showTagDialog = false })
    }
}

@Composable
private fun MuscleGroupBar(
    muscleGroup: String,
    count: Int,
    maxCount: Int
) {
    val percentage = (count.toFloat() / maxCount.toFloat())
    val color = when {
        percentage >= 0.8f -> AccentGreen
        percentage >= 0.6f -> AccentOrange
        else -> AccentRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = muscleGroup,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.width(80.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(SurfaceColor, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }

        Text(
            text = "$count",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AchievementsCard(workouts: List<com.bodyforge.domain.models.Workout>, expanded: Boolean, onToggle: () -> Unit) {
    CollapsibleCard("Recent Achievements", expanded, onToggle) {
        if (workouts.isEmpty()) {
            Text(
                text = "Complete workouts to unlock achievements!",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Milestones derived from logged data
            AchievementItem(
                title = "Consistency King",
                description = "${workouts.size} workouts completed"
            )

            if (workouts.size >= 5) {
                AchievementItem(
                    title = "Dedication Unlocked",
                    description = "5+ workouts completed"
                )
            }

            val totalVolume = workouts.sumOf { it.totalVolumePerformed }
            if (totalVolume >= 10000) {
                AchievementItem(
                    title = "Volume Monster",
                    description = "${totalVolume.roundToInt()}kg total volume moved"
                )
            }

            if (workouts.size >= 10) AchievementItem("Double Digits", "10+ workouts completed")
            if (workouts.size >= 25) AchievementItem("Quarter Century", "25+ workouts completed")
            if (workouts.size >= 50) AchievementItem("Half Centurion", "50+ workouts completed")
            if (totalVolume >= 50000) AchievementItem("Heavy Lifter", "50,000kg+ total volume")
            if (totalVolume >= 100000) AchievementItem("Iron Titan", "100,000kg+ total volume")

            val totalSets = workouts.sumOf { it.performedSets }
            if (totalSets >= 100) AchievementItem("Century of Sets", "$totalSets sets logged")
            if (totalSets >= 500) AchievementItem("Set Machine", "$totalSets sets logged")

            val totalReps = workouts.sumOf { w -> w.exercises.sumOf { e -> e.sets.sumOf { it.reps } } }
            if (totalReps >= 1000) AchievementItem("Rep Grinder", "$totalReps total reps performed")

            val heaviestSet = workouts.flatMap { it.exercises }.flatMap { it.sets }.maxOfOrNull { it.weightKg } ?: 0.0
            if (heaviestSet >= 100) AchievementItem("Triple Digits", "Moved ${heaviestSet.roundToInt()}kg in a single set")

            val distinctExercises = workouts.flatMap { it.exercises }.map { it.exercise.id }.distinct().size
            if (distinctExercises >= 15) AchievementItem("Explorer", "$distinctExercises different exercises trained")

            val longestMinutes = workouts.maxOfOrNull { it.durationMinutes ?: 0L } ?: 0L
            if (longestMinutes >= 90) AchievementItem("Marathoner", "$longestMinutes-minute session")
        }
    }
}

@Composable
private fun AchievementItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

private data class PlateauInfo(val name: String, val sessionsSince: Int, val best: Double)

// Exercises with no new estimated-1RM PR in their last several sessions, worst first.
private fun computePlateaus(workouts: List<com.bodyforge.domain.models.Workout>): List<PlateauInfo> {
    val sorted = workouts.sortedBy { it.startedAt }
    val byExercise = LinkedHashMap<String, MutableList<Pair<String, Double>>>()
    sorted.forEach { w ->
        w.exercises.forEach { eiw ->
            val best1rm = eiw.sets
                .filter { !it.isSkipped && it.reps > 0 && it.weightKg > 0.0 }
                .maxOfOrNull { it.weightKg * (1.0 + it.reps / 30.0) }
            if (best1rm != null && best1rm > 0.0) {
                byExercise.getOrPut(eiw.exercise.id) { mutableListOf() }.add(eiw.exercise.name to best1rm)
            }
        }
    }
    return byExercise.values.mapNotNull { sessions ->
        if (sessions.size < 4) return@mapNotNull null
        val series = sessions.map { it.second }
        val best = series.maxOrNull() ?: return@mapNotNull null
        val lastBestIndex = series.indexOfLast { it >= best - 0.001 }
        val since = series.size - 1 - lastBestIndex
        if (since >= 3) PlateauInfo(sessions.last().first, since, best) else null
    }.sortedByDescending { it.sessionsSince }
}

@Composable
private fun PlateauDetectionCard(plateaus: List<PlateauInfo>, expanded: Boolean, onToggle: () -> Unit) {
    CollapsibleCard("Plateau Watch", expanded, onToggle) {
        Text(
            "No new estimated-1RM PR in a while — consider a deload, a rep-range change, or a variation.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        plateaus.take(6).forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("best ${Weights.formatRounded(p.best)} ${Weights.unit} est. 1RM", fontSize = 11.sp, color = TextSecondary)
                }
                Text("${p.sessionsSince} sessions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
            }
        }
    }
}

@Composable
private fun TrainingFrequencyCard(
    workouts: List<com.bodyforge.domain.models.Workout>,
    templates: List<com.bodyforge.domain.models.WorkoutTemplate>,
    phases: List<TrainingPhase>,
    splitAssignments: Map<String, String>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    var showFreqDialog by remember { mutableStateOf(false) }
    CollapsibleCard("Training Frequency", expanded, onToggle) {
        val weeks = 16
        val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
        val counts = remember(workouts) { workouts.groupingBy { it.startDate }.eachCount() }
        val startMonday = remember(today) {
            val mondayThisWeek = today.plus(-(today.dayOfWeek.isoDayNumber - 1), DateTimeUnit.DAY)
            mondayThisWeek.plus(-((weeks - 1) * 7), DateTimeUnit.DAY)
        }
        val last30 = remember(counts, today) {
            val from = today.plus(-29, DateTimeUnit.DAY)
            counts.filterKeys { it >= from }.values.sum()
        }

        Text(
            text = "$last30 ${if (last30 == 1) "session" else "sessions"} in the last 30 days",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // GitHub-style heatmap: one column per week, one cell per day, shaded by workout count.
        Row(modifier = Modifier.clickable { showFreqDialog = true }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (col in 0 until weeks) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (row in 0..6) {
                        val date = startMonday.plus(col * 7 + row, DateTimeUnit.DAY)
                        val count = if (date > today) -1 else (counts[date] ?: 0)
                        val cellColor = when {
                            count < 0 -> Color.Transparent
                            count == 0 -> SurfaceColor.copy(alpha = 0.5f)
                            count == 1 -> AccentGreen.copy(alpha = 0.55f)
                            else -> AccentGreen
                        }
                        Box(modifier = Modifier.size(11.dp).background(cellColor, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Less", fontSize = 10.sp, color = TextSecondary)
            Box(modifier = Modifier.size(10.dp).background(SurfaceColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.size(10.dp).background(AccentGreen.copy(alpha = 0.55f), RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.size(10.dp).background(AccentGreen, RoundedCornerShape(2.dp)))
            Text("More", fontSize = 10.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { showFreqDialog = true },
            colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Count by range, phase, template or split", color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }

    if (showFreqDialog) {
        FrequencyDialog(
            workouts = workouts,
            templates = templates,
            phases = phases,
            splitAssignments = splitAssignments,
            onDismiss = { showFreqDialog = false }
        )
    }
}

private enum class FreqRange(val label: String, val days: Int?) {
    W7("7 days", 7), D30("30 days", 30), D90("90 days", 90), Y365("1 year", 365), ALL("All time", null)
}

@Composable
private fun FreqChipRow(label: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

// Tap-through from the Training Frequency card: how many workouts fall in a chosen window,
// optionally narrowed to a phase, template or split.
@Composable
private fun FrequencyDialog(
    workouts: List<com.bodyforge.domain.models.Workout>,
    templates: List<com.bodyforge.domain.models.WorkoutTemplate>,
    phases: List<TrainingPhase>,
    splitAssignments: Map<String, String>,
    onDismiss: () -> Unit
) {
    var range by remember { mutableStateOf(FreqRange.D30) }
    var phaseId by remember { mutableStateOf<String?>(null) }
    var templateId by remember { mutableStateOf<String?>(null) }
    var split by remember { mutableStateOf<String?>(null) }

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val splitNames = remember(splitAssignments) { splitAssignments.values.filter { it.isNotBlank() }.distinct().sorted() }

    val from = range.days?.let { today.plus(-it, DateTimeUnit.DAY) }
    val phase = phases.firstOrNull { it.id == phaseId }
    val matched = workouts.filter { w ->
        (from == null || w.startDate >= from) &&
        (phase == null || (w.startDate >= phase.startDate && (phase.endDate?.let { e -> w.startDate <= e } ?: true))) &&
        (templateId == null || w.templateId == templateId) &&
        (split == null || (w.templateId != null && splitAssignments[w.templateId] == split))
    }
    val count = matched.size

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Workout frequency", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$count", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                        Text(if (count == 1) "workout" else "workouts", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    FreqChipRow("Time range") {
                        FreqRange.values().forEach { r -> FilterChip(r.label, r == range) { range = r } }
                    }
                    if (phases.isNotEmpty()) {
                        FreqChipRow("Phase") {
                            FilterChip("Any", phaseId == null) { phaseId = null }
                            phases.forEach { p -> FilterChip(if (SettingsState.emojiMode) "${p.phaseType.emoji} ${p.name}" else p.name, phaseId == p.id) { phaseId = p.id } }
                        }
                    }
                    if (templates.isNotEmpty()) {
                        FreqChipRow("Template") {
                            FilterChip("Any", templateId == null) { templateId = null }
                            templates.forEach { t -> FilterChip(t.name, templateId == t.id) { templateId = t.id } }
                        }
                    }
                    if (splitNames.isNotEmpty()) {
                        FreqChipRow("Split") {
                            FilterChip("Any", split == null) { split = null }
                            splitNames.forEach { s -> FilterChip(s, split == s) { split = s } }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePhaseDialog(
    onDismiss: () -> Unit,
    onCreatePhase: (String, String) -> Unit
) {
    var phaseName by remember { mutableStateOf("") }
    var phaseType by remember { mutableStateOf("Strength") }

    val phaseTypes = listOf("Strength", "Hypertrophy", "Cut", "Bulk", "Powerlifting", "Deload")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Training Phase",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = phaseName,
                    onValueChange = { phaseName = it },
                    label = { Text("Phase Name") },
                    placeholder = { Text("e.g., Summer Cut, Strength Block") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextPrimary,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = SurfaceColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Phase Type:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(phaseTypes) { type ->
                        FilterChip(
                            text = type,
                            isSelected = phaseType == type,
                            onClick = { phaseType = type }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreatePhase(phaseName, phaseType) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentPurple),
                enabled = phaseName.isNotBlank(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Create Phase", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        backgroundColor = CardBackground
    )
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) AccentPurple else SurfaceColor,
            contentColor = if (isSelected) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}