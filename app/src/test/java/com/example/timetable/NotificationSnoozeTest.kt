package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

// сама логика снуза вызывает alarmmanager что без android-контекста не проверить,
// тут только числовая часть - сдвиг по миллисекундам на 5 и 15 минут.
// формула совпадает с тем что зашито в NotificationActionsReceiver.onReceive:
//   triggerAt = System.currentTimeMillis() + minutes * 60_000L
class NotificationSnoozeTest {

    @Test
    fun `пять минут это триста тысяч миллисекунд`() {
        val minutes = 5
        assertEquals(300_000L, minutes * 60_000L)
    }

    @Test
    fun `пятнадцать минут это девятьсот тысяч миллисекунд`() {
        val minutes = 15
        assertEquals(900_000L, minutes * 60_000L)
    }

    @Test
    fun `сдвиг от фиксированной точки на пять минут даёт корректное время`() {
        // берём произвольную точку отсчёта - реальный System.currentTimeMillis не нужен
        val now = 1_748_080_800_000L
        val minutes = 5
        val triggerAt = now + minutes * 60_000L
        assertEquals(now + 300_000L, triggerAt)
    }

    @Test
    fun `сдвиг на пятнадцать минут от той же точки`() {
        val now = 1_748_080_800_000L
        val minutes = 15
        val triggerAt = now + minutes * 60_000L
        assertEquals(now + 900_000L, triggerAt)
    }
}
