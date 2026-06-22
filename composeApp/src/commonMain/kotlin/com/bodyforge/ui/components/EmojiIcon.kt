package com.bodyforge.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.presentation.state.SettingsState

// App blue used when decorative emojis are swapped for icons (Emoji Mode off).
val EmojiIconBlue = Color(0xFF3B82F6)

// Shows the emoji when Emoji Mode is on, otherwise a single-color icon in its place. Centralizes the
// switch so every decorative emoji in the app flips together, reactively via SettingsState.
@Composable
fun EmojiIcon(
    emoji: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    fontSize: TextUnit = 16.sp,
    tint: Color = EmojiIconBlue
) {
    if (SettingsState.emojiMode) {
        Text(text = emoji, fontSize = fontSize, modifier = modifier)
    } else {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier.size(iconSize))
    }
}
