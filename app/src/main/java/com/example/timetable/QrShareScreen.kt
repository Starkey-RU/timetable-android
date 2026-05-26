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
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// эмпирический лимит, дальше QR превращается в мелкую кашу
private const val MAX_QR_CHARS = 1200
private const val INITIAL_QR_HORIZON_DAYS = 14L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tooBig by remember { mutableStateOf(false) }
    var eventsCount by remember { mutableStateOf(0) }
    var packedLength by remember { mutableStateOf(0) }
    var horizonUsed by remember { mutableStateOf(INITIAL_QR_HORIZON_DAYS) }

    LaunchedEffect(Unit) {
        // берём разовые в ограниченном окне + все повторяющиеся
        val bundle = app.eventRepository.exportAll()
        val now = System.currentTimeMillis()

        // сначала сохраняем прежний горизонт, а уменьшаем его только когда QR реально не влезает
        var horizonDays = INITIAL_QR_HORIZON_DAYS
        var slim = bundle
        var packed = ""
        while (true) {
            val horizon = now + horizonDays * 24 * 60 * 60 * 1000
            val pruned = bundle.events.filter { dto ->
                dto.recurrenceMask != 0 || (dto.startMillis in now..horizon)
            }
            slim = bundle.copy(events = pruned)
            packed = TextCompress.pack(Json.encodeToString(slim))
            if (packed.length <= MAX_QR_CHARS) {
                horizonUsed = horizonDays
                break
            }
            // следующая попытка с меньшим окном
            if (horizonDays <= 1L) {
                horizonUsed = horizonDays
                break
            }
            horizonDays /= 2
        }

        if (packed.length > MAX_QR_CHARS) {
            // даже на одном дне не уместилось
            packedLength = packed.length
            eventsCount = slim.events.size
            tooBig = true
        } else {
            eventsCount = slim.events.size
            packedLength = packed.length
            // подсказки кодировщику: маленький margin и явная кодировка
            val hints = mapOf(
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            // 600x600 px - достаточно для большинства сканеров
            bitmap = BarcodeEncoder().encodeBitmap(packed, BarcodeFormat.QR_CODE, 600, 600, hints)
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
                            text = "В одном QR не уместить $eventsCount событий (упаковка $packedLength символов, лимит $MAX_QR_CHARS).\nУменьшается, если убрать повторяющиеся или сократить горизонт. Или экспортируй через JSON и пришли файлом.",
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
                            text = "В коде: $eventsCount событий (разовые до $horizonUsed дней + повторяющиеся)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "упаковка: $packedLength симв.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
