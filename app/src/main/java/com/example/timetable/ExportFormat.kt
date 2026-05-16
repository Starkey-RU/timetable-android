package com.example.timetable

import kotlinx.serialization.Serializable

// версия 1 - простая структура чтоб потом можно было добавить поля без поломки старых выгрузок
@Serializable
data class ExportBundle(
    val version: Int = 1,
    val events: List<EventDto>,
)

@Serializable
data class EventDto(
    val title: String,
    val location: String = "",
    val colorKey: String = "indigo",
    val iconKey: String = "event",
    val startMillis: Long,
    val endMillis: Long,
    val recurrenceMask: Int = 0,
    val weekParity: Int = 0,
)

fun EventEntity.toDto(): EventDto = EventDto(
    title = title,
    location = location,
    colorKey = colorKey,
    iconKey = iconKey,
    startMillis = startMillis,
    endMillis = endMillis,
    recurrenceMask = recurrenceMask,
    weekParity = weekParity,
)

// при импорте id всегда новый - чтоб не затирать существующие события
fun EventDto.toEntity(): EventEntity = EventEntity(
    id = 0,
    title = title,
    location = location,
    colorKey = colorKey,
    iconKey = iconKey,
    startMillis = startMillis,
    endMillis = endMillis,
    recurrenceMask = recurrenceMask,
    weekParity = weekParity,
)
