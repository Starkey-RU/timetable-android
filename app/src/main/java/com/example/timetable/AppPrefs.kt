package com.example.timetable

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ThemeMode(val title: String) {
    Auto("Авто"),
    Light("Светлая"),
    Dark("Тёмная"),
}

// настройки внешнего вида, читаются один раз при старте, пишутся в SharedPreferences при смене
object AppPrefs {
    val palette: MutableState<Palette> = mutableStateOf(Palette.Teal)
    val gradient: MutableState<AppGradient> = mutableStateOf(AppGradient.WineBlack)
    val theme: MutableState<ThemeMode> = mutableStateOf(ThemeMode.Auto)
    val showNavLabels: MutableState<Boolean> = mutableStateOf(true)
    val isGuest: MutableState<Boolean> = mutableStateOf(false)
    val notificationsEnabled: MutableState<Boolean> = mutableStateOf(false)

    // 0 = не удалять, иначе через сколько дней после конца события его можно стереть
    val autoDeleteDays: MutableState<Int> = mutableStateOf(0)

    // на широком экране (планшет/foldable) показывать nav слева вертикально
    val useSideRail: MutableState<Boolean> = mutableStateOf(true)

    // цветной градиентный заголовок на экране сегодня
    val showGradientHeader: MutableState<Boolean> = mutableStateOf(true)

    // настройки для широких экранов / foldable.
    // боковое меню по умолчанию слева, можно перекинуть направо.
    val navRailOnRight: MutableState<Boolean> = mutableStateOf(false)
    // в двухпанельном виде по умолчанию список слева, редактор справа - можно поменять
    val swapTwoPanePanels: MutableState<Boolean> = mutableStateOf(false)
    // пункты бокового меню прижать к центру по вертикали (по умолчанию сверху)
    val navRailCentered: MutableState<Boolean> = mutableStateOf(false)

    // сворачивать секцию "завершено" на экране сегодня по умолчанию
    val collapseDoneByDefault: MutableState<Boolean> = mutableStateOf(true)

    // дата начала семестра в миллисекундах (полночь локали). null = не задана.
    val semesterStart: MutableState<Long?> = mutableStateOf(null)

    // если включено и semesterStart задан - считаем чёт/нечёт недели от семестра, иначе ISO
    val useSemesterWeeks: MutableState<Boolean> = mutableStateOf(false)

    // длительность нового события по умолчанию для каждого типа (минуты)
    val durationsByIcon: MutableState<Map<String, Int>> = mutableStateOf(defaultDurations())

    // короткая вибрация при сохранении и удалении события
    val hapticsEnabled: MutableState<Boolean> = mutableStateOf(true)

    // длительности можно показывать как "190 мин" или как "3 ч 10 мин"
    val useHourDurationFormat: MutableState<Boolean> = mutableStateOf(false)

    // за сколько минут до начала события показывать уведомление
    val reminderLeadMinutes: MutableState<Int> = mutableStateOf(10)

    // после "Дублировать" сразу открывать редактор копии (а не оставаться на сегодня)
    val duplicateOpensEditor: MutableState<Boolean> = mutableStateOf(false)

    // динамические цвета из обоев (material you), доступно с android 12
    val useDynamicColors: MutableState<Boolean> = mutableStateOf(false)

    // тап по карточке открывает шторку с деталями (если выкл - сразу редактор как раньше)
    val useEventDetailSheet: MutableState<Boolean> = mutableStateOf(false)

    // режим учёбы: показывает поля преподаватель/аудитория/номер пары. выкл - они скрыты в редакторе
    val studyMode: MutableState<Boolean> = mutableStateOf(true)

    // при смене времени начала автоматом сдвигать конец на +1 час
    val autoExtendEndTime: MutableState<Boolean> = mutableStateOf(true)

    // пользователь скрыл подсказку про события "сквозь день" навсегда
    val crossDayHintDismissed: MutableState<Boolean> = mutableStateOf(false)

