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

        // Simple placeholder for exercise cards
        item {
            Card(
                backgroundColor = CardBackground,
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏗️ Exercise Cards",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "ActiveExerciseCard components coming next...",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    var selectedExercises by remember { mutableStateOf(listOf<com.bodyforge.domain.models.Exercise>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilters by remember { mutableStateOf(setOf<String>()) }
    var selectedEquipmentFilters by remember { mutableStateOf(setOf<String>()) }
    var selectedTypeFilters by remember { mutableStateOf(setOf<String>()) }
    var showFilters by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var showExerciseDetails by remember { mutableStateOf<com.bodyforge.domain.models.Exercise?>(null) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }

    // Initialize exercises
    LaunchedEffect(Unit) {
        SharedWorkoutState.loadExercises()
    }

    // Filter exercises based on search and filters
    val filteredExercises = remember(exercises, searchQuery, selectedMuscleFilters, selectedEquipmentFilters, selectedTypeFilters) {
        exercises.filter { exercise ->
            val matchesSearch = searchQuery.isEmpty() ||
                    exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.muscleGroups.any { it.contains(searchQuery, ignoreCase = true) } ||
                    exercise.equipmentNeeded.contains(searchQuery, ignoreCase = true)

            val matchesMuscleFilter = selectedMuscleFilters.isEmpty() ||
                    exercise.muscleGroups.any { muscleGroup ->
                        selectedMuscleFilters.any { filter -> muscleGroup.contains(filter, ignoreCase = true) }
                    }

            val matchesEquipmentFilter = selectedEquipmentFilters.isEmpty() ||
                    selectedEquipmentFilters.contains(exercise.equipmentNeeded)

            val matchesTypeFilter = selectedTypeFilters.isEmpty() ||
                    selectedTypeFilters.any { filter ->
                        when (filter) {
                            "Compound" -> exercise.muscleGroups.size >= 2
                            "Isolation" -> exercise.muscleGroups.size == 1
                            "Bodyweight" -> exercise.isBodyweight
                            "Weighted" -> !exercise.isBodyweight
                            else -> true
                        }
                    }

            matchesSearch && matchesMuscleFilter && matchesEquipmentFilter && matchesTypeFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text("←", fontSize = 18.sp, color = TextPrimary)
                    }

                    Column {
                        Text(
                            text = "🏋️ Quick Workout",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select exercises to start training",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = { showCreateExerciseDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
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

        // Selected Exercises Card (sticky when exercises selected)
        if (selectedExercises.isNotEmpty()) {
            item {
                SelectedExercisesCard(
                    selectedExercises = selectedExercises,
                    onRemoveExercise = { exercise ->
                        selectedExercises = selectedExercises - exercise
                    },
                    onStartWorkout = { onStartWorkout(selectedExercises) },
                    onCreateTemplate = { showCreateTemplateDialog = true },
                    isLoading = isLoading
                )
            }
        }

        // Search and Filter Section
        item {
            SimpleSearchSection(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onClearFilters = {
                    searchQuery = ""
                    selectedMuscleFilters = emptySet()
                    selectedEquipmentFilters = emptySet()
                    selectedTypeFilters = emptySet()
                },
                filteredCount = filteredExercises.size,
                hasActiveFilters = searchQuery.isNotEmpty() || selectedMuscleFilters.isNotEmpty() || selectedEquipmentFilters.isNotEmpty() || selectedTypeFilters.isNotEmpty(),
                selectedMuscleFilters = selectedMuscleFilters,
                onMuscleFilterToggle = { muscle ->
                    selectedMuscleFilters = if (selectedMuscleFilters.contains(muscle)) {
                        selectedMuscleFilters - muscle
                    } else {
                        selectedMuscleFilters + muscle
                    }
                },
                selectedTypeFilters = selectedTypeFilters,
                onTypeFilterToggle = { type ->
                    selectedTypeFilters = if (selectedTypeFilters.contains(type)) {
                        selectedTypeFilters - type
                    } else {
                        selectedTypeFilters + type
                    }
                }
            )
        }

        // Loading indicator
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }
        }

        // Show message if no exercises found
        if (filteredExercises.isEmpty() && !isLoading) {
            item {
                SimpleNoExercisesCard(
                    searchQuery = searchQuery,
                    hasFilters = selectedMuscleFilters.isNotEmpty() || selectedEquipmentFilters.isNotEmpty() || selectedTypeFilters.isNotEmpty(),
                    onClearFilters = {
                        searchQuery = ""
                        selectedMuscleFilters = emptySet()
                        selectedEquipmentFilters = emptySet()
                        selectedTypeFilters = emptySet()
                    }
                )
            }
        } else {
            items(filteredExercises) { exercise ->
                SimpleExerciseCard(
                    exercise = exercise,
                    isSelected = selectedExercises.contains(exercise),
                    onToggle = {
                        selectedExercises = if (selectedExercises.contains(exercise)) {
                            selectedExercises - exercise
                        } else {
                            selectedExercises + exercise
                        }
                    },
                    onShowDetails = { showExerciseDetails = exercise }
                )
            }
        }
    }

    // Dialogs
    if (showCreateExerciseDialog) {
        CreateExerciseDialog(
            onDismiss = { showCreateExerciseDialog = false },
            onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
                // TODO: Implement exercise creation
                showCreateExerciseDialog = false
            }
        )
    }

    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            selectedExercises = selectedExercises,
            onDismiss = { showCreateTemplateDialog = false },
            onCreateTemplate = { templateName, description ->
                // Create template with selected exercises
                val newTemplate = com.bodyforge.domain.models.WorkoutTemplate.create(
                    name = templateName,
                    exercises = selectedExercises
                ).copy(description = description)

                // Save template (you'll need to implement this in SharedWorkoutState)
                // SharedWorkoutState.saveTemplate(newTemplate)

                showCreateTemplateDialog = false
            }
        )
    }

    showExerciseDetails?.let { exercise ->
        ExerciseDetailsDialog(
            exercise = exercise,
            onDismiss = { showExerciseDetails = null }
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

    // Initialize templates
    LaunchedEffect(Unit) {
        SharedWorkoutState.loadTemplates()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text("←", fontSize = 18.sp, color = TextPrimary)
                }

                Column {
                    Text(
                        text = "📋 From Template",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose an existing workout routine",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }
        } else if (templates.isEmpty()) {
            item {
                EmptyTemplatesState(
                    onCreateTemplate = { /* TODO: Navigate to template creation */ }
                )
            }
        } else {
            // Templates List
            items(templates) { template ->
                TemplateCard(
                    template = template,
                    onStartWorkout = { onStartFromTemplate(template) },
                    onPreview = { /* TODO: Preview template */ }
                )
            }
        }
    }
}

@Composable
private fun EmptyTemplatesState(
    onCreateTemplate: () -> Unit
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("📋", fontSize = 64.sp)

            Text(
                text = "No Templates Yet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Create your first workout template to quickly start your favorite routines",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCreateTemplate,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📝", fontSize = 16.sp)
                    Text(
                        "Create Your First Template",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: com.bodyforge.domain.models.WorkoutTemplate,
    onStartWorkout: () -> Unit,
    onPreview: () -> Unit
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 20.sp
                        )
                        Text(
                            text = template.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${template.exerciseIds.size} exercises • Created ${formatTemplateDate(template.createdAt)}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (template.description.isNotEmpty()) {
                        Text(
                            text = template.description,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Button(
                    onClick = onPreview,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    elevation = ButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("👁️", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview Button
                Button(
                    onClick = onPreview,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("👁️", fontSize = 14.sp)
                        Text(
                            "Preview",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                // Start Workout Button
                Button(
                    onClick = onStartWorkout,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f),
                    elevation = ButtonDefaults.elevation(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Start Workout",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Simple components for QuickWorkoutFlow
@Composable
private fun SelectedExercisesCard(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    onRemoveExercise: (com.bodyforge.domain.models.Exercise) -> Unit,
    onStartWorkout: () -> Unit,
    onCreateTemplate: () -> Unit,
    isLoading: Boolean
) {
    // Distinctive design - different from exercise selection cards
    Card(
        backgroundColor = AccentOrange,
        elevation = 8.dp, // Higher elevation
        shape = RoundedCornerShape(16.dp), // More rounded
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // More padding
        ) {
            // Header with prominent styling
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
                            text = "🎯",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "${selectedExercises.size} Exercise${if (selectedExercises.size != 1) "s" else ""} Ready",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = if (selectedExercises.size <= 2) {
                            selectedExercises.joinToString(" • ") { it.name }
                        } else {
                            "${selectedExercises.take(2).joinToString(" • ") { it.name }} • +${selectedExercises.size - 2} more"
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create Template Button
                Button(
                    onClick = onCreateTemplate,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📋", fontSize = 14.sp)
                        Text(
                            text = "Save Template",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Prominent Start Button
                Button(
                    onClick = onStartWorkout,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White,
                        contentColor = AccentOrange
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(4.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isLoading) "Starting..." else "Start Workout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Show exercise chips if more than 3 selected
            if (selectedExercises.size > 3) {
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedExercises) { exercise ->
                        SelectedExerciseChip(
                            exercise = exercise,
                            onRemove = { onRemoveExercise(exercise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedExerciseChip(
    exercise: com.bodyforge.domain.models.Exercise,
    onRemove: () -> Unit
) {
    Card(
        backgroundColor = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = exercise.name,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(20.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SimpleSearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    filteredCount: Int,
    hasActiveFilters: Boolean,
    selectedMuscleFilters: Set<String>,
    onMuscleFilterToggle: (String) -> Unit,
    selectedTypeFilters: Set<String>,
    onTypeFilterToggle: (String) -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }

    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Results Count and Filter Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 $filteredCount exercises found",
                    fontSize = 14.sp,
                    color = TextPrimary
                )

                Button(
                    onClick = { showFilters = !showFilters },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (showFilters || selectedMuscleFilters.isNotEmpty() || selectedTypeFilters.isNotEmpty()) AccentOrange else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = if (showFilters) "🔻 Filter" else "🔽 Filter",
                        fontSize = 12.sp,
                        color = if (showFilters || selectedMuscleFilters.isNotEmpty() || selectedTypeFilters.isNotEmpty()) Color.White else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(text = "🔍 Search exercises...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF334155),
                        textColor = TextPrimary,
                        focusedIndicatorColor = AccentOrange,
                        unfocusedIndicatorColor = Color.Transparent,
                        placeholderColor = TextSecondary
                    ),
                    singleLine = true
                )

                if (hasActiveFilters) {
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                        elevation = ButtonDefaults.elevation(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Filter Options (collapsible)
            if (showFilters) {
                Spacer(modifier = Modifier.height(16.dp))

                // Muscle Groups Filter
                Text(
                    text = "💪 Muscle Groups",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val muscleGroups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quadriceps", "Hamstrings", "Glutes", "Core")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(muscleGroups) { muscle ->
                        FilterChip(
                            text = muscle,
                            isSelected = selectedMuscleFilters.contains(muscle),
                            onClick = { onMuscleFilterToggle(muscle) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exercise Type Filter
                Text(
                    text = "🏋️ Exercise Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val exerciseTypes = listOf("Compound", "Isolation", "Bodyweight", "Weighted")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exerciseTypes) { type ->
                        FilterChip(
                            text = type,
                            isSelected = selectedTypeFilters.contains(type),
                            onClick = { onTypeFilterToggle(type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) AccentOrange else Color(0xFF334155),
            contentColor = if (isSelected) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SimpleExerciseCard(
    exercise: com.bodyforge.domain.models.Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onShowDetails: () -> Unit
) {
    Card(
        backgroundColor = if (isSelected) Color(0xFF065F46) else CardBackground,
        elevation = if (isSelected) 2.dp else 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onShowDetails() }
            ) {
                // Exercise name with badges
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

                    // Custom exercise badge
                    if (exercise.isCustom) {
                        Text(
                            text = "CUSTOM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange,
                            modifier = Modifier
                                .background(
                                    color = AccentOrange.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

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

                // Exercise details
                Text(
                    text = "${exercise.muscleGroups.joinToString(", ")} • ${exercise.equipmentNeeded}",
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                )
            }

            IconButton(onClick = onToggle) {
                Text(
                    text = if (isSelected) "✓" else "+",
                    fontSize = 20.sp,
                    color = if (isSelected) AccentGreen else AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SimpleNoExercisesCard(
    searchQuery: String,
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
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
            Text("🔍", fontSize = 48.sp)
            Text(
                text = "No Exercises Found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            if (searchQuery.isNotEmpty() || hasFilters) {
                Button(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text("Clear Search", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper function to format template creation date
private fun formatTemplateDate(instant: kotlinx.datetime.Instant): String {
    val localDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())

    val daysDiff = now.date.toEpochDays() - localDateTime.date.toEpochDays()

    return when {
        daysDiff == 0 -> "today"
        daysDiff == 1 -> "yesterday"
        daysDiff < 7 -> "$daysDiff days ago"
        daysDiff < 30 -> "${daysDiff / 7} weeks ago"
        else -> "${daysDiff / 30} months ago"
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

// Dialog Components
@Composable
private fun CreateTemplateDialog(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    onDismiss: () -> Unit,
    onCreateTemplate: (String, String) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📋 Save as Template",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${selectedExercises.size} exercises selected:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Text(
                    text = selectedExercises.joinToString(", ") { it.name },
                    fontSize = 12.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )

                TextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    placeholder = { Text("Template Name", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF334155),
                        textColor = TextPrimary,
                        focusedIndicatorColor = AccentOrange,
                        unfocusedIndicatorColor = Color.Transparent,
                        placeholderColor = TextSecondary
                    ),
                    singleLine = true
                )

                TextField(
                    value = templateDescription,
                    onValueChange = { templateDescription = it },
                    placeholder = { Text("Description (optional)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF334155),
                        textColor = TextPrimary,
                        focusedIndicatorColor = AccentOrange,
                        unfocusedIndicatorColor = Color.Transparent,
                        placeholderColor = TextSecondary
                    ),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onCreateTemplate(templateName, templateDescription)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                enabled = templateName.isNotBlank(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Save Template", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreateExercise: (String, List<String>, String, Boolean) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedMuscleGroups by remember { mutableStateOf(setOf<String>()) }
    var equipment by remember { mutableStateOf("") }
    var isBodyweight by remember { mutableStateOf(false) }

    val availableMuscleGroups = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Core"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "➕ Create Exercise",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    placeholder = { Text("Exercise Name", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF334155),
                        textColor = TextPrimary,
                        focusedIndicatorColor = AccentOrange,
                        unfocusedIndicatorColor = Color.Transparent,
                        placeholderColor = TextSecondary
                    ),
                    singleLine = true
                )

                Text(
                    text = "Muscle Groups:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                backgroundColor = if (isSelected) AccentOrange else Color(0xFF334155),
                                contentColor = if (isSelected) Color.White else TextSecondary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            elevation = ButtonDefaults.elevation(0.dp)
                        ) {
                            Text(
                                text = muscle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                TextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    placeholder = { Text("Equipment (e.g., Dumbbells)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF334155),
                        textColor = TextPrimary,
                        focusedIndicatorColor = AccentOrange,
                        unfocusedIndicatorColor = Color.Transparent,
                        placeholderColor = TextSecondary
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bodyweight Exercise",
                        fontSize = 14.sp,
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
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                enabled = exerciseName.isNotBlank() && selectedMuscleGroups.isNotEmpty(),
                elevation = ButtonDefaults.elevation(0.dp)
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
private fun ExerciseDetailsDialog(
    exercise: com.bodyforge.domain.models.Exercise,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.name,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (exercise.isCustom) {
                    Text(
                        text = "CUSTOM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange,
                        modifier = Modifier
                            .background(
                                color = AccentOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

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
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(
                    icon = "💪",
                    label = "Muscle Groups",
                    value = exercise.muscleGroups.joinToString(", ")
                )

                DetailRow(
                    icon = "🏋️",
                    label = "Equipment",
                    value = exercise.equipmentNeeded
                )

                DetailRow(
                    icon = "⏱️",
                    label = "Default Rest",
                    value = "${exercise.defaultRestTimeSeconds}s"
                )

                if (exercise.instructions.isNotEmpty()) {
                    DetailRow(
                        icon = "📝",
                        label = "Instructions",
                        value = exercise.instructions
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentOrange)
            }
        },
        backgroundColor = CardBackground
    )
}

@Composable
private fun DetailRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = TextPrimary
            )
        }
    }
}