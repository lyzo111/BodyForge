package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.presentation.state.SharedWorkoutState

// Colors
private val AccentOrange = Color(0xFFFF6B35)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

@Composable
fun LibraryScreen() {
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilters by remember { mutableStateOf(setOf<String>()) }
    var selectedEquipmentFilters by remember { mutableStateOf(setOf<String>()) }
    var showFilters by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }

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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 Exercise Library (${filteredExercises.size})",
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
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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

        // Search and Filter Section
        item {
            SearchFilterSection(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedMuscleFilters = selectedMuscleFilters,
                onMuscleFilterToggle = { muscle ->
                    selectedMuscleFilters = if (selectedMuscleFilters.contains(muscle)) {
                        selectedMuscleFilters - muscle
                    } else {
                        selectedMuscleFilters + muscle
                    }
                },
                selectedEquipmentFilters = selectedEquipmentFilters,
                onEquipmentFilterToggle = { equipment ->
                    selectedEquipmentFilters = if (selectedEquipmentFilters.contains(equipment)) {
                        selectedEquipmentFilters - equipment
                    } else {
                        selectedEquipmentFilters + equipment
                    }
                },
                showFilters = showFilters,
                onToggleFilters = { showFilters = !showFilters },
                onClearFilters = {
                    searchQuery = ""
                    selectedMuscleFilters = emptySet()
                    selectedEquipmentFilters = emptySet()
                },
                availableExercises = exercises
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
                NoExercisesFoundCard(
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
                ExerciseLibraryCard(
                    exercise = exercise,
                    onDelete = if (exercise.isCustom) {
                        { /* TODO: Delete exercise */ }
                    } else null
                )
            }
        }
    }

    // Create Exercise Dialog (placeholder)
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
}

@Composable
private fun SearchFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedMuscleFilters: Set<String>,
    onMuscleFilterToggle: (String) -> Unit,
    selectedEquipmentFilters: Set<String>,
    onEquipmentFilterToggle: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onClearFilters: () -> Unit,
    availableExercises: List<com.bodyforge.domain.models.Exercise>
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
            // Search Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = SurfaceColor,
                            shape = RoundedCornerShape(25.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = TextPrimary
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "🔍 Search exercises...",
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Button(
                    onClick = onToggleFilters,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (showFilters) AccentOrange else SurfaceColor
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = if (showFilters) "🔻" else "🔽",
                        fontSize = 16.sp
                    )
                }

                if (searchQuery.isNotEmpty() || selectedMuscleFilters.isNotEmpty() || selectedEquipmentFilters.isNotEmpty()) {
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.height(48.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text("❌ Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                val availableMuscleGroups = remember(availableExercises) {
                    availableExercises.flatMap { it.muscleGroups }.distinct().sorted()
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMuscleGroups) { muscle ->
                        FilterChip(
                            text = muscle,
                            isSelected = selectedMuscleFilters.contains(muscle),
                            onClick = { onMuscleFilterToggle(muscle) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Equipment Filter
                Text(
                    text = "🏋️ Equipment",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val availableEquipment = remember(availableExercises) {
                    availableExercises.map { it.equipmentNeeded }.distinct().sorted()
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableEquipment) { equipment ->
                        FilterChip(
                            text = equipment,
                            isSelected = selectedEquipmentFilters.contains(equipment),
                            onClick = { onEquipmentFilterToggle(equipment) }
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
            backgroundColor = if (isSelected) AccentOrange else SurfaceColor,
            contentColor = if (isSelected) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(20.dp),
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
private fun NoExercisesFoundCard(
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

            val message = when {
                searchQuery.isNotEmpty() && hasFilters -> "No exercises match \"$searchQuery\" with current filters"
                searchQuery.isNotEmpty() -> "No exercises match \"$searchQuery\""
                hasFilters -> "No exercises match current filters"
                else -> "No exercises available"
            }

            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (searchQuery.isNotEmpty() || hasFilters) {
                Button(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text("Clear Search & Filters", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    exercise: com.bodyforge.domain.models.Exercise,
    onDelete: (() -> Unit)? = null
) {
    Card(
        backgroundColor = CardBackground,
        elevation = 1.dp,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
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

                Text(
                    text = "${exercise.muscleGroups.joinToString(", ")} • ${exercise.equipmentNeeded}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("🗑️", fontSize = 16.sp)
                }
            }
        }
    }
}