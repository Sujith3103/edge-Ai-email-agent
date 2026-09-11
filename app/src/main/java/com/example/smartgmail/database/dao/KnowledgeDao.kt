package com.example.smartgmail.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartgmail.database.entity.KnowledgeEntity

@Dao
interface KnowledgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: KnowledgeEntity)

    @Query("SELECT * FROM knowledge_graph WHERE emailId = :emailId")
    suspend fun getKnowledgeForEmail(emailId: String): KnowledgeEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM knowledge_graph WHERE emailId = :emailId)")
    suspend fun knowledgeExists(emailId: String): Boolean

    @Query("""
        SELECT * FROM knowledge_graph 
        WHERE entitiesJson LIKE '%' || :query || '%' 
        OR factsJson LIKE '%' || :query || '%' 
        OR commitmentsJson LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchKnowledge(query: String, limit: Int = 10): List<KnowledgeEntity>
}
