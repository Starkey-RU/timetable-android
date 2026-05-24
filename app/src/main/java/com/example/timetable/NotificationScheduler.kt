package com.example.timetable

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId

object NotificationScheduler {

    const val CHANNEL_ID = "event_reminders"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_LOCATION = "extra_location"
    const val EXTRA_START = "extra_start"

    // за сколько до начала события - 10 минут
    private const val LEAD_TIME_MS = 10 * 60 * 1000L
    // на сколько вперёд планируем уведомления
    private const val HORIZON_DAYS = 7L
    // ключ в SharedPreferences для списка активных requestCode-ов
    private const val PREFS_FILE = "notif_codes"
    private const val PREFS_KEY = "active"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о событиях",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Уведомление за 10 минут до начала" }
            nm.createNotificationChannel(channel)
        }
    }

    // снимает все ранее поставленные алармы и ставит новые на ближайшие 7 дней
    fun reschedule(
        context: Context,
        events: List<EventEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        // снимаем то что было записано в прошлый раз
        val previous = prefs.getStringSet(PREFS_KEY, emptySet())?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        previous.forEach { code ->
            am.cancel(buildPendingIntent(context, code, null))
        }

        if (!AppPrefs.notificationsEnabled.value) {
            prefs.edit().remove(PREFS_KEY).apply()
            return
        }

        val horizonTo = nowMillis + HORIZON_DAYS * 24 * 60 * 60 * 1000L
        val newCodes = mutableSetOf<String>()

        events.forEach { event ->
            val occurrences = expandRecurrence(event, nowMillis, horizonTo, zone, AppPrefs.effectiveSemesterStart(zone))
            occurrences.forEach { occ ->
                val triggerAt = occ.startMillis - LEAD_TIME_MS
                if (triggerAt <= nowMillis) return@forEach
                // requestCode уникальный по (id, день начала) - чтоб повторяющиеся не затирали друг друга
                val dayOfYear = ((occ.startMillis / (24 * 60 * 60 * 1000L)) % 366).toInt()
                val code = (event.id.toInt() and 0xFFFF) * 1000 + dayOfYear
                val pi = buildPendingIntent(context, code, occ)
                // inexact аларм - под Android 12+ без SCHEDULE_EXACT_ALARM. Для напоминаний хватает
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                newCodes.add(code.toString())
            }
        }

        prefs.edit().putStringSet(PREFS_KEY, newCodes).apply()
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        occurrence: EventEntity?,
    ): PendingIntent {
        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            if (occurrence != null) {
                putExtra(EXTRA_TITLE, occurrence.title)
                putExtra(EXTRA_LOCATION, occurrence.location)
                putExtra(EXTRA_START, occurrence.startMillis)
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
