package com.example.timetable

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// разбирает входной .ics (rfc 5545, упрощённо) на список событий.
// поддерживаем: DTSTART/DTEND (utc-zulu и локальное), SUMMARY, LOCATION,
// RRULE с FREQ=WEEKLY + BYDAY + INTERVAL (для чёт-нечёт).
// то что не понимаем - молча игнорируем, лишь бы не ронять импорт.
object IcsImport {

    // обрабатываем оба формата, что чаще встречаются
    private val utcStamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    private val localStamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    private val dayMap = mapOf(
        "MO" to WeekDays.MON, "TU" to WeekDays.TUE, "WE" to WeekDays.WED,
        "TH" to WeekDays.THU, "FR" to WeekDays.FRI, "SA" to WeekDays.SAT,
        "SU" to WeekDays.SUN,
    )

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): List<EventEntity> {
        // в файле могут быть folded строки (rfc 5545 §3.1) - переносы с пробелом в начале.
        // нормализуем: соединяем строки, начинающиеся с пробела/таба, к предыдущей.
        val unfolded = unfold(text)
        val result = mutableListOf<EventEntity>()

        var inEvent = false
        var title = ""
        var location = ""
        var start: Long? = null
        var end: Long? = null
        var mask = 0
        var parity = 0

        unfolded.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line == "BEGIN:VEVENT" -> {
                    inEvent = true
                    title = ""
                    location = ""
                    start = null
                    end = null
                    mask = 0
                    parity = 0
                }
                line == "END:VEVENT" -> {
                    // событие считается валидным если есть хотя бы start и название
                    val s = start
                    if (inEvent && s != null && title.isNotBlank()) {
                        result.add(EventEntity(
                            title = title,
                            location = location,
                            colorKey = "indigo", // дефолт - импортированные обычно без цвета
                            iconKey = "event",
                            startMillis = s,
                            endMillis = end ?: (s + 60 * 60_000L),
                            recurrenceMask = mask,
                            weekParity = parity,
                        ))
                    }
                    inEvent = false
                }
                inEvent && line.startsWith("DTSTART") -> {
                    start = parseDate(valueOf(line), zone)
                }
                inEvent && line.startsWith("DTEND") -> {
                    end = parseDate(valueOf(line), zone)
                }
                inEvent && line.startsWith("SUMMARY:") -> {
                    title = unescape(line.removePrefix("SUMMARY:"))
                }
                inEvent && line.startsWith("LOCATION:") -> {
                    location = unescape(line.removePrefix("LOCATION:"))
                }
                inEvent && line.startsWith("RRULE:") -> {
                    val (m, p) = parseRrule(line.removePrefix("RRULE:"))
                    mask = m
                    parity = p
                }
            }
        }
        return result
    }

    private fun unfold(text: String): String {
        // объединяем строки: следующая строка начинается с пробела или таба - значит продолжение
        val sb = StringBuilder()
        text.split(Regex("\r?\n")).forEachIndexed { i, line ->
            if (i > 0 && (line.startsWith(" ") || line.startsWith("\t"))) {
                sb.append(line.substring(1))
            } else {
                if (i > 0) sb.append('\n')
                sb.append(line)
            }
        }
        return sb.toString()
    }

    // отрезает параметры до двоеточия: "DTSTART;TZID=Europe/Moscow:20260524T100000" -> "20260524T100000"
    private fun valueOf(line: String): String {
        val idx = line.indexOf(':')
        return if (idx >= 0) line.substring(idx + 1) else line
    }

    private fun parseDate(raw: String, zone: ZoneId): Long? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        return runCatching {
            if (s.endsWith("Z")) {
                utcStamp.parse(s, java.time.Instant::from).toEpochMilli()
            } else {
                LocalDateTime.parse(s, localStamp).atZone(zone).toInstant().toEpochMilli()
            }
        }.getOrNull()
    }

    private fun parseRrule(rule: String): Pair<Int, Int> {
        var mask = 0
        var parity = 0
        rule.split(";").forEach { part ->
            val (k, v) = part.split("=", limit = 2).let {
                if (it.size == 2) it[0] to it[1] else return@forEach
            }
            when (k.uppercase()) {
                "BYDAY" -> v.split(",").forEach { d ->
                    dayMap[d.takeLast(2).uppercase()]?.let { bit -> mask = mask or bit }
                }
                "INTERVAL" -> {
                    // INTERVAL=2 интерпретируем как «чётные недели» - такой же упрощение как в export
                    if (v.trim() == "2") parity = WeekParity.EVEN
                }
            }
        }
        return mask to parity
    }

    // обратно к escape в IcsExport
    private fun unescape(text: String): String = text
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
