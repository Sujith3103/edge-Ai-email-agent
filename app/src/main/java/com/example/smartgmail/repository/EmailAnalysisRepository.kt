package com.example.smartgmail.repository

import androidx.room.withTransaction
import com.example.smartgmail.database.AppDatabase
import com.example.smartgmail.database.entity.EmailAnalysisEntity
import com.example.smartgmail.database.entity.EventEntity
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.model.EmailAnalysis
import org.json.JSONArray
import org.json.JSONObject

class EmailAnalysisRepository(
    private val database: AppDatabase
) {

    private val emailAnalysisDao = database.emailAnalysisDao()
    private val taskDao = database.taskDao()
    private val eventDao = database.eventDao()

    suspend fun saveAnalysis(analysis: EmailAnalysis) {
        database.withTransaction {
            // 1. Handle Idempotency: Delete existing generated items for this email
            taskDao.deleteTasksByEmailId(analysis.emailId)
            eventDao.deleteEventsByEmailId(analysis.emailId)

            // 2. Persist complete AI analysis
            val analysisEntity = EmailAnalysisEntity(
                emailId = analysis.emailId,
                priority = analysis.priority.name,
                summary = analysis.summary,
                actionItemsJson = serializeActionItems(analysis.actionItems),
                deadlinesJson = serializeDeadlines(analysis.deadlines),
                calendarEventsJson = serializeCalendarEvents(analysis.calendarEvents),
                analysisStatus = "COMPLETED"
            )
            emailAnalysisDao.insertAnalysis(analysisEntity)

            // 3. Normalize action items into TaskEntity
            val tasks = analysis.actionItems.map { actionItem ->
                // Intelligent deadline matching: 
                // Check if any deadline description matches this action item
                val matchedDeadline = analysis.deadlines.find { deadline ->
                    actionItem.contains(deadline.description, ignoreCase = true) ||
                            deadline.description.contains(actionItem, ignoreCase = true)
                }

                TaskEntity(
                    emailId = analysis.emailId,
                    description = actionItem,
                    dueDate = matchedDeadline?.date,
                    dueTime = matchedDeadline?.time,
                    completed = false
                )
            }
            if (tasks.isNotEmpty()) {
                taskDao.insertTasks(tasks)
            }

            // 4. Normalize calendar events into EventEntity
            val events = analysis.calendarEvents.map { calEvent ->
                EventEntity(
                    emailId = analysis.emailId,
                    title = calEvent.title,
                    date = calEvent.date,
                    startTime = calEvent.startTime,
                    endTime = calEvent.endTime,
                    location = calEvent.location,
                    description = calEvent.description
                )
            }
            if (events.isNotEmpty()) {
                eventDao.insertEvents(events)
            }
        }
    }

    suspend fun saveFailedAnalysis(emailId: String) {
        val analysisEntity = EmailAnalysisEntity(
            emailId = emailId,
            priority = "LOW",
            summary = "Analysis failed",
            actionItemsJson = "[]",
            deadlinesJson = "[]",
            calendarEventsJson = "[]",
            analysisStatus = "FAILED"
        )
        emailAnalysisDao.insertAnalysis(analysisEntity)
    }

    private fun serializeActionItems(items: List<String>): String {
        val array = JSONArray()
        items.forEach { array.put(it) }
        return array.toString()
    }

    private fun serializeDeadlines(deadlines: List<com.example.smartgmail.model.Deadline>): String {
        val array = JSONArray()
        deadlines.forEach { deadline ->
            val obj = JSONObject()
            obj.put("description", deadline.description)
            obj.put("date", deadline.date ?: JSONObject.NULL)
            obj.put("time", deadline.time ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    private fun serializeCalendarEvents(events: List<com.example.smartgmail.model.CalendarEvent>): String {
        val array = JSONArray()
        events.forEach { event ->
            val obj = JSONObject()
            obj.put("title", event.title)
            obj.put("date", event.date ?: JSONObject.NULL)
            obj.put("startTime", event.startTime ?: JSONObject.NULL)
            obj.put("endTime", event.endTime ?: JSONObject.NULL)
            obj.put("location", event.location ?: JSONObject.NULL)
            obj.put("description", event.description ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }
}
