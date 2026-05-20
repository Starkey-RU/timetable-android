package com.example.timetable

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class TimetableApplication : Application() {

    val database by lazy { TimetableDatabase.get(this) }
    val eventRepository by lazy { EventRepository(database.eventDao()) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        PinManager.init(this)
        NotificationScheduler.ensureChannel(this)

        // подписываемся на список событий и на тоггл уведомлений - при любом изменении перепланируем
        scope.launch {
            combine(
                eventRepository.observeAll(),
                snapshotFlow { AppPrefs.notificationsEnabled.value }.distinctUntilChanged(),
            ) { events, _ -> events }
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
