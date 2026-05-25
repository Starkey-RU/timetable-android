package com.example.timetable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.ui.theme.TimetableTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// крч проверяем что когда событий нет - на экране сегодня видна плашка-заглушка
@RunWith(AndroidJUnit4::class)
class TodayScreenUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // чтоб старые тестовые данные не светились - чистим базу перед каждым тестом
        val app = ApplicationProvider.getApplicationContext<TimetableApplication>()
        runBlocking { app.database.clearAllTables() }
    }

    @Test
    fun пустой_стейт_показывает_плашку() {
        rule.setContent {
            TimetableTheme {
                TodayScreen()
            }
        }
        // ждём пока flow из room прочитает пустой список и compose всё перерисует
        rule.waitForIdle()
        rule.onNodeWithText("Сегодня событий нет").assertIsDisplayed()
    }
}
