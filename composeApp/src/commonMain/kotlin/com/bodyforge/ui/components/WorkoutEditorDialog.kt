package com.bodyforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyforge.data.Weights
import com.bodyforge.domain.models.Exercise
import com.bodyforge.domain.models.ExerciseInWorkout
import com.bodyforge.domain.models.Workout
import com.bodyforge.domain.models.WorkoutSet
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Full-screen editor for a logged workout (History and the Analytics progress detail both open
// it): rename, edit notes and duration, change reps/weights per set, add or remove sets and
// whole exercises. Nothing is persisted until Save.
@Composable
fun WorkoutEditorDialog(
    workout: Workout,
    onDismiss: () -> Unit,
    onSave: (Workout) -> Unit
) {
    var workoutName by remember(workout.id) { mutableStateOf(workout.name) }
    var workoutNotes by remember(workout.id) { mutableStateOf(workout.notes) }
    var editedWorkout by remember(workout.id) { mutableStateOf(workout) }
    var editingRepsSetId by remember { mutableStateOf<String?>(null) }
    var editingWeightSetId by remember { mutableStateOf<String?>(null) }
    var editingDuration by remember { mutableStateOf(false) }
    var showAddExercise by remember { mutableStateOf(false) }
    var removeExerciseId by remember { mutableStateOf<String?>(null) }
    var editingExerciseNoteId by remember { mutableStateOf<String?>(null) }
    var editingVariationId by remember { mutableStateOf<String?>(null) }
    var retroactiveVariation by remember { mutableStateOf<RetroactiveVariationRequest?>(null) }
    val variationScope = rememberCoroutineScope()
    var editingSetNoteId by remember { mutableStateOf<String?>(null) }
    val availableExercises by SharedWorkoutState.exercises.collectAsState()
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()

    val meta = remember(workout.id, editedWorkout.finishedAt) {
        val formatter = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
        "${formatter.format(Date(workout.startedAt.epochSeconds * 1000))} · ${editedWorkout.durationMinutes ?: 0} min"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Edit workout", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(meta, fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor, contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                        modifier = Modifier.height(44.dp)
                    ) { Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    Button(
                        onClick = { onSave(editedWorkout.copy(name = workoutName.trim(), notes = workoutNotes.trim())) },
                        enabled = workoutName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                        modifier = Modifier.height(44.dp)
                    ) { Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White) }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("Name", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = workoutName,
                            onValueChange = { workoutName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (workoutName.isEmpty()) Text("Workout name", color = TextSecondary, fontSize = 14.sp)
                                inner()
                            }
                        )
                    }

                    item {
                        Text("Notes", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = workoutNotes,
                            onValueChange = { workoutNotes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                            decorationBox = { inner ->
                                if (workoutNotes.isEmpty()) Text("How it felt, injuries, PRs…", color = TextSecondary, fontSize = 14.sp)
                                inner()
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duration", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                "${editedWorkout.durationMinutes ?: 0} min",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .background(SurfaceColor, RoundedCornerShape(8.dp))
                                    .clickable { editingDuration = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    items(editedWorkout.exercises, key = { it.exercise.id }) { exerciseInWorkout ->
                        EditorExerciseCard(
                            exerciseInWorkout = exerciseInWorkout,
                            onEditReps = { setId -> editingRepsSetId = setId },
                            onEditWeight = { setId -> editingWeightSetId = setId },
                            onStepReps = { setId, delta ->
                                editedWorkout = updateSetIn(editedWorkout, setId) { it.copy(reps = (it.reps + delta).coerceIn(0, 999)) }
                            },
                            onStepWeight = { setId, delta ->
                                editedWorkout = updateSetIn(editedWorkout, setId) {
                                    it.copy(weightKg = (it.weightKg + delta).coerceIn(0.0, 999.5))
                                }
                            },
                            onRemoveSet = { setId ->
                                editedWorkout = removeSetIn(editedWorkout, setId)
                            },
                            onAddSet = {
                                editedWorkout = addSetTo(editedWorkout, exerciseInWorkout.exercise.id)
                            },
                            onRemoveExercise = { removeExerciseId = exerciseInWorkout.exercise.id },
                            onEditNote = { editingExerciseNoteId = exerciseInWorkout.exercise.id },
                            onEditVariation = { editingVariationId = exerciseInWorkout.exercise.id },
                            onEditSetNote = { setId -> editingSetNoteId = setId }
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                                .clickable { showAddExercise = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ Add exercise", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    editingRepsSetId?.let { setId ->
        val current = editedWorkout.exercises.flatMap { it.sets }.firstOrNull { it.id == setId }?.reps ?: 0
        EditorNumberDialog(currentValue = current, label = "Reps", onDismiss = { editingRepsSetId = null }, onConfirm = { newReps ->
            editedWorkout = updateSetIn(editedWorkout, setId) { it.copy(reps = newReps.coerceIn(0, 999)) }
            editingRepsSetId = null
        })
    }

    editingWeightSetId?.let { setId ->
        val current = editedWorkout.exercises.flatMap { it.sets }.firstOrNull { it.id == setId }?.weightKg ?: 0.0
        EditorWeightDialog(currentWeight = current, onDismiss = { editingWeightSetId = null }, onConfirm = { newWeight ->
            editedWorkout = updateSetIn(editedWorkout, setId) { it.copy(weightKg = newWeight.coerceIn(0.0, 999.5)) }
            editingWeightSetId = null
        })
    }

    if (editingDuration) {
        EditorNumberDialog(currentValue = (editedWorkout.durationMinutes ?: 0).toInt(), label = "Duration (minutes)", onDismiss = { editingDuration = false }, onConfirm = { newMinutes ->
            editedWorkout = editedWorkout.copy(finishedAt = Instant.fromEpochSeconds(editedWorkout.startedAt.epochSeconds + newMinutes.coerceAtLeast(0) * 60L))
            editingDuration = false
        })
    }

    removeExerciseId?.let { exerciseId ->
        val name = editedWorkout.exercises.firstOrNull { it.exercise.id == exerciseId }?.exercise?.name ?: "Exercise"
        AlertDialog(onDismissRequest = { removeExerciseId = null }, title = { Text("Remove exercise?", fontWeight = FontWeight.Bold, color = TextPrimary) }, text = { Text("\"$name\" and its sets are removed from this logged workout.", color = TextSecondary) }, confirmButton = {
            Button(onClick = { editedWorkout = editedWorkout.copy(exercises = editedWorkout.exercises.filter { it.exercise.id != exerciseId }); removeExerciseId = null }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed), elevation = ButtonDefaults.elevation(0.dp)) { Text("Remove", color = Color.White, fontWeight = FontWeight.Bold) }
        }, dismissButton = { TextButton(onClick = { removeExerciseId = null }) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
    }

    if (showAddExercise) {
        EditorAddExerciseDialog(exercises = availableExercises.filter { ex -> editedWorkout.exercises.none { it.exercise.id == ex.id } }, onDismiss = { showAddExercise = false }, onPick = { picked ->
            editedWorkout = addExerciseTo(editedWorkout, picked)
            showAddExercise = false
        })
    }

    editingExerciseNoteId?.let { exerciseId ->
        val eiw = editedWorkout.exercises.firstOrNull { it.exercise.id == exerciseId }
        EditorTextDialog(title = "${eiw?.exercise?.name ?: "Exercise"} — note", initial = eiw?.notes ?: "", placeholder = "e.g. seat height, grip width, cues…", onDismiss = { editingExerciseNoteId = null }, onConfirm = { newNote ->
            editedWorkout = updateExerciseIn(editedWorkout, exerciseId) { it.copy(notes = newNote.trim()) }
            editingExerciseNoteId = null
        })
    }

    editingVariationId?.let { exerciseId ->
        val eiw = editedWorkout.exercises.firstOrNull { it.exercise.id == exerciseId }
        val knownVariations = remember(exerciseId, completedWorkouts) { completedWorkouts.flatMap { w -> w.exercises.filter { it.exercise.id == exerciseId } }.map { it.variation }.filter { it.isNotBlank() }.distinct() }
        EditorTextDialog(title = "${eiw?.exercise?.name ?: "Exercise"} — variation", initial = eiw?.variation ?: "", placeholder = "No variation", suggestions = knownVariations, onDismiss = { editingVariationId = null }, onConfirm = { newVariation ->
            val variation = newVariation.trim()
            editedWorkout = updateExerciseIn(editedWorkout, exerciseId) { it.copy(variation = variation) }
            editingVariationId = null
            if (variation.isNotBlank()) retroactiveVariation = RetroactiveVariationRequest(exerciseId, variation, editedWorkout)
        })
    }

    retroactiveVariation?.let { request ->
        RetroactiveVariationDialog(request = request, allWorkouts = completedWorkouts, onDismiss = { retroactiveVariation = null }, onConfirm = { selected ->
            retroactiveVariation = null
            variationScope.launch {
                selected.forEach { target -> SharedWorkoutState.workoutRepo.updateWorkout(updateExerciseIn(target, request.exerciseId) { it.copy(variation = request.variation) }) }
                SharedWorkoutState.loadCompletedWorkouts()
            }
        })
    }

    editingSetNoteId?.let { setId ->
        val current = editedWorkout.exercises.flatMap { it.sets }.firstOrNull { it.id == setId }?.notes ?: ""
        EditorTextDialog(title = "Set note", initial = current, placeholder = "e.g. paused reps, felt heavy…", onDismiss = { editingSetNoteId = null }, onConfirm = { newNote ->
            editedWorkout = updateSetIn(editedWorkout, setId) { it.copy(notes = newNote.trim()) }
            editingSetNoteId = null
        })
    }
}

private const val STRONG_PERFORMANCE_DEVIATION = 0.15
private enum class RetroactiveVariationOption(val title: String) { THIS_WORKOUT("Only this workout"), ALL_WORKOUTS("All workouts"), SAME_VARIATION_WORKOUTS("All workouts of this variation"), SPLIT_UNTIL_LAST_VARIATION("All workouts of the split until the last variation"), UNTIL_STRONG_SPIKE("Until the last strong spike in the performance graph") }
private data class RetroactiveVariationRequest(val exerciseId: String, val variation: String, val currentWorkout: Workout)
private fun RetroactiveVariationOption.description() = when (this) {
    RetroactiveVariationOption.THIS_WORKOUT -> "This workout only"
    RetroactiveVariationOption.ALL_WORKOUTS -> "Past completed workouts with this exercise that have no variation"
    RetroactiveVariationOption.SAME_VARIATION_WORKOUTS -> "Past workouts with the same template and this exercise that have no variation"
    RetroactiveVariationOption.SPLIT_UNTIL_LAST_VARIATION -> "Walk backwards through all workouts until an existing variation is found"
    RetroactiveVariationOption.UNTIL_STRONG_SPIKE -> "Walk backwards until performance differs by more than 15%"
}
private fun retroactiveCandidates(request: RetroactiveVariationRequest, completed: List<Workout>, option: RetroactiveVariationOption): List<Workout> {
    val past = completed.asSequence().filter { it.id != request.currentWorkout.id && it.isCompleted && it.startedAt < request.currentWorkout.startedAt }.filter { w -> w.exercises.any { it.exercise.id == request.exerciseId } }.sortedByDescending { it.startedAt }.toList()
    fun untagged(w: Workout) = w.exercises.first { it.exercise.id == request.exerciseId }.variation.isBlank()
    return when (option) {
        RetroactiveVariationOption.THIS_WORKOUT -> emptyList()
        RetroactiveVariationOption.ALL_WORKOUTS -> past.filter(::untagged)
        RetroactiveVariationOption.SAME_VARIATION_WORKOUTS -> past.filter { it.templateId == request.currentWorkout.templateId && untagged(it) }
        RetroactiveVariationOption.SPLIT_UNTIL_LAST_VARIATION -> past.takeWhile(::untagged)
        RetroactiveVariationOption.UNTIL_STRONG_SPIKE -> {
            val result = mutableListOf<Workout>()
            for (index in past.indices) {
                val score = bestEstimatedOneRm(past[index], request.exerciseId)
                val previous = past.drop(index + 1).mapNotNull { bestEstimatedOneRm(it, request.exerciseId) }.take(3)
                if (score != null && previous.size == 3 && previous.average() > 0.0 && kotlin.math.abs(score - previous.average()) / previous.average() > STRONG_PERFORMANCE_DEVIATION) break
                if (untagged(past[index])) result += past[index]
            }
            result
        }
    }
}
private fun bestEstimatedOneRm(workout: Workout, exerciseId: String): Double? = workout.exercises.firstOrNull { it.exercise.id == exerciseId }?.sets?.mapNotNull { set -> if (set.reps > 0 && set.weightKg > 0.0) set.weightKg * (1.0 + set.reps / 30.0) else null }?.maxOrNull()

@Composable
private fun RetroactiveVariationDialog(request: RetroactiveVariationRequest, allWorkouts: List<Workout>, onDismiss: () -> Unit, onConfirm: (List<Workout>) -> Unit) {
    var option by remember(request) { mutableStateOf(RetroactiveVariationOption.THIS_WORKOUT) }
    val selected = retroactiveCandidates(request, allWorkouts, option)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Apply variation retroactively?", fontWeight = FontWeight.Bold, color = TextPrimary) }, text = {
        Column(modifier = Modifier.fillMaxWidth()) {
            RetroactiveVariationOption.values().forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().clickable { option = item }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = option == item, onClick = { option = item }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                    Column(modifier = Modifier.padding(vertical = 4.dp)) { Text(item.title, color = TextPrimary, fontSize = 14.sp); Text(item.description(), color = TextSecondary, fontSize = 11.sp) }
                }
            }
            if (option != RetroactiveVariationOption.THIS_WORKOUT) {
                Spacer(Modifier.height(8.dp)); Text("Affected workouts (${selected.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (selected.isEmpty()) Text("No matching workouts", color = TextSecondary, fontSize = 12.sp) else LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(selected, key = { it.id }) { target ->
                        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(target.startedAt.epochSeconds * 1000)); Text("$date · ${target.name}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }
        }
    }, confirmButton = { Button(onClick = { onConfirm(selected) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) { Text("Confirm", color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
}

// Free-text entry for notes and variation labels, with optional one-tap suggestion chips.
@Composable
private fun EditorTextDialog(title: String, initial: String, placeholder: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit, suggestions: List<String> = emptyList()) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(12.dp), textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary), decorationBox = { inner -> if (text.isEmpty()) Text(placeholder, color = TextSecondary.copy(alpha = 0.7f), fontSize = 15.sp); inner() })
            if (suggestions.isNotEmpty()) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { suggestions.take(4).forEach { label -> Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (text == label) Color.White else AccentPurple, maxLines = 1, softWrap = false, modifier = Modifier.background(if (text == label) AccentPurple else AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).clickable { text = label }.padding(horizontal = 10.dp, vertical = 6.dp)) } }
        }
    }, confirmButton = { Button(onClick = { onConfirm(text) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
}

@Composable
private fun EditorExerciseCard(exerciseInWorkout: ExerciseInWorkout, onEditReps: (String) -> Unit, onEditWeight: (String) -> Unit, onStepReps: (String, Int) -> Unit, onStepWeight: (String, Double) -> Unit, onRemoveSet: (String) -> Unit, onAddSet: () -> Unit, onRemoveExercise: () -> Unit, onEditNote: () -> Unit, onEditVariation: () -> Unit, onEditSetNote: (String) -> Unit) {
    val exercise = exerciseInWorkout.exercise
    Card(backgroundColor = CardBackground, shape = RoundedCornerShape(14.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
                if (exercise.isBodyweight) Text("BW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGreen, modifier = Modifier.background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                Text("✕", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AccentRed, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onRemoveExercise).padding(horizontal = 10.dp, vertical = 8.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (exerciseInWorkout.variation.isBlank()) "＋ variation" else exerciseInWorkout.variation, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (exerciseInWorkout.variation.isBlank()) TextSecondary.copy(alpha = 0.7f) else AccentPurple, maxLines = 1, softWrap = false, modifier = Modifier.background(if (exerciseInWorkout.variation.isBlank()) Color.Transparent else AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).clickable(onClick = onEditVariation).padding(horizontal = 6.dp, vertical = 3.dp))
                Text(if (exerciseInWorkout.notes.isBlank()) "＋ note" else "✎ ${exerciseInWorkout.notes}", fontSize = 12.sp, color = if (exerciseInWorkout.notes.isBlank()) TextSecondary.copy(alpha = 0.7f) else TextSecondary, modifier = Modifier.weight(1f).clickable(onClick = onEditNote).padding(vertical = 3.dp))
            }
            Spacer(Modifier.height(10.dp)); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) { Text("Set", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp)); Text("Reps", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)); Text(if (exercise.isBodyweight) "+ ${Weights.unit}" else Weights.unit, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1.15f)); Spacer(Modifier.width(32.dp)) }
            exerciseInWorkout.sets.forEachIndexed { index, set -> Spacer(Modifier.height(6.dp)); EditorSetRow(setNumber = index + 1, set = set, isBodyweight = exercise.isBodyweight, onEditReps = { onEditReps(set.id) }, onEditWeight = { onEditWeight(set.id) }, onStepReps = { delta -> onStepReps(set.id, delta) }, onStepWeight = { delta -> onStepWeight(set.id, delta) }, onRemove = { onRemoveSet(set.id) }); Text(if (set.notes.isBlank()) "＋ note" else "✎ ${set.notes}", fontSize = 12.sp, color = if (set.notes.isBlank()) TextSecondary.copy(alpha = 0.6f) else TextSecondary, modifier = Modifier.padding(start = 25.dp, top = 3.dp).clickable { onEditSetNote(set.id) }.padding(vertical = 2.dp)) }
            Spacer(Modifier.height(8.dp)); Box(modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(11.dp)).border(BorderStroke(1.dp, TextSecondary.copy(alpha = 0.35f)), RoundedCornerShape(11.dp)).clickable(onClick = onAddSet), contentAlignment = Alignment.Center) { Text("+ Add set", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary) }
        }
    }
}

@Composable
private fun EditorSetRow(setNumber: Int, set: WorkoutSet, isBodyweight: Boolean, onEditReps: () -> Unit, onEditWeight: () -> Unit, onStepReps: (Int) -> Unit, onStepWeight: (Double) -> Unit, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(SurfaceColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("$setNumber", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
        EditorCapsule(value = "${set.reps}", valueColor = TextPrimary, onMinus = { if (set.reps > 0) onStepReps(-1) }, onPlus = { onStepReps(1) }, onTapValue = onEditReps, modifier = Modifier.weight(1f))
        val weightLabel = if (isBodyweight) when { set.weightKg > 0 -> "+${Weights.format(set.weightKg)}"; set.weightKg < 0 -> Weights.format(set.weightKg); else -> "0" } else Weights.format(set.weightKg)
        EditorCapsule(value = weightLabel, valueColor = if (isBodyweight) AccentGreen else TextPrimary, onMinus = { if (set.weightKg > 0) onStepWeight(-Weights.toKg(com.bodyforge.presentation.state.SettingsState.weightStep)) }, onPlus = { onStepWeight(Weights.toKg(com.bodyforge.presentation.state.SettingsState.weightStep)) }, onTapValue = onEditWeight, modifier = Modifier.weight(1.15f))
        Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentRed, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onRemove).padding(vertical = 13.dp))
    }
}

@Composable
private fun EditorCapsule(value: String, valueColor: Color, onMinus: () -> Unit, onPlus: () -> Unit, onTapValue: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = TextSecondary.copy(alpha = 0.25f)
    Row(modifier = modifier.height(46.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceColor.copy(alpha = 0.45f)).border(1.dp, borderColor, RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(30.dp).fillMaxHeight().clickable(onClick = onMinus), contentAlignment = Alignment.Center) { Text("−", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        Box(Modifier.width(1.dp).fillMaxHeight().background(borderColor))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onTapValue), contentAlignment = Alignment.Center) {
            var fitSize by remember(value) { mutableStateOf(16.sp) }
            Text(value, color = valueColor, fontSize = fitSize, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, onTextLayout = { if (it.hasVisualOverflow && fitSize.value > 9f) fitSize = (fitSize.value - 1f).sp }, modifier = Modifier.padding(horizontal = 3.dp))
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(borderColor))
        Box(modifier = Modifier.width(30.dp).fillMaxHeight().clickable(onClick = onPlus), contentAlignment = Alignment.Center) { Text("+", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
    }
}
private fun updateExerciseIn(workout: Workout, exerciseId: String, transform: (ExerciseInWorkout) -> ExerciseInWorkout): Workout { val exercise = workout.exercises.firstOrNull { it.exercise.id == exerciseId } ?: return workout; return workout.updateExercise(exerciseId, transform(exercise)) }
private fun updateSetIn(workout: Workout, setId: String, transform: (WorkoutSet) -> WorkoutSet): Workout { val exercise = workout.exercises.firstOrNull { e -> e.sets.any { it.id == setId } } ?: return workout; return workout.updateExercise(exercise.exercise.id, exercise.copy(sets = exercise.sets.map { if (it.id == setId) transform(it) else it })) }
private fun removeSetIn(workout: Workout, setId: String): Workout { val exercise = workout.exercises.firstOrNull { e -> e.sets.any { it.id == setId } } ?: return workout; return workout.updateExercise(exercise.exercise.id, exercise.copy(sets = exercise.sets.filter { it.id != setId })) }
private fun addSetTo(workout: Workout, exerciseId: String): Workout { val exercise = workout.exercises.firstOrNull { it.exercise.id == exerciseId } ?: return workout; val last = exercise.sets.lastOrNull(); val newSet = WorkoutSet.createEmpty(exerciseId, exercise.sets.size + 1, exercise.exercise.defaultRestTimeSeconds).copy(reps = last?.reps ?: 8, weightKg = last?.weightKg ?: 0.0, completed = true); return workout.updateExercise(exerciseId, exercise.copy(sets = exercise.sets + newSet)) }
private fun addExerciseTo(workout: Workout, exercise: Exercise): Workout { val newSets = (1..3).map { n -> WorkoutSet.createEmpty(exercise.id, n, exercise.defaultRestTimeSeconds).copy(reps = 8, completed = true) }; return workout.copy(exercises = workout.exercises + ExerciseInWorkout(exercise = exercise, sets = newSets, orderInWorkout = (workout.exercises.maxOfOrNull { it.orderInWorkout } ?: 0) + 1)) }

@Composable
private fun EditorAddExerciseDialog(exercises: List<Exercise>, onDismiss: () -> Unit, onPick: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.muscleGroups.any { m -> m.contains(query, ignoreCase = true) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add exercise", fontWeight = FontWeight.Bold, color = TextPrimary) }, text = {
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search") }, singleLine = true, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentOrange, unfocusedBorderColor = SurfaceColor), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { exercise -> Column(modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).clickable { onPick(exercise) }.padding(12.dp)) { Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary); Text(exercise.muscleGroups.joinToString(", "), fontSize = 12.sp, color = TextSecondary) } } }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
}
@Composable
private fun EditorNumberDialog(currentValue: Int, label: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }
    AlertDialog(onDismissRequest = onDismiss, text = { Column { Text("Edit $label", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(modifier = Modifier.height(16.dp)); BasicTextField(value = textValue, onValueChange = { newText -> val f = newText.filter { it.isDigit() }; if (f.length <= 4) textValue = f }, modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(16.dp), textStyle = TextStyle(fontSize = 24.sp, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), singleLine = true) } }, confirmButton = { Button(onClick = { onConfirm(textValue.toIntOrNull() ?: 0) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) { Text("OK", color = Color.White, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
}
@Composable
private fun EditorWeightDialog(currentWeight: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var textValue by remember { mutableStateOf(if (currentWeight > 0) Weights.format(currentWeight) else "") }
    AlertDialog(onDismissRequest = onDismiss, text = { Column { Text("Edit Weight (${Weights.unit})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(modifier = Modifier.height(16.dp)); BasicTextField(value = textValue, onValueChange = { newText -> val f = newText.filter { it.isDigit() || it == '.' }; if (f.count { it == '.' } <= 1 && f.length <= 7) textValue = f }, modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(8.dp)).padding(16.dp), textStyle = TextStyle(fontSize = 24.sp, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), singleLine = true) } }, confirmButton = { Button(onClick = { onConfirm(Weights.toKg(textValue.toDoubleOrNull() ?: 0.0)) }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange), elevation = ButtonDefaults.elevation(0.dp)) { Text("OK", color = Color.White, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
}
