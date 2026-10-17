package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ScrapMasterDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBase,
    primaryContainer = DarkCard,
    onPrimaryContainer = NeonCyan,
    secondary = NeonEmerald,
    onSecondary = DarkBase,
    secondaryContainer = DarkCard,
    onSecondaryContainer = NeonEmerald,
    tertiary = NeonAmber,
    onTertiary = DarkBase,
    background = DarkBase,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = NeonRose,
    onError = DarkBase
)

private val ScrapMasterLightColorScheme = lightColorScheme(
    primary = LightNeonCyan,
    onPrimary = LightBase,
    primaryContainer = LightCard,
    onPrimaryContainer = LightNeonCyan,
    secondary = LightNeonEmerald,
    onSecondary = LightBase,
    secondaryContainer = LightCard,
    onSecondaryContainer = LightNeonEmerald,
    tertiary = LightNeonAmber,
    onTertiary = LightBase,
    background = LightBase,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = LightNeonRose,
    onError = LightBase
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val colorScheme = if (darkTheme) ScrapMasterDarkColorScheme else ScrapMasterLightColorScheme

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = (if (darkTheme) DarkBase else LightBase).toArgb()
                window.navigationBarColor = (if (darkTheme) DarkBase else LightBase).toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
