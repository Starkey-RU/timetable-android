package com.example.timetable

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// фон для режима фокуса. амолед чёрный по умолчанию (батарею не жрёт на oled),
// остальные градиенты подобраны тёмные чтоб ночью не слепило, но настроение разное.
enum class FocusGradient(val title: String, private val from: Color, private val to: Color) {
    Amoled("Amoled (чёрный)", Color(0xFF000000), Color(0xFF000000)),
    TealMist("Морская дымка", Color(0xFF0A3D3A), Color(0xFF111C1B)),
    IndigoNight("Индиго", Color(0xFF1A1F4E), Color(0xFF080B1E)),
    VioletNebula("Фиолетовая туманность", Color(0xFF2D1A4E), Color(0xFF0E0816)),
    WarmAmber("Тёплый янтарь", Color(0xFF3D2A0A), Color(0xFF1A0E00)),
    Aurora("Северное сияние", Color(0xFF003C2F), Color(0xFF000F2E)),
    SunsetWine("Закат-вино", Color(0xFF4E1A2D), Color(0xFF0E0814)),
    OceanDeep("Глубокий океан", Color(0xFF001F3F), Color(0xFF000814));

    // диагональный градиент - на oled-чёрном бесшовно стыкуется, на цветных тянет глаз
    val brush: Brush get() = Brush.linearGradient(listOf(from, to))
}
