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
import com.bodyforge.data.Weights
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.presentation.state.SettingsState
import com.bodyforge.ui.theme.*
import com.bodyforge.ui.screens.WorkoutScreen
import com.bodyforge.ui.screens.TemplatesScreen
import com.bodyforge.ui.screens.AnalyticsScreen
import com.bodyforge.ui.screens.HistoryScreen
import com.bodyforge.ui.screens.SettingsScreen
import com.bodyforge.resources.Res
import com.bodyforge.resources.bodyforge_logo
import org.jetbrains.compose.resources.painterResource

// Colours come from com.bodyforge.ui.theme so the palette can be switched at runtime.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App() {
    // Route weight formatting through the reactive unit setting so unit changes recompose instantly.
    remember { Weights.useLbsProvider = { SettingsState.useLbs }; ThemeState.reload(); Unit }

    // Initialize shared state
    LaunchedEffect(Unit) {
        SharedWorkoutState.refreshAll()
    }

    val activeWorkout by SharedWorkoutState.activeWorkout.collectAsState()
    val error by SharedWorkoutState.error.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
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

                // Main Content with 5 tabs (Settings is a regular pager page) + HorizontalPager.
                MainContent(hasActiveWorkout = activeWorkout != null)
            }

            if (showSplash) {
                SplashScreen()
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
private fun MainContent(hasActiveWorkout: Boolean) {
    val tabs = listOf(
        TabItem("workout", Icons.Filled.FitnessCenter, "Workout"),
        TabItem("templates", Icons.Filled.Assignment, "Templates"),
        TabItem("analytics", Icons.Filled.Timeline, "Analytics"),
        TabItem("history", Icons.Filled.Schedule, "History"),
        TabItem("settings", Icons.Filled.Settings, "Settings")
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
    val settingsListState = rememberLazyListState()
    val listStates = listOf(workoutListState, templatesListState, analyticsListState, historyListState, settingsListState)

    // "Break is over" banner: only relevant while the user is away from the Workout tab (the rest
    // bar already lives there). Returning to Workout clears it.
    val restJustEnded by SharedWorkoutState.restJustEnded.collectAsState()
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) SharedWorkoutState.dismissRestEndedNotice()
    }

    Column {
        if (restJustEnded && pagerState.currentPage != 0) {
            BreakOverBanner(
                onGoToWorkout = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                onDismiss = { SharedWorkoutState.dismissRestEndedNotice() }
            )
        }

        // Horizontal Pager for Tab Content. Keep every page composed (there are only five) so a
        // tab never gets torn down and rebuilt — that is what reset each screen's scroll position
        // (most visibly Analytics) when switching away and back.
        HorizontalPager(
            state = pagerState,
            beyondBoundsPageCount = 4,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.7f),
            modifier = Modifier.weight(1f).fillMaxWidth()
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
                3 -> HistoryScreen(listState = historyListState, onResumed = { coroutineScope.launch { pagerState.animateScrollToPage(0) } })
                4 -> SettingsScreen(listState = settingsListState)
            }
        }

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
            }
        )
    }
}

@Composable
private fun TabNavigationBar(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    hasActiveWorkout: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = if (isActive) AccentOrange else TextSecondary,
                    modifier = Modifier.size(22.dp)
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