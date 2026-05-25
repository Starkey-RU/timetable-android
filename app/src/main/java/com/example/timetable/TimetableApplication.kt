package com.example.timetable

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class TimetableApplication : Application() {

    val database by lazy { TimetableDatabase.get(this) }
    val eventRepository by lazy { EventRepository(database.eventDao(), database.archivedEventDao()) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        PinManager.init(this)
        NotificationScheduler.ensureChannel(this)

        // чистим прошедшие разовые события при старте, если в настройках включено
        scope.launch {
            runCatching { eventRepository.purgePastSingles(AppPrefs.autoDeleteDays.value) }
        }

        // и просим workmanager раз в сутки делать то же самое - на случай если
        // приложение не открывают неделями
        val daily = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("cleanup_past", ExistingPeriodicWorkPolicy.KEEP, daily)

        // и потом - на каждое изменение настройки тоже прогоняем
        scope.launch {
            snapshotFlow { AppPrefs.autoDeleteDays.value }
                .distinctUntilChanged()
                .collect { days ->
                    runCatching { eventRepository.purgePastSingles(days) }
                }
        }

        // подписываемся на список событий, тоггл уведомлений и время напоминания -
        // при любом изменении перепланируем
        scope.launch {
            combine(
                eventRepository.observeAll(),
                snapshotFlow { AppPrefs.notificationsEnabled.value }.distinctUntilChanged(),
                snapshotFlow { AppPrefs.reminderLeadMinutes.value }.distinctUntilChanged(),
            ) { events, _, _ -> events }
                .collect { events ->
                    NotificationScheduler.reschedule(this@TimetableApplication, events)
                }
        }

        // дёргаем виджет на главном экране чтоб обновился когда события поменялись
        scope.launch {
            eventRepository.observeAll().collect {
                runCatching { TodayWidget().updateAll(this@TimetableApplication) }
            }
        }
    }
}
