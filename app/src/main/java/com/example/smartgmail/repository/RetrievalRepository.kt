package com.example.smartgmail.repository

import android.util.Log
import com.example.smartgmail.ai.LocalLLM
import com.example.smartgmail.database.dao.EmailDao
import com.example.smartgmail.database.dao.EventDao
import com.example.smartgmail.database.dao.TaskDao
import org.json.JSONArray
import java.time.LocalDate

class RetrievalRepository(
    private val emailDao: EmailDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao
) {

    suspend fun getRelevantContext(query: String, llm: LocalLLM): String {
        Log.d("Retrieval", "Starting context retrieval for: $query")
        llm.resetContext()

        val contextBuilder = StringBuilder("\n--- DATABASE CONTEXT ---\n")
        var foundAny = false

        val lowerQuery = query.lowercase()

        // 1. Broad Fetch for Temporal Queries
        if (lowerQuery.contains("deadline") || lowerQuery.contains("task") || lowerQuery.contains("next") || lowerQuery.contains("upcoming") || lowerQuery.contains("todo")) {
            val tasks = taskDao.getUpcomingTasks(10)
            if (tasks.isNotEmpty()) {
                foundAny = true
                contextBuilder.append("UPCOMING TASKS & DEADLINES:\n")
                tasks.forEach { 
                    contextBuilder.append("- ${it.description} (Due: ${it.dueDate ?: "No date"} ${it.dueTime ?: ""})\n")
                }
            }
        }

        if (lowerQuery.contains("event") || lowerQuery.contains("meeting") || lowerQuery.contains("calendar") || lowerQuery.contains("schedule")) {
            val events = eventDao.getUpcomingEvents(10)
            if (events.isNotEmpty()) {
                foundAny = true
                contextBuilder.append("UPCOMING CALENDAR EVENTS:\n")
                events.forEach {
                    contextBuilder.append("- ${it.title} (Date: ${it.date}, Time: ${it.startTime}-${it.endTime ?: ""}, Loc: ${it.location ?: "N/A"})\n")
                }
            }
        }

        // 2. Keyword Extraction & Targeted Search
        val extractorPrompt = """
            Identify 2-3 specific search keywords from the user's query. 
            Focus on proper nouns, projects, or specific topics.
            
            QUERY: "$query"
            
            Return ONLY a JSON array of strings.
            Example: ["inspection", "building", "maintenance"]
        """.trimIndent()

        var extractorResponse = ""
        val keywords = mutableSetOf<String>()
        
        try {
            llm.generate(extractorPrompt, 64).collect { extractorResponse += it }
            val extracted = try {
                val arr = JSONArray(extractJsonArray(extractorResponse))
                List(arr.length()) { i -> arr.getString(i) }
            } catch (e: Exception) { emptyList() }
            keywords.addAll(extracted)
        } catch (e: Exception) {
            Log.e("Retrieval", "Keyword extraction failed", e)
        }

        // 3. Fallback: Split query if no keywords extracted
        if (keywords.isEmpty()) {
            val words = query.split(" ")
                .filter { it.length > 4 } // Only search for meaningful words
                .map { it.replace(Regex("[^a-zA-Z0-9]"), "") }
                .filter { it.isNotBlank() }
            keywords.addAll(words)
        }

        // 4. Execute Search
        if (keywords.isNotEmpty()) {
            Log.d("Retrieval", "Searching database with keywords: $keywords")
            
            keywords.forEach { keyword ->
                if (keyword.length < 3) return@forEach

                // Target Emails
                val emails = emailDao.searchEmails(keyword, 3)
                emails.forEach { email ->
                    foundAny = true
                    contextBuilder.append("EMAIL [ID: ${email.id}] Subject: ${email.subject}\n")
                    contextBuilder.append("From: ${email.sender}\n")
                    contextBuilder.append("Summary: ${email.summary ?: "N/A"}\n")
                    contextBuilder.append("Content Snippet: ${email.body.take(200)}...\n")
                    contextBuilder.append("---\n")
                }
                
                // Target Tasks
                val tasks = taskDao.searchTasks(keyword, 3)
                tasks.forEach { task ->
                    foundAny = true
                    contextBuilder.append("TASK: ${task.description} (Due: ${task.dueDate ?: "N/A"})\n")
                }
                
                // Target Events
                val events = eventDao.searchEvents(keyword, 3)
                events.forEach { event ->
                    foundAny = true
                    contextBuilder.append("EVENT: ${event.title} (Date: ${event.date ?: "N/A"})\n")
                }
            }
        }

        val finalContext = if (foundAny) contextBuilder.toString() else ""
        Log.d("Retrieval", "Retrieval finished. Context found: $foundAny")
        return finalContext
    }

    private fun extractJsonArray(text: String): String {
        val start = text.indexOf("[")
        val end = text.lastIndexOf("]")
        return if (start != -1 && end != -1) text.substring(start, end + 1) else "[]"
    }
}
