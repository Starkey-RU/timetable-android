package com.example.timetable

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EventRepository(private val dao: EventDao) {

    fun observeAll(): Flow<List<EventEntity>> = dao.observeAll()

    fun observeInRange(fromMillis: Long, toMillis: Long): Flow<List<EventEntity>> =
        dao.observeInRange(fromMillis, toMillis)

    suspend fun getById(id: Long): EventEntity? = dao.getById(id)

    suspend fun add(event: EventEntity): Long = dao.insert(event)

    suspend fun update(event: EventEntity) = dao.update(event)

    suspend fun delete(event: EventEntity) = dao.delete(event)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    // создаёт копию события со сдвигом на dayOffset дней, обнуляет повторение -
    // повторяющиеся клонировать как разовый экземпляр привычнее, чем тянуть всю серию.
    // возвращает id новой записи или null если исходного не нашли.
    suspend fun duplicate(id: Long, dayOffset: Long): Long? {
        val original = dao.getById(id) ?: return null
        val shiftMs = dayOffset * 24L * 60L * 60L * 1000L
        val copy = original.copy(
            id = 0,
            startMillis = original.startMillis + shiftMs,
            endMillis = original.endMillis + shiftMs,
            recurrenceMask = 0,
            weekParity = 0,
        )
        return dao.insert(copy)
    }

    // чистит разовые события, у которых конец был раньше чем daysOld дней назад.
    // повторяющиеся не трогаем - они продолжают создавать вхождения вперёд.
    // если daysOld <= 0 - выключено, ничего не делаем.
    suspend fun purgePastSingles(daysOld: Int, nowMillis: Long = System.currentTimeMillis()): Int {
        if (daysOld <= 0) return 0
        val cutoff = nowMillis - daysOld * 24L * 60L * 60L * 1000L
        return dao.deleteSinglePastBefore(cutoff)
    }

    // выгрузка всех событий в формат для шаринга через буфер обмена
    suspend fun exportAll(): ExportBundle {
        val all = dao.observeAll().first()
        return ExportBundle(events = all.map { it.toDto() })
    }

    // добавляет всё что пришло как новые события (не обновляет существующие)
    suspend fun importEvents(bundle: ExportBundle): Int {
        bundle.events.forEach { dao.insert(it.toEntity()) }
        return bundle.events.size
    }

    // забивает базу тестовыми событиями для демонстрации.
    // ~4 одиночных в день за +/-14 дней + пара повторяющихся шаблонов = 120+ записей.
    suspend fun seedTestData(zone: ZoneId = ZoneId.systemDefault()): Int {
        val sample = generateTestEvents(zone)
        dao.insertAll(sample)
        return sample.size
    }

    // готовый набор расписания под роль - чтоб быстро показать как смотрится.
    suspend fun seedPreset(kind: PresetKind, zone: ZoneId = ZoneId.systemDefault()): Int {
        val sample = generatePreset(kind, zone)
        dao.insertAll(sample)
        return sample.size
    }
}

// варианты типовых расписаний для демонстрации
enum class PresetKind { UNIVERSITY, SCHOOL, WORK }

internal fun generateTestEvents(zone: ZoneId): List<EventEntity> {
    val today = LocalDate.now(zone)
    val out = mutableListOf<EventEntity>()

    // пул событий разного типа - не только учёба. параллельные списки чтоб не плодить классы.
    val titles = listOf(
        "Встреча с командой", "Зал", "Стоматолог", "Лекция по матанализу",
        "Обед с другом", "Дедлайн отчёта", "Английский", "Покупки",
        "Прогулка с собакой", "Семинар по программированию", "Звонок маме", "Кино",
    )
    val places = listOf(
        "Офис", "World Class", "Клиника на Невском", "Ауд. 312",
        "Кафе", "Дома", "Zoom", "Магнит",
        "Парк", "Ауд. 218", "Дома", "Авроры",
    )
    val colors = listOf(
        "indigo", "emerald", "rose", "indigo",
        "amber", "coral", "violet", "slate",
        "emerald", "teal", "rose", "violet",
    )
    val icons = listOf(
        "chat", "fitness", "event", "book",
        "food", "event", "chat", "event",
        "fitness", "book", "chat", "event",
    )
    val hours  = listOf(11, 19, 14, 9, 13, 17, 18, 12, 8, 11, 21, 20)
    val durMin = listOf(60, 90, 30, 90, 60, 45, 60, 30, 45, 90, 20, 120)

    // 4 события в день в течение +/-14 дней - выбираем циклически из пула, выходит ~120
    val perDay = 4
    for (offset in -14..14) {
        val day = today.plusDays(offset.toLong())
        for (slot in 0 until perDay) {
            // циклический индекс с учётом отрицательного offset
            var idx = (offset * perDay + slot) % titles.size
            if (idx < 0) idx += titles.size
            val start = day.atTime(LocalTime.of(hours[idx], 0)).atZone(zone).toInstant().toEpochMilli()
            val end = start + durMin[idx] * 60_000L
            out.add(EventEntity(
                title = titles[idx],
                location = places[idx],
                colorKey = colors[idx],
                iconKey = icons[idx],
                startMillis = start,
                endMillis = end,
            ))
        }
    }

    // два повторяющихся шаблона чтоб recurring тоже было видно
    val anchor = today.minusDays(7)
    out.add(EventEntity(
        title = "Английский",
        location = "Zoom",
        colorKey = "rose",
        iconKey = "chat",
        startMillis = anchor.atTime(LocalTime.of(19, 0)).atZone(zone).toInstant().toEpochMilli(),
        endMillis = anchor.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant().toEpochMilli(),
        recurrenceMask = WeekDays.TUE or WeekDays.THU,
    ))
    out.add(EventEntity(
        title = "Тренажёрный зал",
        location = "World Class",
        colorKey = "emerald",
        iconKey = "fitness",
        startMillis = anchor.atTime(LocalTime.of(20, 30)).atZone(zone).toInstant().toEpochMilli(),
        endMillis = anchor.atTime(LocalTime.of(21, 45)).atZone(zone).toInstant().toEpochMilli(),
        recurrenceMask = WeekDays.MON or WeekDays.WED or WeekDays.FRI,
    ))

    return out
}
