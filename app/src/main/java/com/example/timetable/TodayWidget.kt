package com.example.timetable

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TimetableApplication
        val events = app.eventRepository.observeAll().first()
        val state = composeToday(events, System.currentTimeMillis(), ZoneId.systemDefault())
        provideContent {
            GlanceTheme {
                WidgetBody(state)
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

@Composable
private fun WidgetBody(state: TodayState) {
    // приоритет: сейчас > следующее > ничего
    val main = state.now.firstOrNull() ?: state.next
    // сколько ещё событий впереди помимо показанной карточки
    val restCount = state.now.size + (if (state.next != null) 1 else 0) +
        state.later.size - (if (main != null) 1 else 0)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = "Сегодня",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (main == null) {
            EmptyCard()
        } else {
            EventCardWidget(
                event = main,
                isNow = state.now.isNotEmpty() && main == state.now.first(),
                nowMillis = state.nowMillis,
            )
            if (restCount > 0) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "+ ещё $restCount",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EventCardWidget(event: EventEntity, isNow: Boolean, nowMillis: Long) {
    val stripeColor = ColorProvider(EventColors.stripe(event.colorKey))
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp),
    ) {
        // полоска-цвет события слева, как в карточках на экране сегодня
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .fillMaxHeight()
                .background(stripeColor),
        ) {}
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (isNow) "СЕЙЧАС" else "ДАЛЬШЕ",
                style = TextStyle(
                    color = stripeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = event.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            val timeLine = if (isNow) {
                val leftMin = ((event.endMillis - nowMillis) / 60_000L).coerceAtLeast(0)
                "до ${formatHM(event.endMillis)} · осталось $leftMin мин"
            } else {
                "${formatHM(event.startMillis)} - ${formatHM(event.endMillis)}"
            }
            Text(
                text = timeLine,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
            if (event.location.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = event.location,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "на сегодня пусто",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp,
            ),
        )
    }
}

private fun formatHM(millis: Long): String {
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(t.hour, t.minute)
}
