package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Notes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.data.Weights
import com.bodyforge.ui.components.EmojiIcon
import com.bodyforge.domain.models.Workout
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.rememberCsvImporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val AccentOrange = Color(0xFFFF6B35)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

@Composable
fun HistoryScreen(listState: LazyListState, onResumed: () -> Unit) {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

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
            IconButton(onClick = { showImportInfo = true }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Filled.Info, contentDescription = "About CSV import", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
            Button(
                onClick = launchCsvImport,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Import CSV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        onResume = {
                            if (activeWorkout != null && activeWorkout?.id != workout.id) {
                                resumeConfirmWorkout = workout
                            } else {
                                coroutineScope.launch {
                                    SharedWorkoutState.resumeWorkout(workout)
                                    onResumed()
                                }
                            }
                        },
                        onDelete = { deleteConfirmationWorkout = workout },
                        onEdit = { editingWorkout = workout }
                    )
                }
            }
        }
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
        EditWorkoutDialog(
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
private fun HistoryWorkoutCard(workout: Workout, onResume: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showAllExercises by remember { mutableStateOf(false) }
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayTitle = if (workout.exercises.isNotEmpty() && workout.exercises.all { it.exercise.isCardio }) "Cardio" else workout.name
                    Text(displayTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val dateFormatter = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
                    Text(dateFormatter.format(Date(workout.startedAt.epochSeconds * 1000)), fontSize = 12.sp, color = TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onResume, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Resume", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Edit", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Delete", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WorkoutStat("🏋️", Icons.Filled.FitnessCenter, "${workout.exercises.size}", "Exercises")
                WorkoutStat("💪", Icons.Filled.FitnessCenter, "${workout.performedSets}", "Sets")
                WorkoutStat("⏱️", Icons.Filled.Timer, "${workout.durationMinutes ?: 0}m", "Duration")
                if (workout.totalVolumePerformed > 0) WorkoutStat("📊", Icons.Filled.BarChart, "${Weights.formatRounded(workout.totalVolumePerformed)}${Weights.unit}", "Volume")
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

@Composable
private fun EditWorkoutDialog(workout: Workout, onDismiss: () -> Unit, onSave: (Workout) -> Unit) {
    var workoutName by remember { mutableStateOf(workout.name) }
    var workoutNotes by remember { mutableStateOf(workout.notes) }
    var editedWorkout by remember { mutableStateOf(workout) }
    var editingRepsSetId by remember { mutableStateOf<String?>(null) }
    var editingWeightSetId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            backgroundColor = CardBackground,
            shape = RoundedCornerShape(16.dp),
            elevation = 0.dp,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Edit Workout", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(backgroundColor = SurfaceColor, shape = RoundedCornerShape(12.dp), elevation = 0.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Workout Name", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                                BasicTextField(
                                    value = workoutName,
                                    onValueChange = { workoutName = it },
                                    modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(8.dp)).padding(12.dp),
                                    textStyle = TextStyle(fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
                                    singleLine = true,
                                    decorationBox = { innerTextField -> if (workoutName.isEmpty()) Text("Enter workout name...", color = TextSecondary, fontSize = 16.sp); innerTextField() }
                                )
                            }
                        }
                    }

                    item {
                        Card(backgroundColor = SurfaceColor, shape = RoundedCornerShape(12.dp), elevation = 0.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Notes", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                                BasicTextField(
                                    value = workoutNotes,
                                    onValueChange = { workoutNotes = it },
                                    modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(8.dp)).padding(12.dp),
                                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                    decorationBox = { innerTextField -> if (workoutNotes.isEmpty()) Text("How it felt, injuries, PRs…", color = TextSecondary, fontSize = 15.sp); innerTextField() }
                                )
                            }
                        }
                    }

                    items(editedWorkout.exercises) { exerciseInWorkout ->
                        Card(backgroundColor = SurfaceColor, shape = RoundedCornerShape(12.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exerciseInWorkout.exercise.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(exerciseInWorkout.exercise.muscleGroups.joinToString(" • "), fontSize = 12.sp, color = TextSecondary)
                                    }
                                    if (exerciseInWorkout.exercise.isBodyweight) {
                                        Text("BW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGreen, modifier = Modifier.background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Set", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
                                    Text("Reps", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(if (exerciseInWorkout.exercise.isBodyweight) "Added ${Weights.unit}" else "Weight", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                exerciseInWorkout.sets.forEachIndexed { index, set ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)

                                        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${set.reps}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.background(CardBackground, RoundedCornerShape(8.dp)).clickable { editingRepsSetId = set.id }.padding(horizontal = 20.dp, vertical = 8.dp)
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (set.weightKg > 0) formatWeight(set.weightKg) else if (exerciseInWorkout.exercise.isBodyweight) "BW" else "0",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.background(CardBackground, RoundedCornerShape(8.dp)).clickable { editingWeightSetId = set.id }.padding(horizontal = 20.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(editedWorkout.copy(name = workoutName.trim(), notes = workoutNotes.trim())) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                        shape = RoundedCornerShape(25.dp),
                        enabled = workoutName.isNotBlank(),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    editingRepsSetId?.let { setId ->
        val current = editedWorkout.exercises.flatMap { it.sets }.firstOrNull { it.id == setId }?.reps ?: 0
        HistoryNumberInputDialog(
            currentValue = current,
            label = "Reps",
            onDismiss = { editingRepsSetId = null },
            onConfirm = { newReps ->
                editedWorkout = updateSetValue(editedWorkout, setId) { it.copy(reps = newReps) }
                editingRepsSetId = null
            }
        )
    }

    editingWeightSetId?.let { setId ->
        val current = editedWorkout.exercises.flatMap { it.sets }.firstOrNull { it.id == setId }?.weightKg ?: 0.0
        HistoryWeightInputDialog(
            currentWeight = current,
            onDismiss = { editingWeightSetId = null },
            onConfirm = { newWeight ->
                editedWorkout = updateSetValue(editedWorkout, setId) { it.copy(weightKg = newWeight) }
                editingWeightSetId = null
            }
        )
    }
}

// Applies a transform to the single set with the given id, returning the updated workout.
private fun updateSetValue(
    workout: Workout,
    setId: String,
    transform: (com.bodyforge.domain.models.WorkoutSet) -> com.bodyforge.domain.models.WorkoutSet
): Workout {
    val exercise = workout.exercises.firstOrNull { e -> e.sets.any { it.id == setId } } ?: return workout
    val updatedSets = exercise.sets.map { if (it.id == setId) transform(it) else it }
    return workout.updateExercise(exercise.exercise.id, exercise.copy(sets = updatedSets))
}

@Composable
private fun HistoryNumberInputDialog(currentValue: Int, label: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text("Edit $label", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText -> val f = newText.filter { it.isDigit() }; if (f.length <= 4) textValue = f },
                    modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(16.dp),
                    textStyle = TextStyle(fontSize = 24.sp, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(textValue.toIntOrNull() ?: 0) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange)) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

@Composable
private fun HistoryWeightInputDialog(currentWeight: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var textValue by remember { mutableStateOf(if (currentWeight > 0) formatWeight(currentWeight) else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text("Edit Weight (${Weights.unit})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText -> val f = newText.filter { it.isDigit() || it == '.' }; if (f.count { it == '.' } <= 1 && f.length <= 7) textValue = f },
                    modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(16.dp),
                    textStyle = TextStyle(fontSize = 24.sp, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(Weights.toKg(textValue.toDoubleOrNull() ?: 0.0)) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange)) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

private fun formatWeight(weight: Double): String = Weights.format(weight)