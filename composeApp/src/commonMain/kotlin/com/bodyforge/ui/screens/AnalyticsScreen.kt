package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.presentation.state.SharedWorkoutState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Colors
private val AccentOrange = Color(0xFFFF6B35)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)

@Composable
fun AnalyticsScreen() {
    val completedWorkouts by SharedWorkoutState.completedWorkouts.collectAsState()
    val isLoading by SharedWorkoutState.isLoading.collectAsState()
    var showCreatePhaseDialog by remember { mutableStateOf(false) }

    // Initialize data
    LaunchedEffect(Unit) {
        SharedWorkoutState.loadCompletedWorkouts()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Analytics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Button(
                    onClick = { showCreatePhaseDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentPurple),
                    shape = RoundedCornerShape(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Phase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }
        } else if (completedWorkouts.isEmpty()) {
            item {
                EmptyAnalyticsCard()
            }
        } else {
            // Current Phase (Placeholder)
            item {
                CurrentPhaseCard()
            }

            // Quick Stats Row
            item {
                QuickStatsRow(completedWorkouts)
            }

            // Volume Progression Chart
            item {
                VolumeProgressionCard(completedWorkouts)
            }

            // Muscle Group Balance
            item {
                MuscleGroupBalanceCard(completedWorkouts)
            }

            // Recent PRs & Achievements
            item {
                AchievementsCard(completedWorkouts)
            }

            // Training Frequency Heatmap (Placeholder)
            item {
                TrainingFrequencyCard(completedWorkouts)
            }
        }
    }

    // Create Phase Dialog (Placeholder)
    if (showCreatePhaseDialog) {
        CreatePhaseDialog(
            onDismiss = { showCreatePhaseDialog = false },
            onCreatePhase = { phaseName, phaseType ->
                // TODO: Create new training phase
                showCreatePhaseDialog = false
            }
        )
    }
}

