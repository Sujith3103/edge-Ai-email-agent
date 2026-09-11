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
        val todayDate: String = "",
        val inboxStats: InboxStats = InboxStats(),
        val heroItem: HeroItem? = null,
        val todaysEvents: List<EventEntity> = emptyList(),
        val upcomingDeadlines: List<TaskEntity> = emptyList(),
        val categorizedItems: Map<String, List<BriefItem>> = emptyMap()
    )

    data class InboxStats(
        val taskCount: Int = 0,
        val eventCount: Int = 0,
        val highPriorityCount: Int = 0
    )

    data class HeroItem(
        val title: String,
        val detail: String,
        val priority: String = "High Priority"
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
        initialValue = BriefState(
            greeting = getGreeting(),
            todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        )
    )

    private fun calculateBrief(
        emails: List<InboxEmail>,
        tasks: List<TaskEntity>,
        events: List<EventEntity>
    ): BriefState {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val tomorrowStr = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        // 1. STATS
        val stats = InboxStats(
            taskCount = tasks.size,
            eventCount = events.count { 
                (it.date ?: "") > todayStr || 
                (it.date == todayStr && (it.startTime == null || it.startTime!! >= nowTimeStr))
            },
            highPriorityCount = emails.count { it.priority == "HIGH" } + tasks.count { it.dueDate == todayStr }
        )

        // 2. HERO ITEM
        val urgentTask = tasks.find { it.dueDate == todayStr || it.dueDate == tomorrowStr }
        val hero = urgentTask?.let {
            HeroItem(
                title = it.description,
                detail = "Due ${if (it.dueDate == todayStr) "today" else "tomorrow"} at ${it.dueTime ?: ""}"
            )
        } ?: events.find { 
            (it.date ?: "") > todayStr || 
            (it.date == todayStr && (it.startTime == null || it.startTime!! >= nowTimeStr))
        }?.let {
            HeroItem(
                title = it.title,
                detail = "${if (it.date == todayStr) "Today" else it.date} at ${it.startTime ?: ""}"
            )
        }

        // 3. SECTIONS
        // Upcoming Events: Strictly next 5 relative to current time
        val todaysEvents = events
            .filter { it.date != null }
            .filter { 
                (it.date!! > todayStr) || 
                (it.date == todayStr && (it.startTime == null || it.startTime!! >= nowTimeStr))
            }
            .sortedWith(compareBy({ it.date }, { it.startTime ?: "00:00" }))
            .take(5)
        
        // UPCOMING DEADLINES: Strictly next 2 relative to current time across all future dates
        val upcomingDeadlines = tasks
            .filter { it.dueDate != null }
            .filter { 
                (it.dueDate!! > todayStr) || 
                (it.dueDate == todayStr && (it.dueTime == null || it.dueTime!! >= nowTimeStr))
            }
            .sortedWith(compareBy({ it.dueDate }, { it.dueTime ?: "23:59" }))
            .take(2)

        // 4. CATEGORIZATION
        val categories = mutableMapOf<String, MutableList<BriefItem>>()
        val importantEmails = emails.filter { it.priority == "HIGH" }.take(3).map {
            BriefItem(it.subject, it.summary ?: "", BriefItem.ItemType.EMAIL)
        }
        if (importantEmails.isNotEmpty()) categories["Important"] = importantEmails.toMutableList()

        val shoppingList = emails.filter { isShopping(it) }.map {
            BriefItem(it.subject, it.summary ?: "", BriefItem.ItemType.EMAIL)
        }
        if (shoppingList.isNotEmpty()) categories["Shopping"] = shoppingList.toMutableList()

        return BriefState(
            greeting = getGreeting(),
            todayDate = today.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
            inboxStats = stats,
            heroItem = hero,
            todaysEvents = todaysEvents,
            upcomingDeadlines = upcomingDeadlines,
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
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
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
