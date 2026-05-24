package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.timetable.ui.theme.TimetableTheme
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun SettingsScreen(
    onOpenReports: () -> Unit = {},
    onOpenPinSetup: () -> Unit = {},
    onOpenFoldableSettings: () -> Unit = {},
) {
    val palette by AppPrefs.palette
    val gradient by AppPrefs.gradient
    val theme by AppPrefs.theme
    val guestMode by AppPrefs.isGuest
    val notificationsOn by AppPrefs.notificationsEnabled
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { (context.applicationContext as TimetableApplication).eventRepository }

    // запрос разрешения POST_NOTIFICATIONS, нужен только на 13+
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            AppPrefs.notificationsEnabled.value = true
        } else {
            Toast.makeText(context, "Без разрешения уведомления не придут", Toast.LENGTH_LONG).show()
        }
    }

    // тик чтоб перечитать isPinSet после clear, при возврате с setup compose сам перерисует
    var pinChangedTick by remember { mutableStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") pinChangedTick
    val pinEnabled = PinManager.isPinSet()

    // диалоги для шаринга/импорта через буфер
    var showShare by remember { mutableStateOf(false) }
    var shareText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HeroBanner(gradient) }

        item { SectionTitle("Тема") }
        item { ThemeSelector(theme) { AppPrefs.theme.value = it } }

        item { SectionTitle("Палитра") }
        item {
            PaletteButton(current = palette, onPick = { AppPrefs.palette.value = it })
        }

        item {
            GradientButton(current = gradient, onPick = { AppPrefs.gradient.value = it })
        }
        item {
            val showGrad by AppPrefs.showGradientHeader
            SwitchRow(
                title = "Цветной заголовок",
                subtitle = "градиент сверху на экране сегодня",
                checked = showGrad,
                onCheckedChange = { AppPrefs.showGradientHeader.value = it },
            )
        }

        item { SectionTitle("Внешний вид") }
        item {
            val showLabels by AppPrefs.showNavLabels
            SwitchRow(
                title = "Подписи у пунктов меню",
                subtitle = "если выключить - в навигации останутся только иконки",
                checked = showLabels,
                onCheckedChange = { AppPrefs.showNavLabels.value = it },
            )
        }
        item {
            val collapseDone by AppPrefs.collapseDoneByDefault
            SwitchRow(
                title = "Сворачивать прошедшие",
                subtitle = "блок завершённых сегодня по умолчанию свёрнут",
                checked = collapseDone,
                onCheckedChange = { AppPrefs.collapseDoneByDefault.value = it },
            )
        }
        item {
            val haptics by AppPrefs.hapticsEnabled
            SwitchRow(
                title = "Вибрация",
                subtitle = "короткий отклик при сохранении и удалении",
                checked = haptics,
                onCheckedChange = { AppPrefs.hapticsEnabled.value = it },
            )
        }
        item {
            OutlinedButton(
                onClick = onOpenFoldableSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Для широкого экрана")
            }
        }
        item {
            val days by AppPrefs.autoDeleteDays
            AutoDeleteRow(currentDays = days, onPick = { AppPrefs.autoDeleteDays.value = it })
        }

        item { SectionTitle("Доступ") }
        item {
            SwitchRow(
                title = "Гостевой режим",
                subtitle = "только просмотр, без правок",
                checked = guestMode,
                onCheckedChange = { AppPrefs.isGuest.value = it },
            )
        }
        item {
            SwitchRow(
                title = "Уведомления",
                subtitle = "напомнить за 10 минут до начала",
                checked = notificationsOn,
                onCheckedChange = { enable ->
                    if (!enable) {
                        AppPrefs.notificationsEnabled.value = false
                        return@SwitchRow
                    }
                    // на 13+ просим рантайм-разрешение, иначе уведомления молча не покажутся
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) AppPrefs.notificationsEnabled.value = true
                        else notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        AppPrefs.notificationsEnabled.value = true
                    }
                },
            )
        }
        item {
            SwitchRow(
                title = "PIN-код",
                subtitle = "запрос пин-кода при запуске",
                checked = pinEnabled,
                onCheckedChange = { enable ->
                    if (enable) {
                        onOpenPinSetup()
                    } else {
                        PinManager.clear()
                        pinChangedTick++
                    }
                },
            )
        }
        item { HorizontalDivider() }
        item { SectionTitle("Учёба") }
        item {
            val useSem by AppPrefs.useSemesterWeeks
            SwitchRow(
                title = "Считать недели от семестра",
                subtitle = "иначе чёт/нечёт берётся по календарным неделям года",
                checked = useSem,
                onCheckedChange = { AppPrefs.useSemesterWeeks.value = it },
            )
        }
        item {
            val start by AppPrefs.semesterStart
            SemesterStartRow(currentMillis = start, onPick = { AppPrefs.semesterStart.value = it })
        }
        item {
            val durations by AppPrefs.durationsByIcon
            DurationsRow(current = durations, onPick = { AppPrefs.durationsByIcon.value = it })
        }
        item { HorizontalDivider() }
        item { SectionTitle("Информация") }
        item {
            OutlinedButton(
                onClick = onOpenReports,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Отчёты (ВКР)")
            }
        }

        item { HorizontalDivider() }
        item { SectionTitle("Поделиться") }
        item {
            Button(
                onClick = {
                    scope.launch {
                        val bundle = repo.exportAll()
                        val json = Json.encodeToString(bundle)
                        // упаковываем чтоб не вываливать на собеседника километр текста
                        shareText = TextCompress.pack(json)
                        showShare = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скопировать (сжатый текст)")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val bundle = repo.exportAll()
                        shareText = Json.encodeToString(bundle)
                        showShare = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скопировать как обычный JSON")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val events = repo.exportAll().events.map { it.toEntity() }
                        shareText = IcsExport.build(events)
                        showShare = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скопировать как .ics (для календарей)")
            }
        }
        item {
            OutlinedButton(
                onClick = { showImport = true },
                enabled = !guestMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Импортировать из текста")
            }
        }

        item { HorizontalDivider() }
        item {
            Button(
                onClick = {
                    scope.launch {
                        val n = repo.seedTestData()
                        Toast.makeText(context, "Добавлено $n событий", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !guestMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Заполнить тестовыми данными")
            }
        }
    }

    if (showShare) {
        ShareDialog(
            text = shareText,
            onCopy = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("расписание", shareText))
                Toast.makeText(context, "Скопировано в буфер", Toast.LENGTH_SHORT).show()
                showShare = false
            },
            onDismiss = { showShare = false },
        )
    }

    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImport = { input ->
                scope.launch {
                    try {
                        // если строка начинается с { - думаем что это сырой JSON.
                        // иначе пробуем разжать (наш сжатый формат)
                        val raw = if (input.trimStart().startsWith("{")) input
                                  else TextCompress.unpack(input)
                        val bundle = Json.decodeFromString<ExportBundle>(raw)
                        val n = repo.importEvents(bundle)
                        Toast.makeText(context, "Добавлено $n событий", Toast.LENGTH_SHORT).show()
                        showImport = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "Не получилось разобрать текст", Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun ShareDialog(text: String, onCopy: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Расписание текстом") },
        text = {
            Column {
                Text(
                    text = "Скопируй или жми кнопку - получится длинный JSON, его можно отправить хоть в чат, хоть в почту.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(top = 8.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) { Text("Скопировать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт из текста") },
        text = {
            Column {
                Text(
                    text = "Вставь сюда расписание, которое тебе прислали.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(top = 8.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = { onImport(input) },
            ) { Text("Импортировать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun HeroBanner(gradient: GradientPreset) {
    // компактная плашка-разделитель сверху, просто чтоб было видно текущий градиент
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient.brush)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "Расписание",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ThemeSelector(current: ThemeMode, onPick: (ThemeMode) -> Unit) {
    // три варианта в одну горизонтальную строку, чтоб не растягивать настройки на пол-экрана
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPick(mode) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == mode, onClick = { onPick(mode) })
                Text(text = mode.title, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// кнопка-карточка с текущей палитрой, по клику открывает диалог-выбор
@Composable
private fun PaletteButton(current: Palette, onPick: (Palette) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(current.primary),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(text = "Палитра", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = current.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Сменить",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    if (open) {
        PalettePickerDialog(
            current = current,
            onPick = {
                onPick(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun PalettePickerDialog(
    current: Palette,
    onPick: (Palette) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Палитра") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Palette.entries.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(p) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(p.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (p == current) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                        Text(
                            text = p.title,
                            modifier = Modifier.padding(start = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

// 0 = выкл, остальное - сколько дней после конца события его можно стереть
private val autoDeletePresets = listOf(0, 1, 7, 30, 90)

private fun autoDeleteLabel(days: Int): String = when (days) {
    0 -> "никогда"
    1 -> "через 1 день"
    else -> "через $days дней"
}

@Composable
private fun AutoDeleteRow(currentDays: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Удалять прошедшие", style = MaterialTheme.typography.bodyLarge)
            // фиксированно две строки чтобы при смене пресета строка не прыгала по высоте
            Text(
                text = "разовые события, которые уже прошли\nповторяющиеся не трогаем",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
            )
        }
        Text(
            text = autoDeleteLabel(currentDays),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Удалять прошедшие") },
            text = {
                Column {
                    autoDeletePresets.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(d)
                                    open = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = d == currentDays, onClick = {
                                onPick(d)
                                open = false
                            })
                            Text(text = autoDeleteLabel(d), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Закрыть") }
            },
        )
    }
}

// одна строка-кнопка с превью текущего градиента, остальные варианты прячутся в диалог
@Composable
private fun GradientButton(current: GradientPreset, onPick: (GradientPreset) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(current.brush),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(text = "Градиент", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = current.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Сменить",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Градиент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientPreset.entries.forEach { g ->
                        val outlineColor = if (g == current) MaterialTheme.colorScheme.onSurface
                                           else Color.Transparent
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(g.brush)
                                .border(2.dp, outlineColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    onPick(g)
                                    open = false
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (g == current) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                                Text(
                                    text = g.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Закрыть") }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(name = "Строка-переключатель", showBackground = true)
@Composable
private fun SwitchRowPreview() {
    TimetableTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SwitchRow(
                title = "Гостевой режим",
                subtitle = "только просмотр, без правок",
                checked = true,
                onCheckedChange = {},
            )
        }
    }
}

// строка-кнопка с текущей датой семестра, по клику открывает Material DatePicker
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SemesterStartRow(currentMillis: Long?, onPick: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val zone = java.time.ZoneId.systemDefault()
    val label = if (currentMillis == null) "не задано"
                else java.time.Instant.ofEpochMilli(currentMillis).atZone(zone).toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru")))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Начало семестра", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "от этой даты считаются номера недель",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    if (open) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = currentMillis)
        AlertDialog(
            onDismissRequest = { open = false },
            text = { androidx.compose.material3.DatePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onPick(state.selectedDateMillis)
                    open = false
                }) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onPick(null)
                    open = false
                }) { Text("Сбросить") }
            },
        )
    }
}

// редактор длительностей по типу события - 6 строк с шагом 15 минут
@Composable
private fun DurationsRow(current: Map<String, Int>, onPick: (Map<String, Int>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Длительность по умолчанию", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "сколько минут предлагать для нового события каждого типа",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Изменить",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    if (open) {
        DurationsDialog(initial = current, onConfirm = {
            onPick(it)
            open = false
        }, onDismiss = { open = false })
    }
}

@Composable
private fun DurationsDialog(
    initial: Map<String, Int>,
    onConfirm: (Map<String, Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    // локальная копия чтоб правки не уезжали в AppPrefs пока пользователь крутит цифры
    var draft by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Длительность по умолчанию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventIcons.keys.forEach { key ->
                    val minutes = draft[key] ?: 60
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = EventIcons.vector(key),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = EventIcons.labels[key] ?: key,
                            modifier = Modifier.padding(start = 12.dp).weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            val v = (minutes - 15).coerceAtLeast(15)
                            draft = draft.toMutableMap().apply { this[key] = v }
                        }) { Text("-15") }
                        Text(
                            text = "$minutes мин",
                            modifier = Modifier.width(60.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            val v = (minutes + 15).coerceAtMost(8 * 60)
                            draft = draft.toMutableMap().apply { this[key] = v }
                        }) { Text("+15") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
