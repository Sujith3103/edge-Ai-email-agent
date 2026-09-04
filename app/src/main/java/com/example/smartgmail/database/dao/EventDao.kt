package com.example.smartgmail.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smartgmail.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert
    suspend fun insertEvent(
        event: EventEntity
    )

    @Insert
    suspend fun insertEvents(
        events: List<EventEntity>
    )

    @Query(
        "SELECT * FROM events ORDER BY date ASC, startTime ASC"
    )
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("DELETE FROM events WHERE emailId = :emailId")
    suspend fun deleteEventsByEmailId(emailId: String)
}