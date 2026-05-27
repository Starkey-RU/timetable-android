package com.example.timetable

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

// полноэкранный режим "фокус": показывает только текущее или ближайшее событие
// большим шрифтом, с обратным отсчётом. фон чёрный амолед чтоб батарею не жрать.
@Composable
fun FocusModeScreen(onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as TimetableApplication
    // переиспользуем тот же view model что и обычный экран сегодня - всё уже посчитано
    val vm: TodayViewModel = viewModel(factory = remember { TodayViewModel.factory(app.eventRepository) })
    val state by vm.state.collectAsState()

    // тикает раз в секунду чтоб таймер шёл плавно, а не раз в минуту как в обычном today
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    // прячем статус-бар и навигацию пока висит фокус. на выходе возвращаем как было.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.findActivity()
        val window = activity?.window
        // снимаем значение pref один раз чтоб смена в настройках не дёргала флаг по ходу сессии
        val keepOn = AppPrefs.focusKeepScreenOn.value
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousBehavior = controller.systemBarsBehavior
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // если в настройках включено - не даём экрану гаснуть пока висит фокус
            if (keepOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
                if (keepOn) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        } else {
            onDispose { }
        }
    }

    // подсказка про тап висит первые 4 секунды, потом плавно тает
    var hintVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(4000)
        hintVisible = false
    }

    // обычный экран обновляет группы раз в минуту, а здесь граница события должна быть точной
    val focusState = focusStateAt(state, nowMs)
    val current: EventEntity? = focusState.now.firstOrNull()
    val upcoming: EventEntity? = current ?: focusState.next

    // текущее время для часов слева сверху, формат HH:mm
    val zone = java.time.ZoneId.systemDefault()
    val clockText = java.time.Instant.ofEpochMilli(nowMs).atZone(zone)
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

    // цвет события используем как акцент для иконки и таймера
    val accentColor = upcoming?.let { EventColors.stripe(it.colorKey) } ?: Color.White

    // фон берём из настроек - амолед-чёрный (по умолчанию) или один из тёмных градиентов
    val bgBrush = AppPrefs.focusGradient.value.brush

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            // тап в любом месте закрывает - так и в подсказке написано
            .clickable { onClose() },
    ) {
        // маленькие часы в левом верхнем углу
        Text(
            text = clockText,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )

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
                // иконка категории сверху, крупная, в цвет события
                Icon(
                    imageVector = EventIcons.vector(upcoming.iconKey),
                    contentDescription = null,
                    tint = accentColor,
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
                        color = Color.White.copy(alpha = 0.7f),
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
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                // таймер в цвет события, на чёрном смотрится сочно
                Text(
                    text = formatCountdown(targetMs - nowMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // подсказка снизу про выход по тапу - сама прячется через 4 сек
        AnimatedVisibility(
            visible = hintVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Тапни в любом месте чтобы выйти",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
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

// идём по цепочке ContextWrapper-ов пока не найдём активити (либо null)
private fun View.findActivity(): Activity? {
    var ctx: Context? = this.context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
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
