package com.example.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class EditorForm(
    val title: String = "",
    val location: String = "",
    val colorKey: String = "indigo",
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(10, 0),
    val date: LocalDate = LocalDate.now(),
    val recurrenceMask: Int = 0,
    val weekParity: Int = WeekParity.ALL,
)

class EventEditorViewModel(
    private val repo: EventRepository,
    private val eventId: Long?,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _form = MutableStateFlow(EditorForm())
    val form: StateFlow<EditorForm> = _form.asStateFlow()

    val isEditing: Boolean = eventId != null

    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    init {
        if (eventId != null) {
            viewModelScope.launch {
                val ev = repo.getById(eventId)
                if (ev == null) {
                    // событие удалили из-под нас, экран закроется
                    _notFound.value = true
                    return@launch
                }
                _form.value = EditorForm(
                    title = ev.title,
                    location = ev.location,
                    colorKey = ev.colorKey,
                    start = ev.startMillis.toLocalTime(),
                    end = ev.endMillis.toLocalTime(),
                    date = ev.startMillis.toLocalDate(),
                    recurrenceMask = ev.recurrenceMask,
                    weekParity = ev.weekParity,
                )
            }
        }
    }

    fun setTitle(value: String) { _form.value = _form.value.copy(title = value) }
    fun setLocation(value: String) { _form.value = _form.value.copy(location = value) }
    fun setColor(key: String) { _form.value = _form.value.copy(colorKey = key) }
    fun setStart(time: LocalTime) { _form.value = _form.value.copy(start = time) }
    fun setEnd(time: LocalTime) { _form.value = _form.value.copy(end = time) }
    fun setDate(date: LocalDate) { _form.value = _form.value.copy(date = date) }

    fun toggleDay(bit: Int) {
        val cur = _form.value.recurrenceMask
        _form.value = _form.value.copy(recurrenceMask = cur xor bit)
    }

    fun setParity(parity: Int) { _form.value = _form.value.copy(weekParity = parity) }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (f.title.isBlank()) return
        viewModelScope.launch {
            val startMillis = f.date.atTime(f.start).atZone(zone).toInstant().toEpochMilli()
            // если конец раньше или равен старту - значит событие через полночь, кидаем конец на след день
            val endDate = if (f.end > f.start) f.date else f.date.plusDays(1)
            val endMillis = endDate.atTime(f.end).atZone(zone).toInstant().toEpochMilli()
            val entity = EventEntity(
                id = eventId ?: 0,
                title = f.title.trim(),
                location = f.location.trim(),
                colorKey = f.colorKey,
                startMillis = startMillis,
                endMillis = endMillis,
                recurrenceMask = f.recurrenceMask,
                weekParity = f.weekParity,
            )
            repo.add(entity)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = eventId ?: return
        viewModelScope.launch {
            repo.deleteById(id)
            onDone()
        }
    }

    private fun Long.toLocalTime(): LocalTime =
        java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalTime().withSecond(0).withNano(0)

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    companion object {
        fun factory(repo: EventRepository, eventId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer { EventEditorViewModel(repo, eventId) }
        }
    }
}
