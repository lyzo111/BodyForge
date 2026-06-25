package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.bodyforge.ui.components.pagerSafeHorizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bodyforge.ui.theme.*
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
import com.bodyforge.ui.components.EmojiIcon
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

// Stable accent colour per folder name, so routine/split folders are visually distinguishable.
private val folderPalette = listOf(
    Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFFF6B35),
    Color(0xFFEF4444), Color(0xFFEAB308), Color(0xFF06B6D4), Color(0xFFEC4899)
)
private fun folderColor(name: String): Color = folderPalette[(name.hashCode() and 0x7fffffff) % folderPalette.size]

@Composable
fun TemplatesScreen(listState: LazyListState, onStartWorkout: () -> Unit = {}) {
    val templates by SharedWorkoutState.templates.collectAsState()
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val splitAssignments by SharedWorkoutState.splitAssignments.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var deleteConfirmationTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var startConfirmTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var assigningSplitTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    // Templates list grouping: false = by routine (default), true = by split.
    var groupBySplit by remember { mutableStateOf(com.bodyforge.data.AppSettings.groupTemplatesBySplit) }
    // Which routine/split folders are expanded. Folders start collapsed, so a routine with
    // variations (e.g. Upper A / Upper B) shows as a single "Upper" entry until tapped open.
    val expandedRoutines = remember { mutableStateMapOf<String, Boolean>() }

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
            Text("My Templates", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Button(
                onClick = { showCreateTemplateDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        TextButton(
            onClick = { showImportDialog = true },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            Text("Import a shared template", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (templates.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Group by", fontSize = 13.sp, color = TextSecondary)
                GroupChip("Routine", !groupBySplit) { groupBySplit = false; com.bodyforge.data.AppSettings.groupTemplatesBySplit = false }
                GroupChip("Split", groupBySplit) { groupBySplit = true; com.bodyforge.data.AppSettings.groupTemplatesBySplit = true }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
            templates.isEmpty() -> EmptyTemplatesCard { showCreateTemplateDialog = true }
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (groupBySplit) {
                    val bySplit = templates.groupBy { splitAssignments[it.id]?.takeIf { s -> s.isNotBlank() } ?: "No split" }
                    val orderedSplits = bySplit.keys.sortedWith(compareBy({ it == "No split" }, { it }))
                    orderedSplits.forEach { splitName ->
                        val inSplit = bySplit[splitName] ?: emptyList()
                        val splitKey = "split_$splitName"
                        val isExpanded = expandedRoutines[splitKey] == true
                        item(key = splitKey) {
                            RoutineFolderHeader(
                                name = splitName,
                                subtitle = "${inSplit.size} template${if (inSplit.size == 1) "" else "s"}",
                                expanded = isExpanded,
                                onToggle = { expandedRoutines[splitKey] = !isExpanded }
                            )
                        }
                        if (isExpanded) {
                            items(inSplit, key = { "sp_${it.id}" }) { template ->
                                TemplateCard(
                                    template = template,
                                    exercises = exercises,
                                    split = splitAssignments[template.id],
                                    onStart = { requestStart(template) },
                                    onEdit = { editingTemplate = template },
                                    onDelete = { deleteConfirmationTemplate = template },
                                    onShare = { SharedWorkoutState.shareTemplate(template) },
                                    onAssignSplit = { assigningSplitTemplate = template }
                                )
                            }
                        }
                    }
                } else {
                    val groupedByRoutine = templates.filter { it.routineId.isNotBlank() }.groupBy { it.routineId }
                    val ungroupedTemplates = templates.filter { it.routineId.isBlank() }

                    groupedByRoutine.forEach { (routineId, routineTemplates) ->
                        val variations = routineTemplates.sortedBy { it.variationLabel }
                        val isExpanded = expandedRoutines[routineId] == true
                        item(key = "routine_$routineId") {
                            RoutineFolderHeader(
                                name = variations.first().routineName.ifBlank { "Routine" },
                                subtitle = "${variations.size} variation${if (variations.size == 1) "" else "s"}",
                                expanded = isExpanded,
                                onToggle = { expandedRoutines[routineId] = !isExpanded }
                            )
                        }
                        if (isExpanded) {
                            items(variations, key = { it.id }) { template ->
                                TemplateCard(
                                    template = template,
                                    exercises = exercises,
                                    split = splitAssignments[template.id],
                                    onStart = { requestStart(template) },
                                    onEdit = { editingTemplate = template },
                                    onDelete = { deleteConfirmationTemplate = template },
                                    onShare = { SharedWorkoutState.shareTemplate(template) },
                                    onAssignSplit = { assigningSplitTemplate = template }
                                )
                            }
                        }
                    }

                    items(ungroupedTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            exercises = exercises,
                            split = splitAssignments[template.id],
                            onStart = { requestStart(template) },
                            onEdit = { editingTemplate = template },
                            onDelete = { deleteConfirmationTemplate = template },
                            onShare = { SharedWorkoutState.shareTemplate(template) },
                            onAssignSplit = { assigningSplitTemplate = template }
                        )
                    }
                }
            }
        }
    }

    assigningSplitTemplate?.let { template ->
        AssignSplitDialog(
            current = splitAssignments[template.id] ?: "",
            existingSplits = splitAssignments.values.filter { it.isNotBlank() }.distinct().sorted(),
            onDismiss = { assigningSplitTemplate = null },
            onAssign = { name ->
                SharedWorkoutState.assignSplit(template.id, name)
                assigningSplitTemplate = null
            }
        )
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

    if (showImportDialog) {
        ImportTemplateDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                val shared = com.bodyforge.data.TemplateSharing.decode(text)
                if (shared != null) {
                    coroutineScope.launch { SharedWorkoutState.importSharedTemplate(shared) }
                    true
                } else {
                    false
                }
            }
        )
    }
}

@Composable
private fun ImportTemplateDialog(onDismiss: () -> Unit, onImport: (String) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Template", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text("Paste a shared BodyForge template code.", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; showError = false },
                    label = { Text("Template code") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Couldn't read a template from that text.", color = AccentRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (onImport(text)) onDismiss() else showError = true },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("Import", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

@Composable
private fun EmptyTemplatesCard(onCreateClick: () -> Unit) {
    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("No Templates Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Text("Create workout templates to quickly start your favorite routines", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Button(onClick = onCreateClick, colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue), shape = RoundedCornerShape(25.dp), elevation = ButtonDefaults.elevation(0.dp)) {
                Text("Create Your First Template", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemplateCard(template: WorkoutTemplate, exercises: List<com.bodyforge.domain.models.Exercise>, split: String?, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit, onAssignSplit: () -> Unit) {
    val templateExercises = remember(template, exercises) { template.exerciseIds.mapNotNull { id -> exercises.firstOrNull { it.id == id } } }
    var showAllExercises by remember { mutableStateOf(false) }

    Card(backgroundColor = CardBackground, elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (template.hasVariation) {
                    Text(
                        template.variationLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.background(AccentBlue, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    template.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("${template.exerciseIds.size} exercises", fontSize = 14.sp, color = TextSecondary)
            if (template.description.isNotEmpty()) Text(template.description, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Split", fontSize = 12.sp, color = TextSecondary)
                Text(
                    if (!split.isNullOrBlank()) split else "+ Add",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!split.isNullOrBlank()) Color.White else TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .background(if (!split.isNullOrBlank()) AccentOrange else SurfaceColor, RoundedCornerShape(6.dp))
                        .clickable { onAssignSplit() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (templateExercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val shownExercises = if (showAllExercises) templateExercises else templateExercises.take(3)
                Text(
                    shownExercises.joinToString(", ") { it.name },
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
                if (templateExercises.size > 3) {
                    Text(
                        if (showAllExercises) "Show less..." else "Show more...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp).clickable { showAllExercises = !showAllExercises }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Share", color = AccentBlue, fontSize = 12.sp) }
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Edit", color = TextSecondary, fontSize = 12.sp) }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Delete", color = AccentRed, fontSize = 12.sp) }
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

// A collapsible routine folder. Tapping toggles whether its variations are listed below it.
@Composable
private fun RoutineFolderHeader(name: String, subtitle: String, expanded: Boolean, onToggle: () -> Unit) {
    val accent = folderColor(name)
    Card(
        backgroundColor = SurfaceColor,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(accent, RoundedCornerShape(50)))
            Text(if (expanded) "▾" else "▸", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
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

@Composable
private fun GroupChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) AccentOrange else SurfaceColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AssignSplitDialog(current: String, existingSplits: List<String>, onDismiss: () -> Unit, onAssign: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Assign to split", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                Text("Common splits", fontSize = 12.sp, color = TextSecondary)
                val presetScrollState = rememberScrollState()
                Row(modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(presetScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PPL", "Upper/Lower", "Full Body", "Push", "Pull", "Legs", "Upper", "Lower", "Arms").forEach { s -> GroupChip(s, s == name) { name = s } }
                }
                com.bodyforge.ui.components.HScrollIndicator(presetScrollState)
                if (existingSplits.isNotEmpty()) {
                    Text("Pick existing", fontSize = 12.sp, color = TextSecondary)
                    val splitScrollState = rememberScrollState()
                    Row(modifier = Modifier.fillMaxWidth().pagerSafeHorizontalScroll(splitScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingSplits.forEach { s -> GroupChip(s, s == name) { name = s } }
                    }
                    com.bodyforge.ui.components.HScrollIndicator(splitScrollState)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Split name") },
                    placeholder = { Text("e.g., PPL, Upper/Lower") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAssign(name.trim()) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (current.isNotBlank()) TextButton(onClick = { onAssign("") }) { Text("Remove", color = AccentRed) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
            }
        },
        backgroundColor = CardBackground
    )
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

// Reorder lives in its own popup so the create/edit form stays compact; same up/down behaviour.
@Composable
private fun ReorderDialog(
    selected: List<com.bodyforge.domain.models.Exercise>,
    onReorder: (List<com.bodyforge.domain.models.Exercise>) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Reorder Exercises", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    SelectedOrderSection(selected, onReorder)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
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
    var showOrderDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredExercises = remember(exercises, searchQuery) {
        exercises.filter { searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.muscleGroups.any { m -> m.contains(searchQuery, ignoreCase = true) } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.88f)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmojiIcon("📋", Icons.Filled.Assignment, fontSize = 20.sp, iconSize = 22.dp)
                    Text("Create Template", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = templateName, onValueChange = { templateName = it }, label = { Text("Template Name") }, placeholder = { Text("e.g., Push Day") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = templateDescription, onValueChange = { templateDescription = it }, label = { Text("Description (optional)") }, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth(), maxLines = 2)
                RoutineVariationFields(routineName = routineName, onRoutineChange = { routineName = it }, variationLabel = variationLabel, onVariationChange = { variationLabel = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Exercises (${selectedExercises.size})", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    NewExerciseButton(onClick = { showCreateExerciseDialog = true })
                }
                Button(
                    onClick = { showOrderDialog = true },
                    enabled = selectedExercises.size >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = SurfaceColor,
                        disabledBackgroundColor = SurfaceColor.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        "↕ Reorder Exercises",
                        color = if (selectedExercises.size >= 2) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
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

    if (showOrderDialog) {
        ReorderDialog(
            selected = selectedExercises,
            onReorder = { selectedExercises = it },
            onDismiss = { showOrderDialog = false }
        )
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
    var showOrderDialog by remember { mutableStateOf(false) }
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
                Button(
                    onClick = { showOrderDialog = true },
                    enabled = selectedExercises.size >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = SurfaceColor,
                        disabledBackgroundColor = SurfaceColor.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        "↕ Reorder Exercises",
                        color = if (selectedExercises.size >= 2) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
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

    if (showOrderDialog) {
        ReorderDialog(
            selected = selectedExercises,
            onReorder = { selectedExercises = it },
            onDismiss = { showOrderDialog = false }
        )
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