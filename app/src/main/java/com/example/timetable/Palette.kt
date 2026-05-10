package com.example.timetable

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 4 пресета цветовой схемы для material3, влияет на primary/secondary/tertiary
enum class Palette(
    val title: String,
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val tertiary: Color,
    val tertiaryDark: Color,
) {
    Teal(
        title = "Teal",
        primary = Color(0xFF0E4F4C),
        primaryDark = Color(0xFF8DD3CC),
        secondary = Color(0xFF4A6361),
        secondaryDark = Color(0xFFB0CCC9),
        tertiary = Color(0xFF456179),
        tertiaryDark = Color(0xFFADCAE5),
    ),
    Forest(
        title = "Forest",
        primary = Color(0xFF2E5D3A),
        primaryDark = Color(0xFF9BD0A4),
        secondary = Color(0xFF4F6852),
        secondaryDark = Color(0xFFB7CDB7),
        tertiary = Color(0xFF38616C),
        tertiaryDark = Color(0xFFA0CCDA),
    ),
    Slate(
        title = "Slate",
        primary = Color(0xFF3E4A5C),
        primaryDark = Color(0xFFB6C2D6),
        secondary = Color(0xFF565E6E),
        secondaryDark = Color(0xFFC0C6D4),
        tertiary = Color(0xFF6E5A78),
        tertiaryDark = Color(0xFFD4BFE0),
    ),
    Aubergine(
        title = "Aubergine",
        primary = Color(0xFF5B2A66),
        primaryDark = Color(0xFFD7B0E0),
        secondary = Color(0xFF6E5C72),
        secondaryDark = Color(0xFFD7C5DA),
        tertiary = Color(0xFF7A4A5C),
        tertiaryDark = Color(0xFFE6B5C5),
    );
}

enum class GradientPreset(val title: String, val from: Color, val to: Color) {
    WineBlack("Wine -> Black", Color(0xFF95122C), Color(0xFF100C08)),
    OrangeCrimson("Orange -> Crimson", Color(0xFFFF6B00), Color(0xFFE8003A)),
    GoldOrangeRed("Gold -> Orange-Red", Color(0xFFFFD700), Color(0xFFFF4500));

    val brush: Brush get() = Brush.linearGradient(listOf(from, to))
}
