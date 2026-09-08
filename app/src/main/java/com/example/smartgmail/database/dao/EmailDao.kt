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
        "SELECT * FROM emails WHERE isDeleted = 0 ORDER BY date DESC"
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
        WHERE e.isDeleted = 0 AND (a.analysisStatus = 'COMPLETED' OR a.analysisStatus IS NULL)
        ORDER BY e.date DESC
    """)
    fun getInboxEmails(): Flow<List<InboxEmail>>

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
        WHERE e.isDeleted = 0 AND a.analysisStatus = 'FAILED'
        ORDER BY e.date DESC
    """)
    fun getFailedEmails(): Flow<List<InboxEmail>>

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
        WHERE e.isDeleted = 1
        ORDER BY e.date DESC
    """)
    fun getDeletedEmails(): Flow<List<InboxEmail>>

    @Query("UPDATE emails SET isDeleted = 1 WHERE id = :emailId")
    suspend fun markAsDeleted(emailId: String)

    @Query("UPDATE emails SET isDeleted = 0 WHERE id = :emailId")
    suspend fun restoreEmail(emailId: String)

    @Query("DELETE FROM emails WHERE id = :emailId")
    suspend fun deletePermanently(emailId: String)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM emails WHERE id = :emailId)"
    )
    suspend fun emailExists(
        emailId: String
    ): Boolean
}
