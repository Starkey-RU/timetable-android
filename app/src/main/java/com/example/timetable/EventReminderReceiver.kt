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

        val builder = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(context)
            .notify(title.hashCode(), builder.build())
    }

    private fun formatTime(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return DateTimeFormatter.ofPattern("HH:mm").format(time)
    }
}
