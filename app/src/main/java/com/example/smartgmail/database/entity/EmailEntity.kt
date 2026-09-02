package com.example.smartgmail.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emails")
data class EmailEntity(

    @PrimaryKey
    val id: String,

    val threadId: String,

    val sender: String,

    val recipient: String,

    val subject: String,

    val date: String,

    val body: String
)