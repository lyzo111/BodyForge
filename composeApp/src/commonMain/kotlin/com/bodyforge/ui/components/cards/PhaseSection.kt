package com.bodyforge.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.domain.models.PhaseType
import com.bodyforge.domain.models.TrainingPhase
import com.bodyforge.presentation.state.SharedWorkoutState
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private val AccentPurple = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

private fun formatDate(date: LocalDate): String {
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}

@Composable
fun PhaseSection() {
    val phases by SharedWorkoutState.phases.collectAsState()
    val activePhase by SharedWorkoutState.activePhase.collectAsState()
    val scope = rememberCoroutineScope()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TrainingPhase?>(null) }
    var deleting by remember { mutableStateOf<TrainingPhase?>(null) }

    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎯 Training Phase", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Button(
                    onClick = { showCreate = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentPurple),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Phase", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            val active = activePhase
            if (active != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${active.phaseType.emoji} ${active.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${active.phaseType.displayName} · since ${formatDate(active.startDate)}", fontSize = 12.sp, color = TextSecondary)
                            if (active.description.isNotBlank()) {
                                Text(active.description, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(onClick = { editing = active }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("Edit", color = AccentBlue, fontSize = 12.sp)
                            }
                            TextButton(onClick = { scope.launch { SharedWorkoutState.completePhase(active.id) } }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("Complete", color = AccentGreen, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Text(
                    "No active phase. Start one to mark this block of training (e.g. a strength or cut phase) and compare it later.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            val pastPhases = phases.filter { !it.isActive }
            if (pastPhases.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("History", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pastPhases.forEach { phase ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${phase.phaseType.emoji} ${phase.name}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                val range = phase.endDate?.let { "${formatDate(phase.startDate)} – ${formatDate(it)}" } ?: "since ${formatDate(phase.startDate)}"
                                Text("${phase.phaseType.displayName} · $range", fontSize = 11.sp, color = TextSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { editing = phase }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text("Edit", color = AccentBlue, fontSize = 11.sp)
                                }
                                TextButton(onClick = { deleting = phase }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text("Delete", color = AccentRed, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        PhaseEditorDialog(
            heading = "New Training Phase",
            initial = null,
            confirmLabel = "Start Phase",
            onDismiss = { showCreate = false },
            onConfirm = { name, type, description ->
                scope.launch { SharedWorkoutState.startPhase(name, type, description) }
                showCreate = false
            }
        )
    }

    editing?.let { phase ->
        PhaseEditorDialog(
            heading = "Edit Phase",
            initial = phase,
            confirmLabel = "Save",
            onDismiss = { editing = null },
            onConfirm = { name, type, description ->
                scope.launch { SharedWorkoutState.updatePhase(phase.copy(name = name, phaseType = type, description = description)) }
                editing = null
            }
        )
    }

    deleting?.let { phase ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete Phase", color = TextPrimary) },
            text = { Text("Delete \"${phase.name}\"? This does not affect your logged workouts.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { SharedWorkoutState.deletePhase(phase.id) }
                        deleting = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = TextSecondary) } },
            backgroundColor = CardBackground
        )
    }
}

@Composable
private fun PhaseEditorDialog(
    heading: String,
    initial: TrainingPhase?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, PhaseType, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.phaseType ?: PhaseType.HYPERTROPHY) }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(heading, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Phase Name") },
                    placeholder = { Text("e.g., Summer Cut") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentPurple, unfocusedBorderColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Type", fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhaseType.entries.forEach { option ->
                        PhaseTypeChip(option, option == type) { type = option }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (optional)") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary, focusedBorderColor = AccentPurple, unfocusedBorderColor = SurfaceColor),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), type, description.trim()) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentPurple),
                enabled = name.isNotBlank(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text(confirmLabel, color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

@Composable
private fun PhaseTypeChip(type: PhaseType, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) AccentPurple else SurfaceColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            "${type.emoji} ${type.displayName}",
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