    // фон режима фокуса
    val focusGradient: MutableState<AppGradient> = mutableStateOf(AppGradient.Amoled)

    // показывать кнопку фокус-режима в топбаре только когда nav вертикальный (т.е. на широких скрыть)
    val focusButtonCompactOnly: MutableState<Boolean> = mutableStateOf(false)

    // не давать экрану гаснуть пока открыт фокус-режим
    val focusKeepScreenOn: MutableState<Boolean> = mutableStateOf(true)

    // сколько событий показывать на виджете today (диапазон 1..5)
    val widgetEventLimit: MutableState<Int> = mutableStateOf(3)

    // используется в expandRecurrence: null если режим выключен или дата не задана
    fun effectiveSemesterStart(zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
        if (!useSemesterWeeks.value) return null
        val millis = semesterStart.value ?: return null
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }

    private fun defaultDurations(): Map<String, Int> = mapOf(
        "event" to 60,
        "book" to 90,
        "food" to 30,
        "laptop" to 60,
        "fitness" to 60,
        "chat" to 30,
    )

    private const val FILE = "app_prefs"
    private const val K_PALETTE = "palette"
    private const val K_GRADIENT = "gradient"
    private const val K_THEME = "theme"
    private const val K_NAV_LABELS = "nav_labels"
    private const val K_GUEST = "guest"
    private const val K_NOTIF = "notifications"
    private const val K_AUTODEL = "auto_delete_days"
    private const val K_SIDE_RAIL = "side_rail"
    private const val K_GRAD_HDR = "gradient_header"
    private const val K_RAIL_RIGHT = "rail_right"
    private const val K_SWAP_PANES = "swap_panes"
    private const val K_RAIL_CENTER = "rail_center"
    private const val K_COLLAPSE_DONE = "collapse_done"
    private const val K_SEMESTER_START = "semester_start"
    private const val K_USE_SEMESTER_WEEKS = "use_semester_weeks"
    private const val K_DURATIONS = "durations_by_icon"
    private const val K_HAPTICS = "haptics_enabled"
    private const val K_DURATION_HOURS = "duration_hours_format"
    private const val K_REMINDER_LEAD = "reminder_lead_min"
    private const val K_DUP_OPEN_EDITOR = "duplicate_open_editor"
    private const val K_DYNAMIC_COLORS = "dynamic_colors"
    private const val K_EVENT_DETAIL_SHEET = "event_detail_sheet"
    private const val K_STUDY_MODE = "study_mode"
    private const val K_AUTO_EXTEND_END = "auto_extend_end"
    private const val K_CROSS_DAY_HINT_DISMISSED = "cross_day_hint_dismissed"
    private const val K_FOCUS_GRADIENT = "focus_gradient"
    private const val K_FOCUS_BTN_COMPACT_ONLY = "focus_btn_compact_only"
    private const val K_FOCUS_KEEP_SCREEN_ON = "focus_keep_screen_on"
    private const val K_WIDGET_EVENT_LIMIT = "widget_event_limit"
    private const val NO_SEMESTER = Long.MIN_VALUE

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        palette.value = readEnum(prefs, K_PALETTE, Palette.entries) ?: Palette.Teal
        gradient.value = readEnum(prefs, K_GRADIENT, AppGradient.entries) ?: AppGradient.WineBlack
        theme.value = readEnum(prefs, K_THEME, ThemeMode.entries) ?: ThemeMode.Auto
        showNavLabels.value = prefs.getBoolean(K_NAV_LABELS, true)
        isGuest.value = prefs.getBoolean(K_GUEST, false)
        notificationsEnabled.value = prefs.getBoolean(K_NOTIF, false)
        autoDeleteDays.value = prefs.getInt(K_AUTODEL, 0)
        useSideRail.value = prefs.getBoolean(K_SIDE_RAIL, true)
        showGradientHeader.value = prefs.getBoolean(K_GRAD_HDR, true)
        navRailOnRight.value = prefs.getBoolean(K_RAIL_RIGHT, false)
        swapTwoPanePanels.value = prefs.getBoolean(K_SWAP_PANES, false)
        navRailCentered.value = prefs.getBoolean(K_RAIL_CENTER, false)
        collapseDoneByDefault.value = prefs.getBoolean(K_COLLAPSE_DONE, true)
        val storedSemester = prefs.getLong(K_SEMESTER_START, NO_SEMESTER)
        semesterStart.value = if (storedSemester == NO_SEMESTER) null else storedSemester
        useSemesterWeeks.value = prefs.getBoolean(K_USE_SEMESTER_WEEKS, false)
        durationsByIcon.value = parseDurations(prefs.getString(K_DURATIONS, null))
        hapticsEnabled.value = prefs.getBoolean(K_HAPTICS, true)
        useHourDurationFormat.value = prefs.getBoolean(K_DURATION_HOURS, false)
        reminderLeadMinutes.value = prefs.getInt(K_REMINDER_LEAD, 10)
        duplicateOpensEditor.value = prefs.getBoolean(K_DUP_OPEN_EDITOR, false)
        useDynamicColors.value = prefs.getBoolean(K_DYNAMIC_COLORS, false)
        useEventDetailSheet.value = prefs.getBoolean(K_EVENT_DETAIL_SHEET, false)
        studyMode.value = prefs.getBoolean(K_STUDY_MODE, true)
        autoExtendEndTime.value = prefs.getBoolean(K_AUTO_EXTEND_END, true)
        crossDayHintDismissed.value = prefs.getBoolean(K_CROSS_DAY_HINT_DISMISSED, false)
        focusGradient.value = readEnum(prefs, K_FOCUS_GRADIENT, AppGradient.entries) ?: AppGradient.Amoled
        focusButtonCompactOnly.value = prefs.getBoolean(K_FOCUS_BTN_COMPACT_ONLY, false)
        focusKeepScreenOn.value = prefs.getBoolean(K_FOCUS_KEEP_SCREEN_ON, true)
        widgetEventLimit.value = prefs.getInt(K_WIDGET_EVENT_LIMIT, 3).coerceIn(1, 5)

