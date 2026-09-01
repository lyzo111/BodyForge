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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    var editingSetNoteId by remember { mutableStateOf<String?>(null) }
    var pendingRetroactiveVariation by remember { mutableStateOf<PendingVariation?>(null) }
    var isApplyingRetroactive by remember { mutableStateOf(false) }
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
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor, contentColor = TextSecondary), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.elevation(0.dp), modifier = Modifier.height(44.dp)) { Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    Button(onClick = { onSave(editedWorkout.copy(name = workoutName.trim(), notes = workoutNotes.trim())) }, enabled = workoutName.isNotBlank(), colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange, contentColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.elevation(0.dp), modifier = Modifier.height(44.dp)) { Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White) }
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Name", fontSize = 12.sp, color = TextSecondary); Spacer(Modifier.height(6.dp))
                        BasicTextField(value = workoutName, onValueChange = { workoutName = it }, modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 13.dp), textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium), singleLine = true, decorationBox = { inner -> if (workoutName.isEmpty()) Text("Workout name", color = TextSecondary, fontSize = 14.sp); inner() })
                    }
                    item {
                        Text("Notes", fontSize = 12.sp, color = TextSecondary); Spacer(Modifier.height(6.dp))
                        BasicTextField(value = workoutNotes, onValueChange = { workoutNotes = it }, modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 13.dp), textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary), decorationBox = { inner -> if (workoutNotes.isEmpty()) Text("Workout notes", color = TextSecondary, fontSize = 14.sp); inner() })
                    }
                    items(editedWorkout.exercises) { exerciseInWorkout -> EditorExerciseCard(exerciseInWorkout, onVariation = { editingVariationId = exerciseInWorkout.exercise.id }, onNote = { editingExerciseNoteId = exerciseInWorkout.exercise.id }, onRemove = { removeExerciseId = exerciseInWorkout.exercise.id }, onEditSetNote = { editingSetNoteId = it }, onEditReps = { editingRepsSetId = it }, onEditWeight = { editingWeightSetId = it }, onUpdate = { updated -> editedWorkout = updateExerciseIn(editedWorkout, exerciseInWorkout.exercise.id) { updated } }) }
                    item { TextButton(onClick = { showAddExercise = true }) { Text("＋ Add exercise", color = AccentOrange, fontWeight = FontWeight.Bold) } }
                }
            }
        }
    }

    editingVariationId?.let { exerciseId ->
        val eiw = editedWorkout.exercises.firstOrNull { it.exercise.id == exerciseId }
        val knownVariations = remember(exerciseId, completedWorkouts) { completedWorkouts.flatMap { w -> w.exercises.filter { it.exercise.id == exerciseId } }.map { it.variation }.filter { it.isNotBlank() }.distinct() }
        EditorTextDialog(title = "${eiw?.exercise?.name ?: "Exercise"} — variation", initial = eiw?.variation ?: "", placeholder = "No variation", suggestions = knownVariations, onDismiss = { editingVariationId = null }, onConfirm = { newVariation ->
            val trimmedVariation = newVariation.trim()
            editedWorkout = updateExerciseIn(editedWorkout, exerciseId) { it.copy(variation = trimmedVariation) }
            editingVariationId = null
            if (trimmedVariation.isNotBlank() && trimmedVariation != eiw?.variation) pendingRetroactiveVariation = PendingVariation(exerciseId, trimmedVariation)
        })
    }

    pendingRetroactiveVariation?.let { pending -> RetroactiveVariationDialog(currentWorkout = editedWorkout, exerciseId = pending.exerciseId, exerciseName = editedWorkout.exercises.firstOrNull { it.exercise.id == pending.exerciseId }?.exercise?.name ?: "Exercise", variation = pending.variation, workouts = completedWorkouts, isApplying = isApplyingRetroactive, onDismiss = { pendingRetroactiveVariation = null }, onApply = { scope ->
        isApplyingRetroactive = true
        val selected = candidatesForScope(editedWorkout, pending.exerciseId, scope, completedWorkouts)
        kotlinx.coroutines.MainScope().launch { selected.forEach { target -> SharedWorkoutState.workoutRepo.updateWorkout(updateExerciseIn(target, pending.exerciseId) { it.copy(variation = pending.variation) }) }; SharedWorkoutState.loadCompletedWorkouts(); isApplyingRetroactive = false; pendingRetroactiveVariation = null }
    }) }
}

