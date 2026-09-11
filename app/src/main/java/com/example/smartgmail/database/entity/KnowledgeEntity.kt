package com.example.smartgmail.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_graph")
data class KnowledgeEntity(
    @PrimaryKey
    val emailId: String,
    val entitiesJson: String,
    val relationshipsJson: String,
    val commitmentsJson: String,
    val factsJson: String,
    val extractionTimestamp: Long
)
