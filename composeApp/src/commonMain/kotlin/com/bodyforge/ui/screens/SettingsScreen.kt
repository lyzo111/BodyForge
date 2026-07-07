package com.bodyforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyforge.data.AppSettings
import com.bodyforge.presentation.state.SettingsState
import com.bodyforge.ui.theme.*

// Full settings page (a pager tab, not a modal). Each control writes through to AppSettings
// immediately, so there is no Save button — leaving the page keeps whatever is set.
@Composable
fun SettingsScreen(listState: LazyListState) {
    var isolationRest by remember { mutableStateOf(AppSettings.isolationRestSeconds) }
    var compoundRest by remember { mutableStateOf(AppSettings.compoundRestSeconds) }
    var vibrate by remember { mutableStateOf(AppSettings.vibrateOnTimerEnd) }
    var editCompleted by remember { mutableStateOf(AppSettings.editCompletedSets) }
    var useLbs by remember { mutableStateOf(AppSettings.useLbs) }
    var emojiMode by remember { mutableStateOf(AppSettings.emojiMode) }
    var bigButtons by remember { mutableStateOf(AppSettings.bigButtonMode) }
    var showBigButtonInfo by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (SettingsState.emojiMode) "⚙️ Settings" else "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            SettingsCard("Rest timer") {
                RestSettingRow("Isolation rest", isolationRest) {
                    isolationRest = it.coerceIn(15, 600)
                    AppSettings.isolationRestSeconds = isolationRest
                }
                Spacer(Modifier.height(10.dp))
                RestSettingRow("Compound rest", compoundRest) {
                    compoundRest = it.coerceIn(15, 600)
                    AppSettings.compoundRestSeconds = compoundRest
                }
                Spacer(Modifier.height(6.dp))
                ToggleRow("Vibrate when timer ends", vibrate) {
                    vibrate = it
                    AppSettings.vibrateOnTimerEnd = it
                }
            }
        }

        item {
            SettingsCard("Behavior") {
                ToggleRow("Edit sets after completing", editCompleted) {
                    editCompleted = it
                    AppSettings.editCompletedSets = it
                    SettingsState.reload()
                }
                ToggleRow(
                    label = "Big Button Mode",
                    checked = bigButtons,
                    onInfo = { showBigButtonInfo = true }
                ) {
                    bigButtons = it
                    AppSettings.bigButtonMode = it
                    SettingsState.reload()
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Weight unit", color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitChip("kg", !useLbs) {
                            useLbs = false
                            AppSettings.useLbs = false
                            SettingsState.reload()
                        }
                        UnitChip("lbs", useLbs) {
                            useLbs = true
                            AppSettings.useLbs = true
                            SettingsState.reload()
                        }
                    }
                }
                ToggleRow("Emoji mode", emojiMode) {
                    emojiMode = it
                    AppSettings.emojiMode = it
                    SettingsState.reload()
                }
            }
        }

        item {
            SettingsCard("Theme") {
                appThemes.chunked(3).forEachIndexed { rowIndex, rowThemes ->
                    if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowThemes.forEach { option ->
                            ThemeTile(
                                option = option,
                                selected = option.name == ThemeState.themeName,
                                modifier = Modifier.weight(1f)
                            ) {
                                AppSettings.themeName = option.name
                                ThemeState.applyTheme(option.name)
                            }
                        }
                        repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showBigButtonInfo) {
        AlertDialog(
            onDismissRequest = { showBigButtonInfo = false },
            title = { Text("Big Button Mode", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Shows the active workout with large stacked buttons for each set — easier to hit mid-set, but longer to scroll. Off, sets use the compact one-line rows.",
                    color = TextSecondary, fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(onClick = { showBigButtonInfo = false }, colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue), elevation = ButtonDefaults.elevation(0.dp)) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = CardBackground
        )
    }
}

// One settings section: a card with a bold section title and its rows below, so related settings
// read as a group.
@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(backgroundColor = CardBackground, shape = RoundedCornerShape(16.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onInfo: (() -> Unit)? = null,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        if (onInfo != null) {
            // The "i" floats just above the switch, mirroring the CSV-import info button pattern.
            Box {
                Switch(
                    checked = checked,
                    onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange, checkedTrackColor = AccentOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 10.dp)
                )
                IconButton(
                    onClick = onInfo,
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "About $label", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange, checkedTrackColor = AccentOrange.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun RestSettingRow(label: String, seconds: Int, onChange: (Int) -> Unit) {
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
                modifier = Modifier.size(38.dp),
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
                modifier = Modifier.size(38.dp),
                elevation = ButtonDefaults.elevation(0.dp)
            ) { Text("+", color = Color.White, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun UnitChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selected) AccentOrange else SurfaceColor,
            contentColor = if (selected) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        elevation = ButtonDefaults.elevation(0.dp)
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// A theme option tile: swatch background, three accent dots and the theme name, drawn in the
// option's own colours so each palette previews itself.
@Composable
private fun ThemeTile(option: AppThemeOption, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(option.colors.background)
            .border(
                width = 2.dp,
                color = if (selected) AccentOrange else option.colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(option.colors.accentOrange))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(option.colors.accentBlue))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(option.colors.accentGreen))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            option.name,
            color = if (selected) option.colors.textPrimary else option.colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
