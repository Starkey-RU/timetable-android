package com.example.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class DayBucket(val date: LocalDate, val count: Int)

// насколько в будущее/прошлое можно листать недели от текущей. ±13 ≈ ±90 дней
const val WEEK_OFFSET_MAX = 13

class WeekViewModel(private val repo: EventRepository) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    val days: StateFlow<List<DayBucket>> = combine(repo.observeAll(), _weekOffset) { all, offset ->
        val today = LocalDate.now(zone)
        val monday = today.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
        buildWeekFromMonday(all, monday, zone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun prevWeek() {
        if (_weekOffset.value > -WEEK_OFFSET_MAX) _weekOffset.value -= 1
    }

    fun nextWeek() {
        if (_weekOffset.value < WEEK_OFFSET_MAX) _weekOffset.value += 1
    }

    fun resetWeek() {
        _weekOffset.value = 0
    }

    companion object {
        fun factory(repo: EventRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { WeekViewModel(repo) }
        }
    }
}

// разворачивает повторяющиеся и считает сколько событий приходится на каждый день недели,
// начиная с указанного понедельника.
internal fun buildWeekFromMonday(all: List<EventEntity>, monday: LocalDate, zone: ZoneId): List<DayBucket> {
    return (0..6).map { offset ->
        val day = monday.plusDays(offset.toLong())
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val count = all.sumOf { expandRecurrence(it, from, to, zone).size }
        DayBucket(day, count)
    }
}

// оставлено ради обратной совместимости с тестами/превью
internal fun buildWeek(all: List<EventEntity>, today: LocalDate, zone: ZoneId): List<DayBucket> {
    val monday = today.with(DayOfWeek.MONDAY)
    return buildWeekFromMonday(all, monday, zone)
}
