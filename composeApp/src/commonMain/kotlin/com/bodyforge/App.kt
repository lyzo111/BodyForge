package com.bodyforge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.bodyforge.presentation.state.SharedWorkoutState
import com.bodyforge.ui.screens.WorkoutScreen
import com.bodyforge.ui.screens.TemplatesScreen
import com.bodyforge.ui.screens.AnalyticsScreen
import com.bodyforge.ui.screens.HistoryScreen
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
                HeaderSection()

                // Error Display
                error?.let { errorMessage ->
                    ErrorCard(
                        error = errorMessage,
                        onDismiss = { SharedWorkoutState.clearError() }
                    )
                }

                // Main Content with 4 Tabs + HorizontalPager
                MainContent(hasActiveWorkout = activeWorkout != null)
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🏋️",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "BodyForge",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            }

            Text(
                text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent(hasActiveWorkout: Boolean) {
    val tabs = listOf(
        TabItem("workout", "🏋️💪", "Workout"),
        TabItem("templates", "📋", "Templates"),
        TabItem("analytics", "📈", "Analytics"),
        TabItem("history", "📊", "History")
    )

    val pagerState = rememberPagerState(
        initialPage = if (hasActiveWorkout) 0 else 0, // Start on Workout tab
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Column {
        // Tab Navigation Bar
        TabNavigationBar(
            tabs = tabs,
            selectedTabIndex = pagerState.currentPage,
            hasActiveWorkout = hasActiveWorkout,
            onTabSelected = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        )

        // Horizontal Pager for Tab Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WorkoutScreen()
                1 -> TemplatesScreen()
                2 -> AnalyticsScreen()
                3 -> HistoryScreen()
            }
        }
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
            .background(Color(0xFF1E293B))
            .padding(horizontal = 4.dp)
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
    Box(
        modifier = modifier.padding(4.dp)
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isActive) AccentOrange.copy(alpha = 0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isActive) AccentOrange else TextSecondary
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tab.icon,
                    fontSize = 18.sp
                )
                Text(
                    text = tab.title,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
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
    val icon: String,
    val title: String
)