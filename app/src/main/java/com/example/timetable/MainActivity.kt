package com.example.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.timetable.ui.theme.TimetableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val palette by AppPrefs.palette
            val theme by AppPrefs.theme
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (theme) {
                ThemeMode.Auto -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            TimetableTheme(palette = palette, darkTheme = darkTheme) {
                // если включён пин, до разблокировки показываем экран ввода
                var unlocked by remember { mutableStateOf(!PinManager.isPinSet()) }
                if (unlocked) {
                    AppScaffold()
                } else {
                    PinLockScreen(onUnlock = { unlocked = true })
                }
            }
        }
    }
}
