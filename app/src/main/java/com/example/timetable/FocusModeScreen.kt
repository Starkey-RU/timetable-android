package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

// полноэкранный режим "фокус": показывает только текущее или ближайшее событие
// большим шрифтом, с обратным отсчётом. фон - градиент из настроек.
@Composable
fun FocusModeScreen(onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as TimetableApplication
    // переиспользуем тот же view model что и обычный экран сегодня - всё уже посчитано
    val vm: TodayViewModel = viewModel(factory = remember { TodayViewModel.factory(app.eventRepository) })
    val state by vm.state.collectAsState()
    val gradient by AppPrefs.gradient

    // тикает раз в секунду чтоб таймер шёл плавно, а не раз в минуту как в обычном today
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    // обычный экран обновляет группы раз в минуту, а здесь граница события должна быть точной
    val focusState = focusStateAt(state, nowMs)
    val current: EventEntity? = focusState.now.firstOrNull()
    val upcoming: EventEntity? = current ?: focusState.next

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient.brush)
            // тап в любом месте закрывает - так и в подсказке написано
            .clickable { onClose() },
    ) {
        // крестик в правом верхнем углу - дублирует тап
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Закрыть",
                tint = Color.White,
            )
        }

        if (upcoming == null) {
            // на сегодня вообще пусто - просто короткая фраза по центру
            Text(
                text = "На сегодня всё, отдохни",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // иконка категории сверху, крупная
                Icon(
                    imageVector = EventIcons.vector(upcoming.iconKey),
                    contentDescription = null,
                    tint = EventColors.stripe(upcoming.colorKey),
                    modifier = Modifier.size(96.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
                // название самым крупным шрифтом
                Text(
                    text = upcoming.title,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                if (upcoming.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = upcoming.location,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(36.dp))
                // если событие идёт сейчас - считаем до конца, иначе до начала
                val isNow = current != null
                val targetMs = if (isNow) upcoming.endMillis else upcoming.startMillis
                val label = if (isNow) "осталось" else "до начала"
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCountdown(targetMs - nowMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // подсказка снизу про выход по тапу
        Text(
            text = "Тапни в любом месте чтобы выйти",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

// форматирует миллисекунды в строку 1:23:45 или 27:14
private fun formatCountdown(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
           else "%02d:%02d".format(m, s)
}

internal fun focusStateAt(state: TodayState, nowMillis: Long): TodayState {
    val todayEvents = buildList {
        addAll(state.now)
        state.next?.let { add(it) }
        addAll(state.later)
        addAll(state.done)
    }
    return groupForToday(todayEvents, nowMillis)
}
