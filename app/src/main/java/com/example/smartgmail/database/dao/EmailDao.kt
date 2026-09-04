package com.example.smartgmail.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartgmail.database.entity.EmailEntity
import com.example.smartgmail.database.entity.InboxEmail
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertEmail(
        email: EmailEntity
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertEmails(
        emails: List<EmailEntity>
    )

    @Query(
        "SELECT * FROM emails ORDER BY date DESC"
    )
    fun getAllEmails(): Flow<List<EmailEntity>>

    @Query(
        "SELECT * FROM emails WHERE id = :emailId LIMIT 1"
    )
    suspend fun getEmail(
        emailId: String
    ): EmailEntity?

    @Query("""
        SELECT
            e.id,
            e.threadId,
            e.sender,
            e.recipient,
            e.subject,
            e.date,
            e.body,
            a.priority,
            a.summary,
            a.analysisStatus
        FROM emails e
        LEFT JOIN email_analysis a
            ON e.id = a.emailId
        ORDER BY e.date DESC
    """)
    fun getInboxEmails(): Flow<List<InboxEmail>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM emails WHERE id = :emailId)"
    )
    suspend fun emailExists(
        emailId: String
    ): Boolean
}