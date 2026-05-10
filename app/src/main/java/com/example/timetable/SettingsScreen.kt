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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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

@Composable
fun SettingsScreen() {
    var guestMode by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(false) }
    val palette by AppPrefs.palette
    val gradient by AppPrefs.gradient
    val theme by AppPrefs.theme

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
                onCheckedChange = { guestMode = it },
            )
        }
        item {
            SwitchRow(
                title = "PIN-код",
                subtitle = "запрос пин-кода при запуске",
                checked = pinEnabled,
                onCheckedChange = { pinEnabled = it },
            )
        }

        item { HorizontalDivider() }
        item {
            Button(
                onClick = { /* потом */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Заполнить тестовыми данными")
            }
        }
    }
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
