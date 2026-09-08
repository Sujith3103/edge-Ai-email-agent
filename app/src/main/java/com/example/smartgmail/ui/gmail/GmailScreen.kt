package com.example.smartgmail.ui.gmail

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun GmailScreen(
    modifier: Modifier = Modifier,
    mode: GmailScreenMode = GmailScreenMode.INBOX,
    onEmailClick: (InboxEmail) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication

    val aiManager = app.aiManager
    val gmailManager = app.gmailManager
    val gmailAuth = gmailManager.getAuth()

    val database = app.database
    val emailRepository = remember { EmailRepository(database.emailDao()) }

    val viewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.Factory(emailRepository, aiManager)
    )

    val emails by when (mode) {
        GmailScreenMode.INBOX -> viewModel.inboxEmails
        GmailScreenMode.FAILED -> viewModel.failedEmails
        GmailScreenMode.DELETED -> viewModel.deletedEmails
    }.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (mode == GmailScreenMode.FAILED && emails.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.refreshFailed(context) },
                    icon = { Icon(Icons.Default.Refresh, null) },
                    text = { Text("Retry All") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (mode) {
                            GmailScreenMode.INBOX -> "Emails analyzed by Brill AI"
                            GmailScreenMode.FAILED -> "Analysis errors found here"
                            GmailScreenMode.DELETED -> "Discarded emails"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            if (emails.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .height(400.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No emails here",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Pull down to sync or check later",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            items(emails, key = { it.id }) { email ->
                GmailEmailCard(
                    email = email,
                    mode = mode,
                    onClick = { onEmailClick(email) },
                    onDelete = { viewModel.deleteEmail(email.id) },
                    onRestore = { viewModel.restoreEmail(email.id) },
                    onDeletePermanently = { viewModel.deletePermanently(email.id) }
                )
            }
        }
    }
}

@Composable
fun GmailEmailCard(
    email: InboxEmail,
    mode: GmailScreenMode,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email.sender.substringAfter("<").substringBefore(">"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = email.date.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!email.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = email.summary,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Tag
                if (mode == GmailScreenMode.INBOX) {
                    PriorityTag(priority = email.priority, status = email.analysisStatus)
                } else if (mode == GmailScreenMode.FAILED) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Processing Failed",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "Deleted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Action Buttons
                Row {
                    if (mode != GmailScreenMode.DELETED) {
                        FilledTonalIconButton(
                            onClick = onDelete,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Default.Delete, "Move to Trash")
                        }
                    } else {
                        IconButton(onClick = onRestore) {
                            Icon(
                                Icons.Default.Restore,
                                "Restore",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDeletePermanently) {
                            Icon(
                                Icons.Default.DeleteForever,
                                "Delete Forever",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityTag(priority: String?, status: String?) {
    val isAnalyzing = status?.uppercase() == "ANALYZING"
    val color = when (priority?.uppercase()) {
        "HIGH" -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFF57C00)
        "LOW" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Surface(
        color = if (isAnalyzing) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = 0.15f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isAnalyzing) MaterialTheme.colorScheme.outline else color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAnalyzing) "ANALYZING..." else (priority?.uppercase() ?: "PENDING"),
                style = MaterialTheme.typography.labelSmall,
                color = if (isAnalyzing) MaterialTheme.colorScheme.onSurfaceVariant else color,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
