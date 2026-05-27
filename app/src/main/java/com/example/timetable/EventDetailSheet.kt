package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId

// шторка с деталями события. открывается тапом по карточке если в настройках включён
// useEventDetailSheet. кнопки внизу: редактировать, копировать, удалить.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: EventEntity,
    onEdit: () -> Unit,
    onCopy: () -> Unit = {},
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val stripe = EventColors.stripe(event.colorKey)
    val studyMode by AppPrefs.studyMode
    val isGuest by AppPrefs.isGuest

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // шапка - цветная полоска + название и место
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(stripe),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (event.location.isNotBlank()) {
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider()

            // блок деталей - время, повторение, опционально учебные поля
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailRow(
                    icon = Icons.Filled.AccessTime,
                    text = "${formatHhMm(event.startMillis)} - ${formatHhMm(event.endMillis)}",
                )
                if (event.recurrenceMask != 0) {
                    DetailRow(
                        icon = Icons.Filled.Repeat,
                        text = buildRecurrenceSummary(event),
                    )
                }
                if (studyMode) {
                    val tch = event.teacher?.trim().orEmpty()
                    if (tch.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.Person, text = tch)
                    }
                    val rm = event.room?.trim().orEmpty()
                    if (rm.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.Place, text = "ауд. $rm")
                    }
                    val cls = event.classNumber?.trim().orEmpty()
                    if (cls.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.School, text = "$cls пара")
                    }
                }
            }

            // прогресс если событие идёт прямо сейчас
            val now = System.currentTimeMillis()
            if (now in event.startMillis..event.endMillis) {
                val total = (event.endMillis - event.startMillis).coerceAtLeast(1L)
                val progress = ((now - event.startMillis).toFloat() / total).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    color = stripe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider()

            // ряд кнопок: главная "Редактировать" + две квадратные иконки
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = if (isGuest) "Открыть" else "Редактировать")
                }
                if (!isGuest) {
                    FilledTonalIconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Копировать",
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Удалить",
                        )
                    }
                }
            }
        }
    }
}

// одна строка деталей - иконка слева, текст справа
@Composable
private fun DetailRow(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// HH:mm в локальной зоне
private fun formatHhMm(millis: Long): String {
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(t.hour, t.minute)
}

// собираем подпись "Пн, Ср, Пт, чёт" из маски + чётности
private fun buildRecurrenceSummary(event: EventEntity): String {
    val days = WeekDays.all.zip(WeekDays.labels)
        .filter { (bit, _) -> event.recurrenceMask and bit != 0 }
        .joinToString(", ") { it.second }
    val parity = when (event.weekParity) {
        WeekParity.EVEN -> ", чёт"
        WeekParity.ODD -> ", неч"
        else -> ""
    }
    return "$days$parity"
}
