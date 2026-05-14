package com.example.timetable

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

// биты дней недели (1..7 в java.time -> биты 0..6 у нас)
// пн = 1, вт = 2, ср = 4, чт = 8, пт = 16, сб = 32, вс = 64
object WeekDays {
    const val MON = 1
    const val TUE = 2
    const val WED = 4
    const val THU = 8
    const val FRI = 16
    const val SAT = 32
    const val SUN = 64

    val all = listOf(MON, TUE, WED, THU, FRI, SAT, SUN)
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    fun bitFor(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> MON
        DayOfWeek.TUESDAY -> TUE
        DayOfWeek.WEDNESDAY -> WED
        DayOfWeek.THURSDAY -> THU
        DayOfWeek.FRIDAY -> FRI
        DayOfWeek.SATURDAY -> SAT
        DayOfWeek.SUNDAY -> SUN
    }
}

object WeekParity {
    const val ALL = 0
    const val EVEN = 1
    const val ODD = 2
}

// разворачивает шаблон повторяющегося события в конкретные вхождения,
// пересекающиеся с диапазоном [from, to).
// для одиночного события (mask == 0) возвращает сам event если он попадает в диапазон.
fun expandRecurrence(
    event: EventEntity,
    fromMillis: Long,
    toMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<EventEntity> {
    if (event.recurrenceMask == 0) {
        return if (event.startMillis < toMillis && event.endMillis > fromMillis) listOf(event)
        else emptyList()
    }

    val durationMillis = event.endMillis - event.startMillis
    if (durationMillis <= 0) return emptyList()

    val templateStart = Instant.ofEpochMilli(event.startMillis).atZone(zone)
    val timeOfDay = templateStart.toLocalTime()
    val templateDate = templateStart.toLocalDate()

    // диапазон сканируем чуть пошире чтоб не упустить вхождение, начавшееся за день до from
    val scanFrom = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate().minusDays(1)
    val scanTo = Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()

    val result = mutableListOf<EventEntity>()
    var d = scanFrom
    while (!d.isAfter(scanTo)) {
        if (!d.isBefore(templateDate) && dayMatches(d, event.recurrenceMask) && parityMatches(d, event.weekParity)) {
            val startInst = d.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
            val endInst = startInst + durationMillis
            if (startInst < toMillis && endInst > fromMillis) {
                result += event.copy(startMillis = startInst, endMillis = endInst)
            }
        }
        d = d.plusDays(1)
    }
    return result
}

private fun dayMatches(date: LocalDate, mask: Int): Boolean {
    val bit = WeekDays.bitFor(date.dayOfWeek)
    return (mask and bit) != 0
}

private fun parityMatches(date: LocalDate, parity: Int): Boolean {
    if (parity == WeekParity.ALL) return true
    val week = date.get(WeekFields.ISO.weekOfWeekBasedYear())
    return when (parity) {
        WeekParity.EVEN -> week % 2 == 0
        WeekParity.ODD -> week % 2 == 1
        else -> true
    }
}