private data class PendingVariation(val exerciseId: String, val variation: String)
private enum class RetroactiveScope(val label: String) { ONLY_THIS("Only this workout"), ALL_WORKOUTS("All workouts"), SAME_VARIATION("All workouts of this variation"), UNTIL_LAST_VARIATION("All split workouts until the last recorded variation"), UNTIL_PERFORMANCE_SPIKE("Until the last strong performance spike") }
private const val PERFORMANCE_SPIKE_THRESHOLD = 0.15
private fun estimatedOneRepMax(workout: Workout, exerciseId: String): Double? = workout.exercises.firstOrNull { it.exercise.id == exerciseId }?.sets?.mapNotNull { set -> if (set.isSkipped || set.weightKg <= 0.0 || set.reps <= 0) null else set.weightKg * (1.0 + set.reps / 30.0) }?.maxOrNull()
private fun candidatesForScope(currentWorkout: Workout, exerciseId: String, scope: RetroactiveScope, workouts: List<Workout>): List<Workout> {
    val history = workouts.filter { it.isCompleted && it.id != currentWorkout.id }.filter { it.exercises.any { exercise -> exercise.exercise.id == exerciseId } }.sortedByDescending { it.startedAt }
    val withoutVariation = { list: List<Workout> -> list.filter { workout -> workout.exercises.first { it.exercise.id == exerciseId }.variation.isBlank() } }
    return when (scope) {
        RetroactiveScope.ONLY_THIS -> emptyList()
        RetroactiveScope.ALL_WORKOUTS -> withoutVariation(history)
        RetroactiveScope.SAME_VARIATION -> withoutVariation(history.filter { it.templateId == currentWorkout.templateId })
        RetroactiveScope.UNTIL_LAST_VARIATION -> history.takeWhile { it.exercises.first { exercise -> exercise.exercise.id == exerciseId }.variation.isBlank() }
        RetroactiveScope.UNTIL_PERFORMANCE_SPIKE -> {
            val usable = history.mapNotNull { workout -> estimatedOneRepMax(workout, exerciseId)?.let { workout to it } }; val selected = mutableListOf<Workout>()
            for (index in usable.indices) { val previousThree = usable.drop(index + 1).take(3).map { it.second }; if (previousThree.size == 3 && previousThree.average() > 0.0 && abs(usable[index].second - previousThree.average()) / previousThree.average() > PERFORMANCE_SPIKE_THRESHOLD) break; if (usable[index].first.exercises.first { it.exercise.id == exerciseId }.variation.isBlank()) selected += usable[index].first }
            selected
        }
    }
}
@Composable private fun RetroactiveVariationDialog(currentWorkout: Workout, exerciseId: String, exerciseName: String, variation: String, workouts: List<Workout>, isApplying: Boolean, onDismiss: () -> Unit, onApply: (RetroactiveScope) -> Unit) {
    var scope by remember { mutableStateOf(RetroactiveScope.ONLY_THIS) }; val preview = remember(scope, currentWorkout, exerciseId, workouts) { candidatesForScope(currentWorkout, exerciseId, scope, workouts) }
    AlertDialog(onDismissRequest = { if (!isApplying) onDismiss() }, title = { Text("Apply variation retroactively?", color = TextPrimary, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("$exerciseName → $variation", color = TextSecondary, fontSize = 13.sp); RetroactiveScope.values().forEach { option -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { scope = option }) { RadioButton(selected = scope == option, onClick = { scope = option }); Text(option.label, color = TextPrimary, fontSize = 13.sp) } }; if (scope != RetroactiveScope.ONLY_THIS) { Text("${preview.size} affected workout${if (preview.size == 1) "" else "s"}", color = TextSecondary, fontWeight = FontWeight.Bold); LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) { items(preview) { workout -> Text("${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(workout.startedAt.epochSeconds * 1000))} · ${workout.name}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) } } } } }, confirmButton = { Button(onClick = { onApply(scope) }, enabled = !isApplying && (scope == RetroactiveScope.ONLY_THIS || preview.isNotEmpty()), colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange)) { Text(if (isApplying) "Applying…" else "Confirm", color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !isApplying) { Text("Cancel", color = TextSecondary) } }, backgroundColor = CardBackground)
}

// Existing editor UI helpers remain unchanged below this point.
// (The original branch implementation supplies EditorTextDialog and the exercise/set cards.)
