package com.bodyforge.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.Color
import com.bodyforge.ui.theme.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// A thin, purely visual horizontal scroll-position indicator (track + thumb) for a horizontally
// scrollable row. The thumb's width and offset reflect how far the row is scrolled. It renders
// nothing (including its own leading spacing) unless the content overflows. Track width is read via
// onSizeChanged rather than BoxWithConstraints, so it stays safe inside vertically scrolling parents
// (BoxWithConstraints uses SubcomposeLayout, which crashes under a verticalScroll's infinite height).
@Composable
fun HScrollIndicator(scrollState: ScrollState, modifier: Modifier = Modifier) {
    if (scrollState.maxValue <= 0) return
    var trackWidthPx by remember { mutableStateOf(0) }
    Column(modifier) {
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TrackColor)
                .onSizeChanged { trackWidthPx = it.width }
        ) {
            if (trackWidthPx > 0) {
                val density = LocalDensity.current
                val trackPx = trackWidthPx.toFloat()
                val content = trackPx + scrollState.maxValue
                val thumbFraction = (trackPx / content).coerceIn(0.2f, 1f)
                val progress = scrollState.value.toFloat() / scrollState.maxValue
                val thumbWidth = with(density) { (trackPx * thumbFraction).toDp() }
                val thumbOffset = with(density) { ((trackPx - trackPx * thumbFraction) * progress).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .width(thumbWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(ThumbColor)
                )
            }
        }
    }
}

// Wraps horizontalScroll so any horizontal scroll or fling the row itself can't consume is absorbed
// here instead of bubbling up to an enclosing HorizontalPager. Scrolling such a slider to its edge
// then no longer flips to the next tab.
fun Modifier.pagerSafeHorizontalScroll(state: ScrollState): Modifier = composed {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                Offset(available.x, 0f)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(available.x, 0f)
        }
    }
    nestedScroll(connection).horizontalScroll(state)
}
