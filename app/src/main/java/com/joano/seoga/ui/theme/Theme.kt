package com.joano.seoga.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color(0xFF0F102A),
    primaryContainer = AccentTertiary,
    onPrimaryContainer = Color(0xFFE2E3FF),
    secondary = AccentSecondary,
    onSecondary = Color(0xFF14152A),
    secondaryContainer = DarkSurfaceSecondary,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = SemanticWarning,
    onTertiary = Color(0xFF2B1700),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    outlineVariant = Color(0xFF262734),
    error = SemanticError,
    onError = Color(0xFF370001)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5357D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E3FF),
    onPrimaryContainer = Color(0xFF0E1045),
    secondary = Color(0xFF5E6088),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceSecondary,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = Color(0xFFEFEFF7),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun SeogaTheme(
    darkTheme: Boolean = true, // Default to dark theme as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
