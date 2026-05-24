package com.example.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class TodayState(
    val now: List<EventEntity> = emptyList(),
    val next: EventEntity? = null,
    val later: List<EventEntity> = emptyList(),
    val done: List<EventEntity> = emptyList(),
    val nowMillis: Long = 0L,
)

class TodayViewModel(private val repo: EventRepository) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    // observeAll вместо observeInRange - повторяющиеся хранятся как один шаблон,
    // экземпляры на сегодня собираем сами через expandRecurrence
    val state: StateFlow<TodayState> = combine(
        repo.observeAll(),
        nowTicker(),
    ) { all, nowMillis ->
        composeToday(all, nowMillis, zone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayState(),
    )

    fun delete(id: Long) {
        if (AppPrefs.isGuest.value) return
        viewModelScope.launch { repo.deleteById(id) }
    }

    // тикаем раз в минуту чтоб карточка "сейчас" не зависла
    private fun nowTicker() = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(60_000)
        }
    }

    companion object {
        fun factory(repo: EventRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(repo) }
        }
    }
}

// собирает Today из всего списка событий: повторяющиеся разворачиваем, потом группируем
internal fun composeToday(
    all: List<EventEntity>,
    nowMillis: Long,
    zone: ZoneId,
): TodayState {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val from = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val occurrences = all.flatMap { expandRecurrence(it, from, to, zone, AppPrefs.effectiveSemesterStart(zone)) }
    return groupForToday(occurrences, nowMillis)
}

internal fun groupForToday(events: List<EventEntity>, nowMillis: Long): TodayState {
    val now = events.filter { it.startMillis <= nowMillis && nowMillis < it.endMillis }
    val future = events.filter { it.startMillis > nowMillis }.sortedBy { it.startMillis }
    val done = events.filter { it.endMillis <= nowMillis }
    return TodayState(
        now = now,
        next = future.firstOrNull(),
        later = future.drop(1),
        done = done,
        nowMillis = nowMillis,
    )
}
