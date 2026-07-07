package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.bodyforge.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.data.Weights
import com.bodyforge.ui.components.EmojiIcon
import com.bodyforge.ui.util.formatThousands
import com.bodyforge.domain.models.Workout
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.rememberCsvImporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(listState: LazyListState, onResumed: () -> Unit) {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var menuWorkout by remember { mutableStateOf<Workout?>(null) }
    var editingWorkout by remember { mutableStateOf<Workout?>(null) }
    var resumeConfirmWorkout by remember { mutableStateOf<Workout?>(null) }
    var deleteConfirmationWorkout by remember { mutableStateOf<Workout?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var showImportInfo by remember { mutableStateOf(false) }
    val launchCsvImport = rememberCsvImporter { csv ->
        coroutineScope.launch {
            val (imported, skipped) = SharedWorkoutState.importWorkoutsFromCsv(csv)
            importMessage = "Imported $imported workout${if (imported == 1) "" else "s"}" +
                if (skipped > 0) " · $skipped row${if (skipped == 1) "" else "s"} skipped" else ""
        }
    }

    LaunchedEffect(Unit) {
        SharedWorkoutState.loadCompletedWorkouts()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Workout History",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                Button(
                    onClick = launchCsvImport,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.elevation(0.dp),
                    modifier = Modifier.padding(end = 14.dp)
                ) {
                    Text("Import CSV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                IconButton(
                    onClick = { showImportInfo = true },
                    modifier = Modifier.align(Alignment.TopEnd).offset(y = (-14).dp).size(22.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "About CSV import", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
            completedWorkouts.isEmpty() -> EmptyHistoryCard()
            else -> LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(completedWorkouts) { workout ->
                    HistoryWorkoutCard(
                        workout = workout,
                        onMenu = { menuWorkout = workout }
                    )
                }
            }
        }
    }

    menuWorkout?.let { workout ->
        WorkoutActionSheet(
            workout = workout,
            onDismiss = { menuWorkout = null },
            onResume = {
                menuWorkout = null
                if (activeWorkout != null && activeWorkout?.id != workout.id) {
                    resumeConfirmWorkout = workout
                } else {
                    coroutineScope.launch {
                        SharedWorkoutState.resumeWorkout(workout)
                        onResumed()
                    }
                }
            },
            onEdit = {
                menuWorkout = null
                editingWorkout = workout
            },
            onDelete = {
                menuWorkout = null
                deleteConfirmationWorkout = workout
            }
        )
    }

    importMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { importMessage = null },
            title = { Text("CSV Import", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = TextSecondary) },
            confirmButton = {
                Button(onClick = { importMessage = null }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue)) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = CardBackground
        )
    }

    if (showImportInfo) {
        AlertDialog(
            onDismissRequest = { showImportInfo = false },
            title = { Text("Import CSV", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Bulk-import past workouts into your history. Pick a CSV file and each row is added as one completed set, back-dated to its date.",
                        color = TextSecondary, fontSize = 14.sp
                    )
                    Text("Format — one row per set:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(
                            "date,workout,exercise,reps,weight,unit,notes\n2025-01-15,Push,Bench Press,8,80,kg,felt strong\n2025-01-15,Push,Bench Press,7,80,kg,\n2025-01-18,Pull,Deadlift,5,275,lbs,belt on",
                            color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        "• Header row optional. Date: YYYY-MM-DD or DD.MM.YYYY.\n• unit (kg/lbs) and notes are optional; weight is stored in kg and lbs is converted on import.\n• Multiple set notes for an exercise show semicolon-separated in Analytics → Progress (tap a point).\n• Same date + workout = one session; unknown exercises are auto-created by name.",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showImportInfo = false }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue)) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = CardBackground
        )
    }

    editingWorkout?.let { workout ->
        com.bodyforge.ui.components.WorkoutEditorDialog(
            workout = workout,
            onDismiss = { editingWorkout = null },
            onSave = { updatedWorkout ->
                coroutineScope.launch {
                    SharedWorkoutState.workoutRepo.updateWorkout(updatedWorkout)
                    SharedWorkoutState.loadCompletedWorkouts()
                }
                editingWorkout = null
            }
        )
    }

    resumeConfirmWorkout?.let { workout ->
        AlertDialog(
            onDismissRequest = { resumeConfirmWorkout = null },
            title = { Text("Resume Workout", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("You have an active workout. Resuming \"${workout.name}\" will finish the current one and continue this session.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            SharedWorkoutState.resumeWorkout(workout)
                            onResumed()
                        }
                        resumeConfirmWorkout = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen)
                ) { Text("Resume", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { resumeConfirmWorkout = null }) { Text("Cancel", color = TextSecondary) } },
            backgroundColor = CardBackground
        )
    }

    deleteConfirmationWorkout?.let { workout ->
        AlertDialog(
            onDismissRequest = { deleteConfirmationWorkout = null },
            title = { Text("Delete Workout", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${workout.name}\"?\n\nThis cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            SharedWorkoutState.workoutRepo.deleteWorkout(workout.id)
                            SharedWorkoutState.loadCompletedWorkouts()
                        }
                        deleteConfirmationWorkout = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmationWorkout = null }) { Text("Cancel", color = TextSecondary) } },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EmojiIcon("📊", Icons.Filled.BarChart, iconSize = 48.dp, fontSize = 48.sp)
            Text("No Workouts Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Text("Complete your first workout to see your training history here", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HistoryWorkoutCard(workout: Workout, onMenu: () -> Unit) {
    var showAllExercises by remember { mutableStateOf(false) }
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayTitle = if (workout.exercises.isNotEmpty() && workout.exercises.all { it.exercise.isCardio }) "Cardio" else workout.name
                    Text(displayTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val dateFormatter = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
                    Text(dateFormatter.format(Date(workout.startedAt.epochSeconds * 1000)), fontSize = 12.sp, color = TextSecondary)
                }
                Text(
                    "⋯",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier
                        .background(SurfaceColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onMenu)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WorkoutStat("🏋️", Icons.Filled.FitnessCenter, "${workout.exercises.size}", "Exercises")
                WorkoutStat("💪", Icons.Filled.Repeat, "${workout.performedSets}", "Sets")
                WorkoutStat("⏱️", Icons.Filled.Timer, "${workout.durationMinutes ?: 0}m", "Duration")
                if (workout.totalVolumePerformed > 0) WorkoutStat("📊", Icons.Filled.BarChart, "${formatThousands(Weights.toDisplay(workout.totalVolumePerformed))}${Weights.unit}", "Volume")
            }

            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text("Exercises:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    val shownExercises = if (showAllExercises) workout.exercises else workout.exercises.take(3)
                    shownExercises.forEach { exerciseInWorkout ->
                        Text("• ${exerciseInWorkout.exercise.name} (${exerciseInWorkout.performedSets} sets)", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                    }
                    if (workout.exercises.size > 3) {
                        Text(
                            if (showAllExercises) "Show less..." else "Show more...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp).clickable { showAllExercises = !showAllExercises }
                        )
                    }
                }
            }

            if (workout.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmojiIcon("📝", Icons.Filled.Notes, fontSize = 12.sp, iconSize = 14.dp)
                    Text(workout.notes, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WorkoutStat(emoji: String, icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EmojiIcon(emoji, icon, fontSize = 16.sp, iconSize = 18.dp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

// Bottom action sheet for a history workout, opened from the card's ⋯ button.
@Composable
private fun WorkoutActionSheet(
    workout: Workout,
    onDismiss: () -> Unit,
    onResume: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .background(CardBackground, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 10.dp)
                    .padding(top = 8.dp, bottom = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(SurfaceColor, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    "${workout.name} · ${dateFormatter.format(Date(workout.startedAt.epochSeconds * 1000))}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
                SheetAction("▶️", Icons.Filled.PlayArrow, "Resume workout", TextPrimary, onResume)
                SheetAction("✏️", Icons.Filled.Edit, "Edit workout", TextPrimary, onEdit)
                SheetAction("🗑️", Icons.Filled.Delete, "Delete", AccentRed, onDelete)
            }
        }
    }
}

@Composable
private fun SheetAction(emoji: String, icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EmojiIcon(emoji, icon, fontSize = 16.sp, iconSize = 20.dp)
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
    }
}