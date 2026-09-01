package com.example.smartgmail.model

data class EmailAnalysis(
    val emailId: String,

    val priority: Priority,

    val summary: String,

    val actionItems: List<String>,

    val deadlines: List<Deadline>,

    val calendarEvents: List<CalendarEvent>
)

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

data class Deadline(
    val description: String,
    val date: String?,
    val time: String?
)

data class CalendarEvent(
    val title: String,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val description: String?
)