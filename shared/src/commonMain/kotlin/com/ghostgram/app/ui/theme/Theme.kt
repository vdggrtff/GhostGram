package com.ghostgram.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GhostColorScheme = darkColorScheme(
    primary = GhostPrimary,
    secondary = GhostSecondary,
    background = GhostBackground,
    surface = GhostSurface,
    surfaceVariant = GhostSurfaceElevated,
    onPrimary = GhostTextPrimary,
    onSecondary = GhostTextPrimary,
    onBackground = GhostTextPrimary,
    onSurface = GhostTextPrimary,
    onSurfaceVariant = GhostTextSecondary,
    outline = GhostBorder
)

@Composable
fun GhostGramTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostColorScheme,
        shapes = GhostShapes,
        content = content
    )
}