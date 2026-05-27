package com.example.timetable

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Database(entities = [EventEntity::class, ArchivedEventEntity::class], version = 5, exportSchema = false)
abstract class TimetableDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    abstract fun archivedEventDao(): ArchivedEventDao

    companion object {
        // имя файла бд - вынесли в константу, чтоб переиспользовать в бэкапе
        internal const val NAME = "timetable.db"

        @Volatile private var instance: TimetableDatabase? = null

        fun get(context: Context): TimetableDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): TimetableDatabase {
            // первый раз заполним пачкой событий, чтоб экран не был пустой
            val seedScope = CoroutineScope(Dispatchers.IO)
            lateinit var db: TimetableDatabase
            db = Room.databaseBuilder(
                context.applicationContext,
                TimetableDatabase::class.java,
                NAME,
            )
                // на учебном проекте схема ещё крутится, миграции пока не пишем
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

    // примерный микс: работа + личное, чтоб приложение не выглядело только-учебным
    return listOf(
        EventEntity(title = "Стендап", location = "Zoom", colorKey = "indigo", iconKey = "chat", startMillis = at(10, 0), endMillis = at(10, 30)),
        EventEntity(title = "Лекция по матанализу", location = "Ауд. 312", colorKey = "teal", iconKey = "book", startMillis = at(11, 0), endMillis = at(12, 30)),
        EventEntity(title = "Обед", location = "Кафе у дома", colorKey = "amber", iconKey = "food", startMillis = at(13, 0), endMillis = at(13, 45)),
        EventEntity(title = "Дедлайн отчёта", location = "Дома", colorKey = "coral", iconKey = "event", startMillis = at(15, 0), endMillis = at(17, 0)),
        EventEntity(title = "Зал", location = "World Class", colorKey = "emerald", iconKey = "fitness", startMillis = at(19, 0), endMillis = at(20, 30)),
        EventEntity(title = "Звонок маме", location = "Дома", colorKey = "rose", iconKey = "chat", startMillis = at(21, 0), endMillis = at(21, 20)),
    )
}
