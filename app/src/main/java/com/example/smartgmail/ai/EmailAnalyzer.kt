package com.example.smartgmail.ai

import android.util.Log

import com.example.smartgmail.context.EmailContextBuilder
import com.example.smartgmail.model.CalendarEvent
import com.example.smartgmail.model.Deadline
import com.example.smartgmail.model.Email
import com.example.smartgmail.model.EmailAnalysis
import com.example.smartgmail.model.Priority

import kotlinx.coroutines.flow.collect

import org.json.JSONArray
import org.json.JSONObject


class EmailAnalyzer(
    private val localLLM: LocalLLM
) {

    suspend fun analyze(
        email: Email
    ): EmailAnalysis {

        Log.d(
            "SmartGmail",
            "1. ANALYSIS STARTED"
        )


        /*
         * Build the context that will be
         * given to the LLM.
         */

        val context =
            EmailContextBuilder.build(email)


        Log.d(
            "SmartGmail",
            "2. CONTEXT BUILT"
        )

        Log.d(
            "SmartGmail",
            "Context length = ${context.length}"
        )


        /*
         * Build the LLM prompt.
         */

        val prompt = """
    You are SmartGmail's email analysis engine.

    Analyze the email below and extract only information
    explicitly present in the email.

    PRIORITY RULES:

    HIGH:
    Use HIGH when the email contains at least one of:
    - an explicit deadline
    - an appointment or meeting at a specific time
    - an interview at a specific time
    - an exam or test at a specific time
    - an urgent request requiring immediate action
    - a payment that is due
    - a time-sensitive task that must be completed

    MEDIUM:
    Use MEDIUM when:
    - the email is potentially important
    - the email contains an opportunity or useful information
    - the email requires attention but has no immediate deadline
    - the email is related to work, college, applications, or projects

    LOW:
    Use LOW when:
    - advertisement
    - marketing
    - promotional content
    - newsletter
    - generic automated notification
    - no meaningful action or information for the user


    SUMMARY:

    Write a short summary of what the email is about.


    ACTION ITEMS:

    Extract things the user is explicitly asked or expected to do.

    Do not create action items that are not stated or clearly implied
    by the email.


    DEADLINES:

    A deadline is something the user must COMPLETE BY a specific
    date or time.

    Examples:

    "Submit the report by Friday at 5 PM."
    → DEADLINE

    "Please send the application before September 10."
    → DEADLINE

    IMPORTANT:
    A deadline is NOT a calendar event.

    Do NOT put meetings, appointments, interviews, classes,
    or other events into the deadlines array.


    CALENDAR EVENTS:

    A calendar event is something that HAPPENS at a specific
    date or time.

    Examples:

    "Project review meeting Friday from 10 AM to 11 AM."
    → CALENDAR EVENT

    "Your interview is September 8 at 2 PM."
    → CALENDAR EVENT

    "Class starts Monday at 9 AM."
    → CALENDAR EVENT

    IMPORTANT:

    Do NOT put deadlines into the calendarEvents array.

    "Submit the report by Friday at 5 PM."
    → deadline ONLY

    "Project review meeting Friday from 10 AM to 11 AM."
    → calendar event ONLY

    If an email contains both a deadline and a calendar event,
    put each item in its appropriate array.

    NEVER duplicate the same item in both arrays.

    Do not invent dates, times, titles, or information.


    DATE AND TIME RULES:

    Use dates in YYYY-MM-DD format when the date is known.

    Use 24-hour time when possible.

    If a value is unknown, use null.


    Return ONLY valid JSON.

    Use exactly this structure:

    {
      "priority": "HIGH",
      "summary": "short summary",
      "actionItems": [],
      "deadlines": [
        {
          "description": "what must be completed",
          "date": "YYYY-MM-DD",
          "time": "HH:MM"
        }
      ],
      "calendarEvents": [
        {
          "title": "event title",
          "date": "YYYY-MM-DD",
          "startTime": "HH:MM",
          "endTime": "HH:MM",
          "location": null,
          "description": null
        }
      ]
    }

    EMAIL:

    $context
""".trimIndent()
//        val prompt = """
//    Analyze this email.
//
//    Return ONLY JSON.
//
//    {
//      "priority": "HIGH",
//      "summary": "short summary",
//      "actionItems": [],
//      "deadlines": [],
//      "calendarEvents": []
//    }
//
//    Email:
//    Please submit the project report by Friday at 5 PM.
//""".trimIndent()

        Log.d(
            "SmartGmail",
            "3. PROMPT BUILT"
        )

        Log.d(
            "SmartGmail",
            "Prompt length = ${prompt.length}"
        )

        Log.d(
            "SmartGmail",
            "Prompt ending = ${prompt.takeLast(500)}"
        )


        /*
         * Start inference.
         */

        var response = ""

        Log.d(
            "SmartGmail",
            "4. CALLING QWEN"
        )


        localLLM
            .generate(
                prompt = prompt,

                /*
                 * We don't need 512 tokens
                 * for this structured response.
                 */
                maxTokens = 512
            )
            .collect { token ->

                response += token

                /*
                 * Don't log every token yet.
                 * Just confirm that generation
                 * has started.
                 */

                if (response.length <= token.length + 1) {

                    Log.d(
                        "SmartGmail",
                        "5. FIRST TOKEN RECEIVED"
                    )
                }
            }


        /*
         * Qwen finished.
         */

        Log.d(
            "SmartGmail",
            "6. QWEN FINISHED"
        )

        Log.d(
            "SmartGmail",
            "Raw response length = ${response.length}"
        )

        Log.d(
            "SmartGmail",
            "RAW QWEN RESPONSE:"
        )

        Log.d(
            "SmartGmail",
            response
        )


        /*
         * Convert the JSON returned by Qwen
         * into EmailAnalysis.
         */

        Log.d(
            "SmartGmail",
            "7. PARSING RESPONSE"
        )

        val analysis =
            parseResponse(
                emailId = email.id,
                response = response
            )


        Log.d(
            "SmartGmail",
            "8. ANALYSIS COMPLETE"
        )

        Log.d(
            "SmartGmail",
            "Priority = ${analysis.priority}"
        )

        Log.d(
            "SmartGmail",
            "Summary = ${analysis.summary}"
        )

        Log.d(
            "SmartGmail",
            "Action items = ${analysis.actionItems}"
        )

        Log.d(
            "SmartGmail",
            "Deadlines = ${analysis.deadlines}"
        )

        Log.d(
            "SmartGmail",
            "Calendar events = ${analysis.calendarEvents}"
        )


        return analysis
    }


    private fun parseResponse(
        emailId: String,
        response: String
    ): EmailAnalysis {

        val jsonText =
            extractJson(response)

        val json =
            JSONObject(jsonText)


        val priority =
            when (
                json.optString("priority")
                    .uppercase()
            ) {

                "HIGH" ->
                    Priority.HIGH

                "MEDIUM" ->
                    Priority.MEDIUM

                else ->
                    Priority.LOW
            }


        val summary =
            json.optString(
                "summary",
                ""
            )


        val actionItems =
            parseStringArray(
                json.optJSONArray(
                    "actionItems"
                )
            )


        val deadlines =
            parseDeadlines(
                json.optJSONArray(
                    "deadlines"
                )
            )


        val calendarEvents =
            parseCalendarEvents(
                json.optJSONArray(
                    "calendarEvents"
                )
            )


        return EmailAnalysis(

            emailId = emailId,

            priority = priority,

            summary = summary,

            actionItems = actionItems,

            deadlines = deadlines,

            calendarEvents = calendarEvents
        )
    }


    private fun extractJson(
        response: String
    ): String {

        val start =
            response.indexOf("{")

        val end =
            response.lastIndexOf("}")


        if (
            start == -1 ||
            end == -1 ||
            end <= start
        ) {

            throw IllegalStateException(
                "LLM did not return valid JSON:\n$response"
            )
        }


        return response.substring(
            start,
            end + 1
        )
    }


    private fun parseStringArray(
        array: JSONArray?
    ): List<String> {

        if (array == null) {
            return emptyList()
        }


        val result =
            mutableListOf<String>()


        for (i in 0 until array.length()) {

            val value =
                array.optString(i)


            if (value.isNotBlank()) {

                result.add(value)
            }
        }


        return result
    }


    private fun parseDeadlines(
        array: JSONArray?
    ): List<Deadline> {

        if (array == null) {

            return emptyList()
        }


        val result =
            mutableListOf<Deadline>()


        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue


            result.add(

                Deadline(

                    description =
                        item.optString(
                            "description",
                            ""
                        ),

                    date =
                        item.optString(
                            "date",
                            null
                        ),

                    time =
                        item.optString(
                            "time",
                            null
                        )
                )
            )
        }


        return result
    }


    private fun parseCalendarEvents(
        array: JSONArray?
    ): List<CalendarEvent> {

        if (array == null) {

            return emptyList()
        }


        val result =
            mutableListOf<CalendarEvent>()


        for (i in 0 until array.length()) {

            val item =
                array.optJSONObject(i)
                    ?: continue


            result.add(

                CalendarEvent(

                    title =
                        item.optString(
                            "title",
                            ""
                        ),

                    date =
                        item.optString(
                            "date",
                            null
                        ),

                    startTime =
                        item.optString(
                            "startTime",
                            null
                        ),

                    endTime =
                        item.optString(
                            "endTime",
                            null
                        ),

                    location =
                        item.optString(
                            "location",
                            null
                        ),

                    description =
                        item.optString(
                            "description",
                            null
                        )
                )
            )
        }


        return result
    }
}