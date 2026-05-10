package com.example.timetable

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Database(entities = [EventEntity::class], version = 2, exportSchema = false)
abstract class TimetableDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        @Volatile private var instance: TimetableDatabase? = null

        fun get(context: Context): TimetableDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): TimetableDatabase {
            // первый раз заполним пачкой событий, чтоб экран не был пустой
            val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            lateinit var db: TimetableDatabase
            db = Room.databaseBuilder(
                context.applicationContext,
                TimetableDatabase::class.java,
                "timetable.db",
            )
                // схема меняется на проде редко, для дипломки норм всё пересоздать
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(con: SupportSQLiteDatabase) {
                        seedScope.launch {
                            db.eventDao().insertAll(seedToday())
                        }
                    }
                })
                .build()
            return db
        }
    }
}

private fun seedToday(): List<EventEntity> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    fun at(h: Int, m: Int): Long =
        today.atTime(LocalTime.of(h, m)).atZone(zone).toInstant().toEpochMilli()

    // прим. сетка как в макетах: будний день студента
    return listOf(
        EventEntity(title = "Алгоритмы и структуры данных", location = "Ауд. 312", colorKey = "indigo", iconKey = "book", startMillis = at(8, 30), endMillis = at(10, 0)),
        EventEntity(title = "Базы данных, семинар", location = "Ауд. 218", colorKey = "indigo", iconKey = "book", startMillis = at(10, 15), endMillis = at(11, 45)),
        EventEntity(title = "Обед", location = "Столовая", colorKey = "amber", iconKey = "food", startMillis = at(12, 30), endMillis = at(13, 0)),
        EventEntity(title = "Подработка: фронтенд", location = "Удалёнка", colorKey = "teal", iconKey = "laptop", startMillis = at(14, 0), endMillis = at(17, 0)),
        EventEntity(title = "Тренажёрный зал", location = "World Class", colorKey = "emerald", iconKey = "fitness", startMillis = at(18, 30), endMillis = at(20, 0)),
        EventEntity(title = "Английский с Анной", location = "Zoom", colorKey = "rose", iconKey = "chat", startMillis = at(21, 0), endMillis = at(22, 0)),
    )
}
