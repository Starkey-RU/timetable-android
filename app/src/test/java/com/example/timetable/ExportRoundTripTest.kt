package com.example.timetable

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportRoundTripTest {

    @Test
    fun `сериализация и обратная разборка не теряют поля`() {
        val original = EventEntity(
            id = 42,
            title = "Лекция",
            location = "Ауд. 312",
            colorKey = "rose",
            iconKey = "book",
            startMillis = 1_700_000_000_000L,
            endMillis = 1_700_003_600_000L,
            recurrenceMask = WeekDays.MON or WeekDays.WED,
            weekParity = WeekParity.EVEN,
        )
        val bundle = ExportBundle(events = listOf(original.toDto()))
        val text = Json.encodeToString(bundle)
        val parsed = Json.decodeFromString<ExportBundle>(text)
        val restored = parsed.events.first().toEntity()

        assertEquals(original.title, restored.title)
        assertEquals(original.location, restored.location)
        assertEquals(original.colorKey, restored.colorKey)
        assertEquals(original.iconKey, restored.iconKey)
        assertEquals(original.startMillis, restored.startMillis)
        assertEquals(original.endMillis, restored.endMillis)
        assertEquals(original.recurrenceMask, restored.recurrenceMask)
        assertEquals(original.weekParity, restored.weekParity)
    }
}
