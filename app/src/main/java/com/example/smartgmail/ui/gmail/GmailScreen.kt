package com.example.smartgmail.ui.gmail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.database.entity.InboxEmail
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.ui.GmailScreenMode
import com.example.smartgmail.ui.components.GlassCard

@Composable
fun GmailScreen(
    modifier: Modifier = Modifier,
    mode: GmailScreenMode = GmailScreenMode.INBOX,
    onEmailClick: (InboxEmail) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication

    val aiManager = app.aiManager
    val emailRepository = remember { EmailRepository(app.database.emailDao()) }

    val viewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.Factory(emailRepository, aiManager)
    )

    val emails by when (mode) {
        GmailScreenMode.INBOX -> viewModel.inboxEmails
        GmailScreenMode.FAILED -> viewModel.failedEmails
        GmailScreenMode.DELETED -> viewModel.deletedEmails
    }.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = when (mode) {
                            GmailScreenMode.INBOX -> "Inbox"
                            GmailScreenMode.FAILED -> "Failed Processing"
                            GmailScreenMode.DELETED -> "Trash"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = when (mode) {
                            GmailScreenMode.INBOX -> "Emails analyzed by Brill AI"
                            GmailScreenMode.FAILED -> "Analysis errors found here"
                            GmailScreenMode.DELETED -> "Discarded emails"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (emails.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.height(400.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No emails found",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            items(emails, key = { it.id }) { email ->
                GmailEmailCardGlass(
                    email = email,
                    mode = mode,
                    onClick = { onEmailClick(email) },
                    onDelete = { viewModel.deleteEmail(email.id) },
                    onRestore = { viewModel.restoreEmail(email.id) },
                    onDeletePermanently = { viewModel.deletePermanently(email.id) }
                )
            }
        }
        
        if (mode == GmailScreenMode.FAILED && emails.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.refreshFailed(context) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                icon = { Icon(Icons.Default.Refresh, null) },
                text = { Text("Retry All") },
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White
            )
        }
    }
}

@Composable
fun GmailEmailCardGlass(
    email: InboxEmail,
    mode: GmailScreenMode,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = email.sender.substringBefore(" <"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = email.date.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!email.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = email.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == GmailScreenMode.INBOX) {
                    PriorityTagGlass(priority = email.priority, status = email.analysisStatus)
                }

                Row {
                    if (mode != GmailScreenMode.DELETED) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.White.copy(alpha = 0.4f))
                        }
                    } else {
                        IconButton(onClick = onRestore) {
                            Icon(Icons.Default.Restore, "Restore", tint = Color(0xFF34A853))
                        }
                        IconButton(onClick = onDeletePermanently) {
                            Icon(Icons.Default.DeleteForever, "Delete Forever", tint = Color(0xFFEA4335))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityTagGlass(priority: String?, status: String?) {
    val isAnalyzing = status?.uppercase() == "ANALYZING"
    val color = when (priority?.uppercase()) {
        "HIGH" -> Color(0xFFEA4335)
        "MEDIUM" -> Color(0xFFFBBC05)
        "LOW" -> Color(0xFF34A853)
        else -> Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp, color = color)
            } else {
                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAnalyzing) "Analyzing" else (priority?.uppercase() ?: "Pending"),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
