package com.example.smartgmail.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.database.entity.EventEntity
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.repository.EventRepository
import com.example.smartgmail.repository.TaskRepository

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    showCompletedOnly: Boolean = false,
    onRefreshSent: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    val taskRepository = remember { TaskRepository(app.database.taskDao()) }
    val eventRepository = remember { EventRepository(app.database.eventDao()) }
    
    val tasksViewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.Factory(taskRepository)
    )
    val eventsViewModel: EventsViewModel = viewModel(
        factory = EventsViewModel.Factory(eventRepository)
    )

    val incompleteTasks by tasksViewModel.incompleteTasks.collectAsState()
    val completedTasks by tasksViewModel.completedTasks.collectAsState()
    val events by eventsViewModel.allEvents.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (showCompletedOnly && onRefreshSent != null) {
                ExtendedFloatingActionButton(
                    onClick = onRefreshSent,
                    icon = { Icon(Icons.Default.Refresh, null) },
                    text = { Text("Refresh Sent Mails") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (showCompletedOnly) "Completed Tasks" else "Tasks & Events",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!showCompletedOnly) {
                // EVENTS SECTION
                if (events.isNotEmpty()) {
                    item {
                        Text(
                            text = "Events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(events, key = { "event_${it.id}" }) { event ->
                        EventCard(
                            event = event,
                            onDelete = { eventsViewModel.deleteEvent(event.id) }
                        )
                    }
                }

                // TASKS SECTION
                if (incompleteTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Upcoming Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(incompleteTasks, key = { "task_${it.id}" }) { task ->
                        TaskCard(
                            task = task,
                            onCheckedChange = { tasksViewModel.updateTaskCompletion(task, it) },
                            onDelete = { tasksViewModel.deleteTask(task.id) }
                        )
                    }
                }

                if (events.isEmpty() && incompleteTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nothing scheduled yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // COMPLETED TASKS ONLY
                if (completedTasks.isNotEmpty()) {
                    items(completedTasks, key = { "task_${it.id}" }) { task ->
                        TaskCard(
                            task = task,
                            onCheckedChange = { tasksViewModel.updateTaskCompletion(task, it) },
                            onDelete = { tasksViewModel.deleteTask(task.id) }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No completed tasks",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: EventEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (event.date != null) {
                Text(
                    text = "${event.date} ${event.startTime ?: ""} ${if (event.endTime != null) "- ${event.endTime}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            if (event.location != null) {
                Row(
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.location, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = onCheckedChange
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.completed) 
                        androidx.compose.ui.text.style.TextDecoration.LineThrough 
                    else null
                )

                if (task.dueDate != null || task.dueTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(task.dueDate, task.dueTime).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
