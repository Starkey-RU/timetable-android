package com.example.timetable

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// готовые наборы повторяющихся событий под три роли: студент, школьник, офис.
// генерим recurring (по будням/субботам) - тогда все будущие недели заполняются автоматом.
internal fun generatePreset(kind: PresetKind, zone: ZoneId): List<EventEntity> = when (kind) {
    PresetKind.UNIVERSITY -> universityPreset(zone)
    PresetKind.SCHOOL -> schoolPreset(zone)
    PresetKind.WORK -> workPreset(zone)
}

private fun universityPreset(zone: ZoneId): List<EventEntity> {
    // 5 пар в день по будням, чёт-нечёт чередуем для разнообразия
    val anchor = LocalDate.now(zone)
    val slots = listOf(
        Triple("Матанализ", "indigo", "book") to (LocalTime.of(9, 0) to LocalTime.of(10, 30)),
        Triple("Программирование", "emerald", "book") to (LocalTime.of(10, 40) to LocalTime.of(12, 10)),
        Triple("История", "amber", "book") to (LocalTime.of(12, 40) to LocalTime.of(14, 10)),
        Triple("Английский", "rose", "chat") to (LocalTime.of(14, 20) to LocalTime.of(15, 50)),
        Triple("Физкультура", "teal", "fitness") to (LocalTime.of(16, 0) to LocalTime.of(17, 30)),
    )
    val rooms = listOf("Ауд. 312", "Ауд. 218", "Ауд. 104", "Ауд. 401", "Спортзал")
    // распределяем пары по дням недели чтобы не было одинаково
    val schedule = listOf(
        WeekDays.MON to listOf(0, 1, 2),
        WeekDays.TUE to listOf(1, 3, 0),
        WeekDays.WED to listOf(0, 2, 4),
        WeekDays.THU to listOf(1, 2, 3),
        WeekDays.FRI to listOf(3, 4, 0),
    )
    val out = mutableListOf<EventEntity>()
    schedule.forEach { (dayBit, indices) ->
        indices.forEach { i ->
            val (meta, time) = slots[i]
            val start = anchor.atTime(time.first).atZone(zone).toInstant().toEpochMilli()
            val end = anchor.atTime(time.second).atZone(zone).toInstant().toEpochMilli()
            out.add(EventEntity(
                title = meta.first,
                location = rooms[i],
                colorKey = meta.second,
                iconKey = meta.third,
                startMillis = start,
                endMillis = end,
                recurrenceMask = dayBit,
            ))
        }
    }
    return out
}

private fun schoolPreset(zone: ZoneId): List<EventEntity> {
    val anchor = LocalDate.now(zone)
    // школьный день покороче: 6 уроков по 45 минут, перемена 10 минут
    val subjects = listOf(
        Triple("Русский", "rose", "book"),
        Triple("Математика", "indigo", "book"),
        Triple("Английский", "amber", "chat"),
        Triple("Биология", "emerald", "book"),
        Triple("История", "violet", "book"),
        Triple("Физкультура", "teal", "fitness"),
    )
    val schedule = listOf(
        WeekDays.MON to listOf(0, 1, 2, 3, 4),
        WeekDays.TUE to listOf(1, 0, 5, 3, 4),
        WeekDays.WED to listOf(2, 1, 0, 4, 3),
        WeekDays.THU to listOf(0, 3, 1, 2, 5),
        WeekDays.FRI to listOf(4, 0, 1, 2, 3),
    )
    val out = mutableListOf<EventEntity>()
    schedule.forEach { (dayBit, indices) ->
        var startTime = LocalTime.of(8, 30)
        indices.forEach { i ->
            val subj = subjects[i]
            val endTime = startTime.plusMinutes(45)
            val start = anchor.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
            val end = anchor.atTime(endTime).atZone(zone).toInstant().toEpochMilli()
            out.add(EventEntity(
                title = subj.first,
                location = "Школа",
                colorKey = subj.second,
                iconKey = subj.third,
                startMillis = start,
                endMillis = end,
                recurrenceMask = dayBit,
            ))
            startTime = endTime.plusMinutes(10)
        }
    }
    return out
}

private fun workPreset(zone: ZoneId): List<EventEntity> {
    val anchor = LocalDate.now(zone)
    val out = mutableListOf<EventEntity>()
    // дейли каждый рабочий день в 10:00
    val daily = WeekDays.MON or WeekDays.TUE or WeekDays.WED or WeekDays.THU or WeekDays.FRI
    out.add(EventEntity(
        title = "Дейли",
        location = "Zoom",
        colorKey = "indigo",
        iconKey = "chat",
        startMillis = anchor.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        endMillis = anchor.atTime(10, 15).atZone(zone).toInstant().toEpochMilli(),
        recurrenceMask = daily,
    ))
    // обед каждый рабочий день
    out.add(EventEntity(
        title = "Обед",
        location = "Кафе",
        colorKey = "amber",
        iconKey = "food",
        startMillis = anchor.atTime(13, 0).atZone(zone).toInstant().toEpochMilli(),
        endMillis = anchor.atTime(14, 0).atZone(zone).toInstant().toEpochMilli(),
        recurrenceMask = daily,
    ))
    // встречи в разные дни
    val meetings = listOf(
        Triple("Планирование", WeekDays.MON, LocalTime.of(11, 0)),
        Triple("Демо-ревью", WeekDays.WED, LocalTime.of(15, 0)),
        Triple("Ретро", WeekDays.FRI, LocalTime.of(16, 0)),
        Triple("Звонок с заказчиком", WeekDays.TUE, LocalTime.of(17, 0)),
    )
    meetings.forEach { (title, dayBit, time) ->
        val start = anchor.atTime(time).atZone(zone).toInstant().toEpochMilli()
        val end = anchor.atTime(time.plusHours(1)).atZone(zone).toInstant().toEpochMilli()
        out.add(EventEntity(
            title = title,
            location = "Офис",
            colorKey = "emerald",
            iconKey = "event",
            startMillis = start,
            endMillis = end,
            recurrenceMask = dayBit,
        ))
    }
    // спортзал по понедельникам и средам вечером
    out.add(EventEntity(
        title = "Тренажёрный зал",
        location = "World Class",
        colorKey = "teal",
        iconKey = "fitness",
        startMillis = anchor.atTime(19, 30).atZone(zone).toInstant().toEpochMilli(),
        endMillis = anchor.atTime(20, 30).atZone(zone).toInstant().toEpochMilli(),
        recurrenceMask = WeekDays.MON or WeekDays.WED,
    ))
    return out
}
