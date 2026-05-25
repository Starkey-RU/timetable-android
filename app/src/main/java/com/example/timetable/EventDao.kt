package com.example.timetable

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY startMillis")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startMillis < :toMillis AND endMillis > :fromMillis ORDER BY startMillis")
    fun observeInRange(fromMillis: Long, toMillis: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)

    // удаляем только разовые (recurrenceMask = 0), повторяющиеся не трогаем
    @Query("DELETE FROM events WHERE recurrenceMask = 0 AND endMillis < :cutoffMillis")
    suspend fun deleteSinglePastBefore(cutoffMillis: Long): Int

    // полная очистка таблицы - используется при архивации семестра
    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
