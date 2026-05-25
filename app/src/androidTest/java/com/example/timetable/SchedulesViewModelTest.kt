package com.example.timetable

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// проверяем multi-select api у SchedulesViewModel - toggle, clearSelection, deleteSelected.
// база поднимается в памяти, vm берёт обычный репозиторий.
@RunWith(AndroidJUnit4::class)
class SchedulesViewModelTest {

    private lateinit var db: TimetableDatabase
    private lateinit var repo: EventRepository
    private lateinit var vm: SchedulesViewModel
    private val scope = CoroutineScope(Dispatchers.Default)
    private var warmup: Job? = null

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TimetableDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = EventRepository(db.eventDao(), db.archivedEventDao())
        vm = SchedulesViewModel(repo)
        // events внутри vm это stateIn с WhileSubscribed - без подписки value останется пустым.
        // держим лёгкий коллектор всё время теста, чтоб поток был активным.
        warmup = scope.launch { vm.events.collect { } }
    }

    @After
    fun tearDown() {
        warmup?.cancel()
        db.close()
    }

    @Test
    fun `toggle добавляет id в выбор`() {
        vm.toggle(1L)
        assertEquals(setOf(1L), vm.selectedIds.value)
    }

    @Test
    fun `повторный toggle убирает id из выбора`() {
        vm.toggle(5L)
        vm.toggle(7L)
        vm.toggle(5L)
        assertEquals(setOf(7L), vm.selectedIds.value)
    }

    @Test
    fun `clearSelection обнуляет выбор`() {
        vm.toggle(1L)
        vm.toggle(2L)
        vm.toggle(3L)
        vm.clearSelection()
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun `deleteSelected удаляет события из репозитория и чистит выбор`() = runTest {
        val id1 = repo.add(EventEntity(
            title = "лекция", location = "ауд 1", colorKey = "indigo",
            startMillis = 1000L, endMillis = 2000L,
        ))
        val id2 = repo.add(EventEntity(
            title = "семинар", location = "ауд 2", colorKey = "rose",
            startMillis = 3000L, endMillis = 4000L,
        ))
        val id3 = repo.add(EventEntity(
            title = "зал", location = "клуб", colorKey = "emerald",
            startMillis = 5000L, endMillis = 6000L,
        ))

        // ждём пока stateflow внутри vm подхватит все три записи из room
        var tries = 0
        while (vm.events.value.size < 3 && tries < 100) {
            delay(20)
            tries++
        }
        assertEquals(3, vm.events.value.size)

        vm.toggle(id1)
        vm.toggle(id3)
        vm.deleteSelected()

        // deleteSelected запускает корутину - ждём пока id1 пропадёт из базы
        tries = 0
        while (repo.getById(id1) != null && tries < 100) {
            delay(20)
            tries++
        }

        assertNull(repo.getById(id1))
        assertNotNull(repo.getById(id2))
        assertNull(repo.getById(id3))
        assertTrue(vm.selectedIds.value.isEmpty())
    }
}
