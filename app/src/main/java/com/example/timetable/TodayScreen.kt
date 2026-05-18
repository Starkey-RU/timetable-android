package com.example.timetable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(onEventClick: (Long) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as TimetableApplication
    val vm: TodayViewModel = viewModel(factory = remember { TodayViewModel.factory(app.eventRepository) })
    val state by vm.state.collectAsState()
    val isGuest by AppPrefs.isGuest

    // событие, по которому долго нажали - показываем шторку с действиями.
    // в гостевом режиме шторку не открываем, удалять всё равно нельзя
    var pickedForActions by remember { mutableStateOf<EventEntity?>(null) }
    val handleLongClick: (EventEntity) -> Unit = { ev -> if (!isGuest) pickedForActions = ev }

    // одна "сегодняшняя" дата на весь экран чтоб карточки не ушли в разные дни
    val today = remember(state.nowMillis) {
        if (state.nowMillis == 0L) LocalDate.now()
        else Instant.ofEpochMilli(state.nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // секция "позже сегодня" может быть длинной, по умолчанию сворачиваем
    var laterExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Сегодня", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = todayHeader(),
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
        if (state.now.isEmpty() && state.next == null && state.later.isEmpty() && state.done.isEmpty()) {
            EmptyToday(modifier = Modifier.padding(inner))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            // снизу запас чтоб FAB не накрывал последнюю карточку
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.now.forEach { ev ->
                item(key = "now-${ev.id}") {
                    EventCard(
                        event = ev,
                        today = today,
                        badge = "Сейчас",
                        emphasized = true,
                        nowMillis = state.nowMillis,
                        onClick = { onEventClick(ev.id) },
                        onLongClick = { handleLongClick(ev) },
                    )
                }
            }
            state.next?.let { ev ->
                item(key = "next-${ev.id}") {
                    EventCard(
                        event = ev,
                        today = today,
                        badge = "Дальше",
                        onClick = { onEventClick(ev.id) },
                        onLongClick = { handleLongClick(ev) },
                    )
                }
            }
            if (state.later.isNotEmpty()) {
                item("later-h") {
                    SectionHeader(
                        title = "Позже сегодня",
                        count = state.later.size,
                        expanded = laterExpanded,
                        onClick = { laterExpanded = !laterExpanded },
                    )
                }
                if (laterExpanded) {
                    items(state.later, key = { "later-${it.id}" }) { ev ->
                        EventCard(
                            event = ev,
                            today = today,
                            onClick = { onEventClick(ev.id) },
                            onLongClick = { handleLongClick(ev) },
                        )
                    }
                }
            }
            if (state.done.isNotEmpty()) {
                item("done-h") { SectionHeader("Завершено", state.done.size) }
                items(state.done, key = { "done-${it.id}" }) { ev ->
                    EventCard(
                        event = ev,
                        today = today,
                        completed = true,
                        onClick = { onEventClick(ev.id) },
                        onLongClick = { handleLongClick(ev) },
                    )
                }
            }
        }
    }

    pickedForActions?.let { ev ->
        EventActionsSheet(
            event = ev,
            onDismiss = { pickedForActions = null },
            onDelete = {
                vm.delete(ev.id)
                pickedForActions = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventActionsSheet(
    event: EventEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Удалить",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventCard(
    event: EventEntity,
    today: LocalDate,
    badge: String? = null,
    emphasized: Boolean = false,
    completed: Boolean = false,
    nowMillis: Long = 0L,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val stripe = EventColors.stripe(event.colorKey)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(if (emphasized) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(if (emphasized) 6.dp else 4.dp)
                    .fillMaxHeight()
                    .background(stripe),
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (!completed) {
                    EventIconBubble(iconKey = event.iconKey, tint = stripe)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (badge != null) {
                        Text(
                            text = badge.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = stripe,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = event.title,
                        style = if (emphasized) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (completed) TextDecoration.LineThrough else null,
                        color = if (completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val timeStyleColor = MaterialTheme.colorScheme.onSurfaceVariant.let {
                        if (completed) it.copy(alpha = 0.52f) else it
                    }
                    Text(
                        text = formatCrossDayTime(event.startMillis, event.endMillis, today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = timeStyleColor,
                    )
                    if (event.location.isNotBlank()) {
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = timeStyleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (emphasized && nowMillis in event.startMillis..event.endMillis) {
                        val total = (event.endMillis - event.startMillis).coerceAtLeast(1)
                        val progress = ((nowMillis - event.startMillis).toFloat() / total).coerceIn(0f, 1f)
                        val leftMin = ((event.endMillis - nowMillis) / 60_000L).coerceAtLeast(0)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            color = stripe,
                        )
                        Text(
                            text = "осталось $leftMin мин · до ${formatTime(event.endMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventIconBubble(iconKey: String, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(tint.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconFor(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun iconFor(key: String): ImageVector = when (key) {
    "book"    -> Icons.Filled.Book
    "food"    -> Icons.Filled.Restaurant
    "laptop"  -> Icons.Filled.Laptop
    "fitness" -> Icons.Filled.FitnessCenter
    "chat"    -> Icons.AutoMirrored.Filled.Chat
    else      -> Icons.Filled.Event
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(top = 16.dp, bottom = 4.dp)
    Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyToday(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Сегодня событий нет",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        .let { "%02d:%02d".format(it.hour, it.minute) }

// если событие началось не сегодня - добавляем "вчера"/"завтра" к краю
internal fun formatCrossDayTime(
    startMillis: Long,
    endMillis: Long,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val s = Instant.ofEpochMilli(startMillis).atZone(zone)
    val e = Instant.ofEpochMilli(endMillis).atZone(zone)
    val left = dayLabel(s.toLocalDate(), today, "%02d:%02d".format(s.hour, s.minute))
    val right = dayLabel(e.toLocalDate(), today, "%02d:%02d".format(e.hour, e.minute))
    return "$left - $right"
}

private fun dayLabel(date: LocalDate, today: LocalDate, time: String): String = when (date) {
    today -> time
    today.minusDays(1) -> "вчера $time"
    today.plusDays(1) -> "завтра $time"
    // длинные события на несколько дней - редкий случай, печатаем дату
    else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale("ru"))) + " $time"
}

private fun todayHeader(): String {
    val date = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))
    return date.format(fmt).replaceFirstChar { it.titlecase(Locale("ru")) }
}

