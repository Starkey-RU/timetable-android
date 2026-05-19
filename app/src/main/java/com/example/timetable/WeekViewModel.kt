package com.example.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class DayBucket(val date: LocalDate, val count: Int)

class WeekViewModel(private val repo: EventRepository) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    val days: StateFlow<List<DayBucket>> = repo.observeAll()
        .map { all -> buildWeek(all, LocalDate.now(zone), zone) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    companion object {
        fun factory(repo: EventRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { WeekViewModel(repo) }
        }
    }
}

// разворачивает повторяющиеся и считает сколько событий приходится на каждый день недели.
// неделя - текущая, с понедельника.
internal fun buildWeek(all: List<EventEntity>, today: LocalDate, zone: ZoneId): List<DayBucket> {
    val monday = today.with(DayOfWeek.MONDAY)
    return (0..6).map { offset ->
        val day = monday.plusDays(offset.toLong())
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val count = all.sumOf { expandRecurrence(it, from, to, zone).size }
        DayBucket(day, count)
    }
}
