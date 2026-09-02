package com.example.smartgmail.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val emailId: String,

    val description: String,

    val dueDate: String?,

    val dueTime: String?,

    val completed: Boolean = false
)