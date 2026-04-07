package com.tfg.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ─── Global theme state (updated by TfgTheme) ──────────────────────
object ThemeState {
    var isDark by mutableStateOf(true)
}

// Trading accent colours (shared across themes)
val Green400 = Color(0xFF26A69A)
val Green500 = Color(0xFF00C853)
val Green700 = Color(0xFF00897B)
val Red400 = Color(0xFFEF5350)
val Red500 = Color(0xFFFF1744)
val Red700 = Color(0xFFC62828)

val AccentBlue = Color(0xFF58A6FF)
val AccentGold = Color(0xFFFFD700)
val AccentPurple = Color(0xFFBC8CFF)
val AccentOrange = Color(0xFFF0883E)

val ProfitGreen = Green500
val LossRed = Red500

// ─── Raw dark palette constants ─────────────────────────────────────
private val RawDarkBackground = Color(0xFF0D1117)
private val RawDarkSurface = Color(0xFF161B22)
private val RawDarkCard = Color(0xFF1C2333)
private val RawDarkBorder = Color(0xFF30363D)
private val RawDarkTextPrimary = Color(0xFFE6EDF3)
private val RawDarkTextSecondary = Color(0xFF8B949E)
private val RawDarkTextTertiary = Color(0xFF6E7681)

// ─── Raw light palette constants ────────────────────────────────────
private val RawLightBackground = Color(0xFFF6F8FA)
private val RawLightSurface = Color(0xFFFFFFFF)
private val RawLightCard = Color(0xFFFFFFFF)
private val RawLightBorder = Color(0xFFD0D7DE)
private val RawLightTextPrimary = Color(0xFF1F2328)
private val RawLightTextSecondary = Color(0xFF656D76)
private val RawLightTextTertiary = Color(0xFF8C959F)

// ─── Theme-aware dynamic colors ─────────────────────────────────────
// These resolve at read-time based on ThemeState.isDark so every
// existing file that uses DarkCard / TextPrimary etc. becomes reactive.
val DarkBackground: Color get() = if (ThemeState.isDark) RawDarkBackground else RawLightBackground
val DarkSurface: Color get() = if (ThemeState.isDark) RawDarkSurface else RawLightSurface
val DarkCard: Color get() = if (ThemeState.isDark) RawDarkCard else RawLightCard
val DarkBorder: Color get() = if (ThemeState.isDark) RawDarkBorder else RawLightBorder
val DarkTextPrimary: Color get() = if (ThemeState.isDark) RawDarkTextPrimary else RawLightTextPrimary
val DarkTextSecondary: Color get() = if (ThemeState.isDark) RawDarkTextSecondary else RawLightTextSecondary
val DarkTextTertiary: Color get() = if (ThemeState.isDark) RawDarkTextTertiary else RawLightTextTertiary

val LightBackground = RawLightBackground
val LightSurface = RawLightSurface
val LightCard = RawLightCard
val LightBorder = RawLightBorder
val LightTextPrimary = RawLightTextPrimary
val LightTextSecondary = RawLightTextSecondary
val LightTextTertiary = RawLightTextTertiary

// ─── Semantic aliases resolved at runtime via TfgColorPalette ───────
data class TfgColorPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isDark: Boolean
)

val DarkTfgPalette = TfgColorPalette(
    background = RawDarkBackground,
    surface = RawDarkSurface,
    card = RawDarkCard,
    border = RawDarkBorder,
    textPrimary = RawDarkTextPrimary,
    textSecondary = RawDarkTextSecondary,
    textTertiary = RawDarkTextTertiary,
    isDark = true
)

val LightTfgPalette = TfgColorPalette(
    background = RawLightBackground,
    surface = RawLightSurface,
    card = RawLightCard,
    border = RawLightBorder,
    textPrimary = RawLightTextPrimary,
    textSecondary = RawLightTextSecondary,
    textTertiary = RawLightTextTertiary,
    isDark = false
)

val LocalTfgColors = compositionLocalOf { DarkTfgPalette }

// Legacy aliases — dynamic, resolve based on current theme
val TextPrimary: Color get() = DarkTextPrimary
val TextSecondary: Color get() = DarkTextSecondary
val TextTertiary: Color get() = DarkTextTertiary
val NeutralGray: Color get() = TextSecondary
