package com.example.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// отдельный экран с настройками, которые видно только на широком экране / foldable.
// в обычном телефоне эти переключатели всё равно ни на что не повлияют - вынес чтоб не мешались
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldableSettingsScreen(onClose: () -> Unit) {
    val useSideRail by AppPrefs.useSideRail
    val railOnRight by AppPrefs.navRailOnRight
    val railCentered by AppPrefs.navRailCentered
    val swapPanes by AppPrefs.swapTwoPanePanels

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Для широкого экрана") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "настройки активны на планшете и разложенном foldable. на телефоне их не видно.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FoldableSwitchRow(
                    title = "Боковое меню",
                    subtitle = "на широком экране показывать меню сбоку, а не снизу",
                    checked = useSideRail,
                    onCheckedChange = { AppPrefs.useSideRail.value = it },
                )
            }
            item {
                FoldableSwitchRow(
                    title = "Меню справа",
                    subtitle = "по умолчанию меню слева",
                    checked = railOnRight,
                    enabled = useSideRail,
                    onCheckedChange = { AppPrefs.navRailOnRight.value = it },
                )
            }
            item {
                FoldableSwitchRow(
                    title = "Пункты меню по центру",
                    subtitle = "иначе будут сверху как обычно",
                    checked = railCentered,
                    enabled = useSideRail,
                    onCheckedChange = { AppPrefs.navRailCentered.value = it },
                )
            }
            item {
                FoldableSwitchRow(
                    title = "Поменять панели местами",
                    subtitle = "редактор слева, список справа",
                    checked = swapPanes,
                    onCheckedChange = { AppPrefs.swapTwoPanePanels.value = it },
                )
            }
        }
    }
}

@Composable
private fun FoldableSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}
