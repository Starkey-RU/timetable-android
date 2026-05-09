package com.example.timetable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Сегодня", Icons.Filled.CalendarToday),
    Week("week", "Неделя", Icons.Filled.ViewWeek),
    Schedules("schedules", "Расписания", Icons.Filled.Description),
    Settings("settings", "Настройки", Icons.Filled.Settings),
}

@Composable
fun AppScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* потом */ }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить")
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                // чтоб стек не рос если кликать туда-сюда
                                popUpTo(Tab.Today.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Tab.Today.route,
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
        ) {
            composable(Tab.Today.route) { Stub("Сегодня") }
            composable(Tab.Week.route) { Stub("Неделя") }
            composable(Tab.Schedules.route) { Stub("Расписания") }
            composable(Tab.Settings.route) { Stub("Настройки") }
        }
    }
}

@Composable
private fun Stub(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name, скоро будет", style = MaterialTheme.typography.titleMedium)
    }
}
