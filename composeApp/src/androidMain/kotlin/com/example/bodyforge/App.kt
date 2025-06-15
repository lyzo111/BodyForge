import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodyforge.presentation.viewmodel.WorkoutViewModel

@Composable
fun App() {
    MaterialTheme(
        colors = darkColors(
            primary = Color(0xFFFF6B35),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0)
        )
    ) {
        val viewModel: WorkoutViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "🏋️ BodyForge",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Error handling
            uiState.error?.let { error ->
                Card(
                    backgroundColor = Color(0xFF8B1538),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("✕", color = Color.White)
                        }
                    }
                }
            }

            // Current workout or create workout
            if (uiState.currentWorkout != null) {
                ActiveWorkoutScreen(
                    workout = uiState.currentWorkout,
                    onUpdateSet = { exerciseId, setId, reps, weight, completed ->
                        viewModel.updateSet(exerciseId, setId, reps, weight, completed)
                    },
                    onFinishWorkout = { viewModel.completeWorkout() }
                )
            } else {
                CreateWorkoutScreen(
                    availableExercises = uiState.availableExercises,
                    selectedExercises = uiState.selectedExercises,
                    isLoading = uiState.isLoading,
                    onAddExercise = { viewModel.addExerciseToSelection(it) },
                    onRemoveExercise = { viewModel.removeExerciseFromSelection(it) },
                    onStartWorkout = { viewModel.startWorkout() }
                )
            }
        }
    }
}

@Composable
fun CreateWorkoutScreen(
    availableExercises: List<com.bodyforge.domain.models.Exercise>,
    selectedExercises: List<com.bodyforge.domain.models.Exercise>,
    isLoading: Boolean,
    onAddExercise: (com.bodyforge.domain.models.Exercise) -> Unit,
    onRemoveExercise: (com.bodyforge.domain.models.Exercise) -> Unit,
    onStartWorkout: () -> Unit
) {
    LazyColumn {
        // Selected exercises section
        if (selectedExercises.isNotEmpty()) {
            item {
                Text(
                    text = "Selected Exercises (${selectedExercises.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(selectedExercises) { exercise ->
                Card(
                    backgroundColor = Color(0xFF2D5016),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = exercise.muscleGroups.joinToString(", "),
                                fontSize = 12.sp,
                                color = Color(0xFFBBBBBB)
                            )
                        }
                        TextButton(onClick = { onRemoveExercise(exercise) }) {
                            Text("Remove", color = Color(0xFFFF4444))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onStartWorkout,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6B35)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = if (isLoading) "Starting..." else "🚀 Start Workout",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
        }

        // Available exercises section
        item {
            Text(
                text = "Exercise Library",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(availableExercises) { exercise ->
            Card(
                backgroundColor = Color(0xFF1E1E1E),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "${exercise.muscleGroups.joinToString(", ")} • ${exercise.equipmentNeeded}",
                            fontSize = 12.sp,
                            color = Color(0xFFBBBBBB)
                        )
                    }
                    val isSelected = selectedExercises.contains(exercise)
                    TextButton(
                        onClick = { if (isSelected) onRemoveExercise(exercise) else onAddExercise(exercise) }
                    ) {
                        Text(
                            text = if (isSelected) "✓" else "+",
                            color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFFF6B35),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveWorkoutScreen(
    workout: com.bodyforge.domain.models.Workout,
    onUpdateSet: (String, String, Int?, Double?, Boolean?) -> Unit,
    onFinishWorkout: () -> Unit
) {
    LazyColumn {
        item {
            Card(
                backgroundColor = Color(0xFF1E3A8A),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = workout.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Started: ${workout.startedAt}",
                        fontSize = 12.sp,
                        color = Color(0xFFBBBBBB)
                    )
                }
            }
        }

        items(workout.exercises) { exerciseInWorkout ->
            Card(
                backgroundColor = Color(0xFF1E1E1E),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = exerciseInWorkout.exercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    exerciseInWorkout.sets.forEachIndexed { setIndex, set ->
                        Card(
                            backgroundColor = Color(0xFF2D2D2D),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Set ${setIndex + 1}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFBBBBBB),
                                    modifier = Modifier.width(40.dp)
                                )

                                // Reps input
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Reps:", fontSize = 12.sp, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = set.reps.toString(),
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .width(30.dp)
                                            .padding(4.dp)
                                    )
                                    Column {
                                        TextButton(
                                            onClick = {
                                                onUpdateSet(
                                                    exerciseInWorkout.exercise.id,
                                                    set.id,
                                                    set.reps + 1,
                                                    null,
                                                    null
                                                )
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("+", color = Color.White, fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = {
                                                if (set.reps > 0) {
                                                    onUpdateSet(
                                                        exerciseInWorkout.exercise.id,
                                                        set.id,
                                                        set.reps - 1,
                                                        null,
                                                        null
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("-", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Weight input
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Weight:", fontSize = 12.sp, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${set.weightKg}kg",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .width(50.dp)
                                            .padding(4.dp)
                                    )
                                    Column {
                                        TextButton(
                                            onClick = {
                                                onUpdateSet(
                                                    exerciseInWorkout.exercise.id,
                                                    set.id,
                                                    null,
                                                    set.weightKg + 2.5,
                                                    null
                                                )
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("+", color = Color.White, fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = {
                                                if (set.weightKg > 0) {
                                                    onUpdateSet(
                                                        exerciseInWorkout.exercise.id,
                                                        set.id,
                                                        null,
                                                        (set.weightKg - 2.5).coerceAtLeast(0.0),
                                                        null
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("-", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Complete button
                                Button(
                                    onClick = {
                                        onUpdateSet(
                                            exerciseInWorkout.exercise.id,
                                            set.id,
                                            null,
                                            null,
                                            !set.completed
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (set.completed) Color(0xFF4CAF50) else Color(0xFF555555)
                                    ),
                                    modifier = Modifier.size(width = 60.dp, height = 32.dp)
                                ) {
                                    Text(
                                        text = if (set.completed) "✓" else "○",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onFinishWorkout,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8B1538)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "🏁 Finish Workout",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}