package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.timetable.ui.theme.TimetableTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen() {
    val app = LocalContext.current.applicationContext as TimetableApplication
    val vm: WeekViewModel = viewModel(factory = remember { WeekViewModel.factory(app.eventRepository) })
    val days by vm.days.collectAsState()
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Неделя", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = weekHeader(days),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(days, key = { it.date.toEpochDay() }) { bucket ->
                WeekDayRow(bucket = bucket, isToday = bucket.date == today)
            }
        }
    }
}

@Composable
private fun WeekDayRow(bucket: DayBucket, isToday: Boolean) {
    val container = if (isToday) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DayBadge(date = bucket.date, highlight = isToday)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weekdayName(bucket.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = bucket.date.format(longDateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.7f),
                )
            }
            Text(
                text = eventsCountLabel(bucket.count),
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer.copy(alpha = if (bucket.count == 0) 0.5f else 1f),
            )
        }
    }
}

@Composable
private fun DayBadge(date: LocalDate, highlight: Boolean) {
    val bg = if (highlight) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val longDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

private fun weekdayName(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE", Locale("ru")))
        .replaceFirstChar { it.titlecase(Locale("ru")) }

// "1 событие" / "3 события" / "5 событий" - простой склонятор
private fun eventsCountLabel(count: Int): String {
    if (count == 0) return "пусто"
    val mod100 = count % 100
    val mod10 = count % 10
    val word = when {
        mod100 in 11..14 -> "событий"
        mod10 == 1 -> "событие"
        mod10 in 2..4 -> "события"
        else -> "событий"
    }
    return "$count $word"
}

private fun weekHeader(days: List<DayBucket>): String {
    if (days.isEmpty()) return ""
    val first = days.first().date
    val last = days.last().date
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    return "${first.format(fmt)} - ${last.format(fmt)}"
}

@Preview(name = "День (сегодня)", showBackground = true)
@Composable
private fun WeekDayRowTodayPreview() {
    TimetableTheme {
        WeekDayRow(
            bucket = DayBucket(LocalDate.of(2026, 5, 24), 5),
            isToday = true,
        )
    }
}

@Preview(name = "День (обычный)", showBackground = true)
@Composable
private fun WeekDayRowPreview() {
    TimetableTheme {
        WeekDayRow(
            bucket = DayBucket(LocalDate.of(2026, 5, 26), 0),
            isToday = false,
        )
    }
}
