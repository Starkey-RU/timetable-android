package com.example.timetable

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

// раз в сутки чистит старые события и обновляет alarms на следующие семь дней.
// иначе после недели без открытия приложения новые напоминания уже не будут поставлены.
class CleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TimetableApplication
        val days = AppPrefs.autoDeleteDays.value
        runCatching { app.eventRepository.purgePastSingles(days) }
        runCatching {
            val events = app.eventRepository.observeAll().first()
            NotificationScheduler.reschedule(applicationContext, events)
        }
        return Result.success()
    }
}
