package com.example.timetable

import android.app.Application

class TimetableApplication : Application() {

    val database by lazy { TimetableDatabase.get(this) }
    val eventRepository by lazy { EventRepository(database.eventDao()) }

    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        PinManager.init(this)
    }
}
