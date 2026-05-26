package com.example.timetable

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat

// обрабатываем нажатие кнопок snooze в уведомлении о событии
class NotificationActionsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE) return

        val title = intent.getStringExtra(NotificationScheduler.EXTRA_TITLE) ?: return
        val location = intent.getStringExtra(NotificationScheduler.EXTRA_LOCATION).orEmpty()
        val startMillis = intent.getLongExtra(NotificationScheduler.EXTRA_START, 0L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, title.hashCode())
        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 0)
        if (minutes <= 0) return

        // сначала убираем текущее уведомление чтоб не висело
        NotificationManagerCompat.from(context).cancel(notifId)

        // откладываем уведомление на N минут вперёд
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val nextIntent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra(NotificationScheduler.EXTRA_TITLE, title)
            putExtra(NotificationScheduler.EXTRA_LOCATION, location)
            putExtra(NotificationScheduler.EXTRA_START, startMillis)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // requestCode отдельный для snooze-аларма чтоб не конфликтовать с основным расписанием
        val requestCode = notifId xor (0x5A0000 + minutes)
        val pi = PendingIntent.getBroadcast(context, requestCode, nextIntent, flags)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        NotificationScheduler.scheduleAlarm(am, triggerAt, pi)
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.timetable.action.SNOOZE"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}
