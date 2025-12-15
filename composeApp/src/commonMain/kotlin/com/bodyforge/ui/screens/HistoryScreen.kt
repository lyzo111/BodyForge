package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.domain.models.Workout
import com.bodyforge.presentation.state.SharedWorkoutState
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
fun HistoryScreen() {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var editingWorkout by remember { mutableStateOf<Workout?>(null) }
    var deleteConfirmationWorkout by remember { mutableStateOf<Workout?>(null) }

    LaunchedEffect(Unit) {
        SharedWorkoutState.loadCompletedWorkouts()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📊 Workout History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
            completedWorkouts.isEmpty() -> EmptyHistoryCard()
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(completedWorkouts) { workout ->
                    HistoryWorkoutCard(
                        workout = workout,
                        onDelete = { deleteConfirmationWorkout = workout },
                        onEdit = { editingWorkout = workout }
                    )
                }
            }
        }
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
            Text("📊", fontSize = 48.sp)
            Text("No Workouts Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Text("Complete your first workout to see your training history here", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HistoryWorkoutCard(workout: Workout, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(workout.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val dateFormatter = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
                    Text(dateFormatter.format(Date(workout.startedAt.epochSeconds * 1000)), fontSize = 12.sp, color = TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) { Text("✏️", fontSize = 20.sp) }
                    IconButton(onClick = onDelete) { Text("🗑️", fontSize = 20.sp) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WorkoutStat("🏋️", "${workout.exercises.size}", "Exercises")
                WorkoutStat("💪", "${workout.performedSets}", "Sets")
                WorkoutStat("⏱️", "${workout.durationMinutes ?: 0}m", "Duration")
                if (workout.totalVolumePerformed > 0) WorkoutStat("📊", "${workout.totalVolumePerformed.toInt()}kg", "Volume")
            }

            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text("Exercises:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    workout.exercises.take(3).forEach { exerciseInWorkout ->
                        Text("• ${exerciseInWorkout.exercise.name} (${exerciseInWorkout.performedSets} sets)", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                    }
                    if (workout.exercises.size > 3) Text("... and ${workout.exercises.size - 3} more", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkoutStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 16.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun EditWorkoutDialog(workout: Workout, onDismiss: () -> Unit, onSave: (Workout) -> Unit) {
    var workoutName by remember { mutableStateOf(workout.name) }
    var editedWorkout by remember { mutableStateOf(workout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Workout", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Set", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
                                Text("Reps", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                                Text("Weight", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
                                Text("Done", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(50.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            exerciseInWorkout.sets.forEachIndexed { index, set ->
                                var repsText by remember { mutableStateOf(set.reps.toString()) }
                                var weightText by remember { mutableStateOf(if (set.weightKg > 0) formatWeight(set.weightKg) else "") }
                                var isCompleted by remember { mutableStateOf(set.completed) }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isCompleted) AccentGreen else TextPrimary, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)

                                    BasicTextField(
                                        value = repsText,
                                        onValueChange = { newText ->
                                            val filtered = newText.filter { it.isDigit() }
                                            if (filtered.length <= 3) {
                                                repsText = filtered
                                                val newReps = filtered.toIntOrNull() ?: 0
                                                val updatedSet = set.copy(reps = newReps)
                                                val updatedSets = exerciseInWorkout.sets.toMutableList()
                                                updatedSets[index] = updatedSet
                                                val updatedExercise = exerciseInWorkout.copy(sets = updatedSets)
                                                editedWorkout = editedWorkout.updateExercise(exerciseInWorkout.exercise.id, updatedExercise)
                                            }
                                        },
                                        modifier = Modifier.width(60.dp).background(CardBackground, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                                        textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center, color = TextPrimary, fontWeight = FontWeight.Medium),
                                        singleLine = true
                                    )

                                    Row(modifier = Modifier.width(80.dp), verticalAlignment = Alignment.CenterVertically) {
                                        BasicTextField(
                                            value = weightText,
                                            onValueChange = { newText ->
                                                val filtered = newText.filter { it.isDigit() || it == '.' }
                                                if (filtered.count { it == '.' } <= 1 && filtered.length <= 6) {
                                                    weightText = filtered
                                                    val newWeight = filtered.toDoubleOrNull() ?: 0.0
                                                    val updatedSet = set.copy(weightKg = newWeight)
                                                    val updatedSets = exerciseInWorkout.sets.toMutableList()
                                                    updatedSets[index] = updatedSet
                                                    val updatedExercise = exerciseInWorkout.copy(sets = updatedSets)
                                                    editedWorkout = editedWorkout.updateExercise(exerciseInWorkout.exercise.id, updatedExercise)
                                                }
                                            },
                                            modifier = Modifier.weight(1f).background(CardBackground, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                                            textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center, color = TextPrimary, fontWeight = FontWeight.Medium),
                                            singleLine = true,
                                            decorationBox = { innerTextField -> if (weightText.isEmpty()) Text(if (exerciseInWorkout.exercise.isBodyweight) "BW" else "0", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center); innerTextField() }
                                        )
                                        if (!exerciseInWorkout.exercise.isBodyweight) Text("kg", fontSize = 10.sp, color = TextSecondary)
                                    }

                                    Switch(
                                        checked = isCompleted,
                                        onCheckedChange = { completed ->
                                            isCompleted = completed
                                            val updatedSet = if (completed) set.copy(completed = true, completedAt = kotlinx.datetime.Clock.System.now()) else set.copy(completed = false, completedAt = null)
                                            val updatedSets = exerciseInWorkout.sets.toMutableList()
                                            updatedSets[index] = updatedSet
                                            val updatedExercise = exerciseInWorkout.copy(sets = updatedSets)
                                            editedWorkout = editedWorkout.updateExercise(exerciseInWorkout.exercise.id, updatedExercise)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.5f), uncheckedThumbColor = TextSecondary, uncheckedTrackColor = SurfaceColor),
                                        modifier = Modifier.width(50.dp).scale(0.8f)
                                    )
                                }
                                if (index < exerciseInWorkout.sets.size - 1) Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedWorkout.copy(name = workoutName.trim())) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(25.dp),
                enabled = workoutName.isNotBlank(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

private fun formatWeight(weight: Double): String = if (weight % 1.0 == 0.0) weight.toInt().toString() else String.format("%.1f", weight)