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
    val iconKey: String = "event",
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(10, 0),
    val date: LocalDate = LocalDate.now(),
    val recurrenceMask: Int = 0,
    val weekParity: Int = WeekParity.ALL,
    val teacher: String = "",
    val classNumber: String = "",
    val room: String = "",
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
                    iconKey = ev.iconKey,
                    start = ev.startMillis.toLocalTime(),
                    end = ev.endMillis.toLocalTime(),
                    date = ev.startMillis.toLocalDate(),
                    recurrenceMask = ev.recurrenceMask,
                    weekParity = ev.weekParity,
                    teacher = ev.teacher.orEmpty(),
                    classNumber = ev.classNumber.orEmpty(),
                    room = ev.room.orEmpty(),
                )
            }
        }
    }

    fun setTitle(value: String) { _form.value = _form.value.copy(title = value) }
    fun setLocation(value: String) { _form.value = _form.value.copy(location = value) }
    fun setColor(key: String) { _form.value = _form.value.copy(colorKey = key) }
    fun setIcon(key: String) {
        val f = _form.value
        val newForm = f.copy(iconKey = key)
        // при создании нового события подстраиваем длительность под выбранный тип.
        // если пользователь уже руками подвинул время - не лезем.
        if (eventId == null) {
            val minutes = AppPrefs.durationsByIcon.value[key]
            if (minutes != null && minutes > 0) {
                val newEnd = f.start.plusMinutes(minutes.toLong())
                _form.value = newForm.copy(end = newEnd)
                return
            }
        }
        _form.value = newForm
    }
    fun setStart(time: LocalTime) {
        val f = _form.value
        // если в настройках включён авто-сдвиг конца - двигаем конец на час позже нового начала
        if (AppPrefs.autoExtendEndTime.value) {
            _form.value = f.copy(start = time, end = time.plusHours(1))
        } else {
            _form.value = f.copy(start = time)
        }
    }
    fun setEnd(time: LocalTime) { _form.value = _form.value.copy(end = time) }
    fun setDate(date: LocalDate) { _form.value = _form.value.copy(date = date) }
    fun setTeacher(value: String) { _form.value = _form.value.copy(teacher = value) }
    fun setClassNumber(value: String) { _form.value = _form.value.copy(classNumber = value) }
    fun setRoom(value: String) { _form.value = _form.value.copy(room = value) }

    fun toggleDay(bit: Int) {
        val cur = _form.value.recurrenceMask
        _form.value = _form.value.copy(recurrenceMask = cur xor bit)
    }

    fun setParity(parity: Int) { _form.value = _form.value.copy(weekParity = parity) }

    fun clearRecurrence() {
        _form.value = _form.value.copy(recurrenceMask = 0, weekParity = WeekParity.ALL)
    }

    // быстрый пресет: цвет/иконку ставим всегда, название - только если поле пустое.
    // конец сдвигаем относительно текущего начала на длительность из шаблона.
    fun applyTemplate(tpl: EventTemplate) {
        val f = _form.value
        val newTitle = if (f.title.isBlank()) tpl.titlePrefix else f.title
        val newEnd = f.start.plusMinutes(tpl.defaultDurationMinutes.toLong())
        _form.value = f.copy(
            title = newTitle,
            colorKey = tpl.colorKey,
            iconKey = tpl.iconKey,
            end = newEnd,
        )
    }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (f.title.isBlank()) return
        // подстраховка: если как-то нажали Сохранить при гостевом режиме - молча выходим
        if (AppPrefs.isGuest.value) { onDone(); return }
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
                iconKey = f.iconKey,
                startMillis = startMillis,
                endMillis = endMillis,
                recurrenceMask = f.recurrenceMask,
                weekParity = f.weekParity,
                teacher = f.teacher.trim().ifBlank { null },
                classNumber = f.classNumber.trim().ifBlank { null },
                room = f.room.trim().ifBlank { null },
            )
            if (eventId == null) repo.add(entity) else repo.update(entity)
            onDone()
        }
    }

    // возвращает первое пересечение во времени с уже существующими событиями на тот же день.
    // повторяющиеся не разворачиваем - проверяем только разовое в этот день.
    suspend fun findConflict(repo: EventRepository): EventEntity? {
        val f = _form.value
        val startMillis = f.date.atTime(f.start).atZone(zone).toInstant().toEpochMilli()
        val endDate = if (f.end > f.start) f.date else f.date.plusDays(1)
        val endMillis = endDate.atTime(f.end).atZone(zone).toInstant().toEpochMilli()
        val sameRange = repo.observeInRange(startMillis, endMillis).first()
        return sameRange.firstOrNull { it.id != (eventId ?: -1L) }
    }

    fun delete(onDone: () -> Unit) {
        val id = eventId ?: return
        if (AppPrefs.isGuest.value) { onDone(); return }
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
