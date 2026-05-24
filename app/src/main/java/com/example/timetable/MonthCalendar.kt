package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// упрощённый месячный календарь: сетка 6 строк по 7 ячеек, под цифрой - до 3 цветных точек по событиям дня.
// клик по дню отдаёт LocalDate наружу.
@Composable
fun MonthCalendar(
    events: List<EventEntity>,
    today: LocalDate,
    initialMonth: YearMonth = YearMonth.from(today),
    onPickDay: (LocalDate) -> Unit,
) {
    var month by remember { mutableStateOf(initialMonth) }
    val zone = ZoneId.systemDefault()
    val semesterStart = AppPrefs.effectiveSemesterStart(zone)
    val titleFmt = remember { DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")) }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "пред месяц")
            }
            Text(
                text = month.format(titleFmt).replaceFirstChar { it.titlecase(Locale("ru")) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "след месяц")
            }
        }

        // строка с короткими названиями дней недели
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // сетка 6 строк
        val firstOfMonth = month.atDay(1)
        // понедельник предыдущей или текущей недели для левого верхнего угла сетки
        val gridStart = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())

        // разворачиваем все события на 6 недель сразу - меньше работы внутри ячеек.
        val rangeFrom = gridStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeTo = gridStart.plusDays(42).atStartOfDay(zone).toInstant().toEpochMilli()
        val expanded = events.flatMap { expandRecurrence(it, rangeFrom, rangeTo, zone, semesterStart) }
        // группировка по дате локали - чтоб быстро брать события на день
        val byDate: Map<LocalDate, List<EventEntity>> = expanded.groupBy {
            Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalDate()
        }

        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val date = gridStart.plusDays((row * 7 + col).toLong())
                    val inMonth = date.month == month.month
                    val isToday = date == today
                    val dayEvents = byDate[date] ?: emptyList()
                    DayCell(
                        date = date,
                        inMonth = inMonth,
                        isToday = isToday,
                        events = dayEvents,
                        onClick = { onPickDay(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    events: List<EventEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberColor = when {
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isToday -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val bg = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = numberColor,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            )
            // до 3 точек уникальных цветов
            if (events.isNotEmpty()) {
                val dots = events.map { it.colorKey }.distinct().take(3)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    dots.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(EventColors.stripe(key)),
                        )
                    }
                }
            }
        }
    }
}
