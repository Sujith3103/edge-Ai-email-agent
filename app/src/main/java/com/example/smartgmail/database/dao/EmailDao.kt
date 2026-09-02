package com.example.smartgmail.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartgmail.database.entity.EmailEntity
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

    @Query(
        "SELECT EXISTS(SELECT 1 FROM emails WHERE id = :emailId)"
    )
    suspend fun emailExists(
        emailId: String
    ): Boolean
}