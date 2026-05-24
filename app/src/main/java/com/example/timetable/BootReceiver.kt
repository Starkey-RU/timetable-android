package com.example.timetable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// после перезагрузки телефона все алармы стираются - перепланируем их заново.
// также ловим переустановку apk - после обновления алармы тоже сбрасываются.
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        // системный broadcast - даём себе пару секунд на корутину
        val pending = goAsync()
        val app = context.applicationContext as TimetableApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = app.eventRepository.observeAll().first()
                NotificationScheduler.reschedule(context, events)
            } finally {
                pending.finish()
            }
        }
    }
}
