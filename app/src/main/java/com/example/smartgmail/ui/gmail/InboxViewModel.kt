package com.example.smartgmail.ui.gmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartgmail.ai.AIManager
import com.example.smartgmail.database.entity.InboxEmail
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.worker.GmailSyncScheduler
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val emailRepository: EmailRepository,
    private val aiManager: AIManager
) : ViewModel() {

    val inboxEmails: StateFlow<List<InboxEmail>> =
        emailRepository.getInboxEmails()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val failedEmails: StateFlow<List<InboxEmail>> =
        emailRepository.getFailedEmails()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val deletedEmails: StateFlow<List<InboxEmail>> =
        emailRepository.getDeletedEmails()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteEmail(emailId: String) {
        viewModelScope.launch {
            emailRepository.markAsDeleted(emailId)
        }
    }

    fun restoreEmail(emailId: String) {
        viewModelScope.launch {
            emailRepository.restoreEmail(emailId)
        }
    }

    fun deletePermanently(emailId: String) {
        viewModelScope.launch {
            emailRepository.deletePermanently(emailId)
        }
    }

    fun refreshFailed(context: android.content.Context) {
        viewModelScope.launch {
            val failed = emailRepository.getFailedEmails().first()
            if (failed.isEmpty()) {
                android.widget.Toast.makeText(context, "No failed emails to process", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            android.widget.Toast.makeText(context, "Retrying ${failed.size} emails...", android.widget.Toast.LENGTH_SHORT).show()

            val database = (context.applicationContext as com.example.smartgmail.SmartGmailApplication).database
            val analysisRepository = com.example.smartgmail.repository.EmailAnalysisRepository(database)

            failed.forEach { inboxEmail ->
                val emailEntity = emailRepository.getEmail(inboxEmail.id) ?: return@forEach
                val email = com.example.smartgmail.model.Email(
                    id = emailEntity.id,
                    threadId = emailEntity.threadId,
                    sender = emailEntity.sender,
                    recipient = emailEntity.recipient,
                    subject = emailEntity.subject,
                    date = emailEntity.date,
                    body = emailEntity.body
                )

                try {
                    analysisRepository.saveAnalyzingStatus(email.id)
                    
                    val analysis = aiManager.runAnalysis { llm ->
                        com.example.smartgmail.ai.EmailAnalyzer(llm).analyze(email)
                    }
                    
                    analysisRepository.saveAnalysis(analysis)
                } catch (e: Exception) {
                    Log.e("InboxViewModel", "Retry failed for ${email.id}", e)
                    analysisRepository.saveFailedAnalysis(email.id)
                }
            }
        }
    }

    class Factory(
        private val emailRepository: EmailRepository,
        private val aiManager: AIManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InboxViewModel(emailRepository, aiManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
