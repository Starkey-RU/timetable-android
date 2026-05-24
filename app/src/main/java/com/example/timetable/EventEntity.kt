package com.example.timetable

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String,
    val colorKey: String,
    val iconKey: String = "event",
    val startMillis: Long,
    val endMillis: Long,
    // битовая маска дней недели: бит 0 = пн, ..., бит 6 = вс. 0 = одиночное событие.
    val recurrenceMask: Int = 0,
    // 0 = все недели, 1 = только чётные, 2 = только нечётные
    val weekParity: Int = 0,
    // учебные поля - заполняются по желанию
    val teacher: String? = null,
    val classNumber: String? = null,
    val room: String? = null,
)
