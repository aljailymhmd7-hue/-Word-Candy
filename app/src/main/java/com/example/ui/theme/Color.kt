package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Word Splash Candy Palette
val CandyPinkRed = Color(0xFFFF4B6E)
val CandyBlue = Color(0xFF4B8EFF)
val CandyGreen = Color(0xFF4BFF8E)
val CandyGreenDark = Color(0xFF18B858)
val CandyYellow = Color(0xFFFFE84B)
val CandyGold = Color(0xFFFFC700)
val CandyPurple = Color(0xFFB46EFF)
val CandyOrange = Color(0xFFFF8E4B)
val CandyCyan = Color(0xFF4BFFEA)

// Background Gradient Stops
val BgGradientPurple = Color(0xFF8A5CFF)
val BgGradientOrange = Color(0xFFFF9A5C)
val BgGradientYellow = Color(0xFFFFD93D)

// UI Surfaces & Contrast
val SurfaceFrost = Color(0xF2FFFFFF)
val SurfaceCardWhite = Color(0xFFFFFFFF)
val SurfaceDarkPurple = Color(0xFF2D1657)
val TextOnLight = Color(0xFF2B1B4D)
val TextMuted = Color(0xFF6B5A8E)
val LockedGray = Color(0xFFB8B2C8)
val LockedGrayDark = Color(0xFF8C85A0)

val TilePalette = listOf(
    Color(0xFF5B8DEF), // Blue
    Color(0xFFFF6B6B), // Red
    Color(0xFFA78BFA), // Purple
    Color(0xFF2DD4BF), // Teal
    Color(0xFFFB923C), // Orange
    Color(0xFF4ADE80)  // Green
)

val RainbowColors = listOf(
    CandyPinkRed,
    CandyOrange,
    CandyYellow,
    CandyGreen,
    CandyCyan,
    CandyBlue,
    CandyPurple
)

data class BoardThemeStyle(
    val backgroundStops: List<Color>,
    val boardFrameColor: Color,
    val boardBorderColor: Color,
    val tilePalette: List<Color>,
    val tileCornerRadiusDp: Int
)

fun com.example.model.BoardTheme.style(): BoardThemeStyle = when (this) {
    com.example.model.BoardTheme.CANDY_CLASSIC -> BoardThemeStyle(
        backgroundStops = listOf(BgGradientPurple, BgGradientOrange, BgGradientYellow),
        boardFrameColor = SurfaceDarkPurple.copy(alpha = 0.72f),
        boardBorderColor = Color.White.copy(alpha = 0.78f),
        tilePalette = TilePalette,
        tileCornerRadiusDp = 10
    )
    com.example.model.BoardTheme.BERRY_BLAST -> BoardThemeStyle(
        backgroundStops = listOf(Color(0xFF6A1B9A), CandyPinkRed, Color(0xFFFFB74D)),
        boardFrameColor = Color(0xFF4A0E4E).copy(alpha = 0.78f),
        boardBorderColor = Color(0xFFFFD1DC),
        tilePalette = listOf(
            CandyPinkRed, Color(0xFFAB47BC), Color(0xFFFF7043),
            Color(0xFFEC407A), Color(0xFF7E57C2), Color(0xFFFFA726), Color(0xFF26A69A)
        ),
        tileCornerRadiusDp = 14
    )
    com.example.model.BoardTheme.MINT_CHILL -> BoardThemeStyle(
        backgroundStops = listOf(Color(0xFF1A73E8), Color(0xFF00BFA5), Color(0xFFA7FFEB)),
        boardFrameColor = Color(0xFF0D4D58).copy(alpha = 0.78f),
        boardBorderColor = Color(0xFFB2FFF0),
        tilePalette = listOf(
            Color(0xFF00BFA5), CandyBlue, Color(0xFF26C6DA),
            Color(0xFF66BB6A), Color(0xFF29B6F6), Color(0xFF5C6BC0), Color(0xFF26A69A)
        ),
        tileCornerRadiusDp = 8
    )
    com.example.model.BoardTheme.STAR_GALAXY -> BoardThemeStyle(
        backgroundStops = listOf(Color(0xFF2B1055), Color(0xFF7597DE), Color(0xFFFF8E4B)),
        boardFrameColor = Color(0xFF190A38).copy(alpha = 0.84f),
        boardBorderColor = CandyYellow.copy(alpha = 0.85f),
        tilePalette = listOf(
            Color(0xFF5E35B1), Color(0xFF3949AB), CandyPinkRed,
            CandyOrange, Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60)
        ),
        tileCornerRadiusDp = 12
    )
    com.example.model.BoardTheme.CITRUS_SODA -> BoardThemeStyle(
        backgroundStops = listOf(Color(0xFFFF6F00), Color(0xFFFFB300), Color(0xFF9CCC65)),
        boardFrameColor = Color(0xFF4E2A00).copy(alpha = 0.74f),
        boardBorderColor = Color(0xFFFFF59D),
        tilePalette = listOf(
            Color(0xFFFF7043), Color(0xFF7CB342), Color(0xFFFBC02D),
            Color(0xFFFF8F00), Color(0xFF26A69A), Color(0xFFEF5350), Color(0xFF8BC34A)
        ),
        tileCornerRadiusDp = 14
    )
    com.example.model.BoardTheme.ROYAL_VELVET -> BoardThemeStyle(
        backgroundStops = listOf(Color(0xFF3E1833), Color(0xFF8E2452), Color(0xFFFFCA28)),
        boardFrameColor = Color(0xFF2A1215).copy(alpha = 0.84f),
        boardBorderColor = CandyGold.copy(alpha = 0.9f),
        tilePalette = listOf(
            Color(0xFFAD1457), Color(0xFF6D4C41), Color(0xFFD84315),
            Color(0xFF7B1FA2), Color(0xFFF9A825), Color(0xFFC2185B), Color(0xFF5D4037)
        ),
        tileCornerRadiusDp = 6
    )
}
