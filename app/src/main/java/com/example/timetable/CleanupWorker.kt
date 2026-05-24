package com.example.timetable

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// раз в сутки чистит разовые события старше autoDeleteDays дней.
// в Application мы уже чистим при старте и на смену настройки, но если приложение
// не открывали неделю - оно не запустится и не почистит. workmanager закроет дыру.
class CleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TimetableApplication
        val days = AppPrefs.autoDeleteDays.value
        runCatching { app.eventRepository.purgePastSingles(days) }
        return Result.success()
    }
}