@Composable
private fun EmptyAnalyticsCard() {
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
            Text("📈", fontSize = 48.sp)
            Text(
                text = "No Data Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Complete a few workouts to see your progress analytics",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CurrentPhaseCard() {
    Card(
        backgroundColor = AccentPurple.copy(alpha = 0.8f),
        elevation = 4.dp,
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
                        text = "🎯 Current Phase",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "No active phase • Create your first training phase",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                TextButton(
                    onClick = { /* TODO: Manage phases */ }
                ) {
                    Text("Manage", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(workouts: List<com.bodyforge.domain.models.Workout>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Total Workouts
        QuickStatCard(
            icon = "🏋️",
            value = "${workouts.size}",
            label = "Workouts",
            color = AccentBlue,
            modifier = Modifier.weight(1f)
        )

        // Total Volume
        val totalVolume = workouts.sumOf { it.totalVolumePerformed }.roundToInt()
        QuickStatCard(
            icon = "📊",
            value = "${totalVolume}kg",
            label = "Total Volume",
            color = AccentGreen,
            modifier = Modifier.weight(1f)
        )

        // Avg Duration
        val avgDuration = workouts.mapNotNull { it.durationMinutes }.average().takeIf { !it.isNaN() } ?: 0.0
        QuickStatCard(
            icon = "⏱️",
            value = "${avgDuration.roundToInt()}m",
            label = "Avg Duration",
            color = AccentOrange,
            modifier = Modifier.weight(1f)
        )

        // This Week
        val thisWeekWorkouts = workouts.filter {
            val workoutDate = it.startDate
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val daysDiff = today.toEpochDays() - workoutDate.toEpochDays()
            daysDiff <= 7
        }.size
        QuickStatCard(
            icon = "📅",
            value = "$thisWeekWorkouts",
            label = "This Week",
            color = AccentPurple,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = color.copy(alpha = 0.1f),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun VolumeProgressionCard(workouts: List<com.bodyforge.domain.models.Workout>) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Volume Progression",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "📈",
                    fontSize = 20.sp,
                    color = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simple volume chart (placeholder)
            VolumeChart(workouts)
        }
    }
}

@Composable
private fun VolumeChart(workouts: List<com.bodyforge.domain.models.Workout>) {
    // Simple bar chart representation
    val recentWorkouts = workouts.takeLast(10)

    if (recentWorkouts.isEmpty()) {
        Text(
            text = "No volume data available",
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        return
    }

    val maxVolume = recentWorkouts.maxOfOrNull { it.totalVolumePerformed } ?: 1.0

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(recentWorkouts) { workout ->
            val height = if (maxVolume > 0) (workout.totalVolumePerformed / maxVolume * 80).coerceAtLeast(8.0) else 8.0

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(height.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(AccentBlue, AccentBlue.copy(alpha = 0.6f))
                            ),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${workout.totalVolumePerformed.roundToInt()}",
                    fontSize = 8.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupBalanceCard(workouts: List<com.bodyforge.domain.models.Workout>) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💪 Muscle Group Balance",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate muscle group frequencies
            val muscleGroupCounts = mutableMapOf<String, Int>()
            workouts.forEach { workout ->
                workout.exercises.forEach { exerciseInWorkout ->
                    exerciseInWorkout.exercise.muscleGroups.forEach { muscleGroup ->
                        muscleGroupCounts[muscleGroup] = muscleGroupCounts.getOrDefault(muscleGroup, 0) + 1
                    }
                }
            }

            val maxCount = muscleGroupCounts.values.maxOrNull() ?: 1
            val topMuscleGroups = muscleGroupCounts.toList().sortedByDescending { it.second }.take(6)

            if (topMuscleGroups.isEmpty()) {
                Text(
                    text = "No muscle group data available",
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                topMuscleGroups.forEach { (muscleGroup, count) ->
                    MuscleGroupBar(
                        muscleGroup = muscleGroup,
                        count = count,
                        maxCount = maxCount
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupBar(
    muscleGroup: String,
    count: Int,
    maxCount: Int
) {
    val percentage = (count.toFloat() / maxCount.toFloat())
    val color = when {
        percentage >= 0.8f -> AccentGreen
        percentage >= 0.6f -> AccentOrange
        else -> AccentRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = muscleGroup,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.width(80.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(SurfaceColor, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }

        Text(
            text = "$count",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AchievementsCard(workouts: List<com.bodyforge.domain.models.Workout>) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🏆 Recent Achievements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (workouts.isEmpty()) {
                Text(
                    text = "Complete workouts to unlock achievements!",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Mock achievements based on data
                AchievementItem(
                    icon = "🎯",
                    title = "Consistency King",
                    description = "${workouts.size} workouts completed"
                )

                if (workouts.size >= 5) {
                    AchievementItem(
                        icon = "💪",
                        title = "Dedication Unlocked",
                        description = "5+ workouts completed"
                    )
                }

                val totalVolume = workouts.sumOf { it.totalVolumePerformed }
                if (totalVolume >= 10000) {
                    AchievementItem(
                        icon = "📊",
                        title = "Volume Monster",
                        description = "${totalVolume.roundToInt()}kg total volume moved"
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TrainingFrequencyCard(workouts: List<com.bodyforge.domain.models.Workout>) {
    Card(
        backgroundColor = CardBackground,
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📅 Training Frequency",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Weekly average: ${(workouts.size.toFloat() / 4).let { "%.1f".format(it) }} workouts",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Simple frequency visualization (placeholder)
            Text(
                text = "🟩🟩🟨⬜🟩🟩⬜ This week",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = "🟩 = Workout, 🟨 = Light, ⬜ = Rest",
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CreatePhaseDialog(
    onDismiss: () -> Unit,
    onCreatePhase: (String, String) -> Unit
) {
    var phaseName by remember { mutableStateOf("") }
    var phaseType by remember { mutableStateOf("Strength") }

    val phaseTypes = listOf("Strength", "Hypertrophy", "Cut", "Bulk", "Powerlifting", "Deload")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🎯 Create Training Phase",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = phaseName,
                    onValueChange = { phaseName = it },
                    label = { Text("Phase Name") },
                    placeholder = { Text("e.g., Summer Cut, Strength Block") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextPrimary,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = SurfaceColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Phase Type:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(phaseTypes) { type ->
                        FilterChip(
                            text = type,
                            isSelected = phaseType == type,
                            onClick = { phaseType = type }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreatePhase(phaseName, phaseType) },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentPurple),
                enabled = phaseName.isNotBlank(),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Create Phase", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) AccentPurple else SurfaceColor,
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