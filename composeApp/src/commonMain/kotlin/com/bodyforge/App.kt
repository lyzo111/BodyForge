package com.bodyforge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn

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
private fun CreateWorkoutContent(
    uiState: com.bodyforge.presentation.viewmodel.WorkoutUiState,
    viewModel: WorkoutViewModel
) {
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<com.bodyforge.domain.models.Exercise?>(null) }

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilters by remember { mutableStateOf(setOf<String>()) }
    var selectedEquipmentFilters by remember { mutableStateOf(setOf<String>()) }
    var showFilters by remember { mutableStateOf(false) }

    // Filter exercises based on search and filters
    val filteredExercises = remember(uiState.availableExercises, searchQuery, selectedMuscleFilters, selectedEquipmentFilters) {
        uiState.availableExercises.filter { exercise ->
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
        // Templates Section
        item {
            TemplatesSection(
                templates = uiState.workoutTemplates,
                onLoadTemplate = { template -> viewModel.loadWorkoutTemplate(template) },
                onEditTemplate = { template -> viewModel.editWorkoutTemplate(template) },
                onDeleteTemplate = { template -> viewModel.deleteWorkoutTemplate(template) },
                onCreateTemplate = { showCreateTemplateDialog = true }
            )
        }

        // Sticky Selected Exercises Card (only when exercises are selected)
        if (uiState.selectedExercises.isNotEmpty()) {
            item {
                StickySelectedExercisesCard(
                    selectedExercises = uiState.selectedExercises,
                    isLoading = uiState.isLoading,
                    onStartWorkout = { viewModel.startWorkout() },
                    onSaveAsTemplate = { showCreateTemplateDialog = true }
                )
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
                availableExercises = uiState.availableExercises
            )
        }

        // Exercise Library Header with Create Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Exercise Library (${filteredExercises.size})",
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

        // Show message if no exercises found
        if (filteredExercises.isEmpty()) {
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
                ExerciseCard(
                    exercise = exercise,
                    isSelected = uiState.selectedExercises.contains(exercise),
                    onToggle = {
                        if (uiState.selectedExercises.contains(exercise)) {
                            viewModel.removeExerciseFromSelection(exercise)
                        } else {
                            viewModel.addExerciseToSelection(exercise)
                        }
                    },
                    onDelete = if (exercise.isCustom) { { showDeleteConfirmDialog = exercise } } else null
                )
            }
        }
    }

    // Dialogs
    CreateExerciseDialog(
        showDialog = showCreateExerciseDialog,
        onDismiss = { showCreateExerciseDialog = false },
        onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
            viewModel.createCustomExercise(name, muscleGroups, equipment, isBodyweight)
        }
    )

    CreateTemplateDialog(
        showDialog = showCreateTemplateDialog,
        selectedExercises = uiState.selectedExercises,
        onDismiss = { showCreateTemplateDialog = false },
        onCreateTemplate = { name -> viewModel.createWorkoutTemplate(name, uiState.selectedExercises) }
    )

    DeleteExerciseConfirmDialog(
        exercise = showDeleteConfirmDialog,
        onDismiss = { showDeleteConfirmDialog = null },
        onConfirmDelete = { exercise ->
            viewModel.deleteCustomExercise(exercise.id)
            showDeleteConfirmDialog = null
        }
    )
}

