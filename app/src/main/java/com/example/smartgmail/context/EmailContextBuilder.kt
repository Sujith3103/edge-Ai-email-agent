package com.example.smartgmail.context

import com.example.smartgmail.model.Email

object EmailContextBuilder {

    fun build(email: Email): String {

        return """
            EMAIL

            From: ${email.sender}
            To: ${email.recipient}
            Date: ${email.date}
            Subject: ${email.subject}

            BODY:
            ${email.body}
        """.trimIndent()
    }
}
