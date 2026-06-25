package com.bodyforge.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.bodyforge.ui.components.pagerSafeHorizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodyforge.data.Weights
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.presentation.viewmodel.WorkoutViewModel
import com.bodyforge.ui.PlatformBackHandler
import com.bodyforge.ui.components.cards.CreateExerciseDialog
import com.bodyforge.ui.components.inputs.BodyweightInput
import com.bodyforge.ui.components.EmojiIcon
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

// Colors matching the screenshot
private val AccentOrange = Color(0xFFFF6B35)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)
private val ButtonRed = Color(0xFF8B4513).copy(alpha = 0.8f)
private val ButtonGreen = Color(0xFF2E7D32).copy(alpha = 0.8f)
private val SelectedGreen = Color(0xFF065F46)

@Composable
fun WorkoutScreen(listState: LazyListState, onGoToTemplates: () -> Unit = {}) {
    val viewModel: WorkoutViewModel = viewModel()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val bodyweight by SharedWorkoutState.bodyweight.collectAsState()
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeWorkout != null) {
            ActiveWorkoutView(
                workout = activeWorkout!!,
                bodyweight = bodyweight,
                isLoading = isLoading,
                viewModel = viewModel,
                listState = listState
            )
        } else {
            QuickStartView(
                isLoading = isLoading,
                viewModel = viewModel,
                onGoToTemplates = onGoToTemplates
            )
        }
    }
}

