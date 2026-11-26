package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WordSplashColorScheme = lightColorScheme(
    primary = CandyPinkRed,
    onPrimary = Color.White,
    primaryContainer = CandyOrange,
    onPrimaryContainer = Color.White,
    secondary = BgGradientPurple,
    onSecondary = Color.White,
    secondaryContainer = CandyPurple,
    onSecondaryContainer = Color.White,
    tertiary = CandyBlue,
    onTertiary = Color.White,
    background = BgGradientPurple,
    onBackground = Color.White,
    surface = SurfaceCardWhite,
    onSurface = TextOnLight,
    surfaceVariant = Color(0xFFF3ECFF),
    onSurfaceVariant = TextMuted
)

@Composable
fun WordSplashTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WordSplashColorScheme,
        typography = WordSplashTypography,
        content = content
    )
}
