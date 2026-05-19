package com.example.timetable

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// собирает .ics (стандарт rfc 5545) для всех событий, чтоб открыть в google calendar / outlook / apple
object IcsExport {

    private val stamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    fun build(events: List<EventEntity>, nowMillis: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Timetable//RU")
        events.forEach { ev -> appendEvent(sb, ev, nowMillis) }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun appendEvent(sb: StringBuilder, ev: EventEntity, nowMillis: Long) {
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:${ev.id}@timetable")
        sb.appendLine("DTSTAMP:${stamp.format(Instant.ofEpochMilli(nowMillis))}")
        sb.appendLine("DTSTART:${stamp.format(Instant.ofEpochMilli(ev.startMillis))}")
        sb.appendLine("DTEND:${stamp.format(Instant.ofEpochMilli(ev.endMillis))}")
        sb.appendLine("SUMMARY:${escape(ev.title)}")
        if (ev.location.isNotBlank()) {
            sb.appendLine("LOCATION:${escape(ev.location)}")
        }
        rruleFor(ev)?.let { sb.appendLine(it) }
        sb.appendLine("END:VEVENT")
    }

    private fun rruleFor(ev: EventEntity): String? {
        if (ev.recurrenceMask == 0) return null
        val days = WeekDays.all.zip(listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU"))
            .filter { (bit, _) -> ev.recurrenceMask and bit != 0 }
            .joinToString(",") { it.second }
        if (days.isEmpty()) return null
        // чёт/нечёт неделя в стандарте точно не выражается, упрощаем до INTERVAL=2
        val interval = when (ev.weekParity) {
            WeekParity.EVEN, WeekParity.ODD -> ";INTERVAL=2"
            else -> ""
        }
        return "RRULE:FREQ=WEEKLY;BYDAY=$days$interval"
    }

    // спецсимволы в текстовых полях по rfc 5545
    private fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace(",", "\\,")
        .replace(";", "\\;")
        .replace("\n", "\\n")
}
