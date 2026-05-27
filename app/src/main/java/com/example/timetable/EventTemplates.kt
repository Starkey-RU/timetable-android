package com.example.timetable

// шаблон события - чип в шапке редактора, по тапу заполняет название/цвет/иконку/длительность
data class EventTemplate(
    val id: String,
    val label: String,
    val colorKey: String,
    val iconKey: String,
    val defaultDurationMinutes: Int,
    val titlePrefix: String,
)

// набор быстрых пресетов - чтоб не вбивать одно и то же руками каждый раз
val DEFAULT_TEMPLATES: List<EventTemplate> = listOf(
    EventTemplate("lecture",  "Лекция",     "indigo",  "book",    90, "Лекция по "),
    EventTemplate("seminar",  "Семинар",    "violet",  "book",    90, "Семинар по "),
    EventTemplate("lunch",    "Обед",       "amber",   "food",    30, "Обед"),
    EventTemplate("sport",    "Тренировка", "emerald", "fitness", 60, "Тренировка"),
    EventTemplate("meeting",  "Встреча",    "teal",    "chat",    60, "Встреча с "),
)