        // подписываемся на изменения и пишем назад. drop(1) чтоб не записать стартовое значение
        scope.launch { snapshotFlow { palette.value }.drop(1).collect { write(prefs, K_PALETTE, it.name) } }
        scope.launch { snapshotFlow { gradient.value }.drop(1).collect { write(prefs, K_GRADIENT, it.name) } }
        scope.launch { snapshotFlow { theme.value }.drop(1).collect { write(prefs, K_THEME, it.name) } }
        scope.launch { snapshotFlow { showNavLabels.value }.drop(1).collect { writeBool(prefs, K_NAV_LABELS, it) } }
        scope.launch { snapshotFlow { isGuest.value }.drop(1).collect { writeBool(prefs, K_GUEST, it) } }
        scope.launch { snapshotFlow { notificationsEnabled.value }.drop(1).collect { writeBool(prefs, K_NOTIF, it) } }
        scope.launch { snapshotFlow { autoDeleteDays.value }.drop(1).collect { writeInt(prefs, K_AUTODEL, it) } }
        scope.launch { snapshotFlow { useSideRail.value }.drop(1).collect { writeBool(prefs, K_SIDE_RAIL, it) } }
        scope.launch { snapshotFlow { showGradientHeader.value }.drop(1).collect { writeBool(prefs, K_GRAD_HDR, it) } }
        scope.launch { snapshotFlow { navRailOnRight.value }.drop(1).collect { writeBool(prefs, K_RAIL_RIGHT, it) } }
        scope.launch { snapshotFlow { swapTwoPanePanels.value }.drop(1).collect { writeBool(prefs, K_SWAP_PANES, it) } }
        scope.launch { snapshotFlow { navRailCentered.value }.drop(1).collect { writeBool(prefs, K_RAIL_CENTER, it) } }
        scope.launch { snapshotFlow { collapseDoneByDefault.value }.drop(1).collect { writeBool(prefs, K_COLLAPSE_DONE, it) } }
        scope.launch { snapshotFlow { semesterStart.value }.drop(1).collect { writeLong(prefs, K_SEMESTER_START, it ?: NO_SEMESTER) } }
        scope.launch { snapshotFlow { useSemesterWeeks.value }.drop(1).collect { writeBool(prefs, K_USE_SEMESTER_WEEKS, it) } }
        scope.launch { snapshotFlow { durationsByIcon.value }.drop(1).collect { write(prefs, K_DURATIONS, encodeDurations(it)) } }
        scope.launch { snapshotFlow { hapticsEnabled.value }.drop(1).collect { writeBool(prefs, K_HAPTICS, it) } }
        scope.launch { snapshotFlow { useHourDurationFormat.value }.drop(1).collect { writeBool(prefs, K_DURATION_HOURS, it) } }
        scope.launch { snapshotFlow { reminderLeadMinutes.value }.drop(1).collect { writeInt(prefs, K_REMINDER_LEAD, it) } }
        scope.launch { snapshotFlow { duplicateOpensEditor.value }.drop(1).collect { writeBool(prefs, K_DUP_OPEN_EDITOR, it) } }
        scope.launch { snapshotFlow { useDynamicColors.value }.drop(1).collect { writeBool(prefs, K_DYNAMIC_COLORS, it) } }
        scope.launch { snapshotFlow { useEventDetailSheet.value }.drop(1).collect { writeBool(prefs, K_EVENT_DETAIL_SHEET, it) } }
        scope.launch { snapshotFlow { studyMode.value }.drop(1).collect { writeBool(prefs, K_STUDY_MODE, it) } }
        scope.launch { snapshotFlow { autoExtendEndTime.value }.drop(1).collect { writeBool(prefs, K_AUTO_EXTEND_END, it) } }
        scope.launch { snapshotFlow { crossDayHintDismissed.value }.drop(1).collect { writeBool(prefs, K_CROSS_DAY_HINT_DISMISSED, it) } }
        scope.launch { snapshotFlow { focusGradient.value }.drop(1).collect { write(prefs, K_FOCUS_GRADIENT, it.name) } }
        scope.launch { snapshotFlow { focusButtonCompactOnly.value }.drop(1).collect { writeBool(prefs, K_FOCUS_BTN_COMPACT_ONLY, it) } }
        scope.launch { snapshotFlow { focusKeepScreenOn.value }.drop(1).collect { writeBool(prefs, K_FOCUS_KEEP_SCREEN_ON, it) } }
        scope.launch { snapshotFlow { widgetEventLimit.value }.drop(1).collect { writeInt(prefs, K_WIDGET_EVENT_LIMIT, it) } }
    }

    // формат "ключ:минуты,..." - читаем терпимо, всё что не парсится пропускаем
    private fun parseDurations(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return defaultDurations()
        val out = defaultDurations().toMutableMap()
        raw.split(',').forEach { token ->
            val parts = token.split(':')
            if (parts.size == 2) {
                val key = parts[0].trim()
                val minutes = parts[1].trim().toIntOrNull()
                if (key.isNotEmpty() && minutes != null && minutes > 0) {
                    out[key] = minutes
                }
            }
        }
        return out
    }

    private fun encodeDurations(map: Map<String, Int>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun <E : Enum<E>> readEnum(prefs: SharedPreferences, key: String, values: List<E>): E? {
        val name = prefs.getString(key, null) ?: return null
        return values.firstOrNull { it.name == name }
    }

    private fun write(prefs: SharedPreferences, key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private fun writeBool(prefs: SharedPreferences, key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun writeInt(prefs: SharedPreferences, key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    private fun writeLong(prefs: SharedPreferences, key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
}
