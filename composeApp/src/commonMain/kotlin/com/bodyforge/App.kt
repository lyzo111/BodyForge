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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodyforge.presentation.viewmodel.WorkoutViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

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

                // Loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                    "history" -> HistoryContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
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
private fun BodyweightInputCard(
    bodyweight: Double,
    onBodyweightChange: (Double) -> Unit
) {
    Card(
        backgroundColor = Color(0xFF0F766E),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💪",
                    fontSize = 20.sp
                )
                Text(
                    text = "Your Bodyweight:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decrease button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (bodyweight > 30.0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (bodyweight > 30.0) {
                                val newWeight = (bodyweight - 0.5).coerceAtLeast(30.0)
                                onBodyweightChange(formatToThreeDecimals(newWeight))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        color = if (bodyweight > 30.0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bodyweight input with robust parsing
                var textValue by remember(bodyweight) { mutableStateOf(formatWeight(bodyweight)) }
                var isEditing by remember { mutableStateOf(false) }

                if (isEditing) {
                    TextField(
                        value = textValue,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1 && filtered.length <= 7) {
                                textValue = filtered
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.White.copy(alpha = 0.1f),
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val newValue = parseWeightInput(textValue).coerceIn(30.0, 999.0)
                                onBodyweightChange(formatToThreeDecimals(newValue))
                                textValue = formatWeight(newValue)
                                isEditing = false
                            }
                        )
                    )
                } else {
                    Card(
                        backgroundColor = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .clickable {
                                isEditing = true
                                textValue = formatWeight(bodyweight)
                            }
                    ) {
                        Text(
                            text = "${formatWeight(bodyweight)} kg",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Increase button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (bodyweight < 999.0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (bodyweight < 999.0) {
                                val newWeight = (bodyweight + 0.5).coerceAtMost(999.0)
                                onBodyweightChange(formatToThreeDecimals(newWeight))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        color = if (bodyweight < 999.0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
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
            // Header with responsive button
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Selected Exercises (${selectedExercises.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Full-width button to prevent text wrapping
                Button(
                    onClick = onStartWorkout,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    elevation = ButtonDefaults.elevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Text(
                            text = if (isLoading) "Starting..." else "Start Workout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
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
        // Check if workout contains bodyweight exercises
        val hasBodyweightExercises = currentWorkout.exercises.any { it.exercise.isBodyweight }

        // Active workout screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add bodyweight input if needed
            if (hasBodyweightExercises) {
                item {
                    BodyweightInputCard(
                        bodyweight = uiState.bodyweight,
                        onBodyweightChange = { viewModel.updateBodyweight(it) }
                    )
                }
            }

            item {
                WorkoutHeaderCard(
                    workout = currentWorkout,
                    onFinishWorkout = { viewModel.completeWorkout() }
                )
            }

            items(currentWorkout.exercises) { exerciseInWorkout ->
                ActiveExerciseCard(
                    exerciseInWorkout = exerciseInWorkout,
                    bodyweight = uiState.bodyweight,
                    onUpdateSet = { setId: String, reps: Int?, weight: Double?, completed: Boolean? ->
                        viewModel.updateSet(exerciseInWorkout.exercise.id, setId, reps, weight, completed)
                    },
                    onAddSet = {
                        viewModel.addSetToExercise(exerciseInWorkout.exercise.id)
                    },
                    onRemoveSet = { setId: String ->
                        viewModel.removeSetFromExercise(exerciseInWorkout.exercise.id, setId)
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
                        text = "Started: ${workout.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).time}",
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
    bodyweight: Double,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit
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
            // Exercise header with set controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseInWorkout.exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Add/Remove Set Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Remove set button
                    IconButton(
                        onClick = {
                            if (exerciseInWorkout.sets.isNotEmpty()) {
                                onRemoveSet(exerciseInWorkout.sets.last().id)
                            }
                        },
                        enabled = exerciseInWorkout.sets.isNotEmpty()
                    ) {
                        Text(
                            text = "−",
                            fontSize = 20.sp,
                            color = if (exerciseInWorkout.sets.isNotEmpty()) AccentRed else TextSecondary
                        )
                    }

                    Text(
                        text = "${exerciseInWorkout.sets.size} sets",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Add set button
                    IconButton(onClick = onAddSet) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            color = AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            exerciseInWorkout.sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    exercise = exerciseInWorkout.exercise,
                    bodyweight = bodyweight,
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
    exercise: com.bodyforge.domain.models.Exercise,
    bodyweight: Double,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit
) {
    Card(
        backgroundColor = SurfaceColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Set header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set $setNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                // Complete button
                Button(
                    onClick = { onUpdateSet(set.id, null, null, !set.completed) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (set.completed) AccentGreen else SurfaceColor.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (set.completed) "✓" else "○",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls row - RESPONSIVE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reps control
                ResponsiveValueControl(
                    label = "Reps",
                    value = set.reps,
                    onDecrease = { if (set.reps > 0) onUpdateSet(set.id, set.reps - 1, null, null) },
                    onIncrease = { onUpdateSet(set.id, set.reps + 1, null, null) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Weight control with BW+Xkg support
                ResponsiveWeightControl(
                    label = if (exercise.isBodyweight) "BW+kg" else "Weight",
                    value = set.weightKg,
                    onDecrease = {
                        if (set.weightKg > 0)
                            onUpdateSet(set.id, null, (set.weightKg - 2.5).coerceAtLeast(0.0), null)
                    },
                    onIncrease = { onUpdateSet(set.id, null, set.weightKg + 2.5, null) },
                    onValueChange = { newWeight -> onUpdateSet(set.id, null, newWeight, null) },
                    modifier = Modifier.weight(1f),
                    isBodyweight = exercise.isBodyweight,
                    bodyweight = bodyweight
                )
            }
        }
    }
}

@Composable
private fun ResponsiveValueControl(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Decrease button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AccentOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = AccentOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Value display
            Card(
                backgroundColor = SurfaceColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = value.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Increase button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AccentOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onIncrease() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = AccentOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ResponsiveWeightControl(
    label: String,
    value: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    isBodyweight: Boolean = false,
    bodyweight: Double = 75.0
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Decrease button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AccentOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = AccentOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Weight input/display with BW+Xkg formatting
            var textValue by remember(value) { mutableStateOf(formatWeight(value)) }
            var isEditing by remember { mutableStateOf(false) }

            if (isEditing) {
                TextField(
                    value = textValue,
                    onValueChange = { newText ->
                        val filtered = newText.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1 && filtered.length <= 8) {
                            textValue = filtered
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = SurfaceColor.copy(alpha = 0.7f),
                        focusedIndicatorColor = AccentOrange,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AccentOrange
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val newValue = parseWeightInput(textValue)
                            onValueChange(newValue)
                            textValue = formatWeight(newValue)
                            isEditing = false
                        }
                    )
                )
            } else {
                Card(
                    backgroundColor = SurfaceColor.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isEditing = true
                            textValue = formatWeight(value)
                        }
                ) {
                    Text(
                        text = formatWeightDisplay(
                            weight = value,
                            isBodyweight = isBodyweight,
                            bodyweight = bodyweight,
                            mode = WeightDisplayMode.COMPACT
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Increase button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AccentOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onIncrease() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = AccentOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    uiState: com.bodyforge.presentation.viewmodel.WorkoutUiState,
    viewModel: WorkoutViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 Workout History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (uiState.completedWorkouts.isEmpty()) {
            item {
                EmptyHistoryCard()
            }
        } else {
            items(uiState.completedWorkouts) { workout ->
                HistoryWorkoutCard(
                    workout = workout,
                    onDelete = { viewModel.deleteWorkout(workout.id) }
                )
            }
        }
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
            Text("💪", fontSize = 48.sp)
            Text(
                text = "No Completed Workouts Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Complete your first workout to see it here!",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryWorkoutCard(
    workout: com.bodyforge.domain.models.Workout,
    onDelete: () -> Unit
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
            // Header with Name and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    val finishedDate = workout.finishedAt?.toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        text = "Completed: ${finishedDate?.date} at ${finishedDate?.time}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete workout",
                        tint = AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WorkoutStat(
                    label = "Duration",
                    value = "${workout.durationMinutes ?: 0} min"
                )
                WorkoutStat(
                    label = "Sets",
                    value = "${workout.performedSets}/${workout.totalSets}"
                )
                WorkoutStat(
                    label = "Volume",
                    value = "${workout.totalVolumePerformed.toInt()} kg"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exercises
            Text(
                text = "Exercises:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            workout.exercises.forEach { exerciseInWorkout ->
                Text(
                    text = "• ${exerciseInWorkout.exercise.name} (${exerciseInWorkout.performedSets} sets)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutStat(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

// =============================================================================
// BW+Xkg Display Logic - Smart weight formatting for bodyweight exercises
// =============================================================================

enum class WeightDisplayMode {
    COMPACT,    // "BW+10kg" for SetRows
    DETAILED,   // "BW+10kg (85kg)" for History
    TOTAL_ONLY  // "85kg" for calculations
}

private fun formatWeightDisplay(
    weight: Double,
    isBodyweight: Boolean,
    bodyweight: Double,
    mode: WeightDisplayMode = WeightDisplayMode.COMPACT
): String {
    if (!isBodyweight) {
        // Normal weighted exercise - just show the weight
        return "${formatWeight(weight)}kg"
    }

    // Bodyweight exercise logic
    val totalWeight = bodyweight + weight

    return when (mode) {
        WeightDisplayMode.COMPACT -> {
            if (weight == 0.0) {
                "BW" // Pure bodyweight, no additional weight
            } else {
                "BW+${formatWeight(weight)}kg"
            }
        }

        WeightDisplayMode.DETAILED -> {
            if (weight == 0.0) {
                "BW (${formatWeight(bodyweight)}kg)"
            } else {
                "BW+${formatWeight(weight)}kg (${formatWeight(totalWeight)}kg)"
            }
        }

        WeightDisplayMode.TOTAL_ONLY -> {
            "${formatWeight(totalWeight)}kg"
        }
    }
}

private fun getActualWeight(weight: Double, isBodyweight: Boolean, bodyweight: Double): Double {
    return if (isBodyweight) bodyweight + weight else weight
}

// Robust weight input parsing function
private fun parseWeightInput(input: String): Double {
    if (input.isEmpty() || input == ".") return 0.0

    // Handle common input patterns
    val cleanInput = when {
        input.startsWith(".") -> "0$input"  // ".5" -> "0.5"
        input.endsWith(".") -> input.dropLast(1)  // "100." -> "100"
        else -> input
    }

    // Try direct parsing first
    cleanInput.toDoubleOrNull()?.let { parsed ->
        return parsed.coerceIn(0.0, 9999.0)
    }

    // If that fails, try removing leading zeros and parsing again
    val withoutLeadingZeros = cleanInput.trimStart('0').ifEmpty { "0" }
    withoutLeadingZeros.toDoubleOrNull()?.let { parsed ->
        return parsed.coerceIn(0.0, 9999.0)
    }

    // If all else fails, extract just the numbers
    val numbersOnly = cleanInput.filter { it.isDigit() }
    return if (numbersOnly.isEmpty()) {
        0.0
    } else {
        numbersOnly.toDoubleOrNull()?.coerceIn(0.0, 9999.0) ?: 0.0
    }
}

// Helper functions
private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.3f", weight).trimEnd('0').trimEnd('.')
    }
}

private fun formatToThreeDecimals(value: Double): Double {
    return String.format("%.3f", value).toDouble()
}