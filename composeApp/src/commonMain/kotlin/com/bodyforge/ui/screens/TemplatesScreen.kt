package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.presentation.state.SharedWorkoutState

// Colors
private val AccentOrange = Color(0xFFFF6B35)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)
private val SelectedGreen = Color(0xFF065F46)

@Composable
fun TemplatesScreen() {
    val templates by SharedWorkoutState.templates.collectAsState()
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()

    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<com.bodyforge.domain.models.WorkoutTemplate?>(null) }
    var deleteConfirmationTemplate by remember { mutableStateOf<com.bodyforge.domain.models.WorkoutTemplate?>(null) }

    // Initialize templates and exercises
    LaunchedEffect(Unit) {
        SharedWorkoutState.loadTemplates()
        SharedWorkoutState.loadExercises()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 My Templates",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Button(
                onClick = { showCreateTemplateDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentOrange)
            }
        } else if (templates.isEmpty()) {
            EmptyTemplatesCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onUse = { startWorkoutFromTemplate(template) },
                        onEdit = { editingTemplate = template },
                        onDelete = { deleteConfirmationTemplate = template }
                    )
                }
            }
        }
    }

    // Create Template Dialog
    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            exercises = exercises,
            onDismiss = { showCreateTemplateDialog = false },
            onCreateTemplate = { templateName, selectedExercises, description ->
                createTemplate(templateName, selectedExercises, description)
                showCreateTemplateDialog = false
            }
        )
    }

    // Edit Template Dialog
    editingTemplate?.let { template ->
        EditTemplateDialog(
            template = template,
            exercises = exercises,
            onDismiss = { editingTemplate = null },
            onUpdateTemplate = { updatedTemplate ->
                updateTemplate(updatedTemplate)
                editingTemplate = null
            }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmationTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteConfirmationTemplate = null },
            title = { Text("Delete Template", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete \"${template.name}\"?\nThis action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTemplate(template)
                        deleteConfirmationTemplate = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationTemplate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun EmptyTemplatesCard() {
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
            Text("📋", fontSize = 48.sp)
            Text(
                text = "No Templates Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create workout templates to quickly start your favorite routines",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { /* TODO: Create template flow */ },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Create Your First Template", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: com.bodyforge.domain.models.WorkoutTemplate,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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
                    if (template.description.isNotEmpty()) {
                        Text(
                            text = template.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onEdit) {
                        Text("Edit", color = TextSecondary, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onUse,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text(
                    text = "Start Workout",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CreateTemplateDialog(
    exercises: List<com.bodyforge.domain.models.Exercise>,
    onDismiss: () -> Unit,
    onCreateTemplate: (String, List<com.bodyforge.domain.models.Exercise>, String) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    var selectedExercises by remember { mutableStateOf(setOf<com.bodyforge.domain.models.Exercise>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter exercises based on search
    val filteredExercises = remember(exercises, searchQuery) {
        exercises.filter { exercise ->
            searchQuery.isEmpty() ||
                    exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.muscleGroups.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📋 Create Template",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template Name") },
                        placeholder = { Text("e.g., Push Day, Full Body") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = SurfaceColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = templateDescription,
                        onValueChange = { templateDescription = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("e.g., Chest, Shoulders, Triceps") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = SurfaceColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    Text(
                        text = "Select Exercises (${selectedExercises.size} selected)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search exercises") },
                        placeholder = { Text("🔍 Search...") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = SurfaceColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(filteredExercises) { exercise ->
                    val isSelected = selectedExercises.contains(exercise)

                    Card(
                        backgroundColor = if (isSelected) SelectedGreen else SurfaceColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedExercises = if (isSelected) {
                                    selectedExercises - exercise
                                } else {
                                    selectedExercises + exercise
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Text(
                                    text = exercise.muscleGroups.joinToString(", "),
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                )
                            }

                            if (isSelected) {
                                Text("✓", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank() && selectedExercises.isNotEmpty()) {
                        onCreateTemplate(templateName, selectedExercises.toList(), templateDescription)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
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
private fun EditTemplateDialog(
    template: com.bodyforge.domain.models.WorkoutTemplate,
    exercises: List<com.bodyforge.domain.models.Exercise>,
    onDismiss: () -> Unit,
    onUpdateTemplate: (com.bodyforge.domain.models.WorkoutTemplate) -> Unit
) {
    var templateName by remember { mutableStateOf(template.name) }
    var templateDescription by remember { mutableStateOf(template.description) }
    var selectedExercises by remember {
        mutableStateOf(
            exercises.filter { exercise ->
                template.exerciseIds.contains(exercise.id)
            }.toSet()
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    // Filter exercises based on search
    val filteredExercises = remember(exercises, searchQuery) {
        exercises.filter { exercise ->
            searchQuery.isEmpty() ||
                    exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.muscleGroups.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "✏️ Edit Template",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template Name") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = SurfaceColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = templateDescription,
                        onValueChange = { templateDescription = it },
                        label = { Text("Description (optional)") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = SurfaceColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    Text(
                        text = "Select Exercises (${selectedExercises.size} selected)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search exercises") },
                        placeholder = { Text("🔍 Search...") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = SurfaceColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(filteredExercises) { exercise ->
                    val isSelected = selectedExercises.contains(exercise)

                    Card(
                        backgroundColor = if (isSelected) SelectedGreen else SurfaceColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedExercises = if (isSelected) {
                                    selectedExercises - exercise
                                } else {
                                    selectedExercises + exercise
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Text(
                                    text = exercise.muscleGroups.joinToString(", "),
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                )
                            }

                            if (isSelected) {
                                Text("✓", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank() && selectedExercises.isNotEmpty()) {
                        val updatedTemplate = template.copy(
                            name = templateName,
                            description = templateDescription,
                            exerciseIds = selectedExercises.map { it.id }
                        )
                        onUpdateTemplate(updatedTemplate)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                enabled = templateName.isNotBlank() && selectedExercises.isNotEmpty(),
                elevation = ButtonDefaults.elevation(0.dp)
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

// Template actions
private fun startWorkoutFromTemplate(template: com.bodyforge.domain.models.WorkoutTemplate) {
    // TODO: Implement start workout from template
    // This should load the exercises from the template and start a new workout
}

private fun createTemplate(name: String, exercises: List<com.bodyforge.domain.models.Exercise>, description: String) {
    // TODO: Implement template creation
}

private fun updateTemplate(template: com.bodyforge.domain.models.WorkoutTemplate) {
    // TODO: Implement template update
}

private fun deleteTemplate(template: com.bodyforge.domain.models.WorkoutTemplate) {
    // TODO: Implement template deletion
}