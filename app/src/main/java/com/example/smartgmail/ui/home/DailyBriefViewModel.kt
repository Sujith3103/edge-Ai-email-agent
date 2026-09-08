package com.example.smartgmail.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartgmail.database.entity.EventEntity
import com.example.smartgmail.database.entity.InboxEmail
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.repository.EventRepository
import com.example.smartgmail.repository.TaskRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DailyBriefViewModel(
    private val emailRepository: EmailRepository,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    data class BriefState(
        val greeting: String = "",
        val inboxStats: InboxStats = InboxStats(),
        val categorizedItems: Map<String, List<BriefItem>> = emptyMap()
    )

    data class InboxStats(
        val receivedCount: Int = 0,
        val actionCount: Int = 0,
        val eventCount: Int = 0
    )

    data class BriefItem(
        val title: String,
        val detail: String,
        val type: ItemType
    ) {
        enum class ItemType { TASK, EMAIL, EVENT }
    }

    val state: StateFlow<BriefState> = combine(
        emailRepository.getInboxEmails(),
        taskRepository.getIncompleteTasks(),
        eventRepository.getAllEvents()
    ) { emails, tasks, events ->
        calculateBrief(emails, tasks, events)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BriefState(greeting = getGreeting())
    )

    private fun calculateBrief(
        emails: List<InboxEmail>,
        tasks: List<TaskEntity>,
        events: List<EventEntity>
    ): BriefState {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val tomorrowStr = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val categories = mutableMapOf<String, MutableList<BriefItem>>()

        // 1. STATS
        val stats = InboxStats(
            receivedCount = emails.size,
            actionCount = emails.count { it.priority == "HIGH" || it.priority == "MEDIUM" } + tasks.size,
            eventCount = events.count { it.date == todayStr || it.date == tomorrowStr }
        )

        // 2. CATEGORIZATION
        
        // IMPORTANT: High priority tasks or critical emails
        val importantList = mutableListOf<BriefItem>()
        tasks.forEach {
            val dueText = when (it.dueDate) {
                todayStr -> "today"
                tomorrowStr -> "tomorrow"
                else -> it.dueDate ?: ""
            }
            importantList.add(BriefItem(it.description, "Due $dueText ${it.dueTime ?: ""}", BriefItem.ItemType.TASK))
        }
        emails.filter { it.priority == "HIGH" && !isShopping(it) }.take(2).forEach {
            importantList.add(BriefItem(it.subject, it.summary ?: "", BriefItem.ItemType.EMAIL))
        }
        if (importantList.isNotEmpty()) categories["Important"] = importantList

        // MEETINGS: Today and tomorrow events
        val meetingsList = events.filter { it.date == todayStr || it.date == tomorrowStr }.map {
            val dateText = if (it.date == todayStr) "Today" else "Tomorrow"
            BriefItem(it.title, "$dateText · ${it.startTime ?: ""}", BriefItem.ItemType.EVENT)
        }
        if (meetingsList.isNotEmpty()) categories["Meetings"] = meetingsList.toMutableList()

        // SHOPPING: Filter by keywords
        val shoppingList = emails.filter { isShopping(it) }.map {
            BriefItem(it.subject, it.summary ?: "", BriefItem.ItemType.EMAIL)
        }
        if (shoppingList.isNotEmpty()) categories["Shopping"] = shoppingList.toMutableList()

        return BriefState(
            greeting = getGreeting(),
            inboxStats = stats,
            categorizedItems = categories
        )
    }

    private fun isShopping(email: InboxEmail): Boolean {
        val keywords = listOf("amazon", "order", "ship", "delivery", "receipt", "purchase")
        val text = (email.subject + " " + (email.summary ?: "")).lowercase()
        return keywords.any { text.contains(it) }
    }

    private fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 0..11 -> "Good morning 👋"
            in 12..16 -> "Good afternoon 👋"
            else -> "Good evening 👋"
        }
    }

    class Factory(
        private val emailRepository: EmailRepository,
        private val taskRepository: TaskRepository,
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DailyBriefViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DailyBriefViewModel(emailRepository, taskRepository, eventRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
