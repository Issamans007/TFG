package com.tfg.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlue.copy(alpha = 0.15f),
    secondary = Green500,
    onSecondary = Color.White,
    secondaryContainer = Green500.copy(alpha = 0.15f),
    tertiary = AccentGold,
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1C2333),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    error = Red500,
    onError = Color.White,
    errorContainer = Red500.copy(alpha = 0.15f)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF4FF),
    secondary = Color(0xFF1A7F37),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFFE3),
    tertiary = Color(0xFFBF8700),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = Red500,
    onError = Color.White,
    errorContainer = Red500.copy(alpha = 0.08f)
)

@Composable
fun TfgTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Update global ThemeState so dynamic color getters react
    SideEffect { ThemeState.isDark = darkTheme }

    val colorScheme = when {
        // Material You dynamic colors (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val tfgPalette = if (darkTheme) DarkTfgPalette else LightTfgPalette

    CompositionLocalProvider(LocalTfgColors provides tfgPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TfgTypography,
            content = content
        )
    }
}

object TfgTheme {
    val colors: TfgColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalTfgColors.current
}
