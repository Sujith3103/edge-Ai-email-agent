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

Read the email carefully and extract only information explicitly
supported by the email.

Do not invent facts.
Do not guess missing dates, times, locations, or actions.

1. PRIORITY

Determine priority using the following rules IN THIS EXACT ORDER.

LOW:
First check whether the email is primarily:
- an advertisement
- a promotion
- a sale or discount
- a marketing email
- a newsletter
- a product recommendation
- a routine automated notification
- an email requiring no meaningful action

If YES, priority MUST be LOW.

Examples:

"Weekend Sale - Up to 40% off"
→ LOW

"50% discount on selected products"
→ LOW

"Shop our new collection"
→ LOW

"Newsletter: This week's updates"
→ LOW


HIGH:
Only use HIGH if the email contains a genuine time-sensitive requirement
for the user, such as:
- a deadline the user must meet
- a payment that is due
- an urgent request requiring action
- a meeting, appointment, interview, exam, or class at a specific time
- another clearly time-sensitive requirement

A promotional email MUST NOT be HIGH merely because:
- the promotion ends soon
- the email mentions a sale date
- the email contains a discount
- the email says "limited time"
- the email says "act now"
- the email says "ends today"

A marketing deadline is NOT a user deadline.

For example:

"Sale ends September 4"
→ LOW

"Offer valid until Friday"
→ LOW

"Please submit the payment by Friday"
→ HIGH


MEDIUM:
Use MEDIUM if the email is important or useful to the user
but does not contain a genuine urgent requirement.

If the email does not clearly qualify as HIGH or LOW,
use MEDIUM.

2. SUMMARY

Write one short sentence explaining what the email is about.


3. ACTION ITEMS

List actions the user is explicitly asked or clearly expected to perform.

Do not invent actions.

Do not create an action from a conditional statement.

Example:
"If you have questions, contact me."
→ no action item

Example:
"Please contact me before Friday."
→ action item


4. DEADLINES

A deadline means something the user must COMPLETE BY a date or time.

Examples:
"Submit the report by September 4 at 5 PM."
→ deadline

"Payment is due September 10."
→ deadline

A meeting or appointment is NOT a deadline.

For each deadline return:
- description
- date
- time


5. CALENDAR EVENTS

A calendar event is something that HAPPENS at a specific date or time.

Examples:
"Project review is September 5 from 10 AM to 11 AM."
→ calendar event

"Your interview is September 8 at 2 PM."
→ calendar event

For each event return:
- title
- date
- startTime
- endTime
- location
- description

Do not put deadlines into calendarEvents.

Do not duplicate the same event in deadlines.


6. DATE FORMAT

THIS IS IMPORTANT.

Always convert dates to EXACTLY:

YYYY-MM-DD

Examples:

September 4, 2026
→ 2026-09-04

September 5, 2026
→ 2026-09-05

December 1, 2026
→ 2026-12-01


7. TIME FORMAT

THIS IS IMPORTANT.

Always convert times to EXACTLY:

HH:MM

Use the 24-hour clock.

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

10:00 AM to 11:00 AM

return:

startTime = "10:00"
endTime = "11:00"


NEVER return:

"September 4, 2026 at 5:00 PM"

NEVER return:

"5:00 PM"

NEVER return:

"10:00 AM to 11:00 AM"

Return only the required formatted values.


8. UNKNOWN VALUES

If the email does not provide a date, return:

null

If the email does not provide a time, return:

null

If the email does not provide an end time, return:

null

Never guess missing information.


9. OUTPUT

Return ONLY valid JSON.

Use exactly this structure:

{
  "priority": "",
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


FINAL RULES:

- Dates MUST be YYYY-MM-DD.
- Times MUST be HH:MM.
- Use 24-hour time.
- Use null for unknown values.
- Use [] when there are no items.
- Never invent information.
- Return JSON only.

EMAIL:

$context
""".trimIndent()//        val prompt = """
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

        Log.d(
            "workflow",
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
                        "workflow",
                        "5. FIRST TOKEN RECEIVED"
                    )
                }
            }


        /*
         * Qwen finished.
         */

        Log.d(
            "workflow",
            "6. QWEN FINISHED"
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