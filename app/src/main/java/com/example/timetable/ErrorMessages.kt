package com.example.timetable

// собранные в одно место тексты тостов, снэков и подтверждений.
// если в одном файле - проще править опечатки и собирать в приложение к описанию
object ErrorMessages {

    // импорт-экспорт
    const val IMPORT_ADDED = "Добавлено %d событий"
    const val IMPORT_PARSE_FAILED = "Не получилось разобрать текст"
    const val EXPORT_COPIED = "Скопировано в буфер"

    // qr
    const val QR_SCAN_FAILED = "Не получилось разобрать QR"

    // pin
    const val PIN_WRONG = "Неверный PIN"
    const val PIN_TOO_SHORT = "Минимум 4 цифры"
    const val PIN_MISMATCH = "PIN не совпадает"

    // guest mode
    const val GUEST_READ_ONLY = "Только просмотр"

    // разрешения
    const val NOTIFICATIONS_DENIED = "Без разрешения уведомления не придут"

    // архив
    const val ARCHIVE_DONE = "Архивировано %d событий"
    const val ARCHIVE_CONFIRM_TITLE = "Архивировать семестр?"
    const val ARCHIVE_CONFIRM_TEXT = "Архивировать %d событий? Основное расписание будет очищено."
    const val ARCHIVE_CLEAR_TITLE = "Очистить архив?"
    const val ARCHIVE_CLEAR_TEXT = "Все архивные записи будут удалены без возможности восстановления."

    // редактор события
    const val EVENT_CONFLICT_TITLE = "Накладка по времени"
    const val EVENT_CONFLICT_TEXT = "В это время уже есть: %s. Всё равно сохранить?"
}
