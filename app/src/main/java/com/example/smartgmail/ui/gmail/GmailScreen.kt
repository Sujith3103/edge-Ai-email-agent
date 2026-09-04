package com.example.smartgmail.ui.gmail

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartgmail.SmartGmailApplication

import com.example.smartgmail.gmail.GmailApi

import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.database.entity.InboxEmail
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GmailScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val gmailApi = remember {
        GmailApi()
    }

    val app = context.applicationContext as SmartGmailApplication

    val aiManager = app.aiManager

    val gmailManager = app.gmailManager

    val gmailAuth = gmailManager.getAuth()

//    val localLLM = aiManager.getLLM()

    val aiReady by aiManager.isReady.collectAsState()

//    val modelManager = remember {
//        ModelManager(context)
//    }

    val database = app.database

    val emailRepository = remember {
        EmailRepository(
            database.emailDao()
        )
    }

    val viewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.Factory(emailRepository)
    )

    val inboxEmails by viewModel.inboxEmails.collectAsState()

    var gmailAccessToken by remember {
        mutableStateOf(
            gmailManager.getAccessToken()
        )
    }

    var status by remember {
        mutableStateOf("Gmail not connected")
    }

    val gmailAuthorizationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val data = result.data

                if (data != null) {

                    try {

                        val authorizationResult =
                            gmailAuth.getAuthorizationResult(
                                context as Activity,
                                data
                            )

                        val token =
                            gmailAuth.getAccessToken(
                                authorizationResult
                            )

                        if (token != null) {

                            gmailManager.saveAccessToken(token)

                            status =
                                "Gmail authorization successful"

                        } else {

                            status =
                                "No access token returned"
                        }

                    } catch (e: Exception) {

                        status =
                            "Authorization failed: ${e.message}"
                    }

                } else {

                    status =
                        "No authorization data returned"
                }

            } else {

                status =
                    "Gmail authorization cancelled"
            }
        }

    LaunchedEffect(Unit) {

        gmailAuth.authorize(
            activity = context as Activity,

            authorizationLauncher =
                gmailAuthorizationLauncher,

            onAuthorized = { result ->

                val token =
                    gmailAuth.getAccessToken(result)

                if (token != null) {

                    gmailManager.saveAccessToken(token)

                    gmailAccessToken = token

                    status = "Gmail authorization successful"

                    Toast.makeText(
                        context,
                        "Gmail authorization successful",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    status = "No access token returned"
                }
            },
            onError = { exception ->

                status = "Gmail authorization failed: ${exception.message}"
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {

        /*
         * HEADER / STATUS
         */

        item {
            Text(
                text = status,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        items(
            items = inboxEmails,
            key = { it.id }
        ) { email ->
            GmailEmailCard(email = email)
        }
    }
}

@Composable
fun GmailEmailCard(
    email: InboxEmail,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = email.sender.substringBefore(" <"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = email.date.substringBefore(" "),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )

            if (email.summary != null && email.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Priority Tag
            val priorityText = when {
                email.analysisStatus == "FAILED" -> "Analysis failed"
                email.priority != null -> email.priority.uppercase()
                else -> "Analyzing..."
            }

            val priorityColor = when (email.priority?.uppercase()) {
                "HIGH" -> Color(0xFFD32F2F) // Red
                "MEDIUM" -> Color(0xFFF57C00) // Orange
                "LOW" -> Color(0xFF388E3C) // Green
                else -> Color.Gray
            }

            Surface(
                color = priorityColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = priorityText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = priorityColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
