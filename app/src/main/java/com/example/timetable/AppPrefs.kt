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

    private const val FILE = "app_prefs"
    private const val K_PALETTE = "palette"
    private const val K_GRADIENT = "gradient"
    private const val K_THEME = "theme"
    private const val K_NAV_LABELS = "nav_labels"
    private const val K_GUEST = "guest"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        palette.value = readEnum(prefs, K_PALETTE, Palette.entries) ?: Palette.Teal
        gradient.value = readEnum(prefs, K_GRADIENT, GradientPreset.entries) ?: GradientPreset.WineBlack
        theme.value = readEnum(prefs, K_THEME, ThemeMode.entries) ?: ThemeMode.Auto
        showNavLabels.value = prefs.getBoolean(K_NAV_LABELS, true)
        isGuest.value = prefs.getBoolean(K_GUEST, false)

        // подписываемся на изменения и пишем назад. drop(1) чтоб не записать стартовое значение
        scope.launch { snapshotFlow { palette.value }.drop(1).collect { write(prefs, K_PALETTE, it.name) } }
        scope.launch { snapshotFlow { gradient.value }.drop(1).collect { write(prefs, K_GRADIENT, it.name) } }
        scope.launch { snapshotFlow { theme.value }.drop(1).collect { write(prefs, K_THEME, it.name) } }
        scope.launch { snapshotFlow { showNavLabels.value }.drop(1).collect { writeBool(prefs, K_NAV_LABELS, it) } }
        scope.launch { snapshotFlow { isGuest.value }.drop(1).collect { writeBool(prefs, K_GUEST, it) } }
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
}
