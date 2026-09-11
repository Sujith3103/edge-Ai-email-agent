package com.example.smartgmail.model

data class KnowledgeResult(
    val emailId: String,
    val entitiesJson: String,
    val relationshipsJson: String,
    val commitmentsJson: String,
    val factsJson: String
)
