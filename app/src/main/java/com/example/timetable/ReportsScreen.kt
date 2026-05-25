package com.example.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

// держим статистику за последние 7 суток в одной структурке чтоб удобно тестить
private data class WeekStats(
    val totalMinutes: Long,
    val countsByDay: Map<DayOfWeek, Int>,
    val topByDuration: List<Pair<String, Long>>, // title -> сумма минут
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as TimetableApplication
    val repo = remember { app.eventRepository }
    val events = repo.observeAll().collectAsState(initial = emptyList()).value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёты") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { inner ->
        // считаем статистики прямо тут, событий не так много чтоб выносить в vm
        val total = events.size
        val recurring = events.count { it.recurrenceMask != 0 }
        val byDay = events.groupingBy { dayOfWeek(it.startMillis) }.eachCount()
        val topLocs = events
            .filter { it.location.isNotBlank() }
            .groupingBy { it.location }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
        val daysSorted = byDay.entries.sortedBy { it.key.value }

        // считаем статистики за последние 7 суток, разворачивая повторы в конкретные вхождения
        val nowMs = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val weekStats = remember(events, nowMs) { computeWeekStats(events, nowMs, zone) }
        val weekHours = weekStats.totalMinutes / 60
        val weekRemMin = weekStats.totalMinutes % 60

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Статистика расписания",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { StatCard("Всего событий", total.toString()) }
            item { StatCard("С повторами", recurring.toString()) }

            item { SectionLabel("По дням недели") }
            items(daysSorted, key = { it.key.name }) { entry ->
                StatRow(label = dayLabel(entry.key), value = entry.value.toString())
            }

            if (topLocs.isNotEmpty()) {
                item { SectionLabel("Топ-3 места") }
                items(topLocs, key = { it.key }) { entry ->
                    StatRow(label = entry.key, value = entry.value.toString())
                }
            }

            // блок недельных метрик: общее время, разбивка по дням, топ по длительности
            item { SectionLabel("За последние 7 дней") }
            item { StatCard("Всего за неделю", "$weekHours ч $weekRemMin мин") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "По дням недели",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // выводим все 7 дней подряд, даже если в каком-то дне ноль событий
                        for (d in DayOfWeek.values()) {
                            val cnt = weekStats.countsByDay[d] ?: 0
                            StatRow(label = dayShort(d), value = cnt.toString())
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Топ по времени",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (weekStats.topByDuration.isEmpty()) {
                            // пустой случай чтоб не показывать молчаливо пустую карточку
                            Text(
                                text = "пока нет данных",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            for ((title, mins) in weekStats.topByDuration) {
                                val h = mins / 60
                                val m = mins % 60
                                StatRow(label = title, value = "$h ч $m мин")
                            }
                        }
                    }
                }
            }
        }
    }
}

// чистая функция чтоб потом можно было прогнать в тестах без compose-обвязки.
// берёт окно [now - 7 суток, now), разворачивает повторы и агрегирует:
// - сколько минут всего попало в окно
// - сколько вхождений в каждый день недели
// - топ-3 названий по суммарной длительности (минуты)
private fun computeWeekStats(
    events: List<EventEntity>,
    nowMillis: Long,
    zone: ZoneId,
): WeekStats {
    val weekMs = 7L * 24 * 60 * 60 * 1000
    val from = nowMillis - weekMs
    val to = nowMillis

    var totalMin = 0L
    val perDay = mutableMapOf<DayOfWeek, Int>()
    val perTitle = mutableMapOf<String, Long>()

    for (ev in events) {
        // expandRecurrence сам обработает и одиночные (mask == 0), и повторяющиеся
        val occs = expandRecurrence(ev, from, to, zone)
        for (occ in occs) {
            // обрезаем по границам окна на случай если событие пересекает край
            val s = maxOf(occ.startMillis, from)
            val e = minOf(occ.endMillis, to)
            if (e <= s) continue
            val mins = (e - s) / 60000L
            totalMin += mins
            val dow = Instant.ofEpochMilli(occ.startMillis).atZone(zone).dayOfWeek
            perDay[dow] = (perDay[dow] ?: 0) + 1
            val key = occ.title.ifBlank { "(без названия)" }
            perTitle[key] = (perTitle[key] ?: 0L) + mins
        }
    }

    val top = perTitle.entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key to it.value }

    return WeekStats(totalMinutes = totalMin, countsByDay = perDay, topByDuration = top)
}

// короткие подписи для таблицы дней - чтоб строки не разъезжались
private fun dayShort(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun dayOfWeek(millis: Long): DayOfWeek =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).dayOfWeek

private fun dayLabel(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "Понедельник"
    DayOfWeek.TUESDAY -> "Вторник"
    DayOfWeek.WEDNESDAY -> "Среда"
    DayOfWeek.THURSDAY -> "Четверг"
    DayOfWeek.FRIDAY -> "Пятница"
    DayOfWeek.SATURDAY -> "Суббота"
    DayOfWeek.SUNDAY -> "Воскресенье"
}
