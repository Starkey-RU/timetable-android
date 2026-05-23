package com.example.timetable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

// единый набор иконок-категорий: ключ хранится в БД строкой, тут маппится в material icon
object EventIcons {
    val keys: List<String> = listOf("event", "book", "food", "laptop", "fitness", "chat")
    val labels: Map<String, String> = mapOf(
        "event" to "Дело",
        "book" to "Учёба",
        "food" to "Еда",
        "laptop" to "Работа",
        "fitness" to "Спорт",
        "chat" to "Встреча",
    )

    fun vector(key: String): ImageVector = when (key) {
        "book"    -> Icons.Filled.Book
        "food"    -> Icons.Filled.Restaurant
        "laptop"  -> Icons.Filled.Laptop
        "fitness" -> Icons.Filled.FitnessCenter
        "chat"    -> Icons.AutoMirrored.Filled.Chat
        else      -> Icons.Filled.Event
    }
}
