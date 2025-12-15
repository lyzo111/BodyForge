package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.presentation.viewmodel.WorkoutViewModel
import com.bodyforge.ui.components.inputs.BodyweightInput

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
fun WorkoutScreen() {
    val viewModel: WorkoutViewModel = viewModel()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val bodyweight by SharedWorkoutState.bodyweight.collectAsState()
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
                viewModel = viewModel
            )
        } else {
            QuickStartView(
                isLoading = isLoading,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun QuickStartView(
    isLoading: Boolean,
    viewModel: WorkoutViewModel
) {
    var showQuickWorkoutFlow by remember { mutableStateOf(false) }
    var showTemplateSelection by remember { mutableStateOf(false) }

    if (showQuickWorkoutFlow) {
        QuickWorkoutFlow(
            onBack = { showQuickWorkoutFlow = false },
            onStartWorkout = { selectedExercises ->
                viewModel.startQuickWorkout(selectedExercises)
                showQuickWorkoutFlow = false
            }
        )
    } else if (showTemplateSelection) {
        TemplateSelectionFlow(
            onBack = { showTemplateSelection = false },
            onStartFromTemplate = { template ->
                viewModel.startWorkoutFromTemplate(template)
                showTemplateSelection = false
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
                    icon = "🏋️",
                    title = "Quick Workout",
                    subtitle = "Select exercises & go",
                    color = AccentOrange,
                    onClick = { showQuickWorkoutFlow = true },
                    enabled = !isLoading
                )
            }

            item {
                QuickStartCard(
                    icon = "📋",
                    title = "From Template",
                    subtitle = "Use existing routine",
                    color = AccentBlue,
                    onClick = { showTemplateSelection = true },
                    enabled = !isLoading
                )
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

@Composable
private fun QuickStartCard(
    icon: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        backgroundColor = color.copy(alpha = 0.9f),
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevation(0.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = icon,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
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
    viewModel: WorkoutViewModel
) {
    val hasBodyweightExercises = workout.exercises.any { it.exercise.isBodyweight }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hasBodyweightExercises) {
            item {
                BodyweightInput(
                    bodyweight = bodyweight,
                    onBodyweightChange = { SharedWorkoutState.updateBodyweight(it) }
                )
            }
        }

        item {
            WorkoutHeaderCard(
                workout = workout,
                onFinishWorkout = { viewModel.completeWorkout() },
                isLoading = isLoading
            )
        }

        items(workout.exercises) { exerciseInWorkout ->
            ActiveExerciseCard(
                exerciseInWorkout = exerciseInWorkout,
                bodyweight = bodyweight,
                onUpdateSet = { setId, reps, weight, completed ->
                    viewModel.updateSet(exerciseInWorkout.exercise.id, setId, reps, weight, completed)
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
                }
            )
        }
    }
}

@Composable
private fun WorkoutHeaderCard(
    workout: Workout,
    onFinishWorkout: () -> Unit,
    isLoading: Boolean
) {
    Card(
        backgroundColor = AccentBlue,
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
                        text = if (isLoading) "Finishing..." else "Finish Workout",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exerciseInWorkout: ExerciseInWorkout,
    bodyweight: Double,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit
) {
    val exercise = exerciseInWorkout.exercise

    Card(
        backgroundColor = CardBackground,
        elevation = 3.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Exercise Header with +/- set buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
                }

                // Set count with +/- buttons
                Row(
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sets
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
    }
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
                        if (!isCompleted && set.reps > 0) {
                            onUpdateSet(null, null, true)
                        }
                    },
                    enabled = !isCompleted && set.reps > 0 && (set.weightKg > 0 || exercise.isBodyweight),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isCompleted) AccentGreen else SurfaceColor.copy(alpha = 0.5f),
                        disabledBackgroundColor = SurfaceColor.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(36.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = if (isCompleted) "✓" else "○",
                        color = if (isCompleted) Color.White else TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reps and Weight controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reps control
                ValueControlGroup(
                    label = "Reps",
                    value = set.reps,
                    displayValue = set.reps.toString(),
                    onDecrement = {
                        if (set.reps > 0 && !isCompleted) {
                            onUpdateSet(set.reps - 1, null, null)
                        }
                    },
                    onIncrement = {
                        if (!isCompleted) {
                            onUpdateSet(set.reps + 1, null, null)
                        }
                    },
                    onValueChange = { newReps ->
                        if (!isCompleted) {
                            onUpdateSet(newReps, null, null)
                        }
                    },
                    enabled = !isCompleted,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Weight control
                if (exercise.isBodyweight) {
                    BodyweightValueControl(
                        label = "Weight",
                        additionalWeight = set.weightKg,
                        bodyweight = bodyweight,
                        onDecrement = {
                            if (set.weightKg > 0 && !isCompleted) {
                                onUpdateSet(null, (set.weightKg - 2.5).coerceAtLeast(0.0), null)
                            }
                        },
                        onIncrement = {
                            if (!isCompleted) {
                                onUpdateSet(null, set.weightKg + 2.5, null)
                            }
                        },
                        onValueChange = { newWeight ->
                            if (!isCompleted) {
                                onUpdateSet(null, newWeight, null)
                            }
                        },
                        enabled = !isCompleted,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ValueControlGroup(
                        label = "Weight",
                        value = set.weightKg.toInt(),
                        displayValue = "${formatWeight(set.weightKg)}kg",
                        onDecrement = {
                            if (set.weightKg > 0 && !isCompleted) {
                                onUpdateSet(null, (set.weightKg - 2.5).coerceAtLeast(0.0), null)
                            }
                        },
                        onIncrement = {
                            if (!isCompleted) {
                                onUpdateSet(null, set.weightKg + 2.5, null)
                            }
                        },
                        onValueChange = { newWeight ->
                            if (!isCompleted) {
                                onUpdateSet(null, newWeight.toDouble(), null)
                            }
                        },
                        enabled = !isCompleted,
                        isWeight = true,
                        currentWeight = set.weightKg,
                        onWeightChange = { newWeight ->
                            if (!isCompleted) {
                                onUpdateSet(null, newWeight, null)
                            }
                        },
                        modifier = Modifier.weight(1f)
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
                    .width(60.dp)
                    .height(40.dp)
                    .background(
                        color = SurfaceColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = enabled) { showEditDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) TextPrimary else TextSecondary
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
                    .widthIn(min = 70.dp)
                    .height(40.dp)
                    .background(
                        color = SurfaceColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = enabled) { showEditDialog = true }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayText = when {
                    additionalWeight > 0 -> "BW+${formatWeight(additionalWeight)}kg"
                    additionalWeight < 0 -> "BW${formatWeight(additionalWeight)}kg"
                    else -> "BW"
                }

                Text(
                    text = displayText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) AccentGreen else TextSecondary
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
                text = "Total: ${formatWeight(bodyweight + additionalWeight)}kg",
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
        title = { Text("Edit $label", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
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
        title = {
            Text(
                text = if (isBodyweight) "Edit Additional Weight" else "Edit Weight",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
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
                    val newAdditional = textValue.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "Total: ${formatWeight(totalWeight - currentWeight + newAdditional)}kg",
                        fontSize = 14.sp,
                        color = AccentGreen,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Text(
                    text = "kg",
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
                onClick = { onConfirm(textValue.toDoubleOrNull() ?: 0.0) },
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("← Back", color = TextPrimary)
            }

            Text(
                text = "Select Exercises",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Box(modifier = Modifier.width(80.dp))
        }

        // Selected exercises card
        if (selectedExercises.isNotEmpty()) {
            Card(
                backgroundColor = SelectedGreen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedExercises.size} Selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = selectedExercises.take(2).joinToString(", ") { it.name },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = { onStartWorkout(selectedExercises) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Start", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Search bar
        Card(
            backgroundColor = CardBackground,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredExercises) { exercise ->
                val isSelected = selectedExercises.contains(exercise)

                Card(
                    backgroundColor = if (isSelected) SelectedGreen else CardBackground,
                    shape = RoundedCornerShape(12.dp),
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
                            .padding(16.dp),
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
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )

                                if (exercise.isBodyweight) {
                                    Text(
                                        text = "BW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen,
                                        modifier = Modifier
                                            .background(
                                                AccentGreen.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = exercise.muscleGroups.joinToString(", "),
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (isSelected) AccentGreen else AccentOrange
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
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        SharedWorkoutState.loadTemplates()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("← Back", color = TextPrimary)
            }

            Text(
                text = "Select Template",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Box(modifier = Modifier.width(80.dp))
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
                    Text("📋", fontSize = 48.sp)
                    Text(
                        text = "No Templates Yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Create templates in the Templates tab",
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
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.1f", weight)
    }
}