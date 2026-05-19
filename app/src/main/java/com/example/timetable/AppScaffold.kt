package com.example.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
fun AppScaffold(widthSize: WindowWidthSizeClass = WindowWidthSizeClass.Compact) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTab = Tab.entries.any { it.route == currentRoute }
    val showNavLabels by AppPrefs.showNavLabels
    val isGuest by AppPrefs.isGuest

    // на широком экране (планшет / разложенный foldable) показываем сегодня + редактор рядом.
    // -1L означает "панель закрыта", -2L "новое событие" (просто чтоб не плодить sealed class)
    val isWide = widthSize == WindowWidthSizeClass.Expanded
    var twoPaneSelected by rememberSaveable { mutableStateOf<Long>(-1L) }
    val openInPane: (Long) -> Unit = { id -> twoPaneSelected = id }

    Scaffold(
        floatingActionButton = {
            // фаб только на табах, в редакторе он не нужен. в гостевом режиме тоже прячем - нечего создавать
            if (isTab && !isGuest) {
                FloatingActionButton(onClick = {
                    if (isWide && currentRoute == Tab.Today.route) {
                        // в двухпанельном виде открываем новое событие справа, без отдельного экрана
                        twoPaneSelected = -2L
                    } else {
                        nav.navigate("$EDITOR_ROUTE?$EDITOR_ID_ARG=$NEW_EVENT_ID")
                    }
                }) {
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
                if (isWide) {
                    TodayTwoPane(
                        selected = twoPaneSelected,
                        onSelect = openInPane,
                        onClose = { twoPaneSelected = -1L },
                    )
                } else {
                    TodayScreen(onEventClick = { id ->
                        nav.navigate("$EDITOR_ROUTE?$EDITOR_ID_ARG=$id")
                    })
                }
            }
            composable(Tab.Week.route) {
                WeekScreen()
            }
            composable(Tab.Schedules.route) {
                SchedulesScreen(onEventClick = { id ->
                    nav.navigate("$EDITOR_ROUTE?$EDITOR_ID_ARG=$id")
                })
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

// двухпанельный режим "сегодня": список слева, редактор справа.
// видно только на широком экране - планшет либо разложенный foldable.
@Composable
private fun TodayTwoPane(
    selected: Long,
    onSelect: (Long) -> Unit,
    onClose: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TodayScreen(onEventClick = onSelect)
        }
        VerticalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (selected) {
                -1L -> TwoPanePlaceholder()
                -2L -> EventEditorScreen(eventId = null, onClose = onClose)
                else -> EventEditorScreen(eventId = selected, onClose = onClose)
            }
        }
    }
}

@Composable
private fun TwoPanePlaceholder() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Выбери событие слева или добавь новое",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

