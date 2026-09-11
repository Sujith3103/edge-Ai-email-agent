package com.example.smartgmail.ui.gmail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.database.entity.EmailAnalysisEntity
import com.example.smartgmail.database.entity.InboxEmail
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    email: InboxEmail,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    var analysis by remember { mutableStateOf<EmailAnalysisEntity?>(null) }
    
    LaunchedEffect(email.id) {
        analysis = app.database.emailAnalysisDao().getAnalysis(email.id)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White),
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Delete, "Delete") }
                    IconButton(onClick = { }) { Icon(Icons.Default.Email, "Email") }
                    IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, "More") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Sender Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = email.sender.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = email.sender.substringBefore(" <"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = email.sender.substringAfter("<", "").substringBefore(">"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = email.date.replace(Regex("[+-]\\d{4}$"), "").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject
            Text(
                text = email.subject,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Priority Tag
            if (email.priority != null) {
                PriorityTagDetail(email.priority)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Card
            SummaryCard(email.summary ?: "Analyzing...")

            // Action Items
            analysis?.let {
                ExpandableListCard(
                    title = "Action Items",
                    items = parseJsonArray(it.actionItemsJson),
                    icon = Icons.Default.CheckBox,
                    accentColor = Color(0xFF0B57D0)
                ) { task ->
                    ActionItemRow(task)
                }
            }

            // Deadlines
            analysis?.let {
                val deadlines = parseDeadlines(it.deadlinesJson)
                ExpandableListCard(
                    title = "Deadline",
                    items = deadlines,
                    icon = Icons.Default.CalendarToday,
                    accentColor = Color(0xFFB3261E)
                ) { deadline ->
                    DeadlineItemRow(deadline)
                }
            }

            // Calendar Events
            analysis?.let {
                val events = parseEvents(it.calendarEventsJson)
                ExpandableListCard(
                    title = "Calendar Events",
                    items = events,
                    icon = Icons.Default.Event,
                    accentColor = Color(0xFF146C2E)
                ) { event ->
                    EventItemRow(event)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add to Calendar Button
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Add to Calendar", fontWeight = FontWeight.ExtraBold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PriorityTagDetail(priority: String) {
    val (color, container) = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFB3261E) to Color(0xFFF9DEDC)
        "MEDIUM" -> Color(0xFF8D5000) to Color(0xFFFFE082)
        else -> Color(0xFF146C2E) to Color(0xFFC4EED0)
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Flag, null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${priority.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} Priority",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SummaryCard(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Description, null, tint = Color(0xFF0B57D0))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun <T> ExpandableListCard(
    title: String,
    items: List<T>,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable (T) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accentColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text("$title (${items.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.outline)
        }

        AnimatedVisibility(visible = expanded && items.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                items.forEach { item ->
                    content(item)
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ActionItemRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.RadioButtonUnchecked, 
            null, 
            modifier = Modifier.size(20.dp).padding(top = 2.dp), 
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DeadlineItemRow(deadline: DeadlineData) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        DateBox(deadline.date, Color(0xFFB3261E))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(deadline.description, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${formatDisplayDate(deadline.date)} at ${deadline.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EventItemRow(event: EventData) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        DateBox(event.date, Color(0xFF146C2E))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(event.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${event.startTime} - ${event.endTime ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!event.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!event.description.isNullOrBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DateBox(dateStr: String?, color: Color) {
    val date = remember(dateStr) {
        try { LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE) } catch (e: Exception) { null }
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.size(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().background(color).padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date?.month?.name?.take(3) ?: "DAT",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    date?.dayOfMonth?.toString() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

data class DeadlineData(val description: String, val date: String?, val time: String?)
data class EventData(val title: String, val date: String?, val startTime: String?, val endTime: String?, val location: String?, val description: String?)

private fun parseJsonArray(json: String): List<String> {
    val list = mutableListOf<String>()
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) list.add(arr.getString(i))
    } catch (e: Exception) {}
    return list
}

private fun parseDeadlines(json: String): List<DeadlineData> {
    val list = mutableListOf<DeadlineData>()
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(DeadlineData(
                obj.getString("description"),
                obj.optString("date", ""),
                obj.optString("time", "")
            ))
        }
    } catch (e: Exception) {}
    return list
}

private fun parseEvents(json: String): List<EventData> {
    val list = mutableListOf<EventData>()
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(EventData(
                obj.getString("title"),
                obj.optString("date", ""),
                obj.optString("startTime", ""),
                obj.optString("endTime", ""),
                obj.optString("location", ""),
                obj.optString("description", "")
            ))
        }
    } catch (e: Exception) {}
    return list
}

private fun formatDisplayDate(dateStr: String?): String {
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    } catch (e: Exception) { dateStr ?: "" }
}
