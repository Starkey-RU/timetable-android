package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class WeekViewModelTest {

    private val zone = ZoneOffset.UTC

    private fun ev(
        id: Long,
        date: LocalDate,
        startHour: Int = 9,
        mask: Int = 0,
    ): EventEntity {
        val start = date.atTime(LocalTime.of(startHour, 0)).toInstant(zone).toEpochMilli()
        val end = start + 60 * 60 * 1000L
        return EventEntity(
            id = id,
            title = "e$id",
            location = "x",
            colorKey = "indigo",
            startMillis = start,
            endMillis = end,
            recurrenceMask = mask,
        )
    }

    @Test
    fun `пустая база - семь дней с нулём`() {
        val week = buildWeek(emptyList(), LocalDate.of(2026, 5, 24), zone)
        assertEquals(7, week.size)
        assertEquals(List(7) { 0 }, week.map { it.count })
    }

    @Test
    fun `неделя считается с понедельника`() {
        // 24 мая 2026 - воскресенье. понедельник - 18 мая
        val week = buildWeek(emptyList(), LocalDate.of(2026, 5, 24), zone)
        assertEquals(LocalDate.of(2026, 5, 18), week.first().date)
        assertEquals(DayOfWeek.MONDAY, week.first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 5, 24), week.last().date)
        assertEquals(DayOfWeek.SUNDAY, week.last().date.dayOfWeek)
    }

    @Test
    fun `одиночное событие попадает в свой день`() {
        val wed = LocalDate.of(2026, 5, 20)
        val week = buildWeek(listOf(ev(1, wed)), LocalDate.of(2026, 5, 24), zone)
        // среда - индекс 2 (пн=0)
        assertEquals(1, week[2].count)
        assertEquals(listOf(1L), week[2].events.map { it.id })
        assertEquals(0, week[0].count)
    }

    @Test
    fun `повторяющееся событие считается в каждый отмеченный день`() {
        val anchor = LocalDate.of(2026, 5, 11)
        // пн + ср + пт
        val template = ev(1, anchor, mask = WeekDays.MON or WeekDays.WED or WeekDays.FRI)
        val week = buildWeek(listOf(template), LocalDate.of(2026, 5, 24), zone)
        assertEquals(1, week[0].count) // пн
        assertEquals(0, week[1].count) // вт
        assertEquals(1, week[2].count) // ср
        assertEquals(0, week[3].count) // чт
        assertEquals(1, week[4].count) // пт
        assertEquals(0, week[5].count) // сб
        assertEquals(0, week[6].count) // вс
    }

    @Test
    fun `события внутри дня отсортированы по времени`() {
        val day = LocalDate.of(2026, 5, 20)
        val week = buildWeek(
            listOf(ev(1, day, startHour = 16), ev(2, day, startHour = 9)),
            LocalDate.of(2026, 5, 24),
            zone,
        )
        assertEquals(listOf(2L, 1L), week[2].events.map { it.id })
    }
}
