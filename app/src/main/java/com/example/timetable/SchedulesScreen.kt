package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ScheduleFilter(val title: String) {
    All("Все"),
    Repeating("Повтор"),
    OneTime("Разовые"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(onEventClick: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val vm: SchedulesViewModel = viewModel(factory = remember { SchedulesViewModel.factory(app.eventRepository) })
    val all by vm.events.collectAsState()
    val selected by vm.selectedIds.collectAsState()
    val isGuest by AppPrefs.isGuest

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ScheduleFilter.All) }

    val visible = remember(all, query, filter) { applyFilter(all, query, filter) }
    val selectionMode = selected.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text("Выбрано: ${selected.size}", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text("Расписания", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        // крестик слева сбрасывает выбор
                        IconButton(onClick = { vm.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Отмена")
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        // кнопка удаления справа в шапке
                        Button(
                            onClick = {
                                vm.deleteSelected()
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text("Удалить (${selected.size})")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text("Поиск по названию или месту") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                        }
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.title) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            Text(
                text = countLabel(visible.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank() && filter == ScheduleFilter.All) "Пока пусто"
                               else "Ничего не нашлось",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id }) { ev ->
                    val isSelected = ev.id in selected
                    ScheduleRow(
                        event = ev,
                        isSelected = isSelected,
                        onClick = {
                            if (selectionMode) {
                                // в режиме выбора обычный тап тоже переключает
                                vm.toggle(ev.id)
                            } else {
                                onEventClick(ev.id)
                            }
                        },
                        onLongClick = {
                            if (isGuest) {
                                // гостю редактирование запрещено
                                Toast.makeText(context, "Только просмотр", Toast.LENGTH_SHORT).show()
                            } else {
                                vm.toggle(ev.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleRow(
    event: EventEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val stripe = EventColors.stripe(event.colorKey)
    // выделенную карточку подсвечиваем рамкой + фоном
    val container = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val rowModifier = if (isSelected) {
        Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    Card(
        modifier = rowModifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(stripe),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val info = buildString {
                    append(formatDateTime(event.startMillis))
                    if (event.location.isNotBlank()) append(" · ").append(event.location)
                    if (event.recurrenceMask != 0) append(" · ").append(recurrenceLabel(event))
                }
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun applyFilter(
    all: List<EventEntity>,
    query: String,
    filter: ScheduleFilter,
): List<EventEntity> {
    val q = query.trim().lowercase(Locale.getDefault())
    return all.asSequence()
        .filter { ev ->
            when (filter) {
                ScheduleFilter.All -> true
                ScheduleFilter.Repeating -> ev.recurrenceMask != 0
                ScheduleFilter.OneTime -> ev.recurrenceMask == 0
            }
        }
        .filter { ev ->
            if (q.isEmpty()) true
            else ev.title.lowercase(Locale.getDefault()).contains(q) ||
                 ev.location.lowercase(Locale.getDefault()).contains(q)
        }
        .sortedBy { it.startMillis }
        .toList()
}

private fun recurrenceLabel(event: EventEntity): String {
    val days = WeekDays.all.zip(WeekDays.labels)
        .filter { (bit, _) -> event.recurrenceMask and bit != 0 }
        .joinToString(",") { it.second }
    val parity = when (event.weekParity) {
        WeekParity.EVEN -> " чёт"
        WeekParity.ODD -> " неч"
        else -> ""
    }
    return "$days$parity"
}

private fun formatDateTime(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))
    return Instant.ofEpochMilli(millis).atZone(zone).format(fmt)
}

private fun countLabel(n: Int): String = when {
    n == 0 -> "ничего не найдено"
    n == 1 -> "1 событие"
    n in 2..4 -> "$n события"
    else -> "$n событий"
}
