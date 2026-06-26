package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.bodyforge.ui.components.pagerSafeHorizontalScroll
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bodyforge.ui.theme.*
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
import com.bodyforge.domain.models.BodyMetric
import com.bodyforge.domain.models.analyzePhase
import com.bodyforge.ui.components.EmojiIcon
import kotlinx.coroutines.launch
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

@Composable
fun AnalyticsScreen(listState: LazyListState) {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val templates by SharedWorkoutState.templates.collectAsState()
    val phases by SharedWorkoutState.phases.collectAsState()
    val splitAssignments by SharedWorkoutState.splitAssignments.collectAsState()
    val phaseSplits by SharedWorkoutState.phaseSplits.collectAsState()
    val bodyMetrics by SharedWorkoutState.bodyMetrics.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    val bodyScope = rememberCoroutineScope()

    // Expand state for the analytics dropdowns, hoisted here so "Open all" / "Close all" can drive
    // every section at once while each section keeps its own toggle.
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Completed workouts already stay fresh through app start and workout completion. Re-fetching
    // on every visit swapped the list reference out and bounced the scroll position to the top.
    LaunchedEffect(Unit) {
        if (completedWorkouts.isEmpty()) SharedWorkoutState.loadCompletedWorkouts()
        SharedWorkoutState.loadTemplates()
        SharedWorkoutState.loadPhases()
        SharedWorkoutState.loadBodyMetrics()
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

        // Body data (weight & composition) — independent of whether workouts exist yet
        item {
            BodyDataCard(
                metrics = bodyMetrics,
                onAdd = { m -> bodyScope.launch { SharedWorkoutState.addBodyMetric(m) } },
                onDelete = { id -> bodyScope.launch { SharedWorkoutState.deleteBodyMetric(id) } }
            )
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
                        phaseSplits,
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

// One-decimal sessions-per-week, e.g. 3.5. Kept unit-free since frequency isn't a weight.
private fun formatFrequency(freq: Double): String = ((freq * 10).toInt() / 10.0).toString()

// Groups completed workouts into each training phase by date (via analyzePhase) so blocks of
// training can be compared — the point of the phase feature.
@Composable
private fun PhaseComparisonCard(
    workouts: List<com.bodyforge.domain.models.Workout>,
    phases: List<TrainingPhase>,
    phaseSplits: Map<String, String>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    CollapsibleCard("Phase Comparison", expanded, onToggle) {
        Text(
            "Workouts are grouped into your phases by date, so you can compare training blocks — including which split you ran.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        val ordered = phases.sortedWith(
            compareByDescending<TrainingPhase> { it.isActive }.thenByDescending { it.startDate }
        )
        ordered.forEach { phase ->
            val a = workouts.analyzePhase(phase)
            val split = phaseSplits[phase.id]?.takeIf { it.isNotBlank() }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        if (SettingsState.emojiMode) "${phase.phaseType.emoji} ${phase.name}" else phase.name,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (split != null) {
                        Text(
                            split, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.background(AccentPurple, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("≈ ${formatFrequency(a.weeklyFrequency)} sessions/week", fontSize = 11.sp, color = TextSecondary)
                    val topMuscles = a.volumeByMuscleGroup.entries.sortedByDescending { it.value }.take(4)
                    if (topMuscles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Volume by muscle", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val maxVol = topMuscles.first().value.coerceAtLeast(1.0)
                        topMuscles.forEach { (muscle, vol) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(muscle, fontSize = 11.sp, color = TextPrimary, maxLines = 1, softWrap = false, modifier = Modifier.width(76.dp))
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(CardBackground, RoundedCornerShape(3.dp))) {
                                    Box(modifier = Modifier.fillMaxWidth((vol / maxVol).toFloat().coerceIn(0.02f, 1f)).height(6.dp).background(AccentPurple, RoundedCornerShape(3.dp)))
                                }
                                Text("${Weights.formatRounded(vol)} ${Weights.unit}", fontSize = 10.sp, color = TextSecondary, maxLines = 1, softWrap = false)
                            }
                        }
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

// One-decimal number without a trailing ".0" — used for body-fat percentages.
private fun num1(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else ((v * 10).toInt() / 10.0).toString()

// Body weight & composition: latest value, trend, a weight line graph and recent entries, with a
// dialog to log a new measurement. Independent of workout data.
@Composable
private fun BodyDataCard(
    metrics: List<BodyMetric>,
    onAdd: (BodyMetric) -> Unit,
    onDelete: (String) -> Unit
) {
    var showLog by remember { mutableStateOf(false) }
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmojiIcon("⚖️", Icons.Filled.FitnessCenter, fontSize = 18.sp, iconSize = 20.dp)
                    Text("Body", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Button(onClick = { showLog = true }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue), shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), elevation = ButtonDefaults.elevation(0.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (metrics.isEmpty()) {
                Text("Log your body weight to start tracking your trend over time.", fontSize = 13.sp, color = TextSecondary)
            } else {
                val latest = metrics.last()
                val prev = metrics.getOrNull(metrics.size - 2)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Weights.formatWithUnit(latest.weightKg), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (prev != null) {
                        val diff = latest.weightKg - prev.weightKg
                        val ad = if (diff < 0) -diff else diff
                        val arrow = if (diff > 0) "▲" else if (diff < 0) "▼" else "→"
                        Text("$arrow ${Weights.format(ad)} ${Weights.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    latest.bodyFatPct?.let { Text("Body fat ${num1(it)}%", fontSize = 12.sp, color = TextSecondary) }
                    latest.muscleMassKg?.let { Text("Muscle ${Weights.formatWithUnit(it)}", fontSize = 12.sp, color = TextSecondary) }
                }
                if (metrics.size >= 2) {
                    Spacer(Modifier.height(12.dp))
                    WeightChart(metrics)
                }
                Spacer(Modifier.height(12.dp))
                Text("Recent", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                metrics.asReversed().take(5).forEach { m ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fmtDay(m.date), fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(92.dp))
                        Text(Weights.formatWithUnit(m.weightKg), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("✕", fontSize = 14.sp, color = AccentRed, modifier = Modifier.clickable { onDelete(m.id) }.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
    if (showLog) {
        BodyLogDialog(onDismiss = { showLog = false }, onSave = { m -> onAdd(m); showLog = false })
    }
}

@Composable
private fun WeightChart(metrics: List<BodyMetric>) {
    val weights = metrics.map { it.weightKg }
    val minW = weights.minOrNull() ?: 0.0
    val maxW = weights.maxOrNull() ?: 0.0
    val range = (maxW - minW).takeIf { it > 0.0001 } ?: 1.0
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val n = metrics.size
            val wd = size.width
            val ht = size.height
            val pad = 10f
            fun px(i: Int): Float = if (n <= 1) wd / 2f else pad + (wd - 2 * pad) * i / (n - 1)
            fun py(v: Double): Float = (ht - pad) - (((v - minW) / range).toFloat()) * (ht - 2 * pad)
            for (i in 0 until n - 1) {
                drawLine(AccentBlue, Offset(px(i), py(weights[i])), Offset(px(i + 1), py(weights[i + 1])), strokeWidth = 3f)
            }
            for (i in metrics.indices) {
                drawCircle(AccentBlue, radius = 4f, center = Offset(px(i), py(weights[i])))
            }
        }
        Text(
            "${fmtDay(metrics.first().date)} → ${fmtDay(metrics.last().date)} · ${Weights.formatWithUnit(minW)}–${Weights.formatWithUnit(maxW)}",
            fontSize = 10.sp, color = TextSecondary
        )
    }
}

@Composable
private fun BodyLogDialog(onDismiss: () -> Unit, onSave: (BodyMetric) -> Unit) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var date by remember { mutableStateOf(today) }
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    val weightVal = weight.replace(",", ".").toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Log measurement", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(44.dp))
                    TextButton(onClick = { date = date.plus(-1, DateTimeUnit.DAY) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("◀", color = AccentBlue, fontSize = 16.sp) }
                    Text(fmtDay(date), fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (date < today) date = date.plus(1, DateTimeUnit.DAY) }, enabled = date < today, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("▶", color = if (date < today) AccentBlue else TextSecondary, fontSize = 16.sp) }
                }
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (${Weights.unit})") }, singleLine = true, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentBlue, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = { Text("Body fat % (optional)") }, singleLine = true, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentBlue, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = muscle, onValueChange = { muscle = it }, label = { Text("Muscle mass ${Weights.unit} (optional)") }, singleLine = true, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentBlue, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wv = weightVal
                    if (wv != null && wv > 0) {
                        onSave(
                            BodyMetric.create(
                                date = date,
                                weightKg = Weights.toKg(wv),
                                bodyFatPct = bodyFat.replace(",", ".").toDoubleOrNull(),
                                muscleMassKg = muscle.replace(",", ".").toDoubleOrNull()?.let { Weights.toKg(it) }
                            )
                        )
                    }
                },
                enabled = weightVal != null && weightVal > 0,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
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
    val scrollState = rememberScrollState()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStatCard(value = "${workouts.size}", label = "Workouts", color = AccentBlue)
            QuickStatCard(value = "${Weights.formatRounded(totalVolume)} ${Weights.unit}", label = "Total Volume", color = AccentGreen)
            QuickStatCard(value = "${avgDuration.roundToInt()}m", label = "Avg Duration", color = AccentOrange)
            QuickStatCard(value = "$thisWeekWorkouts", label = "This Week", color = AccentPurple)
        }
        com.bodyforge.ui.components.HScrollIndicator(scrollState)
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
    var showCalendar by remember { mutableStateOf(false) }
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
        Row(modifier = Modifier.clickable { showCalendar = true }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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

    if (showCalendar) {
        WorkoutCalendarDialog(workouts = workouts, onDismiss = { showCalendar = false })
    }
}

// Month calendar reached by tapping the frequency heatmap: workout days are filled, today is ringed,
// and tapping a day lists that day's sessions below.
@Composable
private fun WorkoutCalendarDialog(workouts: List<com.bodyforge.domain.models.Workout>, onDismiss: () -> Unit) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val byDate = remember(workouts) { workouts.groupBy { it.startDate } }
    var firstOfMonth by remember { mutableStateOf(LocalDate(today.year, today.monthNumber, 1)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    val daysInMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY).dayOfMonth
    val leading = firstOfMonth.dayOfWeek.isoDayNumber - 1
    val monthLabel = "${firstOfMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${firstOfMonth.year}"
    val rows = (leading + daysInMonth + 6) / 7

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selected = null; firstOfMonth = firstOfMonth.plus(-1, DateTimeUnit.MONTH) }) {
                        Text("‹", color = AccentOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(monthLabel, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    TextButton(onClick = { selected = null; firstOfMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH) }) {
                        Text("›", color = AccentOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                        Text(d, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                for (rowIdx in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val dayNum = rowIdx * 7 + col - leading + 1
                            if (dayNum in 1..daysInMonth) {
                                val date = LocalDate(firstOfMonth.year, firstOfMonth.monthNumber, dayNum)
                                val count = byDate[date]?.size ?: 0
                                val isToday = date == today
                                val isSelected = date == selected
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(
                                            when {
                                                count > 0 -> AccentGreen.copy(alpha = if (isSelected) 1f else 0.85f)
                                                isSelected -> SurfaceColor
                                                else -> Color.Transparent
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .then(if (isToday) Modifier.border(1.dp, AccentOrange, RoundedCornerShape(8.dp)) else Modifier)
                                        .clickable { selected = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$dayNum",
                                        fontSize = 13.sp,
                                        color = if (count > 0) Color.White else TextPrimary,
                                        fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val sel = selected
                if (sel != null) {
                    val dayWorkouts = byDate[sel].orEmpty()
                    Text(fmtDay(sel), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (dayWorkouts.isEmpty()) {
                        Text("No workout logged", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        dayWorkouts.forEach { w ->
                            val dur = w.durationMinutes?.let { " · ${it}m" } ?: ""
                            Text("• ${w.name}$dur", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private enum class FreqRange(val label: String, val days: Int?) {
    W7("7 days", 7), D30("30 days", 30), D90("90 days", 90), Y365("1 year", 365), ALL("All time", null)
}

@Composable
private fun FreqChipRow(label: String, content: @Composable RowScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
        com.bodyforge.ui.components.HScrollIndicator(scrollState)
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
    var type by remember { mutableStateOf<String?>(null) }

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val splitNames = remember(splitAssignments) { splitAssignments.values.filter { it.isNotBlank() }.distinct().sorted() }

    val from = range.days?.let { today.plus(-it, DateTimeUnit.DAY) }
    val phase = phases.firstOrNull { it.id == phaseId }
    val matched = workouts.filter { w ->
        (from == null || w.startDate >= from) &&
        (phase == null || (w.startDate >= phase.startDate && (phase.endDate?.let { e -> w.startDate <= e } ?: true))) &&
        (templateId == null || w.templateId == templateId) &&
        (split == null || (w.templateId != null && splitAssignments[w.templateId] == split)) &&
        (type == null ||
            (type == "cardio" && w.exercises.any { it.exercise.isCardio }) ||
            (type == "strength" && w.exercises.any { !it.exercise.isCardio }))
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
                    FreqChipRow("Type") {
                        FilterChip("All", type == null) { type = null }
                        FilterChip("Strength", type == "strength") { type = "strength" }
                        FilterChip("Cardio", type == "cardio") { type = "cardio" }
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