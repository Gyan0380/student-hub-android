package com.studenthub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.studenthub.app.util.AppThemeOption

// Mirrors the 4 themes from the web build (Light / Dark / Sepia / Ocean toggle).
private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF64748B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF94A3B8)
)

private val SepiaColors = lightColorScheme(
    primary = Color(0xFF8B5E34),
    secondary = Color(0xFFA9835A),
    background = Color(0xFFF4ECD8),
    surface = Color(0xFFF4ECD8),
    onBackground = Color(0xFF433422),
    onSurface = Color(0xFF433422)
)

private val OceanColors = lightColorScheme(
    primary = Color(0xFF0EA5E9),
    secondary = Color(0xFF06B6D4),
    background = Color(0xFFE6F6FB),
    surface = Color(0xFFECFBFF),
    onBackground = Color(0xFF063B4B),
    onSurface = Color(0xFF063B4B)
)

@Composable
fun StudentHubTheme(option: AppThemeOption, content: @Composable () -> Unit) {
    val colors = when (option) {
        AppThemeOption.LIGHT -> LightColors
        AppThemeOption.DARK -> DarkColors
        AppThemeOption.SEPIA -> SepiaColors
        AppThemeOption.OCEAN -> OceanColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
