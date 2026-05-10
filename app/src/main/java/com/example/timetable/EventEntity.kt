package com.example.timetable

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String,
    val colorKey: String,
    val iconKey: String = "event",
    val startMillis: Long,
    val endMillis: Long,
)
