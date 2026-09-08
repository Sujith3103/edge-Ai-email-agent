package com.example.smartgmail.repository

import com.example.smartgmail.database.dao.EventDao
import com.example.smartgmail.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {
    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()
    suspend fun deleteEvent(eventId: Long) = eventDao.deleteEvent(eventId)
}
