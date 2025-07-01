package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.presentation.viewmodel.WorkoutViewModel
import com.bodyforge.ui.components.inputs.BodyweightInput

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

        // Placeholder for exercise cards - will be implemented in separate component
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

// Placeholder components - Enhanced QuickWorkoutFlow
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
    var showFilters by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var showExerciseDetails by remember { mutableStateOf<com.bodyforge.domain.models.Exercise?>(null) }

    // Initialize exercises
    LaunchedEffect(Unit) {
        SharedWorkoutState.loadExercises()
    }

    // Filter exercises based on search and filters
    val filteredExercises = remember(exercises, searchQuery, selectedMuscleFilters, selectedEquipmentFilters) {
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

            matchesSearch && matchesMuscleFilter && matchesEquipmentFilter
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
                },
                filteredCount = filteredExercises.size,
                hasActiveFilters = searchQuery.isNotEmpty() || selectedMuscleFilters.isNotEmpty() || selectedEquipmentFilters.isNotEmpty()
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
                    hasFilters = selectedMuscleFilters.isNotEmpty() || selectedEquipmentFilters.isNotEmpty(),
                    onClearFilters = {
                        searchQuery = ""
                        selectedMuscleFilters = emptySet()
                        selectedEquipmentFilters = emptySet()
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

    // Dialogs - Simple versions
    if (showCreateExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showCreateExerciseDialog = false },
            title = { Text("Create Exercise", color = TextPrimary) },
            text = { Text("Create Exercise Dialog - TODO", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showCreateExerciseDialog = false }) {
                    Text("OK")
                }
            },
            backgroundColor = CardBackground
        )
    }

    showExerciseDetails?.let { exercise ->
        AlertDialog(
            onDismissRequest = { showExerciseDetails = null },
            title = { Text(exercise.name, color = TextPrimary) },
            text = {
                Text(
                    "Muscle Groups: ${exercise.muscleGroups.joinToString(", ")}\nEquipment: ${exercise.equipmentNeeded}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showExerciseDetails = null }) {
                    Text("Close")
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
    // TODO: Implement template selection flow
    Text("TemplateSelectionFlow - Coming next...")
}

// Simple components for QuickWorkoutFlow
@Composable
private fun SelectedExercisesCard(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    onRemoveExercise: (com.bodyforge.domain.models.Exercise) -> Unit,
    onStartWorkout: () -> Unit,
    isLoading: Boolean
) {
    Card(
        backgroundColor = Color(0xFF065F46),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
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
                onClick = onStartWorkout,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text(
                    text = if (isLoading) "Starting..." else "Start",
                    color = Color.White,
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
    hasActiveFilters: Boolean
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📚 $filteredCount exercises found",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("🔍 Search exercises...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF334155),
                        textColor = TextPrimary
                    )
                )

                if (hasActiveFilters) {
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text("Clear", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
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
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary
                )
                Text(
                    text = exercise.muscleGroups.joinToString(", "),
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                )
            }

            IconButton(onClick = onToggle) {
                Text(
                    text = if (isSelected) "✓" else "+",
                    fontSize = 20.sp,
                    color = if (isSelected) AccentGreen else AccentOrange
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