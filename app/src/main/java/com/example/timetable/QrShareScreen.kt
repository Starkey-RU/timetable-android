package com.example.timetable

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tooBig by remember { mutableStateOf(false) }
    var eventsCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // берём только разовые на ближайшие 14 дней + все повторяющиеся
        val bundle = app.eventRepository.exportAll()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val horizon = now + 14L * 24 * 60 * 60 * 1000
        val pruned = bundle.events.filter { dto ->
            dto.recurrenceMask != 0 || (dto.startMillis in now..horizon)
        }
        val slim = bundle.copy(events = pruned)
        eventsCount = pruned.size
        val json = Json.encodeToString(slim)
        val packed = TextCompress.pack(json)
        if (packed.length > 2500) {
            tooBig = true
        } else {
            // 600x600 px - достаточно для большинства сканеров
            bitmap = BarcodeEncoder().encodeBitmap(packed, BarcodeFormat.QR_CODE, 600, 600)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поделиться по QR") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    tooBig -> {
                        Text(
                            text = "Слишком много событий чтобы упаковать в один QR. Попробуй экспорт через JSON.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    bitmap == null -> {
                        CircularProgressIndicator()
                    }
                    else -> {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "QR-код расписания",
                            modifier = Modifier.size(280.dp),
                        )
                        Text(
                            text = "Открой сканер на другом телефоне и наведи камеру",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "В коде: $eventsCount событий (разовые до 14 дней + повторяющиеся)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
