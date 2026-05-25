package com.example.timetable

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventRepositoryTest {

    private lateinit var db: TimetableDatabase
    private lateinit var repo: EventRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TimetableDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = EventRepository(db.eventDao(), db.archivedEventDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        // TODO observeInRange на пересечение интервалов, deleteById, seedTestData ожидаемо >= 100
        val id = repo.add(
            EventEntity(
                title = "лекция", location = "ауд 1", colorKey = "indigo",
                startMillis = 1000L, endMillis = 2000L,
            )
        )
        val got = repo.getById(id)
        assertNotNull(got)
        assertEquals("лекция", got!!.title)
    }
}
