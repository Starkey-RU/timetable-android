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

enum class ThemeMode(val title: String) {
    Auto("Авто"),
    Light("Светлая"),
    Dark("Тёмная"),
}

// настройки внешнего вида, читаются один раз при старте, пишутся в SharedPreferences при смене
object AppPrefs {
    val palette: MutableState<Palette> = mutableStateOf(Palette.Teal)
    val gradient: MutableState<GradientPreset> = mutableStateOf(GradientPreset.WineBlack)
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        palette.value = readEnum(prefs, K_PALETTE, Palette.entries) ?: Palette.Teal
        gradient.value = readEnum(prefs, K_GRADIENT, GradientPreset.entries) ?: GradientPreset.WineBlack
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
    }

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
}
