package com.bodyforge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodyforge.presentation.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

// Modern Color Palette
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)
private val AccentOrange = Color(0xFFFF6B35)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val SelectedGreen = Color(0xFF065F46)

@Composable
fun App() {
    MaterialTheme(
        colors = darkColors(
            primary = AccentOrange,
            background = DarkBackground,
            surface = CardBackground,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    ) {
        val viewModel: WorkoutViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Column {
                // Header with gradient
                HeaderSection()

                // Navigation Tabs
                NavigationTabs(
                    activeTab = uiState.activeTab,
                    onTabSelected = { viewModel.setActiveTab(it) },
                    hasActiveWorkout = uiState.currentWorkout != null
                )

                // Error handling
                uiState.error?.let { error ->
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }

                // Content based on active tab
                when (uiState.activeTab) {
                    "create" -> CreateWorkoutContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    "active" -> ActiveWorkoutContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    "history" -> HistoryContent()
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🏋️",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "BodyForge",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            }

            Text(
                text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun NavigationTabs(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    hasActiveWorkout: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 4.dp)
    ) {
        listOf(
            "create" to "🏗️ Create",
            "active" to "🔥 Active",
            "history" to "📊 History"
        ).forEach { (tab, label) ->
            TabButton(
                text = label,
                isActive = activeTab == tab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
                showBadge = tab == "active" && hasActiveWorkout
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false
) {
    Box(
        modifier = modifier
            .padding(4.dp)
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isActive) AccentOrange.copy(alpha = 0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isActive) AccentOrange else TextSecondary
            )
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AccentRed, RoundedCornerShape(50))
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        backgroundColor = AccentRed.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = Color.White,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            IconButton(onClick = onDismiss) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun CreateWorkoutContent(
    uiState: com.bodyforge.presentation.viewmodel.WorkoutUiState,
    viewModel: WorkoutViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selected Exercises Section
        if (uiState.selectedExercises.isNotEmpty()) {
            item {
                SelectedExercisesCard(
                    selectedExercises = uiState.selectedExercises,
                    isLoading = uiState.isLoading,
                    onRemoveExercise = { viewModel.removeExerciseFromSelection(it) },
                    onStartWorkout = { viewModel.startWorkout() }
                )
            }
        }

        // Exercise Library Section
        item {
            Text(
                text = "🎯 Exercise Library",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(uiState.availableExercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                isSelected = uiState.selectedExercises.contains(exercise),
                onToggle = {
                    if (uiState.selectedExercises.contains(exercise)) {
                        viewModel.removeExerciseFromSelection(exercise)
                    } else {
                        viewModel.addExerciseToSelection(exercise)
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectedExercisesCard(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    isLoading: Boolean,
    onRemoveExercise: (com.bodyforge.domain.models.Exercise) -> Unit,
    onStartWorkout: () -> Unit
) {
    Card(
        backgroundColor = SelectedGreen,
        elevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Exercises (${selectedExercises.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = onStartWorkout,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Text(
                            text = if (isLoading) "Starting..." else "Start Workout",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            selectedExercises.forEach { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = exercise.muscleGroups.joinToString(", "),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { onRemoveExercise(exercise) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove",
                            tint = AccentRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: com.bodyforge.domain.models.Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        backgroundColor = if (isSelected) SelectedGreen else CardBackground,
        elevation = if (isSelected) 6.dp else 2.dp,
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
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary
                )
                Text(
                    text = "${exercise.muscleGroups.joinToString(", ")} • ${exercise.equipmentNeeded}",
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                )
            }

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = if (isSelected) "Remove" else "Add",
                    tint = if (isSelected) AccentGreen else AccentOrange
                )
            }
        }
    }
}

@Composable
private fun ActiveWorkoutContent(
    uiState: com.bodyforge.presentation.viewmodel.WorkoutUiState,
    viewModel: WorkoutViewModel
) {
    val currentWorkout = uiState.currentWorkout

    if (currentWorkout == null) {
        // No active workout screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🏋️", fontSize = 64.sp)
                Text(
                    text = "No Active Workout",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Create a workout to start tracking your sets!",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { viewModel.setActiveTab("create") },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Create Workout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Active workout screen - this would be implemented with the actual workout UI
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WorkoutHeaderCard(
                    workout = currentWorkout,
                    onFinishWorkout = { viewModel.completeWorkout() }
                )
            }

            items(currentWorkout.exercises) { exerciseInWorkout ->
                ActiveExerciseCard(
                    exerciseInWorkout = exerciseInWorkout,
                    onUpdateSet = { setId, reps, weight, completed ->
                        viewModel.updateSet(exerciseInWorkout.exercise.id, setId, reps, weight, completed)
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkoutHeaderCard(
    workout: com.bodyforge.domain.models.Workout,
    onFinishWorkout: () -> Unit
) {
    Card(
        backgroundColor = Color(0xFF1E40AF),
        elevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Started: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(workout.startedAt.toEpochMilliseconds()))}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onFinishWorkout,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("🏁 Finish", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exerciseInWorkout: com.bodyforge.domain.models.ExerciseInWorkout,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = exerciseInWorkout.exercise.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            exerciseInWorkout.sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    onUpdateSet = onUpdateSet
                )
            }
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    set: com.bodyforge.domain.models.WorkoutSet,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit
) {
    Card(
        backgroundColor = SurfaceColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Set $setNumber",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.width(50.dp)
            )

            // Reps controls
            SetValueControl(
                label = "Reps",
                value = set.reps,
                onDecrease = { if (set.reps > 0) onUpdateSet(set.id, set.reps - 1, null, null) },
                onIncrease = { onUpdateSet(set.id, set.reps + 1, null, null) }
            )

            // Weight controls
            SetValueControl(
                label = "Weight",
                value = set.weightKg,
                suffix = "kg",
                step = 2.5,
                onDecrease = { if (set.weightKg > 0) onUpdateSet(set.id, null, (set.weightKg - 2.5).coerceAtLeast(0.0), null) },
                onIncrease = { onUpdateSet(set.id, null, set.weightKg + 2.5, null) }
            )

            // Complete button
            Button(
                onClick = { onUpdateSet(set.id, null, null, !set.completed) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (set.completed) AccentGreen else SurfaceColor
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(width = 80.dp, height = 36.dp)
            ) {
                Text(
                    text = if (set.completed) "✓" else "○",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SetValueControl(
    label: String,
    value: Number,
    suffix: String = "",
    step: Double = 1.0,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = onDecrease,
                modifier = Modifier.size(28.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-", color = AccentOrange, fontSize = 16.sp)
            }

            Text(
                text = "${if (value is Double && value % 1.0 == 0.0) value.toInt() else value}$suffix",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onIncrease,
                modifier = Modifier.size(28.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", color = AccentOrange, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun HistoryContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("📊", fontSize = 64.sp)
            Text(
                text = "Workout History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your completed workouts will appear here.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}