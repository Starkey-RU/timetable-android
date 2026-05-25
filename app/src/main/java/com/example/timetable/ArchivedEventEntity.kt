package com.example.timetable

import androidx.room.Entity
import androidx.room.PrimaryKey

// копия события для архива - отдельная таблица, чтоб не путать
// с активным расписанием. структура почти такая же, плюс момент архивации.
@Entity(tableName = "archived_events")
data class ArchivedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String,
    val colorKey: String,
    val iconKey: String = "event",
    val startMillis: Long,
    val endMillis: Long,
    val recurrenceMask: Int = 0,
    val weekParity: Int = 0,
    val teacher: String? = null,
    val classNumber: String? = null,
    val room: String? = null,
    // когда отправили в архив - удобно для сортировки и подписи в карточке
    val archivedAt: Long,
)
