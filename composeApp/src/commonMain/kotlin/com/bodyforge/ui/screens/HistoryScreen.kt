package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)

@Composable
fun HistoryScreen() {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    var editingWorkout by remember { mutableStateOf<com.bodyforge.domain.models.Workout?>(null) }

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
                        onDelete = {
                            // TODO: Delete workout
                        },
                        onEdit = { editingWorkout = workout }
                    )
                }
            }
        }
    }

    // Edit Workout Dialog (placeholder)
    editingWorkout?.let { workout ->
        AlertDialog(
            onDismissRequest = { editingWorkout = null },
            title = { Text("Edit Workout", color = TextPrimary) },
            text = {
                Text(
                    "Edit ${workout.name}\n\nEdit Workout Dialog - TODO",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { editingWorkout = null }) {
                    Text("Close")
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = onEdit) {
                        Text("📝", fontSize = 14.sp)
                    }
                    TextButton(onClick = onDelete) {
                        Text("🗑️", fontSize = 14.sp)
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