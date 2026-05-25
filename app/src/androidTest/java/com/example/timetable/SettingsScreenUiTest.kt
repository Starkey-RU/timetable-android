package com.example.timetable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.ui.theme.TimetableTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// проверяем что тоггл "Гостевой режим" в настройках переключает AppPrefs.isGuest.
// дальше уже FAB на сегодня прячется именно по этому флагу - то есть прямая связь
@RunWith(AndroidJUnit4::class)
class SettingsScreenUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // стартуем с выключенного гостя чтоб точно знать что тоггл изменил состояние
        AppPrefs.isGuest.value = false
    }

    @After
    fun tearDown() {
        // не оставляем мусор после теста - другие тесты могут наткнуться
        AppPrefs.isGuest.value = false
    }

    @Test
    fun тоггл_гостевого_режима_меняет_флаг() {
        rule.setContent {
            TimetableTheme {
                SettingsScreen()
            }
        }
        // экран длинный, тоггл может быть ниже скролла
        rule.onNodeWithText("Гостевой режим").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Гостевой режим").performClick()

        rule.waitForIdle()
        assertTrue("после клика по тогглу гостевой режим должен включиться", AppPrefs.isGuest.value)
    }
}
