package com.example.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
)

class EventEditorViewModel(
    private val repo: EventRepository,
    private val eventId: Long?,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _form = MutableStateFlow(EditorForm())
    val form: StateFlow<EditorForm> = _form.asStateFlow()

    val isEditing: Boolean = eventId != null

    init {
        if (eventId != null) {
            viewModelScope.launch {
                // тащим разовым snapshot, для редактирования стрим не нужен
                val all = repo.observeAll().first()
                val ev = all.firstOrNull { it.id == eventId } ?: return@launch
                _form.value = EditorForm(
                    title = ev.title,
                    location = ev.location,
                    colorKey = ev.colorKey,
                    start = ev.startMillis.toLocalTime(),
                    end = ev.endMillis.toLocalTime(),
                    date = ev.startMillis.toLocalDate(),
                )
            }
        }
    }

    fun setTitle(value: String) { _form.value = _form.value.copy(title = value) }
    fun setLocation(value: String) { _form.value = _form.value.copy(location = value) }
    fun setColor(key: String) { _form.value = _form.value.copy(colorKey = key) }
    fun setStart(time: LocalTime) { _form.value = _form.value.copy(start = time) }
    fun setEnd(time: LocalTime) { _form.value = _form.value.copy(end = time) }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (f.title.isBlank()) return
        viewModelScope.launch {
            val entity = EventEntity(
                id = eventId ?: 0,
                title = f.title.trim(),
                location = f.location.trim(),
                colorKey = f.colorKey,
                startMillis = f.date.atTime(f.start).atZone(zone).toInstant().toEpochMilli(),
                endMillis = f.date.atTime(f.end).atZone(zone).toInstant().toEpochMilli(),
            )
            repo.add(entity)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = eventId ?: return
        viewModelScope.launch {
            // нужен entity для @Delete, проще достать снэпшот и снести
            val all = repo.observeAll().first()
            val ev = all.firstOrNull { it.id == id } ?: return@launch
            repo.delete(ev)
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
