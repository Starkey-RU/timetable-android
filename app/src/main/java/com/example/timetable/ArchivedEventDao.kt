package com.example.timetable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivedEventDao {

    // сначала свежие архивы, внутри одного архива - по времени события
    @Query("SELECT * FROM archived_events ORDER BY archivedAt DESC, startMillis DESC")
    fun observeAll(): Flow<List<ArchivedEventEntity>>

    @Insert
    suspend fun insertAll(items: List<ArchivedEventEntity>)

    @Query("DELETE FROM archived_events")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM archived_events")
    suspend fun count(): Int

    @Query("DELETE FROM archived_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
