package com.example.timetable

import kotlinx.coroutines.flow.Flow

class EventRepository(private val dao: EventDao) {

    fun observeAll(): Flow<List<EventEntity>> = dao.observeAll()

    fun observeInRange(fromMillis: Long, toMillis: Long): Flow<List<EventEntity>> =
        dao.observeInRange(fromMillis, toMillis)

    suspend fun add(event: EventEntity): Long = dao.insert(event)

    suspend fun update(event: EventEntity) = dao.update(event)

    suspend fun delete(event: EventEntity) = dao.delete(event)
}
