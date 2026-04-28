package com.afquintana.mergepdf.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MergeBlue = Color(0xFF2563EB)
val MergeBlueDark = Color(0xFF1D4ED8)
val MergeSurface = Color(0xFFFFFBFE)
val MergeCard = Color(0xFFF8F3F8)
private val MergeDarkBackground = Color(0xFF101318)
private val MergeDarkSurface = Color(0xFF171A21)
private val MergeDarkCard = Color(0xFF232733)

private val LightColors = lightColorScheme(
    primary = MergeBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    background = MergeSurface,
    surface = MergeSurface,
    surfaceVariant = MergeCard,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF002F66),
    primaryContainer = MergeBlueDark,
    onPrimaryContainer = Color(0xFFD6E3FF),
    background = MergeDarkBackground,
    onBackground = Color(0xFFE6E8EF),
    surface = MergeDarkSurface,
    onSurface = Color(0xFFE6E8EF),
    surfaceVariant = MergeDarkCard,
    onSurfaceVariant = Color(0xFFC4C7D1),
    outline = Color(0xFF8E929E),
    outlineVariant = Color(0xFF444955),
)

@Composable
fun MergePdfTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
