package com.studenthub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AuroraDarkColorScheme = darkColorScheme(
    background = AuroraBackground,
    surface = AuroraSurface,
    onBackground = AuroraOnBackground,
    onSurface = AuroraOnBackground,
    primary = AuroraPrimary,
    onPrimary = AuroraOnPrimary,
    primaryContainer = AuroraPrimaryContainer,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = AuroraOnSurfaceVariant,
    tertiary = AuroraTertiary,
    error = AuroraError,
    onError = AuroraOnPrimary,
    outline = AuroraOutline
)

@Composable
fun StudentHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuroraDarkColorScheme,
        typography = AuroraTypography,
        shapes = AuroraShapes,
        content = content
    )
}
