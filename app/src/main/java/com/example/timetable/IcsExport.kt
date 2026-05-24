package com.example.timetable

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// собирает .ics (стандарт rfc 5545) для всех событий, чтоб открыть в google calendar / outlook / apple
object IcsExport {

    private val stamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    // rfc 5545 требует именно CRLF между строками, не lf
    private const val CRLF = "\r\n"

    fun build(events: List<EventEntity>, nowMillis: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        sb.line("BEGIN:VCALENDAR")
        sb.line("VERSION:2.0")
        sb.line("PRODID:-//Timetable//RU")
        events.forEach { ev -> appendEvent(sb, ev, nowMillis) }
        sb.line("END:VCALENDAR")
        return sb.toString()
    }

    private fun appendEvent(sb: StringBuilder, ev: EventEntity, nowMillis: Long) {
        sb.line("BEGIN:VEVENT")
        sb.line("UID:${ev.id}@timetable")
        sb.line("DTSTAMP:${stamp.format(Instant.ofEpochMilli(nowMillis))}")
        sb.line("DTSTART:${stamp.format(Instant.ofEpochMilli(ev.startMillis))}")
        sb.line("DTEND:${stamp.format(Instant.ofEpochMilli(ev.endMillis))}")
        sb.line("SUMMARY:${escape(ev.title)}")
        if (ev.location.isNotBlank()) {
            sb.line("LOCATION:${escape(ev.location)}")
        }
        rruleFor(ev)?.let { sb.line(it) }
        sb.line("END:VEVENT")
    }

    private fun StringBuilder.line(text: String): StringBuilder = append(text).append(CRLF)

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
