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

    @Query(
        "SELECT * FROM events ORDER BY date ASC, startTime ASC"
    )
    fun getAllEvents(): Flow<List<EventEntity>>
}