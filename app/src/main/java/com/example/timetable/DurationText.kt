package com.example.timetable

fun formatDurationShort(minutes: Int, asHours: Boolean = AppPrefs.useHourDurationFormat.value): String {
    val safe = minutes.coerceAtLeast(0)
    if (!asHours || safe < 60) return "$safe мин"

    val hours = safe / 60
    val rest = safe % 60
    return if (rest == 0) "$hours ч" else "$hours ч $rest мин"
}

