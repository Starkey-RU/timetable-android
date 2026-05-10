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
import java.time.LocalDate
import java.time.ZoneId

data class TodayState(
    val now: List<EventEntity> = emptyList(),
    val next: EventEntity? = null,
    val later: List<EventEntity> = emptyList(),
    val done: List<EventEntity> = emptyList(),
    val nowMillis: Long = 0L,
)

class TodayViewModel(repo: EventRepository) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    val state: StateFlow<TodayState> = combine(
        repo.observeInRange(startOfToday(), startOfTomorrow()),
        nowTicker(),
    ) { events, nowMillis ->
        groupForToday(events, nowMillis)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayState(),
    )

    private fun startOfToday(): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun startOfTomorrow(): Long =
        LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

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
