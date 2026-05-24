package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class TodayGroupingTest {

    private fun ev(id: Long, start: Long, end: Long) =
        EventEntity(id = id, title = "e$id", location = "x", colorKey = "indigo", startMillis = start, endMillis = end)

    // удобный конструктор для теста через дату-время в UTC чтоб не зависеть от системной таймзоны
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `активное событие попадает в now`() {
        val s = groupForToday(listOf(ev(1, 100, 500)), nowMillis = 200)
        assertEquals(listOf(1L), s.now.map { it.id })
        assertNull(s.next)
    }

    @Test
    fun `событие началось вчера и идёт сейчас - в now`() {
        val ev = ev(1, at(2026, 5, 19, 23, 0), at(2026, 5, 20, 2, 0))
        val s = groupForToday(listOf(ev), nowMillis = at(2026, 5, 20, 0, 30))
        assertEquals(listOf(1L), s.now.map { it.id })
    }

    @Test
    fun `границы активного события считаются полуоткрытым интервалом`() {
        val event = ev(1, 100, 500)
        assertEquals(listOf(1L), groupForToday(listOf(event), nowMillis = 100).now.map { it.id })
        assertEquals(emptyList<Long>(), groupForToday(listOf(event), nowMillis = 500).now.map { it.id })
    }

    @Test
    fun `из нескольких будущих событий первое становится next`() {
        val events = listOf(ev(1, 300, 400), ev(2, 200, 250))
        val s = groupForToday(events, nowMillis = 100)
        assertEquals(2L, s.next?.id)
        assertEquals(listOf(1L), s.later.map { it.id })
    }
}
