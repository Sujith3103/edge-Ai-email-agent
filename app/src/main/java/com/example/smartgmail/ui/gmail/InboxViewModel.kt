package com.example.smartgmail.ui.gmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartgmail.database.entity.InboxEmail
import com.example.smartgmail.repository.EmailRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InboxViewModel(
    private val emailRepository: EmailRepository
) : ViewModel() {

    val inboxEmails: StateFlow<List<InboxEmail>> =
        emailRepository.getInboxEmails()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    class Factory(
        private val emailRepository: EmailRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InboxViewModel(emailRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
