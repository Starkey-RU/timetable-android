package com.example.timetable

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class IcsExportTest {

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `обычное событие даёт VEVENT с DTSTART и SUMMARY`() {
        val ev = EventEntity(
            id = 7,
            title = "Лекция",
            location = "Ауд. 312",
            colorKey = "indigo",
            startMillis = at(2026, 5, 24, 10, 0),
            endMillis = at(2026, 5, 24, 11, 30),
        )
        val ics = IcsExport.build(listOf(ev), nowMillis = at(2026, 5, 24, 9, 0))
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertTrue(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("UID:7@timetable"))
        assertTrue(ics.contains("DTSTART:20260524T100000Z"))
        assertTrue(ics.contains("DTEND:20260524T113000Z"))
        assertTrue(ics.contains("SUMMARY:Лекция"))
        assertTrue(ics.contains("LOCATION:Ауд. 312"))
    }

    @Test
    fun `повторяющееся даёт RRULE с нужными днями`() {
        val ev = EventEntity(
            id = 1,
            title = "Зал",
            location = "",
            colorKey = "emerald",
            startMillis = at(2026, 5, 18, 20, 0),
            endMillis = at(2026, 5, 18, 21, 0),
            recurrenceMask = WeekDays.MON or WeekDays.WED or WeekDays.FRI,
        )
        val ics = IcsExport.build(listOf(ev))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR"))
    }

    @Test
    fun `чёт-нечёт превращается в INTERVAL=2`() {
        val ev = EventEntity(
            id = 2,
            title = "Английский",
            location = "Zoom",
            colorKey = "rose",
            startMillis = at(2026, 5, 19, 19, 0),
            endMillis = at(2026, 5, 19, 20, 0),
            recurrenceMask = WeekDays.TUE or WeekDays.THU,
            weekParity = WeekParity.EVEN,
        )
        val ics = IcsExport.build(listOf(ev))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;BYDAY=TU,TH;INTERVAL=2"))
    }

    @Test
    fun `запятые и точки с запятой в названии экранируются`() {
        val ev = EventEntity(
            id = 3,
            title = "Встреча, важная; срочно",
            location = "Офис",
            colorKey = "indigo",
            startMillis = at(2026, 5, 24, 12, 0),
            endMillis = at(2026, 5, 24, 13, 0),
        )
        val ics = IcsExport.build(listOf(ev))
        assertTrue(ics.contains("SUMMARY:Встреча\\, важная\\; срочно"))
    }
}
