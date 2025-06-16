package com.bodyforge

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
                Text(
                    text = "Active Workout: ${uiState.currentWorkout}",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
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
    availableExercises: List<String>,
    selectedExercises: List<String>,
    isLoading: Boolean,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
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
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White
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
                        Text(
                            text = exercise,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
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
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White
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
                    Text(
                        text = exercise,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
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