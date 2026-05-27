package com.example.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulesViewModel(private val repo: EventRepository) : ViewModel() {
    val events: StateFlow<List<EventEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // храним id выделенных карточек, пустое множество = режим выбора выключен
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // переключить выделение одной карточки
    fun toggle(id: Long) {
        val cur = _selectedIds.value
        _selectedIds.value = if (id in cur) cur - id else cur + id
    }

    // сбросить весь выбор (крестик в шапке или после удаления)
    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    // удалить одну карточку по id (используется из шторки деталей, без режима выбора)
    fun delete(id: Long) {
        viewModelScope.launch {
            val ev = events.value.firstOrNull { it.id == id } ?: return@launch
            repo.delete(ev)
        }
    }

    // продублировать карточку на ту же дату, открывается из шторки деталей
    fun duplicate(id: Long, dayOffset: Long = 0, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            val newId = repo.duplicate(id, dayOffset)
            onDone(newId)
        }
    }

    // удалить все выделенные события и выйти из режима выбора
    fun deleteSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            // берём текущий список один раз, чтобы не дёргать flow в цикле
            val snapshot = events.value
            ids.forEach { id ->
                val ev = snapshot.firstOrNull { it.id == id }
                if (ev != null) repo.delete(ev)
            }
            _selectedIds.value = emptySet()
        }
    }

    companion object {
        fun factory(repo: EventRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SchedulesViewModel(repo) }
        }
    }
}
