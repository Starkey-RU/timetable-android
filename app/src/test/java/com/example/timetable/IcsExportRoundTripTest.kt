package com.example.timetable

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class IcsExportRoundTripTest {

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `несколько событий проходят roundtrip`() {
        // три разных по сложности события - простое, с локацией, повторяющееся
        val originals = listOf(
            EventEntity(
                id = 1,
                title = "Простое",
                location = "",
                colorKey = "indigo",
                startMillis = at(2026, 5, 24, 9, 0),
                endMillis = at(2026, 5, 24, 10, 0),
            ),
            EventEntity(
                id = 2,
                title = "С локацией",
                location = "Ауд. 218",
                colorKey = "emerald",
                startMillis = at(2026, 5, 25, 11, 0),
                endMillis = at(2026, 5, 25, 12, 30),
            ),
            EventEntity(
                id = 3,
                title = "Повторяющееся",
                location = "Спортзал",
                colorKey = "teal",
                startMillis = at(2026, 5, 18, 20, 0),
                endMillis = at(2026, 5, 18, 21, 0),
                recurrenceMask = WeekDays.MON or WeekDays.WED,
            ),
        )
        val ics = IcsExport.build(originals)
        val parsed = IcsImport.parse(ics, ZoneOffset.UTC)
        assertEquals(originals.size, parsed.size)
        // сопоставляем по порядку - IcsExport кладёт в том же порядке
        originals.zip(parsed).forEach { (orig, got) ->
            assertEquals(orig.title, got.title)
            assertEquals(orig.startMillis, got.startMillis)
            assertEquals(orig.endMillis, got.endMillis)
            assertEquals(orig.recurrenceMask, got.recurrenceMask)
        }
    }

    @Test
    fun `recurring с чёт-нечёт сохраняет weekParity`() {
        val original = EventEntity(
            id = 42,
            title = "Английский",
            location = "Zoom",
            colorKey = "rose",
            startMillis = at(2026, 5, 19, 19, 0),
            endMillis = at(2026, 5, 19, 20, 0),
            recurrenceMask = WeekDays.TUE or WeekDays.THU,
            weekParity = WeekParity.EVEN,
        )
        val ics = IcsExport.build(listOf(original))
        val got = IcsImport.parse(ics, ZoneOffset.UTC).single()
        assertEquals(original.recurrenceMask, got.recurrenceMask)
        assertEquals(original.weekParity, got.weekParity)
    }
}
