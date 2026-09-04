package com.example.smartgmail.database.entity

/**
 * A combined representation of an email and its AI analysis.
 * Used for displaying the Inbox screen.
 */
data class InboxEmail(
    val id: String,
    val threadId: String,
    val sender: String,
    val recipient: String,
    val subject: String,
    val date: String,
    val body: String,

    val priority: String?,
    val summary: String?,
    val analysisStatus: String?
)
