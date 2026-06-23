package com.bodyforge.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val TrackColor = Color(0xFF334155).copy(alpha = 0.4f)
private val ThumbColor = Color(0xFF3B82F6).copy(alpha = 0.8f)

// A thin, purely visual horizontal scroll-position indicator (track + thumb) for a horizontally
// scrollable row. The thumb's width and offset reflect how far the row is scrolled. It renders
// nothing (including its own leading spacing) unless the content actually overflows. Place it
// directly below the scrollable Row, sharing the same ScrollState.
@Composable
fun HScrollIndicator(scrollState: ScrollState, modifier: Modifier = Modifier) {
    if (scrollState.maxValue <= 0) return
    Column(modifier) {
        Spacer(modifier = Modifier.height(6.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TrackColor)
        ) {
            val density = LocalDensity.current
            val trackPx = with(density) { maxWidth.toPx() }
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
