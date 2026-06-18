package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.WorkoutTemplate
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.components.cards.CreateExerciseDialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// Normalises a routine name into a stable grouping key, e.g. "Upper Body" -> "upper_body".
// Blank input yields "" so the template is treated as ungrouped.
fun routineKey(routineName: String): String = buildString {
    var pendingSeparator = false
    for (char in routineName.lowercase()) {
        if (char.isLetterOrDigit()) {
            append(char)
            pendingSeparator = false
        } else if (!pendingSeparator) {
            append('_')
            pendingSeparator = true
        }
    }
}.trim('_')

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
fun TemplatesScreen(onStartWorkout: () -> Unit = {}) {
    val templates by SharedWorkoutState.templates.collectAsState()
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var deleteConfirmationTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var startConfirmTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    // Persists a new custom exercise via shared state and returns it so the template dialog
    // can select it immediately without navigating away.
    val onCreateExercise: suspend (String, List<String>, String, Boolean) -> Exercise =
        { name, muscleGroups, equipment, isBodyweight ->
            SharedWorkoutState.createCustomExercise(name, muscleGroups, equipment, isBodyweight)
        }

    // Launches the template's workout and, on success, asks the host to switch to the Workout tab.
    val launchTemplate: (WorkoutTemplate) -> Unit = { template ->
        coroutineScope.launch {
            val started = SharedWorkoutState.startWorkoutFromTemplate(template)
            if (started != null) onStartWorkout()
        }
    }
    // Guards against silently discarding an in-progress workout before starting a new one.
    val requestStart: (WorkoutTemplate) -> Unit = { template ->
        if (activeWorkout != null) startConfirmTemplate = template else launchTemplate(template)
    }

    LaunchedEffect(Unit) {
        SharedWorkoutState.loadTemplates()
        SharedWorkoutState.loadExercises()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋 My Templates", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Button(
                onClick = { showCreateTemplateDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
            templates.isEmpty() -> EmptyTemplatesCard { showCreateTemplateDialog = true }
            else -> LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val groupedByRoutine = templates.filter { it.routineId.isNotBlank() }.groupBy { it.routineId }
                val ungroupedTemplates = templates.filter { it.routineId.isBlank() }

                groupedByRoutine.forEach { (_, routineTemplates) ->
                    val variations = routineTemplates.sortedBy { it.variationLabel }
                    item {
                        RoutineHeader(
                            name = variations.first().routineName.ifBlank { "Routine" },
                            variationCount = variations.size
                        )
                    }
                    items(variations) { template ->
                        TemplateCard(
                            template = template,
                            exercises = exercises,
                            onStart = { requestStart(template) },
                            onEdit = { editingTemplate = template },
                            onDelete = { deleteConfirmationTemplate = template }
                        )
                    }
                }

                items(ungroupedTemplates) { template ->
                    TemplateCard(
                        template = template,
                        exercises = exercises,
                        onStart = { requestStart(template) },
                        onEdit = { editingTemplate = template },
                        onDelete = { deleteConfirmationTemplate = template }
                    )
                }
            }
        }
    }

    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            exercises = exercises,
            onCreateExercise = onCreateExercise,
            onDismiss = { showCreateTemplateDialog = false },
            onCreateTemplate = { name, selected, desc, routine, variation ->
                coroutineScope.launch {
                    val template = WorkoutTemplate(
                        id = "template_${Clock.System.now().epochSeconds}",
                        name = name,
                        exerciseIds = selected.map { it.id },
                        createdAt = Clock.System.now(),
                        description = desc,
                        routineId = routineKey(routine),
                        routineName = routine.trim(),
                        variationLabel = variation.trim()
                    )
                    SharedWorkoutState.templateRepo.saveTemplate(template)
                    SharedWorkoutState.loadTemplates()
                }
                showCreateTemplateDialog = false
            }
        )
    }

    editingTemplate?.let { template ->
        EditTemplateDialog(
            template = template,
            exercises = exercises,
            onCreateExercise = onCreateExercise,
            onDismiss = { editingTemplate = null },
            onUpdateTemplate = { updated ->
                coroutineScope.launch {
                    SharedWorkoutState.templateRepo.updateTemplate(updated)
                    SharedWorkoutState.loadTemplates()
                }
                editingTemplate = null
            }
        )
    }

    deleteConfirmationTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteConfirmationTemplate = null },
            title = { Text("Delete Template", color = TextPrimary) },
            text = { Text("Delete \"${template.name}\"?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            SharedWorkoutState.templateRepo.deleteTemplate(template.id)
                            SharedWorkoutState.loadTemplates()
                        }
                        deleteConfirmationTemplate = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmationTemplate = null }) { Text("Cancel", color = TextSecondary) } },
            backgroundColor = CardBackground
        )
    }

    startConfirmTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { startConfirmTemplate = null },
            title = { Text("Start new workout?", color = TextPrimary) },
            text = { Text("You already have a workout in progress. Starting \"${template.name}\" will finish the current one.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        startConfirmTemplate = null
                        launchTemplate(template)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) { Text("Start", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { startConfirmTemplate = null }) { Text("Cancel", color = TextSecondary) } },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun EmptyTemplatesCard(onCreateClick: () -> Unit) {
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("📋", fontSize = 48.sp)
            Text("No Templates Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Text("Create workout templates to quickly start your favorite routines", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Button(onClick = onCreateClick, colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue), shape = RoundedCornerShape(25.dp), elevation = ButtonDefaults.elevation(0.dp)) {
                Text("Create Your First Template", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemplateCard(template: WorkoutTemplate, exercises: List<com.bodyforge.domain.models.Exercise>, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val templateExercises = remember(template, exercises) { template.exerciseIds.mapNotNull { id -> exercises.firstOrNull { it.id == id } } }

    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(template.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (template.hasVariation) {
                            Text(
                                template.variationLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.background(AccentBlue, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text("${template.exerciseIds.size} exercises", fontSize = 14.sp, color = TextSecondary)
                    if (template.description.isNotEmpty()) Text(template.description, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("Edit", color = TextSecondary, fontSize = 12.sp) }
                    TextButton(onClick = onDelete) { Text("Delete", color = AccentRed, fontSize = 12.sp) }
                }
            }
            if (templateExercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(templateExercises.take(3).joinToString(", ") { it.name } + if (templateExercises.size > 3) " +${templateExercises.size - 3} more" else "", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Workout", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RoutineVariationFields(
    routineName: String,
    onRoutineChange: (String) -> Unit,
    variationLabel: String,
    onVariationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Routine grouping (optional)", fontSize = 12.sp, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = routineName,
                onValueChange = onRoutineChange,
                label = { Text("Routine") },
                placeholder = { Text("e.g., Upper") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor),
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = variationLabel,
                onValueChange = onVariationChange,
                label = { Text("Variation") },
                placeholder = { Text("A") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoutineHeader(name: String, variationCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🔁", fontSize = 16.sp)
        Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
        Text(
            "$variationCount variation${if (variationCount == 1) "" else "s"}",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun NewExerciseButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("New Exercise", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// Selected exercises shown in their saved order, with up/down controls to change the sequence.
@Composable
private fun SelectedOrderSection(
    selected: List<com.bodyforge.domain.models.Exercise>,
    onReorder: (List<com.bodyforge.domain.models.Exercise>) -> Unit
) {
    if (selected.size < 2) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Order (use arrows to reorder)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        selected.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}. ${exercise.name}", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        if (index > 0) {
                            val list = selected.toMutableList()
                            list[index] = list[index - 1]
                            list[index - 1] = exercise
                            onReorder(list)
                        }
                    },
                    enabled = index > 0,
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) { Text("↑", color = if (index > 0) AccentOrange else TextSecondary, fontSize = 18.sp) }
                TextButton(
                    onClick = {
                        if (index < selected.size - 1) {
                            val list = selected.toMutableList()
                            list[index] = list[index + 1]
                            list[index + 1] = exercise
                            onReorder(list)
                        }
                    },
                    enabled = index < selected.size - 1,
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) { Text("↓", color = if (index < selected.size - 1) AccentOrange else TextSecondary, fontSize = 18.sp) }
            }
        }
    }
}

@Composable
fun CreateTemplateDialog(exercises: List<com.bodyforge.domain.models.Exercise>, onCreateExercise: suspend (String, List<String>, String, Boolean) -> Exercise, onDismiss: () -> Unit, onCreateTemplate: (String, List<com.bodyforge.domain.models.Exercise>, String, String, String) -> Unit) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    var routineName by remember { mutableStateOf("") }
    var variationLabel by remember { mutableStateOf("") }
    var selectedExercises by remember { mutableStateOf(emptyList<com.bodyforge.domain.models.Exercise>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredExercises = remember(exercises, searchQuery) {
        exercises.filter { searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.muscleGroups.any { m -> m.contains(searchQuery, ignoreCase = true) } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.88f)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("📋 Create Template", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = templateName, onValueChange = { templateName = it }, label = { Text("Template Name") }, placeholder = { Text("e.g., Push Day") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = templateDescription, onValueChange = { templateDescription = it }, label = { Text("Description (optional)") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth(), maxLines = 2)
                RoutineVariationFields(routineName = routineName, onRoutineChange = { routineName = it }, variationLabel = variationLabel, onVariationChange = { variationLabel = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Exercises (${selectedExercises.size})", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    NewExerciseButton(onClick = { showCreateExerciseDialog = true })
                }
                SelectedOrderSection(selectedExercises) { selectedExercises = it }
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                filteredExercises.forEach { exercise ->
                    val isSelected = selectedExercises.contains(exercise)
                    Card(backgroundColor = if (isSelected) SelectedGreen else SurfaceColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { selectedExercises = if (isSelected) selectedExercises - exercise else selectedExercises + exercise }) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else TextPrimary)
                                    if (exercise.isBodyweight) Text("BW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGreen, modifier = Modifier.background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                Text(exercise.muscleGroups.joinToString(", "), fontSize = 12.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary)
                            }
                            if (isSelected) Text("✓", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (templateName.isNotBlank() && selectedExercises.isNotEmpty()) onCreateTemplate(templateName, selectedExercises.toList(), templateDescription, routineName, variationLabel) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen), enabled = templateName.isNotBlank() && selectedExercises.isNotEmpty(), elevation = ButtonDefaults.elevation(0.dp)) { Text("Create", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    // Inline exercise creation: stays layered over this dialog so template progress is kept,
    // and the freshly created exercise is selected automatically.
    CreateExerciseDialog(
        showDialog = showCreateExerciseDialog,
        onDismiss = { showCreateExerciseDialog = false },
        onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
            scope.launch {
                val created = onCreateExercise(name, muscleGroups, equipment, isBodyweight)
                selectedExercises = selectedExercises + created
            }
        }
    )
}

@Composable
private fun EditTemplateDialog(template: WorkoutTemplate, exercises: List<com.bodyforge.domain.models.Exercise>, onCreateExercise: suspend (String, List<String>, String, Boolean) -> Exercise, onDismiss: () -> Unit, onUpdateTemplate: (WorkoutTemplate) -> Unit) {
    var templateName by remember { mutableStateOf(template.name) }
    var templateDescription by remember { mutableStateOf(template.description) }
    var routineName by remember { mutableStateOf(template.routineName) }
    var variationLabel by remember { mutableStateOf(template.variationLabel) }
    var selectedExercises by remember { mutableStateOf(template.exerciseIds.mapNotNull { id -> exercises.firstOrNull { it.id == id } }) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredExercises = remember(exercises, searchQuery) {
        exercises.filter { searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.muscleGroups.any { m -> m.contains(searchQuery, ignoreCase = true) } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.88f)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Edit Template", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = templateName, onValueChange = { templateName = it }, label = { Text("Template Name") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = templateDescription, onValueChange = { templateDescription = it }, label = { Text("Description") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth(), maxLines = 2)
                RoutineVariationFields(routineName = routineName, onRoutineChange = { routineName = it }, variationLabel = variationLabel, onVariationChange = { variationLabel = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Exercises (${selectedExercises.size})", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    NewExerciseButton(onClick = { showCreateExerciseDialog = true })
                }
                SelectedOrderSection(selectedExercises) { selectedExercises = it }
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                filteredExercises.forEach { exercise ->
                    val isSelected = selectedExercises.contains(exercise)
                    Card(backgroundColor = if (isSelected) SelectedGreen else SurfaceColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { selectedExercises = if (isSelected) selectedExercises - exercise else selectedExercises + exercise }) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else TextPrimary)
                                Text(exercise.muscleGroups.joinToString(", "), fontSize = 12.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary)
                            }
                            if (isSelected) Text("✓", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (templateName.isNotBlank() && selectedExercises.isNotEmpty()) onUpdateTemplate(template.copy(name = templateName, description = templateDescription, exerciseIds = selectedExercises.map { it.id }, routineId = routineKey(routineName), routineName = routineName.trim(), variationLabel = variationLabel.trim())) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), enabled = templateName.isNotBlank() && selectedExercises.isNotEmpty(), elevation = ButtonDefaults.elevation(0.dp)) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    CreateExerciseDialog(
        showDialog = showCreateExerciseDialog,
        onDismiss = { showCreateExerciseDialog = false },
        onCreateExercise = { name, muscleGroups, equipment, isBodyweight ->
            scope.launch {
                val created = onCreateExercise(name, muscleGroups, equipment, isBodyweight)
                selectedExercises = selectedExercises + created
            }
        }
    )
}