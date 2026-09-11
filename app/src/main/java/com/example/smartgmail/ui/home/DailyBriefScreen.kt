package com.example.smartgmail.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.repository.EventRepository
import com.example.smartgmail.repository.TaskRepository
import com.example.smartgmail.ui.components.GlassCard
import com.example.smartgmail.ui.gmail.DateBox

@Composable
fun DailyBriefScreen(
    modifier: Modifier = Modifier,
    onViewInbox: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    
    val emailRepo = remember { EmailRepository(app.database.emailDao()) }
    val taskRepo = remember { TaskRepository(app.database.taskDao()) }
    val eventRepo = remember { EventRepository(app.database.eventDao()) }

    val viewModel: DailyBriefViewModel = viewModel(
        factory = DailyBriefViewModel.Factory(emailRepo, taskRepo, eventRepo)
    )

    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // HEADER
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${state.greeting}, Sujith",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Turn emails\ninto progress.",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your AI assistant keeps you ahead\nso you can focus on what matters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )
                    }
                    
                    // The Glowing Orb from Image
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4285F4),
                                        Color(0xFF9334E6).copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // STAT CARDS (Glassmorphism style)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckBox,
                        value = state.inboxStats.taskCount.toString(),
                        label = "Tasks",
                        color = Color(0xFF9334E6)
                    )
                    GlassStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        value = state.inboxStats.eventCount.toString(),
                        label = "Events",
                        color = Color(0xFF34A853)
                    )
                    GlassStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Flag,
                        value = state.inboxStats.highPriorityCount.toString(),
                        label = "Priority",
                        color = Color(0xFFEA4335)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // HERO CARD (Glassmorphism with depth)
            state.heroItem?.let { hero ->
                item {
                    Text(
                        "Next up",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Flag, null, tint = Color(0xFFEA4335))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    hero.priority,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFEA4335),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    hero.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    hero.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // TODAY'S CALENDAR
            item {
                SectionHeaderGlass(icon = Icons.Default.CalendarToday, title = "Upcoming Schedule")
            }
            if (state.todaysEvents.isEmpty()) {
                item {
                    EmptyStateGlass("Nothing scheduled")
                }
            } else {
                items(state.todaysEvents) { event ->
                    EventRowGlass(event)
                }
            }

            // UPCOMING DEADLINES
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeaderGlass(icon = Icons.Default.Schedule, title = "Upcoming Deadlines")
            }
            if (state.upcomingDeadlines.isEmpty()) {
                item {
                    EmptyStateGlass("No upcoming deadlines")
                }
            } else {
                items(state.upcomingDeadlines) { task ->
                    DeadlineRowGlass(task)
                }
            }

            // SMART CATEGORIES
            state.categorizedItems.forEach { (category, items) ->
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        items.forEach { briefItem ->
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                Text(briefItem.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(briefItem.detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                                HorizontalDivider(modifier = Modifier.padding(top = 12.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun GlassStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Surface(
        modifier = modifier.height(130.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(color = color.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun SectionHeaderGlass(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color(0xFF4285F4))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
fun EventRowGlass(event: com.example.smartgmail.database.entity.EventEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(80.dp)) {
                Text(event.startTime ?: "--:--", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(event.endTime ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4285F4), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                if (!event.location.isNullOrBlank()) {
                    Text(
                        text = event.location, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
    }
}

@Composable
fun DeadlineRowGlass(task: com.example.smartgmail.database.entity.TaskEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            DateBox(task.dueDate, Color(0xFFEA4335))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(task.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "${if (task.dueDate == java.time.LocalDate.now().toString()) "Today" else "Tomorrow"}, ${task.dueTime ?: "All day"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
    }
}

@Composable
fun EmptyStateGlass(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
    }
}
