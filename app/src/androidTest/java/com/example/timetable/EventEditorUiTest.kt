package com.example.timetable

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timetable.ui.theme.TimetableTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

// проверяем что в редакторе можно вбить название и нажать сохранить - после этого
// колбек onClose дёрнется (редактор уходит) и событие появится в базе
@RunWith(AndroidJUnit4::class)
class EventEditorUiTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var app: TimetableApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        runBlocking { app.database.clearAllTables() }
        // на всякий выключаем гостевой - в нём кнопка Сохранить заблокирована
        AppPrefs.isGuest.value = false
    }

    @Test
    fun ввод_названия_и_сохранение_закрывает_редактор() {
        val closed = AtomicBoolean(false)
        rule.setContent {
            TimetableTheme {
                EventEditorScreen(eventId = null, onClose = { closed.set(true) })
            }
        }

        // в поле Название вводим текст. поле находим по тексту лейбла
        rule.onNodeWithText("Название").performTextInput("Лекция")

        // жмём сохранить - в title только заголовок экрана и кнопка с таким же текстом,
        // нам нужна кнопка, она выводит просто слово "Сохранить"
        rule.onNodeWithText("Сохранить").performClick()

        // ждём пока корутина сохранит и дёрнет onClose
        rule.waitUntil(timeoutMillis = 3000) { closed.get() }
        assertTrue("редактор должен был закрыться после сохранения", closed.get())

        // и убеждаемся что в базе реально лежит одно событие с нашим названием
        val saved = runBlocking { app.eventRepository.observeAll().first() }
        assertEquals(1, saved.size)
        assertEquals("Лекция", saved.first().title)
    }
}
