package com.example.timetable

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_TITLE) ?: return
        val location = intent.getStringExtra(NotificationScheduler.EXTRA_LOCATION).orEmpty()
        val startMillis = intent.getLongExtra(NotificationScheduler.EXTRA_START, 0L)

        // на 13+ без рантайм-разрешения уведомления не появятся, просто молчим
        val granted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        NotificationScheduler.ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java)
        val contentPi = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val timePart = if (startMillis > 0) formatTime(startMillis) else ""
        val body = listOfNotNull(
            if (timePart.isNotBlank()) "Начало в $timePart" else null,
            location.takeIf { it.isNotBlank() },
        ).joinToString(" · ")

        val notifId = title.hashCode()

        // pendingIntent для кнопки snooze - параметризуем минутами и уникальным requestCode
        val snooze5Pi = buildSnoozePi(context, notifId, title, location, startMillis, 5)
        val snooze15Pi = buildSnoozePi(context, notifId, title, location, startMillis, 15)

        val builder = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // кнопки отложить - иконку не задаём, на современном android она и так не рисуется
            .addAction(0, "Отложить 5 мин", snooze5Pi)
            .addAction(0, "Отложить 15 мин", snooze15Pi)

        NotificationManagerCompat.from(context)
            .notify(notifId, builder.build())
    }

    // собираем pendingIntent на наш NotificationActionsReceiver с нужными extras
    private fun buildSnoozePi(
        context: Context,
        notifId: Int,
        title: String,
        location: String,
        startMillis: Long,
        minutes: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionsReceiver::class.java).apply {
            action = NotificationActionsReceiver.ACTION_SNOOZE
            putExtra(NotificationScheduler.EXTRA_TITLE, title)
            putExtra(NotificationScheduler.EXTRA_LOCATION, location)
            putExtra(NotificationScheduler.EXTRA_START, startMillis)
            putExtra(NotificationActionsReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(NotificationActionsReceiver.EXTRA_SNOOZE_MINUTES, minutes)
        }
        // requestCode уникален по (notifId, minutes) чтоб FLAG_UPDATE_CURRENT не путал кнопки
        val requestCode = notifId * 31 + minutes
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatTime(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return DateTimeFormatter.ofPattern("HH:mm").format(time)
    }
}
