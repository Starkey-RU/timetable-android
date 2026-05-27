package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(eventId: Long?, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as TimetableApplication
    // key обязателен - иначе в двухпанельном режиме при клике на разные события
    // viewModel() вернёт тот же экземпляр (один ViewModelStoreOwner), и редактор покажет старые данные.
    // null превращаем в строку чтоб "новое" и id=0 не конфликтовали
    val vm: EventEditorViewModel = viewModel(
        key = "editor-${eventId ?: "new"}",
        factory = remember(eventId) { EventEditorViewModel.factory(app.eventRepository, eventId) },
    )
    val form by vm.form.collectAsState()
    val notFound by vm.notFound.collectAsState()
    val isGuest by AppPrefs.isGuest
    // крч если режим учёбы выключен - не показываем поля препод/пара/аудитория
    val studyMode = AppPrefs.studyMode.value
    val crossDayHintDismissed = AppPrefs.crossDayHintDismissed.value

    // если событие удалили пока редактор грузился - просто выходим
    LaunchedEffect(notFound) {
        if (notFound) onClose()
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<EventEntity?>(null) }
    var pendingSave by remember { mutableStateOf(false) }
    val view = LocalView.current

    // ищем пересечение с другими событиями в этот же диапазон.
    // если нашли - покажем диалог, иначе сразу сохраним.
    LaunchedEffect(pendingSave) {
        if (pendingSave) {
            conflict = vm.findConflict(app.eventRepository)
            if (conflict == null) {
                vm.save(onClose)
            }
            pendingSave = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Редактировать" else "Новое событие") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { inner ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // быстрые шаблоны - тапнул чип, поля сами заполнились
            TemplateChipsRow(onPick = vm::applyTemplate)

            OutlinedTextField(
                value = form.title,
                onValueChange = vm::setTitle,
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.location,
                onValueChange = vm::setLocation,
                label = { Text("Место") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (studyMode) {
                OutlinedTextField(
                    value = form.teacher,
                    onValueChange = vm::setTeacher,
                    label = { Text("Преподаватель") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = form.classNumber,
                        onValueChange = vm::setClassNumber,
                        label = { Text("Пара №") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.room,
                        onValueChange = vm::setRoom,
                        label = { Text("Аудитория") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text("Цвет", style = MaterialTheme.typography.labelLarge)
            ColorChipsRow(selected = form.colorKey, onPick = vm::setColor)

            Text("Иконка", style = MaterialTheme.typography.labelLarge)
            IconChipsRow(selected = form.iconKey, onPick = vm::setIcon)

            DateField(
                value = form.date,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeField(
                    label = "Начало",
                    value = form.start,
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "Конец",
                    value = form.end,
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }

            // если конец раньше начала - событие переезжает через полночь
            // показываем подсказку один раз, пока пользователь не закроет её
            if (form.end <= form.start && !crossDayHintDismissed) {
                CrossDayHint(onDismiss = { AppPrefs.crossDayHintDismissed.value = true })
            }

            RecurrenceField(
                mask = form.recurrenceMask,
                parity = form.weekParity,
                onToggleDay = vm::toggleDay,
                onSetParity = vm::setParity,
                onClear = vm::clearRecurrence,
            )

            Button(
                onClick = {
                    if (AppPrefs.hapticsEnabled.value) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                    pendingSave = true
                },
                enabled = form.title.isNotBlank() && !isGuest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            if (vm.isEditing) {
                OutlinedButton(
                    onClick = {
                        if (AppPrefs.hapticsEnabled.value) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                        vm.delete(onClose)
                    },
                    enabled = !isGuest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Удалить")
                }
            }

            if (isGuest) {
                Text(
                    text = "Гостевой режим - только просмотр. Чтобы менять, выключи в настройках.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showStartPicker) {
        PickTime(
            initial = form.start,
            onDismiss = { showStartPicker = false },
            onPick = {
                vm.setStart(it)
                showStartPicker = false
            },
        )
    }
    if (showEndPicker) {
        PickTime(
            initial = form.end,
            onDismiss = { showEndPicker = false },
            onPick = {
                vm.setEnd(it)
                showEndPicker = false
            },
        )
    }
    if (showDatePicker) {
        PickDate(
            initial = form.date,
            onDismiss = { showDatePicker = false },
            onPick = {
                vm.setDate(it)
                showDatePicker = false
            },
        )
    }

    conflict?.let { other ->
        AlertDialog(
            onDismissRequest = { conflict = null },
            title = { Text(ErrorMessages.EVENT_CONFLICT_TITLE) },
            text = {
                Text(ErrorMessages.EVENT_CONFLICT_TEXT.format(other.title))
            },
            confirmButton = {
                TextButton(onClick = {
                    conflict = null
                    vm.save(onClose)
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { conflict = null }) { Text("Отмена") }
            },
        )
    }
}

// ряд чипов-пресетов поверх формы - тык по чипу и редактор заполнен
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateChipsRow(onPick: (EventTemplate) -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Быстрый шаблон",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            DEFAULT_TEMPLATES.forEach { tpl ->
                val stripe = EventColors.stripe(tpl.colorKey)
                FilterChip(
                    selected = selectedId == tpl.id,
                    onClick = {
                        selectedId = tpl.id
                        onPick(tpl)
                    },
                    label = { Text(tpl.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = stripe.copy(alpha = 0.2f),
                        selectedLabelColor = stripe,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

// баннер про переход через полночь, прячется навсегда после тапа
@Composable
private fun CrossDayHint(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Событие переходит через полночь. Считается что оно идёт с указанного начала до конца на следующий день.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Понятно, больше не показывать")
                }
            }
        }
    }
}

@Composable
private fun DateField(
    value: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Дата",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))

@Composable
private fun TimeField(
    label: String,
    value: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "%02d:%02d".format(value.hour, value.minute),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ColorChipsRow(selected: String, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EventColors.keys.forEach { key ->
            ColorChip(
                color = EventColors.stripe(key),
                isSelected = key == selected,
                onClick = { onPick(key) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColorChip(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "выбрано",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun IconChipsRow(selected: String, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EventIcons.keys.forEach { key ->
            val isSelected = key == selected
            val bg = if (isSelected) MaterialTheme.colorScheme.primary
                     else MaterialTheme.colorScheme.surfaceContainerLow
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary
                     else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onPick(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = EventIcons.vector(key),
                    contentDescription = EventIcons.labels[key],
                    tint = fg,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// одно поле "Повтор: ..." с диалогом - чтоб не растягивать форму подменюшками
@Composable
private fun RecurrenceField(
    mask: Int,
    parity: Int,
    onToggleDay: (Int) -> Unit,
    onSetParity: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Повтор",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = recurrenceSummary(mask, parity),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Повтор") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "По каким дням повторять",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WeekdayChips(mask = mask, onToggle = onToggleDay)
                    if (mask != 0) {
                        Text(
                            text = "На каких неделях",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ParityRadioRow(parity = parity, onPick = onSetParity)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Готово") }
            },
            dismissButton = {
                if (mask != 0) {
                    TextButton(onClick = {
                        onClear()
                        open = false
                    }) { Text("Без повтора") }
                }
            },
        )
    }
}

private val weekdayShortLabels = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

// текстовое описание текущего повтора для свёрнутого поля
private fun recurrenceSummary(mask: Int, parity: Int): String {
    if (mask == 0) return "без повтора"
    val days = WeekDays.all.mapIndexedNotNull { idx, bit ->
        if (mask and bit != 0) weekdayShortLabels[idx] else null
    }.joinToString(", ")
    val suffix = when (parity) {
        WeekParity.EVEN -> " (через неделю, чёт)"
        WeekParity.ODD -> " (через неделю, нечёт)"
        else -> ""
    }
    return days + suffix
}

@Composable
private fun WeekdayChips(mask: Int, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WeekDays.all.forEachIndexed { idx, bit ->
            val selected = (mask and bit) != 0
            WeekdayChip(
                text = WeekDays.labels[idx],
                selected = selected,
                onClick = { onToggle(bit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// свой компактный чип - material FilterChip с минимальным touch target 48dp
// не лезет на узких экранах в одну строку, поэтому делаем плоский Box
@Composable
private fun WeekdayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(17.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun ParityRadioRow(parity: Int, onPick: (Int) -> Unit) {
    val items = listOf(
        WeekParity.ALL to "Каждую неделю",
        WeekParity.EVEN to "Через неделю (чёт)",
        WeekParity.ODD to "Через неделю (нечёт)",
    )
    Column {
        items.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(value) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = parity == value, onClick = { onPick(value) })
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickTime(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onPick: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text("Ок")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        text = { TimePicker(state = state) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickDate(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialMillis = initial.atStartOfDay(zone).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                    onPick(date)
                },
            ) { Text("Ок") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        text = { DatePicker(state = state) },
    )
}

