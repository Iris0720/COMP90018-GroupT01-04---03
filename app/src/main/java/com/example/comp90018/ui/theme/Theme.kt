package com.example.comp90018.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TrailwiseColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDark,
    secondary = Color(0xFF476456),
    background = AppBackground,
    surface = Color.White,
    onSurface = Ink,
    outline = Color(0xFFCBD5CD),
    error = Color(0xFFB3261E)
)

@Composable
fun TrailwiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TrailwiseColors, typography = TrailwiseTypography, content = content)
}
