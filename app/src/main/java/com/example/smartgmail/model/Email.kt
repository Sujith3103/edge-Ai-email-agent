package com.example.smartgmail.model

data class Email(
    val id: String,
    val threadId: String,
    val sender: String,
    val recipient: String,
    val subject: String,
    val date: String,
    val body: String
)