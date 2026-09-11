package com.example.smartgmail.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.database.entity.EventEntity
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.repository.EventRepository
import com.example.smartgmail.repository.TaskRepository
import com.example.smartgmail.ui.components.GlassCard

@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    val taskRepository = remember { TaskRepository(app.database.taskDao()) }
    val eventRepository = remember { EventRepository(app.database.eventDao()) }
    
    val tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory(taskRepository))
    val eventsViewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory(eventRepository))

    val incompleteTasks by tasksViewModel.incompleteTasks.collectAsState()
    val events by eventsViewModel.allEvents.collectAsState()

    val timelineItems = remember(incompleteTasks, events) {
        (events.map { TimelineItem.Event(it) } + incompleteTasks.map { TimelineItem.Task(it) })
            .sortedBy { it.sortKey }
            .groupBy { it.date ?: "No Date" }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (timelineItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Your timeline is empty", color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }

            timelineItems.forEach { (date, items) ->
                item {
                    Surface(
                        color = Color(0xFF4285F4).copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                items(items) { item ->
                    TimelineNodeGlass(item)
                }
            }
        }
    }
}

sealed class TimelineItem {
    abstract val date: String?
    abstract val time: String?
    abstract val title: String
    abstract val subtitle: String?
    
    val sortKey: String
        get() = "${date ?: "9999-12-31"}_${time ?: "23:59"}"

    data class Event(val event: EventEntity) : TimelineItem() {
        override val date = event.date
        override val time = event.startTime
        override val title = event.title
        override val subtitle = event.location
    }

    data class Task(val task: TaskEntity) : TimelineItem() {
        override val date = task.dueDate
        override val time = task.dueTime
        override val title = task.description
        override val subtitle = "Extracted Task"
    }
}

@Composable
fun TimelineNodeGlass(item: TimelineItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 1. Time Column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(56.dp)
        ) {
            Text(
                text = item.time ?: "--:--",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // 2. Connector Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
        ) {
            // Node circle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (item is TimelineItem.Event) Color(0xFF4285F4)
                        else Color(0xFF9334E6)
                    )
            )
            
            // Vertical line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }

        // 3. Content Column
        Box(modifier = Modifier.padding(bottom = 24.dp, end = 8.dp).weight(1f)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item is TimelineItem.Event) Icons.Default.Event else Icons.Default.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (item is TimelineItem.Event) Color(0xFF4285F4) else Color(0xFF9334E6)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (item.subtitle != null) {
                            Text(
                                text = item.subtitle!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
