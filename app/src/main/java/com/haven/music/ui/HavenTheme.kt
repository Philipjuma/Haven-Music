package com.haven.music.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun HavenTheme(
    dominantColor: Color,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFFFF9800), // Haven Orange
        secondary = dominantColor,
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
