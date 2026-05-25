package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class PresetsTest {

    // фиксируем зону чтобы не зависеть от tz машины где гоняются тесты
    private val zone = ZoneOffset.UTC

    @Test
    fun `университетский пресет даёт хотя бы 10 событий и все recurring`() {
        val events = generatePreset(PresetKind.UNIVERSITY, zone)
        // 5 будней по 3 пары = 15, должно быть >= 10
        assertTrue("ожидали >=10 событий, получили ${events.size}", events.size >= 10)
        // каждое событие повторяющееся - иначе смысл пресета теряется
        events.forEach { ev ->
            assertTrue("событие '${ev.title}' должно быть recurring", ev.recurrenceMask != 0)
        }
    }

    @Test
    fun `школьный пресет покрывает все будни`() {
        val events = generatePreset(PresetKind.SCHOOL, zone)
        // склеиваем все маски через OR и проверяем что каждый будний бит есть
        val combined = events.fold(0) { acc, ev -> acc or ev.recurrenceMask }
        assertTrue("нет понедельника", combined and WeekDays.MON != 0)
        assertTrue("нет вторника", combined and WeekDays.TUE != 0)
        assertTrue("нет среды", combined and WeekDays.WED != 0)
        assertTrue("нет четверга", combined and WeekDays.THU != 0)
        assertTrue("нет пятницы", combined and WeekDays.FRI != 0)
    }

    @Test
    fun `рабочий пресет содержит дейли каждый будний день`() {
        val events = generatePreset(PresetKind.WORK, zone)
        val daily = events.firstOrNull { it.title == "Дейли" }
        assertNotNull("в рабочем пресете нет события 'Дейли'", daily)
        val expected = WeekDays.MON or WeekDays.TUE or WeekDays.WED or WeekDays.THU or WeekDays.FRI
        assertEquals(expected, daily!!.recurrenceMask)
    }
}
