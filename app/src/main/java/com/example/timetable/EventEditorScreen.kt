package com.example.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(eventId: Long?, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as TimetableApplication
    val vm: EventEditorViewModel = viewModel(
        factory = remember(eventId) { EventEditorViewModel.factory(app.eventRepository, eventId) },
    )
    val form by vm.form.collectAsState()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Редактировать" else "Новое событие") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = vm::setTitle,
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.location,
                onValueChange = vm::setLocation,
                label = { Text("Место") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Цвет", style = MaterialTheme.typography.labelLarge)
            ColorChipsRow(selected = form.colorKey, onPick = vm::setColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeField(
                    label = "Начало",
                    value = form.start,
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "Конец",
                    value = form.end,
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }

            Box(modifier = Modifier.weight(1f))

            Button(
                onClick = { vm.save(onClose) },
                enabled = form.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            if (vm.isEditing) {
                OutlinedButton(
                    onClick = { vm.delete(onClose) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Удалить")
                }
            }
        }
    }

    if (showStartPicker) {
        PickTime(
            initial = form.start,
            onDismiss = { showStartPicker = false },
            onPick = {
                vm.setStart(it)
                showStartPicker = false
            },
        )
    }
    if (showEndPicker) {
        PickTime(
            initial = form.end,
            onDismiss = { showEndPicker = false },
            onPick = {
                vm.setEnd(it)
                showEndPicker = false
            },
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    value: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "%02d:%02d".format(value.hour, value.minute),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ColorChipsRow(selected: String, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EventColors.keys.forEach { key ->
            ColorChip(
                color = EventColors.stripe(key),
                isSelected = key == selected,
                onClick = { onPick(key) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColorChip(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "выбрано",
                tint = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickTime(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onPick: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text("Ок")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        text = { TimePicker(state = state) },
    )
}

