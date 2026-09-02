package com.example.smartgmail.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val emailId: String,

    val title: String,

    val date: String?,

    val startTime: String?,

    val endTime: String?,

    val location: String?,

    val description: String?
)