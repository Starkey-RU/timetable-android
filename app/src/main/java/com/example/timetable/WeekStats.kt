package com.example.timetable

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

// сводка за неделю в одной структуре - удобно и тестить, и рисовать из compose
data class WeekStats(
    val totalMinutes: Long,
    val countsByDay: Map<DayOfWeek, Int>,
    val topByDuration: List<Pair<String, Long>>, // title -> сумма минут
)

// чистая функция, поэтому в тесте можно прогнать без compose и room.
// окно [now - 7 суток, now), разворачивает повторы и считает:
// - всего минут в окно
// - сколько вхождений в каждый день недели
// - топ-3 названий по суммарной длительности
fun computeWeekStats(
    events: List<EventEntity>,
    nowMillis: Long,
    zone: ZoneId,
): WeekStats {
    val weekMs = 7L * 24 * 60 * 60 * 1000
    val from = nowMillis - weekMs
    val to = nowMillis

    var totalMin = 0L
    val perDay = mutableMapOf<DayOfWeek, Int>()
    val perTitle = mutableMapOf<String, Long>()

    for (ev in events) {
        // expandRecurrence сам обработает и одиночные (mask == 0), и повторяющиеся
        val occs = expandRecurrence(ev, from, to, zone)
        for (occ in occs) {
            // обрезаем по границам окна на случай если событие пересекает край
            val s = maxOf(occ.startMillis, from)
            val e = minOf(occ.endMillis, to)
            if (e <= s) continue
            val mins = (e - s) / 60000L
            totalMin += mins
            val dow = Instant.ofEpochMilli(occ.startMillis).atZone(zone).dayOfWeek
            perDay[dow] = (perDay[dow] ?: 0) + 1
            val key = occ.title.ifBlank { "(без названия)" }
            perTitle[key] = (perTitle[key] ?: 0L) + mins
        }
    }

    val top = perTitle.entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key to it.value }

    return WeekStats(totalMinutes = totalMin, countsByDay = perDay, topByDuration = top)
}
