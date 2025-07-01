package com.bodyforge.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors
private val AccentOrange = Color(0xFFFF6B35)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)
private val SelectedGreen = Color(0xFF065F46)

@Composable
fun SelectedExercisesCard(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    onRemoveExercise: (com.bodyforge.domain.models.Exercise) -> Unit,
    onStartWorkout: () -> Unit,
    isLoading: Boolean
) {
    Card(
        backgroundColor = SelectedGreen,
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
                        text = "${selectedExercises.size} Exercise${if (selectedExercises.size != 1) "s" else ""} Selected",
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
                    enabled = !isLoading && selectedExercises.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
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

            if (selectedExercises.size > 2) {
                Spacer(modifier = Modifier.height(12.dp))

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
        shape = RoundedCornerShape(20.dp)
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
                maxLines = 1
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Text("✕", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EnhancedSearchFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedMuscleFilters: Set<String>,
    onMuscleFilterToggle: (String) -> Unit,
    selectedEquipmentFilters: Set<String>,
    onEquipmentFilterToggle: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onClearFilters: () -> Unit,
    availableExercises: List<com.bodyforge.domain.models.Exercise>,
    filteredCount: Int
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
            // Results Count
            Text(
                text = "📚 $filteredCount exercise${if (filteredCount != 1) "s" else ""} found",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
fun FilterChip(
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
fun SelectableExerciseCard(
    exercise: com.bodyforge.domain.models.Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onShowDetails: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        backgroundColor = if (isSelected) SelectedGreen else CardBackground,
        elevation = if (isSelected) 2.dp else 1.dp,
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onShowDetails() }
            ) {
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

                Text(
                    text = "${exercise.muscleGroups.joinToString(", ")} • ${exercise.equipmentNeeded}",
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button for custom exercises
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete exercise",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Add/Remove toggle button
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
}

@Composable
fun NoExercisesFoundCard(
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
fun CreateExerciseDialog(
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
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Core", "Upper Traps"
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
fun ExerciseDetailsDialog(
    exercise: com.bodyforge.domain.models.Exercise,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (exercise.isCustom && (onEdit != null || onDelete != null)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onEdit != null) {
                            TextButton(onClick = onEdit) {
                                Text("✏️", fontSize = 16.sp)
                            }
                        }
                        if (onDelete != null) {
                            TextButton(onClick = onDelete) {
                                Text("🗑️", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            text = "BODYWEIGHT",
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

                // Muscle Groups
                DetailRow(
                    icon = "💪",
                    label = "Muscle Groups",
                    value = exercise.muscleGroups.joinToString(", ")
                )

                // Equipment
                DetailRow(
                    icon = "🏋️",
                    label = "Equipment",
                    value = exercise.equipmentNeeded
                )

                // Rest Time
                DetailRow(
                    icon = "⏱️",
                    label = "Default Rest",
                    value = "${exercise.defaultRestTimeSeconds}s"
                )

                // Instructions (if available)
                if (exercise.instructions.isNotEmpty()) {
                    DetailRow(
                        icon = "📝",
                        label = "Instructions",
                        value = exercise.instructions
                    )
                }

                // Usage Stats (Placeholder)
                DetailRow(
                    icon = "📊",
                    label = "Usage",
                    value = "Used in X workouts • Last: Y days ago"
                )
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