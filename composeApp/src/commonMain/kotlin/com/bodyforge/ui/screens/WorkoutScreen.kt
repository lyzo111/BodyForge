package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.presentation.viewmodel.WorkoutViewModel
import com.bodyforge.ui.components.inputs.BodyweightInput
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Colors
private val AccentOrange = Color(0xFFFF6B35)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)

@Composable
fun WorkoutScreen() {
    val viewModel: WorkoutViewModel = viewModel()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val bodyweight by SharedWorkoutState.bodyweight.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeWorkout != null) {
            // Active Workout View
            ActiveWorkoutView(
                workout = activeWorkout!!,
                bodyweight = bodyweight,
                isLoading = isLoading,
                viewModel = viewModel
            )
        } else {
            // Quick Start Cards View
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
                    text = "Ready to workout? 💪",
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
                Spacer(modifier = Modifier.height(16.dp))

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
    workout: com.bodyforge.domain.models.Workout,
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

@Composable
private fun WorkoutHeaderCard(
    workout: com.bodyforge.domain.models.Workout,
    onFinishWorkout: () -> Unit,
    isLoading: Boolean
) {
    Card(
        backgroundColor = AccentGreen.copy(alpha = 0.8f),
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
                        text = workout.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${workout.exercises.size} exercises • ${workout.totalSets} sets",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onFinishWorkout,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White,
                        contentColor = AccentGreen
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = if (isLoading) "Finishing..." else "Finish",
                        fontWeight = FontWeight.Bold
                    )
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
        elevation = 3.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Exercise Header
            ExerciseHeader(
                exercise = exerciseInWorkout.exercise,
                completedSets = exerciseInWorkout.completedSets,
                totalSets = exerciseInWorkout.sets.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sets Table
            exerciseInWorkout.sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    exercise = exerciseInWorkout.exercise,
                    bodyweight = bodyweight,
                    onUpdateSet = { reps, weight, completed ->
                        onUpdateSet(set.id, reps, weight, completed)
                    },
                    onRemoveSet = if (exerciseInWorkout.sets.size > 1) {
                        { onRemoveSet(set.id) }
                    } else null
                )

                if (index < exerciseInWorkout.sets.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Set Button
            Button(
                onClick = onAddSet,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("➕", fontSize = 16.sp)
                    Text(
                        "Add Set",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseHeader(
    exercise: com.bodyforge.domain.models.Exercise,
    completedSets: Int,
    totalSets: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Bodyweight badge
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

            Text(
                text = exercise.muscleGroups.joinToString(" • "),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Progress indicator
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$completedSets/$totalSets",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (completedSets == totalSets && totalSets > 0) AccentGreen else TextPrimary
            )
            Text(
                text = "sets done",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    set: com.bodyforge.domain.models.WorkoutSet,
    exercise: com.bodyforge.domain.models.Exercise,
    bodyweight: Double,
    onUpdateSet: (Int?, Double?, Boolean?) -> Unit,
    onRemoveSet: (() -> Unit)? = null
) {
    val isCompleted = set.completed
    val backgroundColor = if (isCompleted) AccentGreen.copy(alpha = 0.1f) else Color(0xFF334155)

    Card(
        backgroundColor = backgroundColor,
        elevation = if (isCompleted) 1.dp else 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Set number
            Text(
                text = "$setNumber",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) AccentGreen else TextPrimary,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )

            // Reps input
            NumberInputField(
                value = if (set.reps > 0) set.reps.toString() else "",
                onValueChange = { newReps ->
                    onUpdateSet(newReps?.toInt(), null, null)
                },
                placeholder = "Reps",
                enabled = !isCompleted,
                modifier = Modifier.weight(1f)
            )

            // Weight input (with bodyweight handling)
            if (exercise.isBodyweight) {
                BodyweightDisplay(
                    additionalWeight = set.weightKg,
                    bodyweight = bodyweight,
                    onWeightChange = { newWeight ->
                        onUpdateSet(null, newWeight, null)
                    },
                    enabled = !isCompleted,
                    modifier = Modifier.weight(1.2f)
                )
            } else {
                NumberInputField(
                    value = if (set.weightKg > 0) formatWeight(set.weightKg) else "",
                    onValueChange = { newWeight ->
                        onUpdateSet(null, newWeight, null)
                    },
                    placeholder = "Weight",
                    suffix = "kg",
                    enabled = !isCompleted,
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Complete button
            Button(
                onClick = {
                    if (!isCompleted && set.reps > 0) {
                        onUpdateSet(null, null, true)
                    }
                },
                enabled = !isCompleted && set.reps > 0 && (set.weightKg > 0 || exercise.isBodyweight),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isCompleted) AccentGreen else AccentOrange,
                    disabledBackgroundColor = Color(0xFF475569)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text(
                    text = if (isCompleted) "✓" else "○",
                    color = if (isCompleted) Color.White else if (set.reps > 0 && (set.weightKg > 0 || exercise.isBodyweight)) Color.White else Color(0xFF64748B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Remove set button
            if (onRemoveSet != null) {
                IconButton(
                    onClick = onRemoveSet,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "🗑️",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberInputField(
    value: String,
    onValueChange: (Double?) -> Unit,
    placeholder: String,
    suffix: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = textValue,
            onValueChange = { newText ->
                val filtered = newText.filter { it.isDigit() || it == '.' }
                if (filtered.length <= 6) {
                    textValue = filtered
                    val parsed = if (filtered.isEmpty()) null else {
                        when {
                            filtered.endsWith(".") -> filtered.dropLast(1).toDoubleOrNull()
                            else -> filtered.toDoubleOrNull()
                        }
                    }
                    onValueChange(parsed)
                }
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            },
            enabled = enabled,
            modifier = if (suffix.isNotEmpty()) Modifier.weight(1f) else Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color(0xFF1E293B),
                textColor = if (enabled) TextPrimary else Color(0xFF64748B),
                focusedIndicatorColor = AccentOrange,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                disabledTextColor = Color(0xFF64748B),
                placeholderColor = Color(0xFF64748B)
            ),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        )

        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun BodyweightDisplay(
    additionalWeight: Double,
    bodyweight: Double,
    onWeightChange: (Double?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var textValue by remember(additionalWeight) {
        mutableStateOf(if (additionalWeight > 0) formatWeight(additionalWeight) else "")
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextField(
                value = textValue,
                onValueChange = { newText ->
                    val filtered = newText.filter { it.isDigit() || it == '.' }
                    if (filtered.length <= 6) {
                        textValue = filtered
                        val parsed = if (filtered.isEmpty()) 0.0 else {
                            when {
                                filtered.endsWith(".") -> filtered.dropLast(1).toDoubleOrNull() ?: 0.0
                                else -> filtered.toDoubleOrNull() ?: 0.0
                            }
                        }
                        onWeightChange(parsed)
                    }
                },
                placeholder = {
                    Text(
                        text = "+Weight",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFF1E293B),
                    textColor = if (enabled) TextPrimary else Color(0xFF64748B),
                    focusedIndicatorColor = AccentGreen,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    placeholderColor = Color(0xFF64748B)
                ),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = "kg",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Total weight display
        val totalWeight = bodyweight + additionalWeight
        Text(
            text = if (additionalWeight > 0) {
                "BW+${formatWeight(additionalWeight)}kg (${formatWeight(totalWeight)}kg)"
            } else {
                "BW (${formatWeight(bodyweight)}kg)"
            },
            fontSize = 10.sp,
            color = AccentGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// QuickWorkoutFlow with Enhanced Search & Filter
@Composable
private fun QuickWorkoutFlow(
    onBack: () -> Unit,
    onStartWorkout: (List<com.bodyforge.domain.models.Exercise>) -> Unit
) {
    // TODO: Implement QuickWorkoutFlow - this is a simplified version
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚧 Quick Workout Flow", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Coming soon...", fontSize = 16.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("← Back")
        }
    }
}

@Composable
private fun TemplateSelectionFlow(
    onBack: () -> Unit,
    onStartFromTemplate: (com.bodyforge.domain.models.WorkoutTemplate) -> Unit
) {
    // TODO: Implement TemplateSelectionFlow - this is a simplified version
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚧 Template Selection", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Coming soon...", fontSize = 16.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("← Back")
        }
    }
}

// Helper function for weight formatting
private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.1f", weight)
    }
}