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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.presentation.state.SharedWorkoutState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*

// Colors
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
    var editingWorkout by remember { mutableStateOf<com.bodyforge.domain.models.Workout?>(null) }
    var deleteConfirmationWorkout by remember { mutableStateOf<com.bodyforge.domain.models.Workout?>(null) }

    // Initialize workout history
    LaunchedEffect(Unit) {
        SharedWorkoutState.loadCompletedWorkouts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "📊 Workout History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentOrange)
            }
        } else if (completedWorkouts.isEmpty()) {
            EmptyHistoryCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

    // Edit Workout Dialog
    editingWorkout?.let { workout ->
        ModernEditWorkoutDialog(
            workout = workout,
            onDismiss = { editingWorkout = null },
            onSaveWorkout = { updatedWorkout ->
                // TODO: Implement workout update
                editingWorkout = null
            }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmationWorkout?.let { workout ->
        AlertDialog(
            onDismissRequest = { deleteConfirmationWorkout = null },
            title = {
                Text(
                    "Delete Workout",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${workout.name}\"?\n\nThis action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Implement workout deletion
                        deleteConfirmationWorkout = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationWorkout = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
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
            Text("📊", fontSize = 48.sp)
            Text(
                text = "No Workouts Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Complete your first workout to see your training history here",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { /* TODO: Navigate to workout tab */ },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Start Your First Workout", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HistoryWorkoutCard(
    workout: com.bodyforge.domain.models.Workout,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    val dateFormatter = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
                    val startDate = Date(workout.startedAt.epochSeconds * 1000)

                    Text(
                        text = dateFormatter.format(startDate),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Text(
                            text = "✏️",
                            fontSize = 20.sp
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Text(
                            text = "🗑️",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WorkoutStat(
                    icon = "🏋️",
                    value = "${workout.exercises.size}",
                    label = "Exercises"
                )
                WorkoutStat(
                    icon = "💪",
                    value = "${workout.performedSets}",
                    label = "Sets"
                )
                WorkoutStat(
                    icon = "⏱️",
                    value = "${workout.durationMinutes ?: 0}m",
                    label = "Duration"
                )
                if (workout.totalVolumePerformed > 0) {
                    WorkoutStat(
                        icon = "📊",
                        value = "${workout.totalVolumePerformed.toInt()}kg",
                        label = "Volume"
                    )
                }
            }

            // Exercise List (first 3)
            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text(
                        text = "Exercises:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )

                    workout.exercises.take(3).forEach { exerciseInWorkout ->
                        Text(
                            text = "• ${exerciseInWorkout.exercise.name} (${exerciseInWorkout.performedSets} sets)",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }

                    if (workout.exercises.size > 3) {
                        Text(
                            text = "... and ${workout.exercises.size - 3} more",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutStat(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun ModernEditWorkoutDialog(
    workout: com.bodyforge.domain.models.Workout,
    onDismiss: () -> Unit,
    onSaveWorkout: (com.bodyforge.domain.models.Workout) -> Unit
) {
    var editedWorkout by remember { mutableStateOf(workout) }
    var workoutName by remember { mutableStateOf(workout.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✏️ Edit Workout",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 20.sp
                )

                Text(
                    text = "${editedWorkout.exercises.size} exercises",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout Name Editor
                item {
                    Card(
                        backgroundColor = SurfaceColor,
                        shape = RoundedCornerShape(12.dp),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Workout Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            BasicTextField(
                                value = workoutName,
                                onValueChange = { workoutName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = CardBackground,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (workoutName.isEmpty()) {
                                        Text(
                                            text = "Enter workout name...",
                                            color = TextSecondary,
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                // Exercise List with Sets
                items(editedWorkout.exercises) { exerciseInWorkout ->
                    ModernEditExerciseCard(
                        exerciseInWorkout = exerciseInWorkout,
                        onUpdateExercise = { updatedExercise ->
                            editedWorkout = editedWorkout.updateExercise(
                                exerciseInWorkout.exercise.id,
                                updatedExercise
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalWorkout = editedWorkout.copy(name = workoutName.trim())
                    onSaveWorkout(finalWorkout)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(25.dp),
                enabled = workoutName.isNotBlank(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("💾 Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun ModernEditExerciseCard(
    exerciseInWorkout: com.bodyforge.domain.models.ExerciseInWorkout,
    onUpdateExercise: (com.bodyforge.domain.models.ExerciseInWorkout) -> Unit
) {
    Card(
        backgroundColor = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseInWorkout.exercise.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = exerciseInWorkout.exercise.muscleGroups.joinToString(" • "),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Bodyweight badge
                if (exerciseInWorkout.exercise.isBodyweight) {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sets Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
                Text("Reps", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                Text("Weight", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
                Text("Done", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(50.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sets
            exerciseInWorkout.sets.forEachIndexed { index, set ->
                ModernEditSetRow(
                    setNumber = index + 1,
                    set = set,
                    exercise = exerciseInWorkout.exercise,
                    onUpdateSet = { updatedSet ->
                        val updatedSets = exerciseInWorkout.sets.toMutableList()
                        updatedSets[index] = updatedSet
                        onUpdateExercise(exerciseInWorkout.copy(sets = updatedSets))
                    }
                )

                if (index < exerciseInWorkout.sets.size - 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernEditSetRow(
    setNumber: Int,
    set: com.bodyforge.domain.models.WorkoutSet,
    exercise: com.bodyforge.domain.models.Exercise,
    onUpdateSet: (com.bodyforge.domain.models.WorkoutSet) -> Unit
) {
    var repsText by remember { mutableStateOf(set.reps.toString()) }
    var weightText by remember { mutableStateOf(if (set.weightKg > 0) formatWeight(set.weightKg) else "") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number
        Text(
            text = "$setNumber",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (set.completed) AccentGreen else TextPrimary,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )

        // Reps Input
        BasicTextField(
            value = repsText,
            onValueChange = { newText ->
                val filtered = newText.filter { it.isDigit() }
                if (filtered.length <= 3) {
                    repsText = filtered
                    val newReps = filtered.toIntOrNull() ?: 0
                    onUpdateSet(set.copy(reps = newReps))
                }
            },
            modifier = Modifier
                .width(60.dp)
                .background(
                    color = CardBackground,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (repsText.isEmpty()) {
                    Text(
                        text = "0",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        )

        // Weight Input
        Row(
            modifier = Modifier.width(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = weightText,
                onValueChange = { newText ->
                    val filtered = newText.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1 && filtered.length <= 6) {
                        weightText = filtered
                        val newWeight = filtered.toDoubleOrNull() ?: 0.0
                        onUpdateSet(set.copy(weightKg = newWeight))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = CardBackground,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (weightText.isEmpty()) {
                        Text(
                            text = if (exercise.isBodyweight) "BW" else "0",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            )

            if (!exercise.isBodyweight) {
                Text("kg", fontSize = 10.sp, color = TextSecondary)
            }
        }

        // Completed Switch
        Switch(
            checked = set.completed,
            onCheckedChange = { completed ->
                onUpdateSet(
                    if (completed) {
                        set.copy(completed = true, completedAt = kotlinx.datetime.Clock.System.now())
                    } else {
                        set.copy(completed = false, completedAt = null)
                    }
                )
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentGreen,
                checkedTrackColor = AccentGreen.copy(alpha = 0.5f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceColor
            ),
            modifier = Modifier
                .width(50.dp)
                .scale(0.8f)
        )
    }
}

// Helper function
private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.1f", weight)
    }
}