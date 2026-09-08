package com.example.smartgmail.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartgmail.database.entity.EmailAnalysisEntity
import kotlinx.coroutines.flow.Flow

//mapping the functions and the queries

@Dao
interface EmailAnalysisDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertAnalysis(
        analysis: EmailAnalysisEntity
    )

    @Query(
        "SELECT * FROM email_analysis WHERE emailId = :emailId LIMIT 1"
    )
    suspend fun getAnalysis(
        emailId: String
    ): EmailAnalysisEntity?

    @Query("INSERT OR IGNORE INTO email_analysis (emailId, priority, summary, actionItemsJson, deadlinesJson, calendarEventsJson, analysisStatus) VALUES (:emailId, 'LOW', '', '[]', '[]', '[]', 'ANALYZING')")
    suspend fun insertAnalyzingStatus(emailId: String)

    @Query("UPDATE email_analysis SET analysisStatus = :status WHERE emailId = :emailId")
    suspend fun updateStatus(emailId: String, status: String)

    @Query(
        "SELECT * FROM email_analysis"
    )
    fun getAllAnalyses(): Flow<List<EmailAnalysisEntity>>
}