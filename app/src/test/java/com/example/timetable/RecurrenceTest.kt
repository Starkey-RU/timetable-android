package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class RecurrenceTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `по будням за вторник - одно вхождение`() {
        // TODO кейсы по чётным/нечётным неделям, граница смены ISO-года, шаблон в будущем
        val tpl = EventEntity(
            id = 1, title = "tpl", location = "", colorKey = "indigo",
            startMillis = millis(2026, 5, 18, 9, 0),
            endMillis = millis(2026, 5, 18, 10, 0),
            recurrenceMask = WeekDays.MON or WeekDays.TUE or WeekDays.WED or WeekDays.THU or WeekDays.FRI,
        )
        // 2026-05-19 - вторник
        val out = expandRecurrence(tpl, millis(2026, 5, 19), millis(2026, 5, 20), zone)
        assertEquals(1, out.size)
    }
}
