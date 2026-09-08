package com.example.smartgmail.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.repository.EventRepository
import com.example.smartgmail.repository.TaskRepository

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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER SECTION
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Daily Digest",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // GREETING
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text(
                        text = state.greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Here's what happened today.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // STATS SECTION
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewInbox() }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatRow(Icons.Default.Email, "${state.inboxStats.receivedCount} emails received")
                    StatRow(Icons.Default.Bolt, "${state.inboxStats.actionCount} require action")
                    StatRow(Icons.Default.CalendarToday, "${state.inboxStats.eventCount} events detected")
                }
            }

            // CATEGORIZED ITEMS
            state.categorizedItems.forEach { (category, items) ->
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    items.forEach { briefItem ->
                        BriefItemRow(briefItem)
                    }
                }
            }

            // FOOTER
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "You're almost caught up ✓",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BriefItemRow(item: DailyBriefViewModel.BriefItem) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = item.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
