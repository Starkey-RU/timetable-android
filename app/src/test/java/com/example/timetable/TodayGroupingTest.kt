package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayGroupingTest {

    private fun ev(id: Long, start: Long, end: Long) =
        EventEntity(id = id, title = "e$id", location = "x", colorKey = "indigo", startMillis = start, endMillis = end)

    @Test
    fun `пустой список — всё пусто`() {
        val s = groupForToday(emptyList(), nowMillis = 1000)
        assertTrue(s.now.isEmpty())
        assertNull(s.next)
        assertTrue(s.later.isEmpty())
        assertTrue(s.done.isEmpty())
    }

    @Test
    fun `события до now попадают в done`() {
        val s = groupForToday(listOf(ev(1, 0, 100), ev(2, 100, 200)), nowMillis = 500)
        assertEquals(listOf(1L, 2L), s.done.map { it.id })
        assertTrue(s.now.isEmpty())
    }

    @Test
    fun `активное событие попадает в now`() {
        val s = groupForToday(listOf(ev(1, 100, 500)), nowMillis = 200)
        assertEquals(listOf(1L), s.now.map { it.id })
        assertNull(s.next)
    }

    @Test
    fun `next — ближайшее будущее, остальные later`() {
        val s = groupForToday(
            events = listOf(ev(1, 100, 200), ev(2, 300, 400), ev(3, 500, 600)),
            nowMillis = 50,
        )
        assertEquals(1L, s.next?.id)
        assertEquals(listOf(2L, 3L), s.later.map { it.id })
    }

    @Test
    fun `события на границе now=end считаются завершёнными`() {
        // условие в коде: end > now -> ещё активно. при end == now -> done
        val s = groupForToday(listOf(ev(1, 0, 100)), nowMillis = 100)
        assertEquals(listOf(1L), s.done.map { it.id })
        assertTrue(s.now.isEmpty())
    }
}
