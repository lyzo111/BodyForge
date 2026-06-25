package com.bodyforge.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.bodyforge.data.AppSettings

// A complete app colour palette. Each selectable theme provides one. Screens read their colours
// through the reactive accessors below rather than hardcoding hex values, so switching the theme
// recomposes the whole UI instantly.
data class AppColors(
    val background: Color,
    val cardBackground: Color,
    val surface: Color,
    val accentOrange: Color,
    val accentRed: Color,
    val accentGreen: Color,
    val accentBlue: Color,
    val accentPurple: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val selectedGreen: Color
)

data class AppThemeOption(val name: String, val colors: AppColors)

private val Midnight = AppColors(
    background = Color(0xFF0F172A), cardBackground = Color(0xFF1E293B), surface = Color(0xFF334155),
    accentOrange = Color(0xFFFF6B35), accentRed = Color(0xFFEF4444), accentGreen = Color(0xFF10B981),
    accentBlue = Color(0xFF3B82F6), accentPurple = Color(0xFF8B5CF6),
    textPrimary = Color(0xFFE2E8F0), textSecondary = Color(0xFF94A3B8), selectedGreen = Color(0xFF065F46)
)

private val Ocean = AppColors(
    background = Color(0xFF0B1B2B), cardBackground = Color(0xFF12283D), surface = Color(0xFF1E3B55),
    accentOrange = Color(0xFFF59E0B), accentRed = Color(0xFFF43F5E), accentGreen = Color(0xFF14B8A6),
    accentBlue = Color(0xFF38BDF8), accentPurple = Color(0xFF818CF8),
    textPrimary = Color(0xFFE3EEF6), textSecondary = Color(0xFF8DB0C6), selectedGreen = Color(0xFF0F4D52)
)

private val Forest = AppColors(
    background = Color(0xFF0C1A12), cardBackground = Color(0xFF15251B), surface = Color(0xFF20392A),
    accentOrange = Color(0xFFF59E0B), accentRed = Color(0xFFEF4444), accentGreen = Color(0xFF22C55E),
    accentBlue = Color(0xFF38BDF8), accentPurple = Color(0xFFA78BFA),
    textPrimary = Color(0xFFE7F0E9), textSecondary = Color(0xFF9CB7A6), selectedGreen = Color(0xFF14532D)
)

private val Ember = AppColors(
    background = Color(0xFF1A1210), cardBackground = Color(0xFF271B17), surface = Color(0xFF3B2823),
    accentOrange = Color(0xFFF97316), accentRed = Color(0xFFEF4444), accentGreen = Color(0xFF84CC16),
    accentBlue = Color(0xFF60A5FA), accentPurple = Color(0xFFC084FC),
    textPrimary = Color(0xFFF5E9E2), textSecondary = Color(0xFFC2A99E), selectedGreen = Color(0xFF4D2D18)
)

private val Graphite = AppColors(
    background = Color(0xFF121212), cardBackground = Color(0xFF1E1E1E), surface = Color(0xFF2F2F2F),
    accentOrange = Color(0xFFFF6B35), accentRed = Color(0xFFEF4444), accentGreen = Color(0xFF10B981),
    accentBlue = Color(0xFF3B82F6), accentPurple = Color(0xFF8B5CF6),
    textPrimary = Color(0xFFECECEC), textSecondary = Color(0xFFA0A0A0), selectedGreen = Color(0xFF065F46)
)

private val Grape = AppColors(
    background = Color(0xFF16121F), cardBackground = Color(0xFF221A31), surface = Color(0xFF342748),
    accentOrange = Color(0xFFFB923C), accentRed = Color(0xFFF43F5E), accentGreen = Color(0xFF34D399),
    accentBlue = Color(0xFF60A5FA), accentPurple = Color(0xFFA855F7),
    textPrimary = Color(0xFFECE6F5), textSecondary = Color(0xFFAAA0B9), selectedGreen = Color(0xFF3B2360)
)

// Order shown in the theme picker. The first entry is the default.
val appThemes: List<AppThemeOption> = listOf(
    AppThemeOption("Midnight", Midnight),
    AppThemeOption("Ocean", Ocean),
    AppThemeOption("Forest", Forest),
    AppThemeOption("Ember", Ember),
    AppThemeOption("Graphite", Graphite),
    AppThemeOption("Grape", Grape)
)

// Holds the active palette. Backed by Compose state, so any composable that reads a colour accessor
// recomposes when the theme changes.
object ThemeState {
    var colors by mutableStateOf(Midnight)
        private set
    var themeName by mutableStateOf(appThemes.first().name)
        private set

    // Loads the persisted theme. Safe to call once settings (SharedPreferences) are available.
    fun reload() = applyTheme(AppSettings.themeName)

    fun applyTheme(name: String) {
        val option = appThemes.firstOrNull { it.name == name } ?: appThemes.first()
        colors = option.colors
        themeName = option.name
    }
}

// Reactive colour accessors. Screens import these (com.bodyforge.ui.theme.*) in place of local
// hardcoded constants; reading them inside composition tracks ThemeState.colors for recomposition.
val DarkBackground: Color get() = ThemeState.colors.background
val CardBackground: Color get() = ThemeState.colors.cardBackground
val CardBg: Color get() = ThemeState.colors.cardBackground
val SurfaceColor: Color get() = ThemeState.colors.surface
val AccentOrange: Color get() = ThemeState.colors.accentOrange
val AccentRed: Color get() = ThemeState.colors.accentRed
val AccentGreen: Color get() = ThemeState.colors.accentGreen
val AccentBlue: Color get() = ThemeState.colors.accentBlue
val AccentPurple: Color get() = ThemeState.colors.accentPurple
val TextPrimary: Color get() = ThemeState.colors.textPrimary
val TextSecondary: Color get() = ThemeState.colors.textSecondary
val SelectedGreen: Color get() = ThemeState.colors.selectedGreen
val TrackColor: Color get() = ThemeState.colors.surface.copy(alpha = 0.4f)
val ThumbColor: Color get() = ThemeState.colors.accentBlue.copy(alpha = 0.8f)

// The workout finish/stop buttons keep their distinct earthy tones across every theme.
val ButtonRed: Color = Color(0xFF8B4513).copy(alpha = 0.8f)
val ButtonGreen: Color = Color(0xFF2E7D32).copy(alpha = 0.8f)
