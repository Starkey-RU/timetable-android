package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTextTest {

    @Test
    fun `минутный формат оставляет минуты`() {
        assertEquals("190 мин", formatDurationShort(190, asHours = false))
    }

    @Test
    fun `часовой формат выводит часы и остаток`() {
        assertEquals("3 ч 10 мин", formatDurationShort(190, asHours = true))
        assertEquals("3 ч", formatDurationShort(180, asHours = true))
    }
}

