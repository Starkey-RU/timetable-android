package com.example.timetable

import android.content.Context
import android.net.Uri
import java.io.File

// сохраняем всю базу одним .db файлом через uri выбранный пользователем
// (room сначала checkpointим чтобы wal слил всё в основной файл)
object BackupHelper {

    // полный путь к db-файлу room. имя совпадает с tablename из builder.
    private fun dbFile(context: Context): File =
        context.applicationContext.getDatabasePath(TimetableDatabase.NAME)

    // принудительно сливаем wal в main db, иначе бэкап получится без последних записей
    private fun checkpoint(context: Context) {
        val db = (context.applicationContext as TimetableApplication).database
        // PRAGMA wal_checkpoint(FULL) - блокирующий, но недолгий
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL);").use { it.moveToFirst() }
    }

    // выгрузить базу в файл по uri (был выбран через CreateDocument)
    fun backupTo(context: Context, uri: Uri): Result<Unit> = runCatching {
        checkpoint(context)
        val src = dbFile(context)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            src.inputStream().use { it.copyTo(out) }
        } ?: error("Не удалось открыть файл для записи")
    }

    // восстановить базу из выбранного uri. сначала закрываем room, копируем поверх,
    // потом приложение нужно перезапустить чтоб room снова открыл файл
    fun restoreFrom(context: Context, uri: Uri): Result<Unit> = runCatching {
        val app = context.applicationContext as TimetableApplication
        app.database.close()
        val dst = dbFile(context)
        // wal/shm файлы лучше удалить чтобы не было рассинхрона со старым main
        File(dst.parentFile, "${dst.name}-wal").takeIf { it.exists() }?.delete()
        File(dst.parentFile, "${dst.name}-shm").takeIf { it.exists() }?.delete()
        context.contentResolver.openInputStream(uri)?.use { input ->
            dst.outputStream().use { input.copyTo(it) }
        } ?: error("Не удалось открыть файл для чтения")
        // нельзя просто заново открыть тот же экземпляр - нужно пересоздать.
        // проще попросить пользователя перезапустить приложение.
    }
}
