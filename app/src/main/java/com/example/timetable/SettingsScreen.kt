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
            PaletteChips(selected = palette, onPick = { AppPrefs.palette.value = it })
        }

        item { SectionTitle("Градиент") }
        item {
            GradientChips(selected = gradient, onPick = { AppPrefs.gradient.value = it })
        }

        item { SectionTitle("Внешний вид") }
        item {
            val showLabels by AppPrefs.showNavLabels
            SwitchRow(
                title = "Подписи в нижней панели",
                subtitle = "если выключить - останутся только иконки",
                checked = showLabels,
                onCheckedChange = { AppPrefs.showNavLabels.value = it },
            )
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
        item { SectionTitle("Информация") }
        item {
            OutlinedButton(
                onClick = onOpenReports,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Отчёты")
            }
        }

        item { HorizontalDivider() }
        item { SectionTitle("Поделиться") }
        item {
            Button(
                onClick = {
                    scope.launch {
                        val bundle = repo.exportAll()
                        shareText = Json.encodeToString(bundle)
                        showShare = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скопировать как JSON")
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
                        val bundle = Json.decodeFromString<ExportBundle>(input)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient.brush)
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = "Расписание",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
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
    Column(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(mode) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == mode, onClick = { onPick(mode) })
                Text(text = mode.title, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PaletteChips(selected: Palette, onPick: (Palette) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Palette.entries.forEach { p ->
            PaletteChip(
                palette = p,
                isSelected = p == selected,
                onClick = { onPick(p) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PaletteChip(
    palette: Palette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
        }
        Text(
            text = palette.title,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun GradientChips(selected: GradientPreset, onPick: (GradientPreset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GradientPreset.entries.forEach { g ->
            val outlineColor = if (g == selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(g.brush)
                    .border(2.dp, outlineColor, RoundedCornerShape(14.dp))
                    .clickable { onPick(g) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (g == selected) {
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