@Composable
private fun TemplatesSection(
    templates: List<com.bodyforge.domain.models.WorkoutTemplate>,
    onLoadTemplate: (com.bodyforge.domain.models.WorkoutTemplate) -> Unit,
    onEditTemplate: (com.bodyforge.domain.models.WorkoutTemplate) -> Unit,
    onDeleteTemplate: (com.bodyforge.domain.models.WorkoutTemplate) -> Unit,
    onCreateTemplate: () -> Unit
) {
    if (templates.isEmpty()) return

    Card(
        backgroundColor = Color(0xFF1E40AF).copy(alpha = 0.8f),
        elevation = 2.dp,
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
                    text = "💾 My Templates",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(
                    onClick = onCreateTemplate,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("+ New", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    TemplateChip(
                        template = template,
                        onLoad = { onLoadTemplate(template) },
                        onEdit = { onEditTemplate(template) },
                        onDelete = { onDeleteTemplate(template) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(
    template: com.bodyforge.domain.models.WorkoutTemplate,
    onLoad: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        backgroundColor = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onLoad() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = template.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "${template.exerciseIds.size} exercises",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("📝", fontSize = 12.sp)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("🗑️", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StickySelectedExercisesCard(
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    isLoading: Boolean,
    onStartWorkout: () -> Unit,
    onSaveAsTemplate: () -> Unit
) {
    Card(
        backgroundColor = SelectedGreen,
        elevation = 2.dp,
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save as Template Button
                    Button(
                        onClick = onSaveAsTemplate,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text(
                            text = "💾",
                            fontSize = 14.sp
                        )
                    }

                    // Start Workout Button
                    Button(
                        onClick = onStartWorkout,
                        enabled = !isLoading,
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
            }
        }
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

                // Filter Toggle Button
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

                // Clear Button (if filters active)
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
private fun ExerciseCard(
    exercise: com.bodyforge.domain.models.Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit,
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
private fun CreateTemplateDialog(
    showDialog: Boolean,
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    onDismiss: () -> Unit,
    onCreateTemplate: (String) -> Unit
) {
    if (!showDialog) return

    var templateName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💾 Create Workout Template",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Template Name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                BasicTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
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
                        if (templateName.isEmpty()) {
                            Text(
                                text = "e.g., Push Day, Pull Day, Leg Day",
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Text(
                    text = "This template will include ${selectedExercises.size} exercises:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(selectedExercises) { exercise ->
                        Text(
                            text = "• ${exercise.name}",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onCreateTemplate(templateName.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                enabled = templateName.isNotBlank() && selectedExercises.isNotEmpty(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Create Template", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun DeleteExerciseConfirmDialog(
    exercise: com.bodyforge.domain.models.Exercise?,
    onDismiss: () -> Unit,
    onConfirmDelete: (com.bodyforge.domain.models.Exercise) -> Unit
) {
    if (exercise == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🗑️ Delete Exercise",
                fontWeight = FontWeight.Bold,
                color = AccentRed
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete \"${exercise.name}\"?",
                    fontSize = 16.sp,
                    color = TextPrimary
                )

                Card(
                    backgroundColor = AccentRed.copy(alpha = 0.1f),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ This will:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRed
                        )
                        Text(
                            text = "• Hide this exercise from the library",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                        Text(
                            text = "• Keep it visible in past workouts",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Text(
                            text = "• This action cannot be undone",
                            fontSize = 13.sp,
                            color = AccentRed,
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(exercise) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Delete Exercise", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun BodyweightInputCard(
    bodyweight: Double,
    onBodyweightChange: (Double) -> Unit
) {
    Card(
        backgroundColor = Color(0xFF0F766E),
        elevation = 0.dp,
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
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        color = if (bodyweight > 30.0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }

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
                    elevation = ButtonDefaults.elevation(0.dp)
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
                    elevation = ButtonDefaults.elevation(0.dp)
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

// Helper functions
private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.3f", weight).trimEnd('0').trimEnd('.')
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

private fun parseNumberInput(input: String): Int {
    if (input.isEmpty()) return 0

    val withoutLeadingZeros = input.trimStart('0').ifEmpty { "0" }
    return withoutLeadingZeros.toIntOrNull() ?: 0
}

// Placeholder functions - need to add the remaining ones
@Composable
private fun WorkoutHeaderCard(workout: com.bodyforge.domain.models.Workout, onFinishWorkout: () -> Unit) {
    Text("WorkoutHeaderCard - TODO: Add implementation")
}

@Composable
private fun ActiveExerciseCard(
    exerciseInWorkout: com.bodyforge.domain.models.ExerciseInWorkout,
    bodyweight: Double,
    onUpdateSet: (String, Int?, Double?, Boolean?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit
) {
    Text("ActiveExerciseCard - TODO: Add implementation")
}

@Composable
private fun EmptyHistoryCard() {
    Text("EmptyHistoryCard - TODO: Add implementation")
}

@Composable
private fun HistoryWorkoutCard(
    workout: com.bodyforge.domain.models.Workout,
    bodyweight: Double,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Text("HistoryWorkoutCard - TODO: Add implementation")
}

@Composable
private fun EditWorkoutDialog(
    workout: com.bodyforge.domain.models.Workout?,
    bodyweight: Double,
    onDismiss: () -> Unit,
    onSaveWorkout: (com.bodyforge.domain.models.Workout) -> Unit
) {
    // EditWorkoutDialog - TODO: Add implementation
}