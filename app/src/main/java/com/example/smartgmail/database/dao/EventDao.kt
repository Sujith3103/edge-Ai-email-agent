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

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long)

    @Query("""
        SELECT * FROM events 
        WHERE title LIKE '%' || :query || '%' 
        OR location LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchEvents(query: String, limit: Int = 5): List<EventEntity>

    @Query("SELECT * FROM events ORDER BY date ASC, startTime ASC LIMIT :limit")
    suspend fun getUpcomingEvents(limit: Int = 5): List<EventEntity>
}
