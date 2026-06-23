package com.bodyforge.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyforge.domain.models.Exercise
import com.bodyforge.presentation.state.SharedWorkoutState
import kotlinx.coroutines.launch

private val CardBg = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentOrange = Color(0xFFFF6B35)
private val AccentGreen = Color(0xFF10B981)

// Lets the user assign muscle groups to their custom and imported exercises. Untagged ones are
// listed first because those are the exercises missing from Muscle Group Balance. Editing reuses the
// custom-exercise dialog, so a saved exercise must have at least one muscle group.
@Composable
fun TagExercisesDialog(onDismiss: () -> Unit) {
    val exercises by SharedWorkoutState.exercises.collectAsState()
    val scope = rememberCoroutineScope()
    val customExercises = exercises
        .filter { it.isCustom }
        .sortedWith(compareBy({ it.muscleGroups.isNotEmpty() }, { it.name.lowercase() }))
    var editing by remember { mutableStateOf<Exercise?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("Tag exercises", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Assign muscle groups to your custom and imported exercises so they show up in Muscle Group Balance.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (customExercises.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No custom or imported exercises yet.", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customExercises) { ex ->
                            val tagged = ex.muscleGroups.isNotEmpty()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceColor, RoundedCornerShape(10.dp))
                                    .clickable { editing = ex }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        if (tagged) ex.muscleGroups.joinToString(" • ") else "No muscle groups",
                                        fontSize = 12.sp,
                                        color = if (tagged) AccentGreen else AccentOrange
                                    )
                                }
                                Text(
                                    if (tagged) "Edit" else "Tag",
                                    color = AccentOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editing?.let { ex ->
        CreateExerciseDialog(
            showDialog = true,
            onDismiss = { editing = null },
            onCreateExercise = { name, groups, equip, bw ->
                scope.launch {
                    SharedWorkoutState.updateCustomExercise(
                        ex.copy(name = name, muscleGroups = groups, equipmentNeeded = equip, isBodyweight = bw)
                    )
                }
            },
            title = "Edit exercise",
            confirmLabel = "Save",
            initialName = ex.name,
            initialMuscleGroups = ex.muscleGroups.toSet(),
            initialEquipment = ex.equipmentNeeded,
            initialBodyweight = ex.isBodyweight
        )
    }
}
