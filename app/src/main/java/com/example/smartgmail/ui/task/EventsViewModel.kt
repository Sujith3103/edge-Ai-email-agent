package com.example.smartgmail.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartgmail.database.entity.EventEntity
import com.example.smartgmail.repository.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventsViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val allEvents: StateFlow<List<EventEntity>> =
        eventRepository.getAllEvents()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }

    class Factory(
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EventsViewModel(eventRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
