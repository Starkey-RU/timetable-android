package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WeekStatsTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

    // фиксируем "сейчас" - пятница 2026-05-29 12:00 UTC.
    // окно по функции: [2026-05-22 12:00, 2026-05-29 12:00)
    private val now = millis(2026, 5, 29, 12, 0)

    @Test
    fun `пустой список даёт нули`() {
        val s = computeWeekStats(emptyList(), now, zone)
        assertEquals(0L, s.totalMinutes)
        assertTrue(s.countsByDay.isEmpty())
        assertTrue(s.topByDuration.isEmpty())
    }

    @Test
    fun `одно событие во вторник учитывается полностью`() {
        // вторник 2026-05-26 10:00 - 11:00 UTC, целиком в окне
        val ev = EventEntity(
            id = 1,
            title = "Лекция",
            location = "ауд 312",
            colorKey = "indigo",
            startMillis = millis(2026, 5, 26, 10, 0),
            endMillis = millis(2026, 5, 26, 11, 0),
        )
        val s = computeWeekStats(listOf(ev), now, zone)
        assertEquals(60L, s.totalMinutes)
        assertEquals(1, s.countsByDay[DayOfWeek.TUESDAY])
        assertEquals(listOf("Лекция" to 60L), s.topByDuration)
    }

    @Test
    fun `повторяющееся по понедельникам даёт ровно одно вхождение в окне`() {
        // шаблон на 2026-05-18 (понедельник) - до окна.
        // в окно [22.05, 29.05) попадает только 2026-05-25 (понедельник).
        val tpl = EventEntity(
            id = 2,
            title = "Английский",
            location = "Zoom",
            colorKey = "rose",
            startMillis = millis(2026, 5, 18, 19, 0),
            endMillis = millis(2026, 5, 18, 20, 0),
            recurrenceMask = WeekDays.MON,
        )
        val s = computeWeekStats(listOf(tpl), now, zone)
        assertEquals(60L, s.totalMinutes)
        assertEquals(1, s.countsByDay[DayOfWeek.MONDAY])
        assertEquals(listOf("Английский" to 60L), s.topByDuration)
    }

    @Test
    fun `событие пересекает левую границу - минуты обрезаются по окну`() {
        // начало в 11:00, левая граница окна в 12:00 - значит зачётно только 60 минут
        val ev = EventEntity(
            id = 3,
            title = "Совещание",
            location = "",
            colorKey = "amber",
            startMillis = millis(2026, 5, 22, 11, 0),
            endMillis = millis(2026, 5, 22, 13, 0),
        )
        val s = computeWeekStats(listOf(ev), now, zone)
        assertEquals(60L, s.totalMinutes)
        // день вхождения берётся по оригинальному старту - пятница
        assertEquals(1, s.countsByDay[DayOfWeek.FRIDAY])
        assertEquals(listOf("Совещание" to 60L), s.topByDuration)
    }

    @Test
    fun `топ по длительности возвращает максимум три записи отсортированные по убыванию`() {
        val a = EventEntity(id = 10, title = "A", location = "", colorKey = "indigo",
            startMillis = millis(2026, 5, 25, 9, 0), endMillis = millis(2026, 5, 25, 9, 30))
        val b = EventEntity(id = 11, title = "B", location = "", colorKey = "indigo",
            startMillis = millis(2026, 5, 26, 9, 0), endMillis = millis(2026, 5, 26, 11, 0))
        val c = EventEntity(id = 12, title = "C", location = "", colorKey = "indigo",
            startMillis = millis(2026, 5, 27, 9, 0), endMillis = millis(2026, 5, 27, 10, 0))
        val d = EventEntity(id = 13, title = "D", location = "", colorKey = "indigo",
            startMillis = millis(2026, 5, 28, 9, 0), endMillis = millis(2026, 5, 28, 9, 15))
        val s = computeWeekStats(listOf(a, b, c, d), now, zone)
        // ожидаем 3 элемента, b (120) > c (60) > a (30), d отрезался
        assertEquals(3, s.topByDuration.size)
        assertEquals("B" to 120L, s.topByDuration[0])
        assertEquals("C" to 60L, s.topByDuration[1])
        assertEquals("A" to 30L, s.topByDuration[2])
    }
}
