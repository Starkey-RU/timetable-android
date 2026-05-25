package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

// фиксируем текущее поведение парсера на DTSTART с параметром TZID.
// учёт самого таймзонного параметра пока не сделан, тест проверяет что парсер не валится
// и время трактуется как local в переданной зоне.
class IcsImportTzidTest {

    private val crlf = "\r\n"

    @Test
    fun `парсер не падает на DTSTART с TZID`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:tzid-1",
            "DTSTART;TZID=Europe/Moscow:20260524T100000",
            "DTEND;TZID=Europe/Moscow:20260524T110000",
            "SUMMARY:Лекция",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val events = IcsImport.parse(ics, ZoneOffset.UTC)
        assertTrue("ожидаем хотя бы одно событие", events.isNotEmpty())
        val ev = events.first()
        assertEquals("Лекция", ev.title)
    }

    @Test
    fun `время с TZID разбирается как local в переданной зоне`() {
        // тут видно что параметр TZID игнорируется: мы передаём zone = UTC,
        // и 10 часов на 24 мая берутся как 10 UTC, не как 10 московских (что было бы 7 UTC).
        // если/когда добавим поддержку TZID - тест нужно будет править.
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "DTSTART;TZID=Europe/Moscow:20260524T100000",
            "DTEND;TZID=Europe/Moscow:20260524T113000",
            "SUMMARY:Семинар",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val ev = IcsImport.parse(ics, ZoneOffset.UTC).single()
        val expectedStart = LocalDateTime.of(2026, 5, 24, 10, 0)
            .toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expectedStart, ev.startMillis)
        assertEquals(90L * 60_000L, ev.endMillis - ev.startMillis)
    }

    @Test
    fun `событие с TZID импортируется с непустым названием и локацией`() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "DTSTART;TZID=Asia/Yekaterinburg:20260601T090000",
            "DTEND;TZID=Asia/Yekaterinburg:20260601T103000",
            "SUMMARY:Практика",
            "LOCATION:Ауд. 401",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString(crlf)

        val ev = IcsImport.parse(ics, ZoneOffset.UTC).single()
        assertNotNull(ev)
        assertEquals("Практика", ev.title)
        assertEquals("Ауд. 401", ev.location)
    }
}