@Composable
private fun QuickStartView(
    isLoading: Boolean,
    viewModel: WorkoutViewModel,
    onGoToTemplates: () -> Unit
) {
    var showQuickWorkoutFlow by remember { mutableStateOf(false) }

    if (showQuickWorkoutFlow) {
        QuickWorkoutFlow(
            onBack = { showQuickWorkoutFlow = false },
            onStartWorkout = { selectedExercises ->
                viewModel.startQuickWorkout(selectedExercises)
                showQuickWorkoutFlow = false
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Ready to workout?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose how you want to start your training session",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            item {
                QuickStartCard(
                    title = "Quick Workout",
                    subtitle = "Select exercises & go",
                    accent = AccentOrange,
                    onClick = { showQuickWorkoutFlow = true },
                    enabled = !isLoading
                )
            }

            item {
                QuickStartCard(
                    title = "From Template",
                    subtitle = "Use existing routine",
                    accent = AccentBlue,
                    onClick = onGoToTemplates,
                    enabled = !isLoading
                )
            }

            if (completedWorkouts.isNotEmpty()) {
                item {
                    ReadyActivitySummary(completedWorkouts)
                }
            }

            item {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Loading...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Small glanceable activity strip under the start options, so the otherwise-empty start screen
// shows this week's count and the most recent session at a glance.
@Composable
private fun ReadyActivitySummary(completedWorkouts: List<Workout>) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val weekAgo = today.plus(-6, DateTimeUnit.DAY)
    val thisWeek = completedWorkouts.count { it.startDate >= weekAgo }
    val last = completedWorkouts.maxByOrNull { it.startedAt }

    Card(
        backgroundColor = CardBackground,
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("This week", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "$thisWeek ${if (thisWeek == 1) "workout" else "workouts"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            }
            last?.let { w ->
                Spacer(modifier = Modifier.height(6.dp))
                val name = if (w.exercises.isNotEmpty() && w.exercises.all { it.exercise.isCardio }) "Cardio" else w.name
                val agoDays = (today.toEpochDays() - w.startDate.toEpochDays()).toInt()
                val ago = when (agoDays) {
                    0 -> "today"
                    1 -> "yesterday"
                    else -> "$agoDays days ago"
                }
                Text("Last: $name · $ago", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun QuickStartCard(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveWorkoutView(
    workout: Workout,
    bodyweight: Double,
    isLoading: Boolean,
    viewModel: WorkoutViewModel,
    listState: LazyListState
) {
    val hasBodyweightExercises = workout.exercises.any { it.exercise.isBodyweight }
    val availableExercises by SharedWorkoutState.exercises.collectAsState()
    val scope = rememberCoroutineScope()
    val baseOffset = 1

    // Rest timer state is owned by SharedWorkoutState so it keeps running and stays visible when
    // the user leaves the Workout tab and returns.
    val restRemaining by SharedWorkoutState.restRemainingSeconds.collectAsState()
    val restTotal by SharedWorkoutState.restTotalSeconds.collectAsState()
    val restEndsAtMillis by SharedWorkoutState.restEndsAtMillis.collectAsState()
    // Per-exercise collapse state for the active workout, hoisted so it survives list recycling.
    val expandedExercises = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = Modifier.fillMaxSize()) {
        if (workout.exercises.size > 1) {
            ExerciseJumpBar(
                exercises = workout.exercises,
                onJump = { index -> scope.launch { listState.animateScrollToItem(baseOffset + index) } }
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            WorkoutHeaderCard(
                workout = workout,
                onFinishWorkout = { viewModel.completeWorkout() },
                onStopWorkout = { viewModel.stopWorkout() },
                isLoading = isLoading
            )
        }

        items(workout.exercises) { exerciseInWorkout ->
            val isSkipped = exerciseInWorkout.sets.isNotEmpty() && exerciseInWorkout.sets.all { it.isSkipped }
            val exId = exerciseInWorkout.exercise.id
            ActiveExerciseCard(
                exerciseInWorkout = exerciseInWorkout,
                bodyweight = bodyweight,
                availableExercises = availableExercises,
                onUpdateSet = { setId, reps, weight, completed ->
                    viewModel.updateSet(exerciseInWorkout.exercise.id, setId, reps, weight, completed)
                    if (completed == true) {
                        // Compound lifts (>= 2 muscle groups) rest longer than isolation work.
                        val isCompound = exerciseInWorkout.exercise.muscleGroups.size >= 2
                        val rest = if (isCompound) com.bodyforge.data.AppSettings.compoundRestSeconds
                                   else com.bodyforge.data.AppSettings.isolationRestSeconds
                        SharedWorkoutState.startRest(rest)
                    }
                },
                onAddSet = {
                    viewModel.addSetToExercise(exerciseInWorkout.exercise.id)
                },
                onRemoveSet = {
                    if (exerciseInWorkout.sets.size > 1) {
                        viewModel.removeSetFromExercise(
                            exerciseInWorkout.exercise.id,
                            exerciseInWorkout.sets.last().id
                        )
                    }
                },
                onSkipToggle = {
                    if (isSkipped) viewModel.resumeExercise(exerciseInWorkout.exercise.id)
                    else viewModel.skipExercise(exerciseInWorkout.exercise.id)
                },
                onSubstitute = { newExercise ->
                    viewModel.substituteExercise(exerciseInWorkout.exercise.id, newExercise)
                },
                onNotesChange = { viewModel.updateExerciseNotes(exerciseInWorkout.exercise.id, it) },
                expanded = expandedExercises[exId] ?: true,
                onToggleExpand = { expandedExercises[exId] = !(expandedExercises[exId] ?: true) },
                onBodyweightChange = { SharedWorkoutState.updateBodyweight(it) },
                onUpdateMetric = { setId, key, value -> viewModel.updateSetMetric(exerciseInWorkout.exercise.id, setId, key, value) }
            )
            }
        }

        if (restEndsAtMillis > 0L) {
            RestTimerBar(
                endsAtMillis = restEndsAtMillis,
                totalSeconds = restTotal,
                onAddTime = { SharedWorkoutState.addRestTime(15) },
                onSkip = { SharedWorkoutState.skipRest() }
            )
        }
    }
}

@Composable
private fun RestTimerBar(
    endsAtMillis: Long,
    totalSeconds: Int,
    onAddTime: () -> Unit,
    onSkip: () -> Unit
) {
    // Recompute from the wall clock every frame, so the bar drains perfectly smoothly and stays
    // correct even if the app was paused (screen locked) during the rest.
    var nowMillis by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(endsAtMillis) {
        while (endsAtMillis > 0L) {
            withFrameMillis { }
            nowMillis = Clock.System.now().toEpochMilliseconds()
        }
    }
    val totalMillis = (totalSeconds * 1000L).coerceAtLeast(1L)
    val remainingMillis = (endsAtMillis - nowMillis).coerceAtLeast(0L)
    val progress = (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    val secondsLeft = ((remainingMillis + 999L) / 1000L).toInt()
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    Surface(color = SurfaceColor, elevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ Rest  $minutes:${seconds.toString().padStart(2, '0')}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseActionChip(text = "+15s", color = AccentBlue, onClick = onAddTime)
                    ExerciseActionChip(text = "Skip", color = AccentRed, onClick = onSkip)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = AccentOrange,
                backgroundColor = CardBackground
            )
        }
    }
}

@Composable
private fun ExerciseJumpBar(
    exercises: List<ExerciseInWorkout>,
    onJump: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            exercises.forEachIndexed { index, exerciseInWorkout ->
                Box(
                    modifier = Modifier
                        .background(SurfaceColor, RoundedCornerShape(16.dp))
                        .clickable { onJump(index) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${index + 1}. ${exerciseInWorkout.exercise.name}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Purely visual scroll-position indicator: a track with a thumb whose width and offset
        // reflect how far the jump buttons are scrolled. Shown only when the row overflows.
        if (scrollState.maxValue > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceColor.copy(alpha = 0.4f))
            ) {
                val density = LocalDensity.current
                val trackPx = with(density) { maxWidth.toPx() }
                val content = trackPx + scrollState.maxValue
                val thumbFraction = (trackPx / content).coerceIn(0.2f, 1f)
                val progress = scrollState.value.toFloat() / scrollState.maxValue
                val thumbWidth = with(density) { (trackPx * thumbFraction).toDp() }
                val thumbOffset = with(density) { ((trackPx - trackPx * thumbFraction) * progress).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .width(thumbWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentBlue.copy(alpha = 0.8f))
                )
            }
        }
    }
}

@Composable
private fun WorkoutHeaderCard(
    workout: Workout,
    onFinishWorkout: () -> Unit,
    onStopWorkout: () -> Unit,
    isLoading: Boolean
) {
    var showStopConfirm by remember { mutableStateOf(false) }

    Card(
        backgroundColor = AccentBlue,
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name.ifEmpty { "Workout" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${workout.exercises.size} exercises",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showStopConfirm = true },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(text = "Stop", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onFinishWorkout,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White,
                        contentColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = if (isLoading) "..." else "Finish",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("Stop workout?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "This discards the current workout without saving it to your history.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirm = false
                        onStopWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) {
                    Text("Keep going", color = TextSecondary)
                }
            },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun ActiveExerciseCard(
    exerciseInWorkout: ExerciseInWorkout,
    bodyweight: Double,
    availableExercises: List<Exercise>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onSkipToggle: () -> Unit,
    onSubstitute: (Exercise) -> Unit,
    onNotesChange: (String) -> Unit,
    onBodyweightChange: (Double) -> Unit,
    onUpdateMetric: (String, String, Double?) -> Unit
) {
    val exercise = exerciseInWorkout.exercise
    val isSkipped = exerciseInWorkout.sets.isNotEmpty() && exerciseInWorkout.sets.all { it.isSkipped }
    val substitutedFromId = exerciseInWorkout.sets.firstOrNull { it.originalExerciseId != null }?.originalExerciseId
    val substitutedFromName = substitutedFromId?.let { id -> availableExercises.firstOrNull { it.id == id }?.name }
    var showSubstitutePicker by remember { mutableStateOf(false) }
    var showBodyweightEdit by remember { mutableStateOf(false) }

    Card(
        backgroundColor = CardBackground,
        elevation = 3.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Exercise name row with a dropdown chevron to collapse/expand the card and save space.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleExpand() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (exercise.isBodyweight) {
                    Text(
                        text = "BW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        modifier = Modifier
                            .background(
                                color = AccentGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = if (expanded) "▾" else "▸",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }

            if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            if (exercise.isBodyweight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bodyweight", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallControlButton("−", AccentRed, { onBodyweightChange((bodyweight - Weights.toKg(0.5)).coerceAtLeast(30.0)) }, bodyweight > 30.0)
                        Text(
                            "${formatWeight(bodyweight)} ${Weights.unit}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            modifier = Modifier.clickable { showBodyweightEdit = true }
                        )
                        SmallControlButton("+", AccentGreen, { onBodyweightChange((bodyweight + Weights.toKg(0.5)).coerceAtMost(999.0)) }, bodyweight < 999.0)
                    }
                }
                if (showBodyweightEdit) {
                    WeightEditDialog(
                        currentWeight = bodyweight,
                        onDismiss = { showBodyweightEdit = false },
                        onConfirm = { kg -> onBodyweightChange(kg.coerceIn(30.0, 999.0)); showBodyweightEdit = false }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Set count controls with skip / substitute actions on the same row (cardio uses metrics).
            if (!exercise.isCardio) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallControlButton(
                    text = "−",
                    color = AccentRed,
                    onClick = onRemoveSet,
                    enabled = exerciseInWorkout.sets.size > 1
                )

                Text(
                    text = "${exerciseInWorkout.sets.size} sets",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                SmallControlButton(
                    text = "+",
                    color = AccentGreen,
                    onClick = onAddSet
                )

                Spacer(modifier = Modifier.weight(1f))

                ExerciseActionChip(
                    text = if (isSkipped) "Resume" else "Skip",
                    color = if (isSkipped) AccentGreen else AccentRed,
                    onClick = onSkipToggle
                )
                ExerciseActionChip(
                    text = "Swap",
                    color = AccentBlue,
                    onClick = { showSubstitutePicker = true }
                )
            }
            }

            if (substitutedFromName != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "↔ from $substitutedFromName",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (exercise.isCardio) {
                val cardioSet = exerciseInWorkout.sets.firstOrNull()
                CardioMetricsEditor(
                    set = cardioSet,
                    onUpdateMetric = { key, value -> if (cardioSet != null) onUpdateMetric(cardioSet.id, key, value) },
                    onToggleComplete = { if (cardioSet != null) onUpdateSet(cardioSet.id, null, null, !cardioSet.completed) }
                )
            } else if (isSkipped) {
                Text(
                    text = "Skipped for this workout — tap Resume to log sets.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            } else {
                exerciseInWorkout.sets.forEachIndexed { index, set ->
                    SetRowWithButtons(
                        setNumber = index + 1,
                        set = set,
                        exercise = exercise,
                        bodyweight = bodyweight,
                        onUpdateSet = { reps, weight, completed ->
                            onUpdateSet(set.id, reps, weight, completed)
                        }
                    )

                    if (index < exerciseInWorkout.sets.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ExerciseNotesField(
                exerciseId = exercise.id,
                notes = exerciseInWorkout.notes,
                onNotesChange = onNotesChange
            )
            }
        }
    }

    if (showSubstitutePicker) {
        SubstituteExerciseDialog(
            currentExerciseId = exercise.id,
            exercises = availableExercises,
            onDismiss = { showSubstitutePicker = false },
            onPick = { picked ->
                onSubstitute(picked)
                showSubstitutePicker = false
            }
        )
    }
}

// Cardio exercises are tracked with machine metrics instead of sets/reps/weight. Each field is
// optional; tapping one opens a number pad. Values are stored in the set's metrics map.
@Composable
private fun CardioMetricsEditor(
    set: WorkoutSet?,
    onUpdateMetric: (String, Double?) -> Unit,
    onToggleComplete: () -> Unit
) {
    if (set == null) {
        Text("No cardio entry.", fontSize = 13.sp, color = TextSecondary)
        return
    }
    val fields = listOf(
        CardioField("Duration", "duration_min", "min"),
        CardioField("Distance", "distance_km", "km"),
        CardioField("Avg watts", "watts", "W"),
        CardioField("Level", "level", ""),
        CardioField("Incline", "incline_pct", "%"),
        CardioField("Calories", "calories", "kcal"),
        CardioField("Avg HR", "hr", "bpm"),
        CardioField("Bodyweight", "bodyweight", Weights.unit)
    )
    var editing by remember { mutableStateOf<CardioField?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        fields.chunked(2).forEach { rowFields ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowFields.forEach { field ->
                    val value = set.metrics[field.key]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { editing = field }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(field.label, fontSize = 11.sp, color = TextSecondary)
                        Text(
                            if (value != null) formatMetric(value) + (if (field.unit.isNotEmpty()) " ${field.unit}" else "") else "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (value != null) TextPrimary else TextSecondary
                        )
                    }
                }
                if (rowFields.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onToggleComplete,
            colors = ButtonDefaults.buttonColors(backgroundColor = if (set.completed) AccentGreen else SurfaceColor.copy(alpha = 0.5f)),
            border = if (set.completed) null else BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            elevation = ButtonDefaults.elevation(0.dp)
        ) {
            Text(
                if (set.completed) "✓ Done · tap to undo" else "Done",
                color = if (set.completed) Color.White else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }

    editing?.let { field ->
        CardioMetricDialog(
            field = field,
            current = set.metrics[field.key],
            onDismiss = { editing = null },
            onConfirm = { v -> onUpdateMetric(field.key, v); editing = null }
        )
    }
}

private data class CardioField(val label: String, val key: String, val unit: String)

private fun formatMetric(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

@Composable
private fun CardioMetricDialog(
    field: CardioField,
    current: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var textValue by remember { mutableStateOf(current?.let { formatMetric(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text(
                    field.label + if (field.unit.isNotEmpty()) " (${field.unit})" else "",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        val f = newText.filter { it.isDigit() || it == '.' }
                        if (f.count { it == '.' } <= 1 && f.length <= 8) textValue = f
                    },
                    modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(16.dp),
                    textStyle = TextStyle(fontSize = 24.sp, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(textValue.toDoubleOrNull()) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange)) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

@Composable
private fun ExerciseNotesField(exerciseId: String, notes: String, onNotesChange: (String) -> Unit) {
    var text by remember(exerciseId) { mutableStateOf(notes) }
    // Debounce persistence so the workout isn't rewritten on every keystroke.
    LaunchedEffect(exerciseId, text) {
        if (text != notes) {
            kotlinx.coroutines.delay(700L)
            onNotesChange(text)
        }
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            EmojiIcon("📝", Icons.Filled.Notes, fontSize = 12.sp, iconSize = 14.dp)
            Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(10.dp),
            textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
            decorationBox = { inner ->
                if (text.isEmpty()) Text("How it felt, form cues, sleep, etc.", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp)
                inner()
            }
        )
    }
}

@Composable
private fun ExerciseActionChip(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SubstituteExerciseDialog(
    currentExerciseId: String,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(exercises, query, currentExerciseId) {
        exercises.filter {
            it.id != currentExerciseId &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                    it.muscleGroups.any { muscle -> muscle.contains(query, ignoreCase = true) })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Substitute Exercise", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { exercise ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor, RoundedCornerShape(8.dp))
                                .clickable { onPick(exercise) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(exercise.muscleGroups.joinToString(", "), fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

@Composable
private fun SmallControlButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color.copy(alpha = if (enabled) 0.8f else 0.3f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(32.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SetRowWithButtons(
    setNumber: Int,
    set: WorkoutSet,
    exercise: Exercise,
    bodyweight: Double,
    onUpdateSet: (Int?, Double?, Boolean?) -> Unit
) {
    val isCompleted = set.completed
    // Completed sets are locked by default; a Settings toggle re-enables editing them.
    val editable = !set.completed || com.bodyforge.presentation.state.SettingsState.editCompletedSets
    val backgroundColor = if (isCompleted) AccentGreen.copy(alpha = 0.15f) else SurfaceColor

    Card(
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Set header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set $setNumber",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) AccentGreen else TextPrimary
                )

                // Completion button
                Button(
                    onClick = {
                        if (isCompleted) {
                            onUpdateSet(null, null, false)
                        } else if (set.reps > 0) {
                            onUpdateSet(null, null, true)
                        }
                    },
                    enabled = isCompleted || (set.reps > 0 && (set.weightKg > 0 || exercise.isBodyweight)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isCompleted) AccentGreen else SurfaceColor.copy(alpha = 0.5f),
                        disabledBackgroundColor = SurfaceColor.copy(alpha = 0.3f)
                    ),
                    border = if (isCompleted) null else BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(36.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = if (isCompleted) "✓ Done · tap to undo" else "Done",
                        color = if (isCompleted) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reps and Weight controls (stacked full-width so values never get cramped)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reps control
                ValueControlGroup(
                    label = "Reps",
                    value = set.reps,
                    displayValue = set.reps.toString(),
                    onDecrement = {
                        if (set.reps > 0 && editable) {
                            onUpdateSet(set.reps - 1, null, null)
                        }
                    },
                    onIncrement = {
                        if (editable) {
                            onUpdateSet(set.reps + 1, null, null)
                        }
                    },
                    onValueChange = { newReps ->
                        if (editable) {
                            onUpdateSet(newReps, null, null)
                        }
                    },
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth()
                )

                // Weight control
                if (exercise.isBodyweight) {
                    BodyweightValueControl(
                        label = "Weight",
                        additionalWeight = set.weightKg,
                        bodyweight = bodyweight,
                        onDecrement = {
                            if (set.weightKg > 0 && editable) {
                                onUpdateSet(null, (set.weightKg - Weights.toKg(2.5)).coerceAtLeast(0.0), null)
                            }
                        },
                        onIncrement = {
                            if (editable) {
                                onUpdateSet(null, set.weightKg + Weights.toKg(2.5), null)
                            }
                        },
                        onValueChange = { newWeight ->
                            if (editable) {
                                onUpdateSet(null, newWeight, null)
                            }
                        },
                        enabled = editable,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ValueControlGroup(
                        label = "Weight",
                        value = set.weightKg.toInt(),
                        displayValue = "${formatWeight(set.weightKg)} ${Weights.unit}",
                        onDecrement = {
                            if (set.weightKg > 0 && editable) {
                                onUpdateSet(null, (set.weightKg - Weights.toKg(2.5)).coerceAtLeast(0.0), null)
                            }
                        },
                        onIncrement = {
                            if (editable) {
                                onUpdateSet(null, set.weightKg + Weights.toKg(2.5), null)
                            }
                        },
                        onValueChange = { newWeight ->
                            if (editable) {
                                onUpdateSet(null, newWeight.toDouble(), null)
                            }
                        },
                        enabled = editable,
                        isWeight = true,
                        currentWeight = set.weightKg,
                        onWeightChange = { newWeight ->
                            if (editable) {
                                onUpdateSet(null, newWeight, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueControlGroup(
    label: String,
    value: Int,
    displayValue: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isWeight: Boolean = false,
    currentWeight: Double = 0.0,
    onWeightChange: (Double) -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minus button
            ControlButton(
                text = "−",
                color = ButtonRed,
                onClick = onDecrement,
                enabled = enabled && value > 0
            )

            // Value display (clickable for direct input)
            var showEditDialog by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = SurfaceColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, TextSecondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { showEditDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) TextPrimary else TextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Plus button
            ControlButton(
                text = "+",
                color = ButtonGreen,
                onClick = onIncrement,
                enabled = enabled
            )

            // Edit Dialog
            if (showEditDialog) {
                if (isWeight) {
                    WeightEditDialog(
                        currentWeight = currentWeight,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { newWeight ->
                            onWeightChange(newWeight)
                            showEditDialog = false
                        }
                    )
                } else {
                    NumberEditDialog(
                        currentValue = value,
                        label = label,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { newValue ->
                            onValueChange(newValue)
                            showEditDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyweightValueControl(
    label: String,
    additionalWeight: Double,
    bodyweight: Double,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueChange: (Double) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minus button
            ControlButton(
                text = "−",
                color = ButtonRed,
                onClick = onDecrement,
                enabled = enabled && additionalWeight > 0
            )

            // BW +/- display
            var showEditDialog by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = SurfaceColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, TextSecondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { showEditDialog = true }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayText = when {
                    additionalWeight > 0 -> "BW+${formatWeight(additionalWeight)}${Weights.unit}"
                    additionalWeight < 0 -> "BW${formatWeight(additionalWeight)}${Weights.unit}"
                    else -> "BW"
                }

                Text(
                    text = displayText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) AccentGreen else TextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Plus button
            ControlButton(
                text = "+",
                color = ButtonGreen,
                onClick = onIncrement,
                enabled = enabled
            )

            // Edit Dialog
            if (showEditDialog) {
                WeightEditDialog(
                    currentWeight = additionalWeight,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { newWeight ->
                        onValueChange(newWeight)
                        showEditDialog = false
                    },
                    isBodyweight = true,
                    totalWeight = bodyweight + additionalWeight
                )
            }
        }

        // Total weight display
        if (additionalWeight != 0.0) {
            Text(
                text = "Total: ${formatWeight(bodyweight + additionalWeight)}${Weights.unit}",
                fontSize = 10.sp,
                color = AccentGreen,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color,
            disabledBackgroundColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun NumberEditDialog(
    currentValue: Int,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text("Edit $label", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        val filtered = newText.filter { it.isDigit() }
                        if (filtered.length <= 4) {
                            textValue = filtered
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textValue.toIntOrNull() ?: 0) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange)
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun WeightEditDialog(
    currentWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    isBodyweight: Boolean = false,
    totalWeight: Double = 0.0
) {
    var textValue by remember { mutableStateOf(formatWeight(currentWeight)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text(
                    text = if (isBodyweight) "Edit Additional Weight" else "Edit Weight",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        val filtered = newText.filter { it.isDigit() || it == '.' || it == '-' }
                        if (filtered.count { it == '.' } <= 1 && filtered.length <= 8) {
                            textValue = filtered
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true
                )

                if (isBodyweight) {
                    val newAdditional = Weights.toKg(textValue.toDoubleOrNull() ?: 0.0)
                    Text(
                        text = "Total: ${formatWeight(totalWeight - currentWeight + newAdditional)}${Weights.unit}",
                        fontSize = 14.sp,
                        color = AccentGreen,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Text(
                    text = Weights.unit,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(Weights.toKg(textValue.toDoubleOrNull() ?: 0.0)) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange)
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
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

// Quick Workout Flow with exercise selection
@Composable
private fun QuickWorkoutFlow(
    onBack: () -> Unit,
    onStartWorkout: (List<Exercise>) -> Unit
) {
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    var selectedExercises by remember { mutableStateOf(listOf<Exercise>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilters by remember { mutableStateOf(setOf<String>()) }
    var showFilters by remember { mutableStateOf(false) }
    var showAddToWorkoutDialog by remember { mutableStateOf<Exercise?>(null) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Android system back / edge-swipe in the picker returns to the "Ready to workout?" screen.
    PlatformBackHandler(enabled = true, onBack = onBack)

    LaunchedEffect(Unit) {
        SharedWorkoutState.loadExercises()
    }

    val filteredExercises = remember(exercises, searchQuery, selectedMuscleFilters) {
        exercises.filter { exercise ->
            val matchesSearch = searchQuery.isEmpty() ||
                    exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.muscleGroups.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesMuscleFilter = selectedMuscleFilters.isEmpty() ||
                    exercise.muscleGroups.any { muscleGroup ->
                        selectedMuscleFilters.any { filter -> muscleGroup.contains(filter, ignoreCase = true) }
                    }

            matchesSearch && matchesMuscleFilter
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("<", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Select Exercises",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            Button(
                onClick = { showCreateExerciseDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("+ New", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        CreateExerciseDialog(
            showDialog = showCreateExerciseDialog,
            onDismiss = { showCreateExerciseDialog = false },
            onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
                scope.launch {
                    val created = SharedWorkoutState.createCustomExercise(name, muscleGroups, equipment, isBodyweight)
                    selectedExercises = selectedExercises + created
                }
            }
        )

        // Selected summary bar — always present so the exercise list below never shifts when
        // selecting. Start stays in place but is greyed out until at least one exercise is picked.
        val canStart = selectedExercises.isNotEmpty() && !isLoading
        Card(
            backgroundColor = SelectedGreen,
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedExercises.size} Selected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = { onStartWorkout(selectedExercises) },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentOrange,
                        disabledBackgroundColor = SurfaceColor
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        "Start",
                        color = if (canStart) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        Card(
            backgroundColor = CardBackground,
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceColor, RoundedCornerShape(25.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        textStyle = TextStyle(fontSize = 16.sp, color = TextPrimary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search exercises...", color = TextSecondary)
                            }
                            innerTextField()
                        }
                    )

                    Button(
                        onClick = { showFilters = !showFilters },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (showFilters) AccentOrange else SurfaceColor
                        ),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.size(44.dp),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text(if (showFilters) "▲" else "▼", fontSize = 16.sp)
                    }
                }

                if (showFilters) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val muscleGroups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quadriceps", "Hamstrings", "Glutes")

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(muscleGroups) { muscle ->
                            val isSelected = selectedMuscleFilters.contains(muscle)
                            Button(
                                onClick = {
                                    selectedMuscleFilters = if (isSelected) {
                                        selectedMuscleFilters - muscle
                                    } else {
                                        selectedMuscleFilters + muscle
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isSelected) AccentOrange else SurfaceColor
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = muscle,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Exercise list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredExercises) { exercise ->
                val isSelected = selectedExercises.contains(exercise)

                Card(
                    backgroundColor = if (isSelected) SelectedGreen else CardBackground,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activeWorkout != null && !isSelected) {
                                    showAddToWorkoutDialog = exercise
                                } else {
                                    selectedExercises = if (isSelected) {
                                        selectedExercises - exercise
                                    } else {
                                        selectedExercises + exercise
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )

                                if (exercise.isBodyweight) {
                                    Text(
                                        text = "BW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen,
                                        modifier = Modifier
                                            .background(
                                                AccentGreen.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = exercise.muscleGroups.joinToString(", "),
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (isSelected) AccentGreen else AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Add to active workout dialog
    showAddToWorkoutDialog?.let { exercise ->
        AlertDialog(
            onDismissRequest = { showAddToWorkoutDialog = null },
            title = {
                Text("Add to Workout?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "You have an active workout. Do you want to add \"${exercise.name}\" to your current workout?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Add exercise to active workout
                        showAddToWorkoutDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen)
                ) {
                    Text("Add to Workout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        selectedExercises = selectedExercises + exercise
                        showAddToWorkoutDialog = null
                    }) {
                        Text("Add to Selection", color = AccentOrange)
                    }
                    TextButton(onClick = { showAddToWorkoutDialog = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun TemplateSelectionFlow(
    onBack: () -> Unit,
    onStartFromTemplate: (com.bodyforge.domain.models.WorkoutTemplate) -> Unit
) {
    val templates by SharedWorkoutState.templates.collectAsState()
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SharedWorkoutState.loadTemplates()
        SharedWorkoutState.loadExercises()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("<", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Select Template",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("+ New", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentOrange)
            }
        } else if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmojiIcon("📋", Icons.Filled.Assignment, iconSize = 48.dp, fontSize = 48.sp)
                    Text(
                        text = "No Templates Yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap + New to create one and start right away",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    Card(
                        backgroundColor = CardBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${template.exerciseIds.size} exercises",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = { onStartFromTemplate(template) },
                                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Text("Start", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(
            exercises = exercises,
            onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
                SharedWorkoutState.createCustomExercise(name, muscleGroups, equipment, isBodyweight)
            },
            onDismiss = { showCreateDialog = false },
            onCreateTemplate = { name, selected, desc, routine, variation ->
                scope.launch {
                    val template = WorkoutTemplate(
                        id = "template_${Clock.System.now().epochSeconds}",
                        name = name,
                        exerciseIds = selected.map { it.id },
                        createdAt = Clock.System.now(),
                        description = desc,
                        routineId = routineKey(routine),
                        routineName = routine.trim(),
                        variationLabel = variation.trim()
                    )
                    SharedWorkoutState.templateRepo.saveTemplate(template)
                    SharedWorkoutState.loadTemplates()
                    showCreateDialog = false
                    onStartFromTemplate(template)
                }
            }
        )
    }
}

private fun formatWeight(weight: Double): String = Weights.format(weight)