package com.example.timetable

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Сегодня", Icons.Filled.CalendarToday),
    Week("week", "Неделя", Icons.Filled.ViewWeek),
    Schedules("schedules", "Расписания", Icons.Filled.Description),
    Settings("settings", "Настройки", Icons.Filled.Settings),
}

private const val EDITOR_ROUTE = "editor"
private const val EDITOR_ID_ARG = "id"
private const val NEW_EVENT_ID = -1L

@Composable
fun AppScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTab = Tab.entries.any { it.route == currentRoute }
    val showNavLabels by AppPrefs.showNavLabels
    val isGuest by AppPrefs.isGuest

    Scaffold(
        floatingActionButton = {
            // фаб только на табах, в редакторе он не нужен. в гостевом режиме тоже прячем - нечего создавать
            if (isTab && !isGuest) {
                FloatingActionButton(onClick = { nav.navigate("$EDITOR_ROUTE?$EDITOR_ID_ARG=$NEW_EVENT_ID") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить")
                }
            }
        },
        bottomBar = {
            if (isTab) {
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
                            label = if (showNavLabels) {
                                {
                                    // на крупном системном шрифте 'Расписания' и 'Настройки'
                                    // переносятся на 2 строки, поэтому жмём в одну с многоточием
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            } else null,
                        )
                    }
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
            composable(Tab.Today.route) {
                TodayScreen(onEventClick = { id ->
                    nav.navigate("$EDITOR_ROUTE?$EDITOR_ID_ARG=$id")
                })
            }
            composable(Tab.Week.route) {
                EmptyScreen(
                    title = "Неделя",
                    subtitle = "недельный календарь появится здесь",
                    icon = Icons.Filled.CalendarToday,
                )
            }
            composable(Tab.Schedules.route) {
                EmptyScreen(
                    title = "Расписания",
                    subtitle = "сохранённые расписания и пресеты появятся здесь",
                    icon = Icons.Filled.Description,
                )
            }
            composable(Tab.Settings.route) {
                SettingsScreen(
                    onOpenReports = { nav.navigate("reports") },
                    onOpenPinSetup = { nav.navigate("pin_setup") },
                )
            }
            composable(
                route = "$EDITOR_ROUTE?$EDITOR_ID_ARG={$EDITOR_ID_ARG}",
                arguments = listOf(
                    navArgument(EDITOR_ID_ARG) {
                        type = NavType.LongType
                        defaultValue = NEW_EVENT_ID
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong(EDITOR_ID_ARG) ?: NEW_EVENT_ID
                EventEditorScreen(
                    eventId = id.takeIf { it != NEW_EVENT_ID },
                    onClose = { nav.popBackStack() },
                )
            }
            composable("reports") {
                ReportsScreen(onClose = { nav.popBackStack() })
            }
            composable("pin_setup") {
                PinSetupScreen(
                    onDone = { nav.popBackStack() },
                    onCancel = { nav.popBackStack() },
                )
            }
        }
    }
}

