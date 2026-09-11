package com.example.smartgmail.repository

import com.example.smartgmail.database.dao.KnowledgeDao
import com.example.smartgmail.database.entity.KnowledgeEntity
import com.example.smartgmail.model.KnowledgeResult

class KnowledgeRepository(private val knowledgeDao: KnowledgeDao) {
    suspend fun saveKnowledge(result: KnowledgeResult) {
        val entity = KnowledgeEntity(
            emailId = result.emailId,
            entitiesJson = result.entitiesJson,
            relationshipsJson = result.relationshipsJson,
            commitmentsJson = result.commitmentsJson,
            factsJson = result.factsJson,
            extractionTimestamp = System.currentTimeMillis()
        )
        knowledgeDao.insertKnowledge(entity)
    }

    suspend fun knowledgeExists(emailId: String): Boolean = knowledgeDao.knowledgeExists(emailId)
}
