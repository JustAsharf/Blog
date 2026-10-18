package com.dailyassistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF13A472)
private val GreenDark = Color(0xFF0C7D56)
private val Sand = Color(0xFFF4A259)
private val Ink = Color(0xFF12161C)

private val LightScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF2E4),
    onPrimaryContainer = Color(0xFF05291C),
    secondary = Color(0xFF3B6EA5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7F8),
    onSecondaryContainer = Color(0xFF0C2340),
    tertiary = Sand,
    onTertiary = Color(0xFF3A2206),
    background = Color(0xFFF7F7FB),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDEFF4),
    onSurfaceVariant = Color(0xFF444C57),
    outline = Color(0xFFB9C0CB),
    error = Color(0xFFC0392B),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF00281A),
    primaryContainer = Color(0xFF0E5C40),
    onPrimaryContainer = Color(0xFFBFF0DC),
    secondary = Color(0xFF8FB8E8),
    onSecondary = Color(0xFF0A1C2E),
    secondaryContainer = Color(0xFF234668),
    onSecondaryContainer = Color(0xFFD6E6F8),
    tertiary = Sand,
    onTertiary = Color(0xFF2A1702),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE6E8EC),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFE6E8EC),
    surfaceVariant = Color(0xFF242932),
    onSurfaceVariant = Color(0xFFBCC3CE),
    outline = Color(0xFF414956),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF3A0905)
)

@Composable
fun AppTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content
    )
}

object Tones {
    val good = Color(0xFF1E9E63)
    val warn = Color(0xFFE08A1E)
    val bad = Color(0xFFD1453B)
    val info = Color(0xFF3B6EA5)
}
