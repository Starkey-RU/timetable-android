package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class IcsImportTest {

    private val crlf = "\r\n"

    @Test
    fun `парсит одно событие с DTSTART DTEND и SUMMARY`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:1@timetable",
            "DTSTART:20260524T100000Z",
            "DTEND:20260524T113000Z",
            "SUMMARY:Лекция",
            "LOCATION:Ауд. 312",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val events = IcsImport.parse(ics, ZoneOffset.UTC)
        assertEquals(1, events.size)
        val ev = events.first()
        assertEquals("Лекция", ev.title)
        assertEquals("Ауд. 312", ev.location)
        // 10:00 UTC = 1748080800000 ms; проверим что между значениями полтора часа
        assertEquals(90L * 60_000L, ev.endMillis - ev.startMillis)
    }

    @Test
    fun `пустой ics даёт пустой список`() {
        val events = IcsImport.parse("BEGIN:VCALENDAR${crlf}END:VCALENDAR")
        assertTrue(events.isEmpty())
    }

    @Test
    fun `BYDAY превращается в recurrenceMask`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "DTSTART:20260518T200000Z",
            "DTEND:20260518T210000Z",
            "SUMMARY:Зал",
            "RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val ev = IcsImport.parse(ics, ZoneOffset.UTC).single()
        assertEquals(WeekDays.MON or WeekDays.WED or WeekDays.FRI, ev.recurrenceMask)
        assertEquals(WeekParity.ALL, ev.weekParity)
    }

    @Test
    fun `INTERVAL=2 даёт чётную неделю`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "DTSTART:20260519T190000Z",
            "DTEND:20260519T200000Z",
            "SUMMARY:Английский",
            "RRULE:FREQ=WEEKLY;BYDAY=TU,TH;INTERVAL=2",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val ev = IcsImport.parse(ics, ZoneOffset.UTC).single()
        assertEquals(WeekDays.TUE or WeekDays.THU, ev.recurrenceMask)
        assertEquals(WeekParity.EVEN, ev.weekParity)
    }

    @Test
    fun `экранированные запятые и точки с запятой разворачиваются`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "DTSTART:20260524T120000Z",
            "DTEND:20260524T130000Z",
            "SUMMARY:Встреча\\, важная\\; срочно",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val ev = IcsImport.parse(ics, ZoneOffset.UTC).single()
        assertEquals("Встреча, важная; срочно", ev.title)
    }

    @Test
    fun `roundtrip export-import возвращает совпадающее событие`() {
        val original = EventEntity(
            id = 99,
            title = "Семинар",
            location = "Ауд. 218",
            colorKey = "violet",
            startMillis = 1_748_080_800_000L,
            endMillis = 1_748_086_200_000L,
            recurrenceMask = WeekDays.MON or WeekDays.WED,
        )
        val ics = IcsExport.build(listOf(original))
        val parsed = IcsImport.parse(ics, ZoneOffset.UTC).single()
        assertEquals(original.title, parsed.title)
        assertEquals(original.location, parsed.location)
        assertEquals(original.startMillis, parsed.startMillis)
        assertEquals(original.endMillis, parsed.endMillis)
        assertEquals(original.recurrenceMask, parsed.recurrenceMask)
    }
}
