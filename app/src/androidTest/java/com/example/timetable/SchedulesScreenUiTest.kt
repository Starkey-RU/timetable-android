package com.example.timetable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.ui.theme.TimetableTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// проверяем что поле поиска реально фильтрует список расписаний на экране.
// кладём в базу два события, вводим запрос - остаётся одно
@RunWith(AndroidJUnit4::class)
class SchedulesScreenUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<TimetableApplication>()
        runBlocking {
            app.database.clearAllTables()
            // два разовых события - одно про математику, одно про обед
            app.eventRepository.add(
                EventEntity(
                    title = "Математика",
                    location = "ауд 312",
                    colorKey = "indigo",
                    startMillis = 1_700_000_000_000L,
                    endMillis = 1_700_000_900_000L,
                )
            )
            app.eventRepository.add(
                EventEntity(
                    title = "Обед",
                    location = "столовая",
                    colorKey = "indigo",
                    startMillis = 1_700_001_000_000L,
                    endMillis = 1_700_001_900_000L,
                )
            )
        }
    }

    @Test
    fun поиск_оставляет_только_подходящее_событие() {
        rule.setContent {
            TimetableTheme {
                SchedulesScreen()
            }
        }
        // ждём пока flow из room прочитается и обе карточки нарисуются
        rule.waitUntil(timeoutMillis = 3000) {
            rule.onAllNodes(hasText("Математика"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        rule.onNodeWithText("Математика").assertIsDisplayed()
        rule.onNodeWithText("Обед").assertIsDisplayed()

        // вбиваем в поиск часть названия - остаётся только математика
        rule.onNodeWithText("Поиск по названию или месту").performTextInput("мат")
        rule.waitForIdle()
        rule.onNodeWithText("Математика").assertIsDisplayed()
        rule.onNodeWithText("Обед").assertDoesNotExist()
    }
}
