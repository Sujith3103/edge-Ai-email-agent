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

Analyze the email and return ONLY valid JSON.

Use ONLY information explicitly present in the email.
Never invent, assume, or guess information.

==================================================
1. PRIORITY
==================================================

Choose exactly ONE priority:

HIGH
MEDIUM
LOW

FOLLOW THESE STEPS IN ORDER.

STEP 1 — HIGH

Ask:

"Does the user need to do something important or time-sensitive?"

If YES, priority = HIGH.

This includes:

- a deadline the user must meet
- a report, assignment, application, or form that must be submitted
- a payment that must be made
- an urgent request
- an interview
- an exam
- a class
- a meeting or appointment at a specific time
- another genuine time-sensitive obligation

Examples:

"Submit the report by September 4 at 5 PM."
→ HIGH

"Please complete the application by Friday."
→ HIGH

"Payment is due September 10."
→ HIGH

"Your interview is September 8 at 2 PM."
→ HIGH

"Project review is September 5 at 10 AM."
→ HIGH


IMPORTANT:

If an email contains a genuine user deadline or obligation,
it MUST NOT be LOW.

A deadline or urgent obligation takes priority over the fact
that the email may be a reminder or automated notification.

Example:

"Reminder: submit your project report by September 4 at 5 PM."
→ HIGH

NOT LOW.


STEP 2 — LOW

Only choose LOW if the email does NOT contain a genuine
user obligation or urgent requirement.

LOW includes emails that are primarily:

- advertisements
- promotions
- sales
- discounts
- marketing
- newsletters
- product recommendations
- routine automated notifications
- information requiring no meaningful action

Examples:

"Weekend Sale - Up to 40% off"
→ LOW

"50% discount on selected products"
→ LOW

"Shop our new collection"
→ LOW

"Newsletter: This week's updates"
→ LOW


A promotional date is NOT a user deadline.

Examples:

"Sale ends September 4."
→ LOW

"Offer valid until Friday."
→ LOW

"Limited time offer."
→ LOW

"Act now before the offer expires."
→ LOW


STEP 3 — MEDIUM

If the email is important or useful but is neither:

- a genuine urgent/user obligation
NOR
- promotional/routine/no-action content

then priority = MEDIUM.

Examples:

"Here are the updated project requirements."
→ MEDIUM

"Your manager shared the new project guidelines."
→ MEDIUM

"Meeting notes from today's discussion."
→ MEDIUM


==================================================
2. SUMMARY
==================================================

Write ONE short sentence describing what the email is about.

Do not add information that is not in the email.


==================================================
3. ACTION ITEMS
==================================================

List actions the user is explicitly asked or clearly expected
to perform.

Examples:

"Submit the report by Friday."
→ "Submit the report"

"Please contact me before Friday."
→ "Contact me"

"Prepare the presentation for the review."
→ "Prepare the presentation"

Do NOT invent actions.

Do NOT turn optional or conditional statements into actions.

Example:

"If you have questions, contact me."
→ no action item


==================================================
4. DEADLINES
==================================================

A DEADLINE is something the USER MUST COMPLETE BY a specific
date or time.

Examples:

"Submit the report by September 4 at 5 PM."
→ deadline

"Payment is due September 10."
→ deadline

"Application must be completed by Friday."
→ deadline


A MEETING, APPOINTMENT, INTERVIEW, EXAM, OR CLASS is NOT a deadline.

For example:

"Project review is September 5 from 10 AM to 11 AM."
→ NOT a deadline


==================================================
5. CALENDAR EVENTS
==================================================

A CALENDAR EVENT is something that HAPPENS at a specific
date or time.

Examples:

"Project review is September 5 from 10 AM to 11 AM."
→ calendar event

"Your interview is September 8 at 2 PM."
→ calendar event

"Class starts at 9 AM on Monday."
→ calendar event


For every calendar event return:

- title
- date
- startTime
- endTime
- location
- description


IMPORTANT:

A meeting belongs in calendarEvents.

A deadline belongs in deadlines.

Do NOT put a meeting in deadlines.

Do NOT put a deadline in calendarEvents.

If an email contains both a deadline and a meeting,
extract them separately.


==================================================
6. DATE FORMAT
==================================================

Convert every known date to:

YYYY-MM-DD

Examples:

September 4, 2026
→ 2026-09-04

September 5, 2026
→ 2026-09-05

December 1, 2026
→ 2026-12-01


==================================================
7. TIME FORMAT
==================================================

