package com.bodyforge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.bodyforge.data.AppSettings
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.screens.WorkoutScreen
import com.bodyforge.ui.screens.TemplatesScreen
import com.bodyforge.ui.screens.AnalyticsScreen
import com.bodyforge.ui.screens.HistoryScreen
import com.bodyforge.resources.Res
import com.bodyforge.resources.bodyforge_logo
import org.jetbrains.compose.resources.painterResource
import java.text.SimpleDateFormat
import java.util.*

// Modern Color Palette
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val SurfaceColor = Color(0xFF334155)
private val AccentOrange = Color(0xFFFF6B35)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App() {
    // Initialize shared state
    LaunchedEffect(Unit) {
        SharedWorkoutState.refreshAll()
    }

    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val error by SharedWorkoutState.error.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1600)
        showSplash = false
    }

    MaterialTheme(
        colors = darkColors(
            primary = AccentOrange,
            background = DarkBackground,
            surface = CardBackground,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Column {
                // Error Display
                error?.let { errorMessage ->
                    ErrorCard(
                        error = errorMessage,
                        onDismiss = { SharedWorkoutState.clearError() }
                    )
                }

                // Main Content with 4 Tabs + HorizontalPager. Settings now lives in the tab bar.
                MainContent(
                    hasActiveWorkout = activeWorkout != null,
                    onSettings = { showSettings = true }
                )
            }

            if (showSplash) {
                SplashScreen()
            }

            if (showSettings) {
                SettingsDialog(onDismiss = { showSettings = false })
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.bodyforge_logo),
                contentDescription = "BodyForge",
                modifier = Modifier.size(140.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "BodyForge",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = AccentOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Forge your body",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    var isolationRest by remember { mutableStateOf(AppSettings.isolationRestSeconds) }
    var compoundRest by remember { mutableStateOf(AppSettings.compoundRestSeconds) }
    var vibrate by remember { mutableStateOf(AppSettings.vibrateOnTimerEnd) }
    var editCompleted by remember { mutableStateOf(AppSettings.editCompletedSets) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Rest timer", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 13.sp)
                RestSetting("Isolation rest", isolationRest) { isolationRest = it.coerceIn(15, 600) }
                RestSetting("Compound rest", compoundRest) { compoundRest = it.coerceIn(15, 600) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate when timer ends", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange, checkedTrackColor = AccentOrange.copy(alpha = 0.5f))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit sets after completing", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = editCompleted,
                        onCheckedChange = { editCompleted = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange, checkedTrackColor = AccentOrange.copy(alpha = 0.5f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AppSettings.isolationRestSeconds = isolationRest
                    AppSettings.compoundRestSeconds = compoundRest
                    AppSettings.vibrateOnTimerEnd = vibrate
                    AppSettings.editCompletedSets = editCompleted
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentOrange),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        backgroundColor = CardBackground
    )
}

@Composable
private fun RestSetting(label: String, seconds: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onChange(seconds - 15) },
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("−", color = Color.White, fontSize = 18.sp) }
            Text(
                text = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(52.dp)
            )
            Button(
                onClick = { onChange(seconds + 15) },
                colors = ButtonDefaults.buttonColors(backgroundColor = SurfaceColor),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("+", color = Color.White, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        backgroundColor = AccentRed.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = Color.White,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            IconButton(onClick = onDismiss) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun BreakOverBanner(
    onGoToWorkout: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(color = AccentGreen, elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onGoToWorkout)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Break's over — back to your workout",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent(hasActiveWorkout: Boolean, onSettings: () -> Unit) {
    val tabs = listOf(
        TabItem("workout", Icons.Filled.FitnessCenter, "Workout"),
        TabItem("templates", Icons.Filled.Assignment, "Templates"),
        TabItem("analytics", Icons.Filled.Timeline, "Analytics"),
        TabItem("history", Icons.Filled.Schedule, "History")
    )

    val pagerState = rememberPagerState(
        initialPage = if (hasActiveWorkout) 0 else 0, // Start on Workout tab
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // One scroll state per tab, hoisted here so positions survive switching tabs. Re-tapping the
    // current tab scrolls it back to the top.
    val workoutListState = rememberLazyListState()
    val templatesListState = rememberLazyListState()
    val analyticsListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    val listStates = listOf(workoutListState, templatesListState, analyticsListState, historyListState)

    // "Break is over" banner: only relevant while the user is away from the Workout tab (the rest
    // bar already lives there). Returning to Workout clears it.
    val restJustEnded by SharedWorkoutState.restJustEnded.collectAsState()
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) SharedWorkoutState.dismissRestEndedNotice()
    }

    Column {
        // Tab Navigation Bar
        TabNavigationBar(
            tabs = tabs,
            selectedTabIndex = pagerState.currentPage,
            hasActiveWorkout = hasActiveWorkout,
            onTabSelected = { index ->
                coroutineScope.launch {
                    if (index == pagerState.currentPage) {
                        listStates[index].animateScrollToItem(0)
                    } else {
                        pagerState.animateScrollToPage(index)
                    }
                }
            },
            onSettings = onSettings
        )

        if (restJustEnded && pagerState.currentPage != 0) {
            BreakOverBanner(
                onGoToWorkout = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                onDismiss = { SharedWorkoutState.dismissRestEndedNotice() }
            )
        }

        // Horizontal Pager for Tab Content. Keep every page composed (there are only four) so a
        // tab never gets torn down and rebuilt — that is what reset each screen's scroll position
        // (most visibly Analytics) when switching away and back.
        HorizontalPager(
            state = pagerState,
            beyondBoundsPageCount = 3,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.7f),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WorkoutScreen(
                    listState = workoutListState,
                    onGoToTemplates = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> TemplatesScreen(
                    listState = templatesListState,
                    onStartWorkout = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                )
                2 -> AnalyticsScreen(listState = analyticsListState)
                3 -> HistoryScreen(listState = historyListState)
            }
        }
    }
}

@Composable
private fun TabNavigationBar(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    hasActiveWorkout: Boolean,
    onTabSelected: (Int) -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            TabButton(
                tab = tab,
                isActive = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f),
                showBadge = tab.id == "workout" && hasActiveWorkout
            )
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun TabButton(
    tab: TabItem,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false
) {
    Box(modifier = modifier.padding(horizontal = 2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .background(if (isActive) AccentOrange.copy(alpha = 0.15f) else Color.Transparent)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            // Single-color icon sits behind the centered label, tinted in the page's dark blue
            // so it reads as a subtle watermark on the lighter tab bar.
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = DarkBackground,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = tab.title,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) AccentOrange else TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }

        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AccentRed, RoundedCornerShape(50))
                    .align(Alignment.TopEnd)
            )
        }
    }
}

data class TabItem(
    val id: String,
    val icon: ImageVector,
    val title: String
)