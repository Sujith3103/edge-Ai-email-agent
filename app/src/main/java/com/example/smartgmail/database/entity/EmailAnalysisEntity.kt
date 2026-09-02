package com.example.smartgmail.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_analysis")
data class EmailAnalysisEntity(

    @PrimaryKey
    val emailId: String,

    val priority: String,

    val summary: String,

    val actionItemsJson: String,

    val deadlinesJson: String,

    val calendarEventsJson: String,

    val analysisStatus: String
)