Convert every known time to 24-hour format:

HH:MM

Examples:

5:00 PM
→ 17:00

10:00 AM
→ 10:00

11:00 AM
→ 11:00

2:30 PM
→ 14:30

12:00 PM
→ 12:00

12:00 AM
→ 00:00


For a time range:

"10:00 AM to 11:00 AM"

return:

startTime = "10:00"
endTime = "11:00"


NEVER return:

"5:00 PM"

NEVER return:

"10:00 AM to 11:00 AM"

NEVER include the date inside a time field.


==================================================
8. UNKNOWN VALUES
==================================================

If information is not explicitly provided, use null.

Unknown date:
null

Unknown time:
null

Unknown end time:
null

Unknown location:
null

Unknown description:
null

Never guess missing information.


==================================================
9. OUTPUT
==================================================

Return ONLY valid JSON.

Do NOT use Markdown.

Do NOT use ```json.

Do NOT write explanations before or after the JSON.

Use exactly this structure:

{
  "priority": "HIGH",
  "summary": "",
  "actionItems": [],
  "deadlines": [
    {
      "description": "",
      "date": null,
      "time": null
    }
  ],
  "calendarEvents": [
    {
      "title": "",
      "date": null,
      "startTime": null,
      "endTime": null,
      "location": null,
      "description": null
    }
  ]
}


==================================================
FINAL PRIORITY CHECK
==================================================

Before returning the JSON, check:

1. Is the user required to do something by a date/time?
   → HIGH

2. Is there another genuine urgent or time-sensitive obligation?
   → HIGH

3. Is the email only promotional, marketing, newsletter,
   or routine/no-action content?
   → LOW

4. Otherwise:
   → MEDIUM


FINAL CHECK FOR THIS DISTINCTION:

"Submit your report by Friday."
→ HIGH

"Reminder to submit your report by Friday."
→ HIGH

"Sale ends Friday."
→ LOW

"Newsletter with this week's updates."
→ LOW

"Project meeting Friday at 10 AM."
→ HIGH + calendar event

"Project requirements have been updated."
→ MEDIUM


EMAIL:

$context
""".trimIndent()//    Analyze this email.
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
            "workflow",
            "3. PROMPT BUILT"
        )

        Log.d(
            "workflow",
            "Prompt length = ${prompt.length}"
        )

        Log.d(
            "workflow",
            "Prompt ending = ${prompt.takeLast(500)}"
        )


        /*
         * Start inference.
         */

        var response = ""

        val inferenceStartTime =
            System.currentTimeMillis()

        var firstTokenReceived = false

        Log.d(
            "workflow",
            "4. CALLING QWEN"
        )

        val responseBuilder =
            StringBuilder()

        localLLM
            .generate(
                prompt = prompt,
                maxTokens = 512
            )
            .collect { token ->

                responseBuilder.append(token)

                response = responseBuilder.toString()

                if (!firstTokenReceived) {

                    firstTokenReceived = true

                    val firstTokenTime =
                        System.currentTimeMillis() - inferenceStartTime

                    Log.d(
                        "workflow",
                        "5. FIRST TOKEN RECEIVED after ${firstTokenTime} ms"
                    )
                }
            }

        val totalInferenceTime =
            System.currentTimeMillis() - inferenceStartTime

        Log.d(
            "workflow",
            "6. QWEN FINISHED"
        )

        Log.d(
            "workflow",
            "Inference time = ${totalInferenceTime} ms"
        )

        Log.d(
            "workflow",
            "Response length = ${response.length}"
        )

        Log.d(
            "workflow",
            "Raw response length = ${response.length}"
        )

        Log.d(
            "workflow",
            "RAW QWEN RESPONSE:"
        )

        Log.d(
            "workflow",
            response
        )


        /*
         * Convert the JSON returned by Qwen
         * into EmailAnalysis.
         */

        Log.d(
            "workflow",
            "7. PARSING RESPONSE"
        )

        val analysis =
            parseResponse(
                emailId = email.id,
                response = response
            )


        Log.d(
            "workflow",
            "8. ANALYSIS COMPLETE"
        )

        Log.d(
            "workflow",
            "Priority = ${analysis.priority}"
        )

        Log.d(
            "workflow",
            "Summary = ${analysis.summary}"
        )

        Log.d(
            "workflow",
            "Action items = ${analysis.actionItems}"
        )

        Log.d(
            "workflow",
            "Deadlines = ${analysis.deadlines}"
        )

        Log.d(
            "workflow",
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