package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulesFilterTest {

    private fun ev(id: Long, title: String, location: String = "", mask: Int = 0) = EventEntity(
        id = id,
        title = title,
        location = location,
        colorKey = "indigo",
        startMillis = id * 1000L,
        endMillis = id * 1000L + 500,
        recurrenceMask = mask,
    )

    private val all = listOf(
        ev(1, "Математика", "Ауд. 312"),
        ev(2, "Физика", "Ауд. 218", mask = WeekDays.MON),
        ev(3, "Зал", "World Class", mask = WeekDays.MON or WeekDays.WED),
        ev(4, "Обед", "Кафе"),
    )

    @Test
    fun `пустой запрос и All - всё подряд по дате`() {
        val r = applyFilter(all, "", ScheduleFilter.All)
        assertEquals(listOf(1L, 2L, 3L, 4L), r.map { it.id })
    }

    @Test
    fun `поиск по названию без учёта регистра`() {
        val r = applyFilter(all, "мат", ScheduleFilter.All)
        assertEquals(listOf(1L), r.map { it.id })
    }

    @Test
    fun `поиск по локации`() {
        val r = applyFilter(all, "class", ScheduleFilter.All)
        assertEquals(listOf(3L), r.map { it.id })
    }

    @Test
    fun `фильтр повторяющихся`() {
        val r = applyFilter(all, "", ScheduleFilter.Repeating)
        assertEquals(listOf(2L, 3L), r.map { it.id })
    }

    @Test
    fun `фильтр разовых`() {
        val r = applyFilter(all, "", ScheduleFilter.OneTime)
        assertEquals(listOf(1L, 4L), r.map { it.id })
    }

    @Test
    fun `поиск и фильтр работают вместе`() {
        val r = applyFilter(all, "физ", ScheduleFilter.Repeating)
        // только Физика - и в названии есть 'физ', и повторяется
        assertEquals(listOf(2L), r.map { it.id })
    }
}
