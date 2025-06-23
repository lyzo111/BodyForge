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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.AlertDialog
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.wrapContentHeight

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
                HeaderSection()

                NavigationTabs(
                    activeTab = uiState.activeTab,
                    onTabSelected = { viewModel.setActiveTab(it) },
                    hasActiveWorkout = uiState.currentWorkout != null
                )

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

                uiState.error?.let { error ->
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }

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
        elevation = 0.dp, // Removed double border
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (bodyweight > 30.0) {
                            val newWeight = (bodyweight - 0.5).coerceAtLeast(30.0)
                            onBodyweightChange(newWeight)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (bodyweight > 30.0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        color = if (bodyweight > 30.0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Editable bodyweight input with leading zero removal
                var textValue by remember(bodyweight) { mutableStateOf(formatWeight(bodyweight)) }
                var isEditing by remember { mutableStateOf(false) }

                if (isEditing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1 && filtered.length <= 8) {
                                textValue = filtered
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val newValue = parseWeightInput(textValue).coerceIn(30.0, 999.0)
                                onBodyweightChange(newValue)
                                textValue = formatWeight(newValue)
                                isEditing = false
                            }
                        )
                    )
                } else {
                    Text(
                        text = "${formatWeight(bodyweight)} kg",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isEditing = true
                                textValue = formatWeight(bodyweight)
                            }
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (bodyweight < 999.0) {
                            val newWeight = (bodyweight + 0.5).coerceAtMost(999.0)
                            onBodyweightChange(newWeight)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (bodyweight < 999.0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
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
    var showCreateExerciseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sticky Selected Exercises Card (only when exercises are selected)
        if (uiState.selectedExercises.isNotEmpty()) {
            item {
                StickySelectedExercisesCard(
                    selectedExercises = uiState.selectedExercises,
                    isLoading = uiState.isLoading,
                    onStartWorkout = { viewModel.startWorkout() }
                )
            }
        }

        // Exercise Library Header with Create Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Exercise Library",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { showCreateExerciseDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("➕", fontSize = 14.sp)
                        Text(
                            text = "Create",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
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

    CreateExerciseDialog(
        showDialog = showCreateExerciseDialog,
        onDismiss = { showCreateExerciseDialog = false },
        onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
            viewModel.createCustomExercise(name, muscleGroups, equipment, isBodyweight)
        }
    )
}

@Composable
private fun StickySelectedExercisesCard(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    isLoading: Boolean,
    onStartWorkout: () -> Unit
) {
    Card(
        backgroundColor = SelectedGreen,
        elevation = 2.dp, // Reduced elevation to avoid double border
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
                    text = "${selectedExercises.size} Exercises Selected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = selectedExercises.take(2).joinToString(", ") { it.name } +
                            if (selectedExercises.size > 2) "..." else "",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onStartWorkout,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AccentOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp) // Removed double border
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                    Text(
                        text = if (isLoading) "Starting..." else "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
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
        elevation = if (isSelected) 2.dp else 1.dp, // Reduced elevation
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
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
                ) {
                    Text("Create Workout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val hasBodyweightExercises = currentWorkout.exercises.any { it.exercise.isBodyweight }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
        elevation = 2.dp, // Reduced elevation
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
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
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
        elevation = 2.dp, // Reduced elevation
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
                Text(
                    text = exerciseInWorkout.exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
            .padding(vertical = 2.dp),
        elevation = 0.dp // Removed double border
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
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

                Button(
                    onClick = { onUpdateSet(set.id, null, null, !set.completed) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (set.completed) AccentGreen else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
                ) {
                    if (set.completed) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "DONE",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResponsiveValueControl(
                    label = "Reps",
                    value = set.reps,
                    onDecrease = { if (set.reps > 0) onUpdateSet(set.id, set.reps - 1, null, null) },
                    onIncrease = { onUpdateSet(set.id, set.reps + 1, null, null) },
                    onValueChange = { newReps -> onUpdateSet(set.id, newReps, null, null) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                ResponsiveWeightControl(
                    label = if (exercise.isBodyweight) "BW+kg" else "Weight (kg)",
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
    onValueChange: ((Int) -> Unit)? = null,
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
            TextButton(
                onClick = onDecrease,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (onValueChange != null) {
                var textValue by remember(value) { mutableStateOf(value.toString()) }
                var isEditing by remember { mutableStateOf(false) }

                if (isEditing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() }
                            if (filtered.length <= 3) {
                                textValue = filtered
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = SurfaceColor.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val newValue = parseNumberInput(textValue).coerceIn(0, 999)
                                onValueChange(newValue)
                                textValue = newValue.toString()
                                isEditing = false
                            }
                        )
                    )
                } else {
                    Text(
                        text = value.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isEditing = true }
                            .background(
                                color = SurfaceColor.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = value.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = SurfaceColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            TextButton(
                onClick = onIncrease,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            TextButton(
                onClick = onDecrease,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            var textValue by remember(value) { mutableStateOf(formatWeight(value)) }
            var isEditing by remember { mutableStateOf(false) }

            if (isEditing) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        val filtered = newText.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1 && filtered.length <= 8) {
                            textValue = filtered
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(
                            color = SurfaceColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
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
                Text(
                    text = formatWeightDisplay(
                        weight = value,
                        isBodyweight = isBodyweight,
                        bodyweight = bodyweight,
                        mode = WeightDisplayMode.COMPACT_WITH_UNIT
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable {
                            isEditing = true
                            textValue = formatWeight(value)
                        }
                        .background(
                            color = SurfaceColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onIncrease,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HistoryContent(
    uiState: com.bodyforge.presentation.viewmodel.WorkoutUiState,
    viewModel: WorkoutViewModel
) {
    var editingWorkout by remember { mutableStateOf<com.bodyforge.domain.models.Workout?>(null) }

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
                    bodyweight = uiState.bodyweight,
                    onDelete = { viewModel.deleteWorkout(workout.id) },
                    onEdit = { editingWorkout = workout }
                )
            }
        }
    }

    EditWorkoutDialog(
        workout = editingWorkout,
        bodyweight = uiState.bodyweight,
        onDismiss = { editingWorkout = null },
        onSaveWorkout = { editedWorkout ->
            viewModel.updateWorkout(editedWorkout)
            editingWorkout = null
        }
    )
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        backgroundColor = CardBackground,
        elevation = 1.dp, // Reduced elevation
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
    bodyweight: Double,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp, // Reduced elevation
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit workout",
                            tint = AccentOrange
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
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Text(
                text = "Exercises:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            workout.exercises.forEach { exerciseInWorkout ->
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                ) {
                    Text(
                        text = "• ${exerciseInWorkout.exercise.name} (${exerciseInWorkout.performedSets} sets)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    exerciseInWorkout.sets.filter { it.completed }.forEachIndexed { index, set ->
                        Text(
                            text = "  Set ${index + 1}: ${set.reps} × ${
                                formatWeightDisplay(
                                    weight = set.weightKg,
                                    isBodyweight = exerciseInWorkout.exercise.isBodyweight,
                                    bodyweight = bodyweight,
                                    mode = WeightDisplayMode.DETAILED
                                )
                            }",
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
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

@Composable
private fun CreateExerciseDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCreateExercise: (String, List<String>, String, Boolean) -> Unit
) {
    if (!showDialog) return

    var exerciseName by remember { mutableStateOf("") }
    var selectedMuscleGroups by remember { mutableStateOf(setOf<String>()) }
    var equipment by remember { mutableStateOf("") }
    var isBodyweight by remember { mutableStateOf(false) }

    val availableMuscleGroups = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Core", "Traps"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Custom Exercise",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Exercise Name",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        BasicTextField(
                            value = exerciseName,
                            onValueChange = { exerciseName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = SurfaceColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = TextPrimary
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (exerciseName.isEmpty()) {
                                    Text(
                                        text = "e.g., Cable Lateral Raises",
                                        color = TextSecondary,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                item {
                    Column {
                        Text(
                            text = "Muscle Groups (select multiple)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(120.dp)
                        ) {
                            items(availableMuscleGroups) { muscle ->
                                val isSelected = selectedMuscleGroups.contains(muscle)
                                Button(
                                    onClick = {
                                        selectedMuscleGroups = if (isSelected) {
                                            selectedMuscleGroups - muscle
                                        } else {
                                            selectedMuscleGroups + muscle
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (isSelected) AccentOrange else SurfaceColor,
                                        contentColor = if (isSelected) Color.White else TextSecondary
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    elevation = ButtonDefaults.elevation(0.dp) // Removed double border
                                ) {
                                    Text(
                                        text = muscle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text(
                            text = "Equipment Needed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        BasicTextField(
                            value = equipment,
                            onValueChange = { equipment = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = SurfaceColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = TextPrimary
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (equipment.isEmpty()) {
                                    Text(
                                        text = "e.g., Dumbbells, Cable Machine",
                                        color = TextSecondary,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bodyweight Exercise",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = isBodyweight,
                            onCheckedChange = { isBodyweight = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentOrange,
                                checkedTrackColor = AccentOrange.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (exerciseName.isNotBlank() && selectedMuscleGroups.isNotEmpty()) {
                        onCreateExercise(
                            exerciseName.trim(),
                            selectedMuscleGroups.toList(),
                            equipment.trim().ifBlank { "None" },
                            isBodyweight
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                enabled = exerciseName.isNotBlank() && selectedMuscleGroups.isNotEmpty(),
                elevation = ButtonDefaults.elevation(0.dp) // Removed double border
            ) {
                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun EditWorkoutDialog(
    workout: com.bodyforge.domain.models.Workout?,
    bodyweight: Double,
    onDismiss: () -> Unit,
    onSaveWorkout: (com.bodyforge.domain.models.Workout) -> Unit
) {
    if (workout == null) return

    var editedWorkout by remember { mutableStateOf(workout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Workout: ${workout.name}",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(editedWorkout.exercises) { exerciseInWorkout ->
                    EditExerciseCard(
                        exerciseInWorkout = exerciseInWorkout,
                        bodyweight = bodyweight,
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
                    onSaveWorkout(editedWorkout)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                elevation = ButtonDefaults.elevation(0.dp) // Removed double border
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun EditExerciseCard(
    exerciseInWorkout: com.bodyforge.domain.models.ExerciseInWorkout,
    bodyweight: Double,
    onUpdateExercise: (com.bodyforge.domain.models.ExerciseInWorkout) -> Unit
) {
    Card(
        backgroundColor = SurfaceColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp // Removed double border
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = exerciseInWorkout.exercise.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            exerciseInWorkout.sets.forEachIndexed { index, set ->
                EditSetRow(
                    setNumber = index + 1,
                    set = set,
                    exercise = exerciseInWorkout.exercise,
                    bodyweight = bodyweight,
                    onUpdateSet = { updatedSet ->
                        val updatedSets = exerciseInWorkout.sets.toMutableList()
                        updatedSets[index] = updatedSet
                        onUpdateExercise(exerciseInWorkout.copy(sets = updatedSets))
                    }
                )
            }
        }
    }
}

@Composable
private fun EditSetRow(
    setNumber: Int,
    set: com.bodyforge.domain.models.WorkoutSet,
    exercise: com.bodyforge.domain.models.Exercise,
    bodyweight: Double,
    onUpdateSet: (com.bodyforge.domain.models.WorkoutSet) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Set $setNumber",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(50.dp)
        )

        var repsText by remember { mutableStateOf(set.reps.toString()) }
        BasicTextField(
            value = repsText,
            onValueChange = { newText ->
                repsText = newText.filter { it.isDigit() }
                val newReps = parseNumberInput(repsText).coerceIn(0, 999)
                onUpdateSet(set.copy(reps = newReps))
            },
            modifier = Modifier
                .width(80.dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp),
            textStyle = TextStyle(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = TextPrimary
            ),
            singleLine = true
        )

        var weightText by remember { mutableStateOf(formatWeight(set.weightKg)) }
        Row(
            modifier = Modifier.width(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = weightText,
                onValueChange = { newText ->
                    weightText = newText.filter { it.isDigit() || it == '.' }
                    val newWeight = parseWeightInput(weightText)
                    onUpdateSet(set.copy(weightKg = newWeight))
                },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                ),
                singleLine = true
            )
            Text("kg", fontSize = 12.sp, color = TextSecondary)
        }

        Switch(
            checked = set.completed,
            onCheckedChange = { completed ->
                onUpdateSet(
                    if (completed) set.complete() else set.copy(completed = false, completedAt = null)
                )
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentGreen,
                checkedTrackColor = AccentGreen.copy(alpha = 0.5f)
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

// Weight display logic with kg unit
enum class WeightDisplayMode {
    COMPACT,               // "BW+10kg" for SetRows
    COMPACT_WITH_UNIT,     // "BW+10kg" or "50kg" - shows unit
    DETAILED,              // "BW+10kg (85kg)" for History
    TOTAL_ONLY             // "85kg" for calculations
}

private fun formatWeightDisplay(
    weight: Double,
    isBodyweight: Boolean,
    bodyweight: Double,
    mode: WeightDisplayMode = WeightDisplayMode.COMPACT
): String {
    if (!isBodyweight) {
        return when (mode) {
            WeightDisplayMode.COMPACT -> formatWeight(weight)
            WeightDisplayMode.COMPACT_WITH_UNIT -> "${formatWeight(weight)}kg"
            WeightDisplayMode.DETAILED -> "${formatWeight(weight)}kg"
            WeightDisplayMode.TOTAL_ONLY -> "${formatWeight(weight)}kg"
        }
    }

    val totalWeight = bodyweight + weight

    return when (mode) {
        WeightDisplayMode.COMPACT -> {
            if (weight == 0.0) "BW" else "BW+${formatWeight(weight)}"
        }
        WeightDisplayMode.COMPACT_WITH_UNIT -> {
            if (weight == 0.0) "BW" else "BW+${formatWeight(weight)}kg"
        }
        WeightDisplayMode.DETAILED -> {
            if (weight == 0.0) {
                "BW (${formatWeight(bodyweight)}kg)"
            } else {
                "BW+${formatWeight(weight)}kg (${formatWeight(totalWeight)}kg)"
            }
        }
        WeightDisplayMode.TOTAL_ONLY -> "${formatWeight(totalWeight)}kg"
    }
}

private fun parseWeightInput(input: String): Double {
    if (input.isEmpty() || input == ".") return 0.0

    val cleanInput = when {
        input.startsWith(".") -> "0$input"
        input.endsWith(".") -> input.dropLast(1)
        else -> input
    }

    cleanInput.toDoubleOrNull()?.let { parsed ->
        return parsed.coerceIn(0.0, 9999.0)
    }

    val withoutLeadingZeros = cleanInput.trimStart('0').ifEmpty { "0" }
    withoutLeadingZeros.toDoubleOrNull()?.let { parsed ->
        return parsed.coerceIn(0.0, 9999.0)
    }

    val numbersOnly = cleanInput.filter { it.isDigit() }
    return if (numbersOnly.isEmpty()) {
        0.0
    } else {
        numbersOnly.toDoubleOrNull()?.coerceIn(0.0, 9999.0) ?: 0.0
    }
}

// Leading zero removal for number inputs
private fun parseNumberInput(input: String): Int {
    if (input.isEmpty()) return 0

    val withoutLeadingZeros = input.trimStart('0').ifEmpty { "0" }
    return withoutLeadingZeros.toIntOrNull() ?: 0
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.3f", weight).trimEnd('0').trimEnd('.')
    }
}