package com.example.timetable

import androidx.compose.ui.graphics.Color

// палитра меток для событий, ключи лежат в БД строкой
object EventColors {
    val keys: List<String> = listOf("indigo", "teal", "amber", "emerald", "rose", "violet", "slate", "coral")

    fun stripe(key: String): Color = when (key) {
        "indigo"  -> Color(0xFF5C6BC0)
        "teal"    -> Color(0xFF0E7C77)
        "amber"   -> Color(0xFFB58A3C)
        "emerald" -> Color(0xFF4E8A66)
        "rose"    -> Color(0xFFB0686E)
        "violet"  -> Color(0xFF7E64A9)
        "slate"   -> Color(0xFF6B7E91)
        "coral"   -> Color(0xFFC16A4E)
        else      -> Color(0xFF6B7E91)
    }
}
