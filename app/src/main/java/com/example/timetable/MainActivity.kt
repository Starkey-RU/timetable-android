package com.example.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.timetable.ui.theme.TimetableTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val palette by AppPrefs.palette
            val theme by AppPrefs.theme
            val dynamic by AppPrefs.useDynamicColors
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (theme) {
                ThemeMode.Auto -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val windowSize = calculateWindowSizeClass(this)
            TimetableTheme(palette = palette, darkTheme = darkTheme, dynamic = dynamic) {
                // если включён пин, до разблокировки показываем экран ввода.
                // rememberSaveable чтоб при складывании foldable activity не теряла unlock
                var unlocked by rememberSaveable { mutableStateOf(!PinManager.isPinSet()) }
                if (unlocked) {
                    AppScaffold(widthSize = windowSize.widthSizeClass)
                } else {
                    PinLockScreen(onUnlock = { unlocked = true })
                }
            }
        }
    }
}
