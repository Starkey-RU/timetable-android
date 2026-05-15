package com.example.timetable

// упрощённое событие для отправки на часы huawei.
// часам не нужны цвет фона / иконка / маска повторов - им важно "что и когда".
// формат фиксированный: дальше будет сериализоваться в json и улетать через Wear Engine.
data class WatchPayload(
    val id: Long,
    val title: String,
    val location: String,
    val startMillis: Long,
    val endMillis: Long,
)

fun EventEntity.toWatchPayload(): WatchPayload = WatchPayload(
    id = id,
    title = title,
    location = location,
    startMillis = startMillis,
    endMillis = endMillis,
)

// TODO подключить Wear Engine SDK когда придёт доступ к Huawei Developer Account.
// сейчас это просто заглушка - готовит данные, не отправляет.
object WatchSync {
    fun prepareForToday(events: List<EventEntity>, nowMillis: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): List<WatchPayload> {
        val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val from = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        // те же вхождения что и Today видит на главном экране
        val occurrences = events.flatMap { expandRecurrence(it, from, to, zone) }
        return occurrences.sortedBy { it.startMillis }.map { it.toWatchPayload() }
    }
}
