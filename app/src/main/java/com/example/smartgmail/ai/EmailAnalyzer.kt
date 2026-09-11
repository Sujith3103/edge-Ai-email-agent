package com.example.smartgmail.ai

import android.util.Log
import com.example.smartgmail.context.EmailContextBuilder
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.model.CalendarEvent
import com.example.smartgmail.model.Deadline
import com.example.smartgmail.model.Email
import com.example.smartgmail.model.EmailAnalysis
import com.example.smartgmail.model.KnowledgeResult
import com.example.smartgmail.model.Priority
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject

class EmailAnalyzer(
    private val localLLM: LocalLLM
) {

    companion object {
        private const val KNOWLEDGE_SYSTEM_PROMPT = """
You are SmartGmail's knowledge extraction engine.

Your job is to extract durable information from an email that may be
useful for understanding future emails, conversations, tasks, projects,
people, or ongoing situations.

Return ONLY valid JSON.

Use ONLY information explicitly present in the email.
Never invent, assume, infer, or guess information.

==================================================
1. KNOWLEDGE RELEVANCE
==================================================

Set knowledgeRelevant = true ONLY when the email contains information
that should be remembered beyond this individual email.

Useful knowledge includes:

- People involved in an ongoing task, project, or conversation.
- A person's explicit role or relationship to another entity.
- Projects being discussed or worked on.
- Ongoing tasks, requests, or responsibilities.
- Commitments made by the user or another person.
- Important deadlines connected to an ongoing task or project.
- Meetings or events connected to an ongoing situation.
- Project status or progress updates.
- Decisions that affect future work.
- Approvals, rejections, or important status changes.
- Information that could help answer a future question about the user's
  work, projects, commitments, or conversations.

Set knowledgeRelevant = false for:

- Advertisements
- Promotional emails
- Generic newsletters
- OTPs
- Password/reset notifications
- Routine automated notifications
- Generic social-media notifications
- Generic job alerts
- One-time delivery/status notifications
- Purchase receipts with no ongoing relevance
- Generic bills or payment notifications with no ongoing context
- Information that is unlikely to be useful after this email

IMPORTANT:

Do NOT set knowledgeRelevant = false simply because the information
is also present in action items, deadlines, or calendar events from
another analysis.

If the email establishes or changes persistent context, set it to true.

==================================================
2. ENTITIES
==================================================

Extract only entities that are explicitly identifiable in the email.

Allowed entity types:

- PERSON
- ORGANIZATION
- PROJECT
- EVENT

For each entity return:

{
  "type": "",
  "name": "",
  "key": ""
}

Rules for key:

PERSON:
- Prefer the person's email address when explicitly available.
- Otherwise use the person's explicit name.

ORGANIZATION:
- Use the explicit organization name.

PROJECT:
- Use the explicit project name.

EVENT:
- Use the explicit event name.

The key must be stable and deterministic.

Do not create entities from vague or generic words.

Do not create an entity unless it is explicitly identifiable.

==================================================
3. RELATIONSHIPS
==================================================

Extract relationships only when they are explicitly supported by the
email.

Allowed relationship types:

- WORKS_AT
- WORKS_WITH
- REQUESTED
- ASSIGNED
- DISCUSSES
- INVOLVES
- RELATED_TO
- SCHEDULED_WITH
- RESPONSIBLE_FOR
- PART_OF
- APPROVED
- DECIDED

Format:

{
  "source": "",
  "type": "",
  "target": ""
}

Rules:

- source and target MUST be entity keys from the entities array.
- Never create a relationship involving an entity that does not exist.
- Never infer a relationship that is not explicitly supported.
- Do not create unnecessary relationships.
- Only extract relationships that may be useful in future context.

==================================================
4. COMMITMENTS
==================================================

Extract explicit commitments, requests, responsibilities, or pending
actions that may matter beyond this email.

Examples:

"I will send the report tomorrow."

"Rahul will send the database schema by Friday."

"Please submit the application by Monday."

Format:

{
  "actor": "",
  "action": "",
  "object": "",
  "deadline": "",
  "status": ""
}

Allowed status values:

- PENDING
- COMPLETED
- CANCELLED

Rules:

- The actor must be explicitly identifiable.
- Use "USER" only when the email explicitly indicates that the user
  must perform the action.
- Use a person's entity key when another person is explicitly
  responsible.
- deadline must use YYYY-MM-DD when a specific date is available.
- If no deadline exists, use an empty string.
- Do not create commitments from general statements.
- Do not turn ordinary information into a commitment.

==================================================
5. IMPORTANT FACTS
==================================================

Extract important durable facts that do not naturally fit into
relationships or commitments but may be useful later.

Format:

{
  "fact": "",
  "relatedEntity": ""
}

Examples:

"The client approved the initial design."

"The project is currently waiting for Android integration."

"Room is being used instead of Firebase."

Rules:

- Only include facts explicitly stated in the email.
- relatedEntity must be an entity key when applicable.
- Do not duplicate commitments or relationships as facts.
- Do not include trivial information.

==================================================
6. OUTPUT
==================================================

Return ONLY this JSON structure:

{
  "knowledgeRelevant": false,

  "entities": [],

  "relationships": [],

  "commitments": [],

  "facts": []
}

==================================================
7. FINAL RULES
==================================================

Accuracy is more important than completeness.

It is better to return an empty array than to guess.

Do not create knowledge merely because the email contains a person's
name, organization name, date, or project-like word.

Extract knowledge only when it represents useful persistent context.

Never invent information that is not explicitly present in the email.
"""
    }

    suspend fun analyze(
        email: Email
    ): EmailAnalysis {

        Log.d("SmartGmail", "1. ANALYSIS STARTED")

        val context = EmailContextBuilder.build(email)
        val emailId = email.id

        Log.d("SmartGmail", "2. CONTEXT BUILT, length = ${context.length}")

        val prompt = """
You are SmartGmail's expert email analysis engine.

Analyze the email and return ONLY raw JSON. No explanations, no markdown tags.
Copy the "emailId" EXACTLY into the JSON.
==================================================
1. PRIORITY
==================================================

Choose EXACTLY ONE: HIGH, MEDIUM, or LOW.

Determine priority from the EMAIL CONTENT.

HIGH:
The email requires the user to do something meaningful.
Examples:
- submit or complete something
- respond or reply
- attend a required meeting
- pay a bill
- complete a required task
- meet a deadline
- resolve an important account or security issue

MEDIUM:
The email is relevant and may require attention, but no meaningful
action is required.
Examples:
- important updates
- schedule changes
- delivery notifications
- account statements
- non-critical reminders

LOW:
The user can ignore the email without consequences.
Examples:
- newsletters
- promotions
- advertisements
- marketing offers
- generic job alerts
- social-media notifications
- routine automated emails
- order confirmations

MARKETING RULE:
If the email is primarily promotional, advertising, or a sales offer,
priority MUST be LOW.

Words such as:
"urgent", "exclusive", "limited time", "sale", "discount",
"act now", or "alert"
do NOT make a marketing email HIGH.

IMPORTANT:
A deadline for a meaningful user task means HIGH.

Do not invent a task or obligation.

FINAL CHECK:
Ask:
"Will the user suffer a meaningful consequence from ignoring this email?"

YES → HIGH
NO, but the information is useful → MEDIUM
NO, and the email can be ignored → LOW

Critical:
A promotional/marketing email is ALWAYS LOW, regardless of words like
"urgent", "exclusive", "limited time", "sale", or "act now", "use code", "apply for jobs",

Only classify a marketing email as HIGH if it contains a separate,
genuine obligation unrelated to the promotion.

Do not invent obligations.

Rule:
MUST ACT → HIGH
SHOULD KNOW → MEDIUM
CAN IGNORE → LOW

==================================================
2. SUMMARY
==================================================
Write a concise 1-2 sentence overview of the email's purpose.

==================================================
3. ACTION ITEMS
==================================================
List ALL specific tasks the USER is explicitly asked to perform (e.g., 'Finish the report', 'Prepare presentation'). 
Do NOT skip tasks just because they have deadlines.
Combine related minor steps for the same goal into one clear objective.

==================================================
4. DEADLINES
==================================================
A DEADLINE is a task the USER must FINISH or SUBMIT BY a specific time.
Example: "Submit report by Friday at 5 PM" -> Deadline.
A MEETING IS NOT A DEADLINE. DO NOT list meetings or events here.
Format: {"description": "Submit project report", "date": "YYYY-MM-DD", "time": "17:00"}

==================================================
5. CALENDAR EVENTS
==================================================
An EVENT is something that HAPPENS at a certain time that the USER attends or is present for.
Example: "Meeting from 10 AM to 11 AM" -> Event.
A MEETING IS ALWAYS AN EVENT, NEVER A DEADLINE.
Format: {"title": "", "date": "YYYY-MM-DD", "startTime": "HH:MM", "endTime": "HH:MM", "location": "", "description": ""}

==================================================
DISTINCTION RULE: DEADLINE vs EVENT
==================================================
- If the user must FINISH, SUBMIT, or DO something BY a specific time: It is a DEADLINE.
- If the user must BE PRESENT, ATTEND, or PARTICIPATE at a specific time: It is an EVENT.
- Meetings, Interviews, and Classes are ALWAYS EVENTS, NOT deadlines.
- Assignments, Applications, and Payments are usually DEADLINES.

==================================================
JSON OUTPUT STRUCTURE
==================================================
{
  "emailId": "$emailId",
  "priority": "HIGH" or "MEDIUM" or "LOW",
  "summary": "",
  "actionItems": [],
  "deadlines": [],
  "calendarEvents": []
}

EMAIL DATA:
$context
""".trimIndent()

        Log.d("workflow", "3. PROMPT BUILT, length = ${prompt.length}")
        Log.d("workflow", "PROMPT given, length = ${prompt}")

        var response = ""
        val inferenceStartTime = System.currentTimeMillis()
        var firstTokenReceived = false

        Log.d("workflow", "4. CALLING LLM")

        val responseBuilder = StringBuilder()
        localLLM.generate(prompt = prompt, maxTokens = 1024).collect { token ->
            responseBuilder.append(token)
            response = responseBuilder.toString()
            if (!firstTokenReceived) {
                firstTokenReceived = true
                Log.d("workflow", "5. FIRST TOKEN RECEIVED after ${System.currentTimeMillis() - inferenceStartTime} ms")
            }
        }

        Log.d("workflow", "6. LLM FINISHED in ${System.currentTimeMillis() - inferenceStartTime} ms")
        Log.d("workflow", "RAW LLM RESPONSE:\n$response")

        val analysis = parseResponse(emailId = email.id, response = response)
        Log.d("workflow", "7. ANALYSIS COMPLETE: Priority = ${analysis.priority}")

        return analysis
    }

    suspend fun analyzeKnowledgeRelevance(
        email: Email
    ): String {
        val context = EmailContextBuilder.build(email)
        val prompt = """
    You are SmartGmail's long-term knowledge filter.
    Your ONLY job is to decide whether this email contains information that should be remembered.

    Be STRICT and CONSERVATIVE.

    Return RELEVANT only when the email contains information that changes,
    establishes, or is likely to affect the user's ongoing context.

    Return IRRELEVANT for ordinary emails, even if they contain dates,
    names, amounts, tasks, or other information.

    ==================================================
    RETURN RELEVANT
    ==================================================
    Return RELEVANT when the email establishes or changes:
    - An ongoing project or work context
    - A person's role or involvement in an ongoing project/task
    - An important project status or progress update
    - A decision that affects future work
    - An approval or rejection that affects future work
    - An ongoing commitment or responsibility
    - A task that another person is waiting for
    - A meaningful change to an existing meeting/event
    - An important ongoing conversation or situation
    - A deadline that is connected to an ongoing task, project, application, meeting, or commitment

    ==================================================
    RETURN IRRELEVANT
    ==================================================
    Return IRRELEVANT for information that is temporary, routine, transactional, promotional, or unlikely to help understand future context.
    Examples: 50% off, OTPs, package delivered, payment success, job alerts, newsletters.

    ==================================================
    EMAIL
    ==================================================
    $context

    ==================================================
    OUTPUT
    ==================================================
    Return ONLY one word: RELEVANT or IRRELEVANT
""".trimIndent()
        var response = ""
        val responseBuilder = StringBuilder()
        localLLM.generate(prompt = prompt, maxTokens = 10).collect { token ->
            responseBuilder.append(token)
        }
        response = responseBuilder.toString().uppercase()

        return if (response.contains("RELEVANT") && !response.contains("IRRELEVANT")) {
            "RELEVANT"
        } else {
            "IRRELEVANT"
        }
    }

    suspend fun extractKnowledge(
        email: Email
    ): KnowledgeResult {
        val context = EmailContextBuilder.build(email)
        val prompt = """
            $KNOWLEDGE_SYSTEM_PROMPT

            EMAIL:
            $context
        """.trimIndent()

        Log.d("workflow", "KNOWLEDGE EXTRACTION STARTED for ${email.id}")
        
        var response = ""
        val responseBuilder = StringBuilder()
        localLLM.generate(prompt = prompt, maxTokens = 1024).collect { token ->
            responseBuilder.append(token)
        }
        response = responseBuilder.toString()

        Log.d("workflow", "RAW KNOWLEDGE RESPONSE:\n$response")

        val jsonText = extractJson(response)
        val json = JSONObject(jsonText)

        return KnowledgeResult(
            emailId = email.id,
            entitiesJson = json.optJSONArray("entities")?.toString() ?: "[]",
            relationshipsJson = json.optJSONArray("relationships")?.toString() ?: "[]",
            commitmentsJson = json.optJSONArray("commitments")?.toString() ?: "[]",
            factsJson = json.optJSONArray("facts")?.toString() ?: "[]"
        )
    }

    suspend fun analyzeSentEmail(
        email: Email,
        pendingTasks: List<TaskEntity>
    ): List<Long> {
        if (pendingTasks.isEmpty()) return emptyList()

        val context = EmailContextBuilder.build(email)
        val tasksList = pendingTasks.joinToString("\n") { "- [ID: ${it.id}] ${it.description}" }

        val prompt = """
You are SmartGmail's task completion engine.
Analyze this SENT email and determine if it completes any of the PENDING TASKS.

PENDING TASKS:
$tasksList

SENT EMAIL:
$context

Return ONLY a JSON array of IDs of the completed tasks. Example: [1, 5]
""".trimIndent()

        var response = ""
        val responseBuilder = StringBuilder()

        localLLM.generate(prompt = prompt, maxTokens = 256).collect { token ->
            responseBuilder.append(token)
        }
        response = responseBuilder.toString()

        return try {
            val start = response.indexOf("[")
            val end = response.lastIndexOf("]")
            if (start != -1 && end != -1 && end > start) {
                val jsonArray = JSONArray(response.substring(start, end + 1))
                val completedIds = mutableListOf<Long>()
                for (i in 0 until jsonArray.length()) {
                    completedIds.add(jsonArray.getLong(i))
                }
                completedIds
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("workflow", "Failed to parse completed task IDs from: $response", e)
            emptyList()
        }
    }


    private fun parseResponse(
        emailId: String,
        response: String
    ): EmailAnalysis {

        val jsonText = extractJson(response)
        val json = JSONObject(jsonText)

        val priority = when (json.optString("priority").uppercase()) {
            "HIGH" -> Priority.HIGH
            "MEDIUM" -> Priority.MEDIUM
            else -> Priority.LOW
        }

        val summary = json.optString("summary", "")
        val actionItems = parseStringArray(json.optJSONArray("actionItems"))
        val deadlines = parseDeadlines(json.optJSONArray("deadlines"))
        val calendarEvents = parseCalendarEvents(json.optJSONArray("calendarEvents"))

        return EmailAnalysis(
            emailId = emailId,
            priority = priority,
            summary = summary,
            actionItems = actionItems,
            deadlines = deadlines,
            calendarEvents = calendarEvents,
            knowledgeRelevant = "NOT_ANALYZED"
        )
    }

    private fun extractJson(response: String): String {
        val start = response.indexOf("{")
        val end = response.lastIndexOf("}")

        if (start == -1 || end == -1 || end <= start) {
            throw IllegalStateException("LLM did not return valid JSON:\n$response")
        }

        return response.substring(start, end + 1)
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val result = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            if (value.isNotBlank()) result.add(value)
        }
        return result
    }

    private fun parseDeadlines(array: JSONArray?): List<Deadline> {
        if (array == null) return emptyList()
        val result = mutableListOf<Deadline>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(
                Deadline(
                    description = item.optString("description", ""),
                    date = item.optString("date", ""),
                    time = item.optString("time", "")
                )
            )
        }
        return result
    }

    private fun parseCalendarEvents(array: JSONArray?): List<CalendarEvent> {
        if (array == null) return emptyList()
        val result = mutableListOf<CalendarEvent>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(
                CalendarEvent(
                    title = item.optString("title", ""),
                    date = item.optString("date", ""),
                    startTime = item.optString("startTime", ""),
                    endTime = item.optString("endTime", ""),
                    location = item.optString("location", ""),
                    description = item.optString("description", "")
                )
            )
        }
        return result
    }
}
