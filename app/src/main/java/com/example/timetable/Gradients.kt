package com.example.timetable

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// общий пул градиентов для шапки на сегодня и фона фокус-режима.
// первые три - яркие, остальные тёмные. амолед чёрный по умолчанию для фокуса (oled бережёт).
enum class AppGradient(val title: String, private val from: Color, private val to: Color) {
    WineBlack("Вино", Color(0xFF95122C), Color(0xFF100C08)),
    OrangeCrimson("Оранжевый-малина", Color(0xFFFF6B00), Color(0xFFE8003A)),
    GoldOrangeRed("Золото-огонь", Color(0xFFFFD700), Color(0xFFFF4500)),
    Amoled("Amoled (чёрный)", Color(0xFF000000), Color(0xFF000000)),
    TealMist("Морская дымка", Color(0xFF0A3D3A), Color(0xFF111C1B)),
    IndigoNight("Индиго", Color(0xFF1A1F4E), Color(0xFF080B1E)),
    VioletNebula("Фиолетовая туманность", Color(0xFF2D1A4E), Color(0xFF0E0816)),
    WarmAmber("Тёплый янтарь", Color(0xFF3D2A0A), Color(0xFF1A0E00)),
    Aurora("Северное сияние", Color(0xFF003C2F), Color(0xFF000F2E)),
    SunsetWine("Закат-вино", Color(0xFF4E1A2D), Color(0xFF0E0814)),
    OceanDeep("Глубокий океан", Color(0xFF001F3F), Color(0xFF000814));

    val brush: Brush get() = Brush.linearGradient(listOf(from, to))
}
