package com.example.timetable

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// тонкая обёртка: открывает сканер zxing, при успехе пытается импортировать сжатый расписание.
@Composable
fun QrScanScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents
        if (text.isNullOrBlank()) {
            onClose()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                // если строка начинается с { - принимаем как сырой JSON, иначе пробуем разжать
                val raw = if (text.trimStart().startsWith("{")) text else TextCompress.unpack(text)
                val bundle = Json.decodeFromString<ExportBundle>(raw)
                val n = app.eventRepository.importEvents(bundle)
                Toast.makeText(context, ErrorMessages.IMPORT_ADDED.format(n), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, ErrorMessages.QR_SCAN_FAILED, Toast.LENGTH_LONG).show()
            }
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Наведи камеру на QR с расписанием")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
        launcher.launch(options)
    }
}
