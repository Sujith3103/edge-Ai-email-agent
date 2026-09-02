package com.example.smartgmail.ui.gmail

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

import com.example.smartgmail.model.Email
import com.example.smartgmail.context.EmailContextBuilder
import com.example.smartgmail.gmail.GmailApi
import com.example.smartgmail.gmail.GmailAuth
import com.example.smartgmail.gmail.GmailMessageParser
import com.example.smartgmail.ai.EmailAnalyzer
import com.example.smartgmail.ai.LocalLLM
import com.example.smartgmail.model.ModelManager
import com.example.smartgmail.database.DatabaseProvider
import com.example.smartgmail.database.AppDatabase
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.worker.GmailSyncScheduler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


import kotlinx.coroutines.launch

@Composable
fun GmailScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val gmailAuth = remember {
        GmailAuth()
    }

    val gmailApi = remember {
        GmailApi()
    }

    val localLLM = remember {
        LocalLLM(context)
    }

    val modelManager = remember {
        ModelManager(context)
    }

    val database = remember {
        DatabaseProvider.getDatabase(context)
    }

    val emailRepository = remember {
        EmailRepository(
            database.emailDao()
        )
    }

    var gmailAccessToken by remember {
        mutableStateOf<String?>(null)
    }

    var status by remember {
        mutableStateOf("Gmail not connected")
    }

    var emails by remember {
        mutableStateOf<List<Email>>(emptyList())
    }

    var modelLoaded by remember {
        mutableStateOf(false)
    }

    var modelLoading by remember {
        mutableStateOf(false)
    }



    val gmailAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
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

                    gmailAccessToken =
                        gmailAuth.getAccessToken(
                            authorizationResult
                        )

                    if (gmailAccessToken != null) {

                        status = "Gmail authorization successful"

                    } else {

                        status = "No access token returned"
                    }

                } catch (e: Exception) {

                    status = "Authorization failed: ${e.message}"
                }

            } else {

                status = "No authorization data returned"
            }

        } else {

            status = "Gmail authorization cancelled"
        }
    }

    LaunchedEffect(Unit) {

        if (!modelManager.isModelInstalled()) {

            return@LaunchedEffect
        }

        if (modelLoaded || modelLoading) {

            return@LaunchedEffect
        }

        modelLoading = true

        try {

            val modelPath =
                modelManager
                    .modelFile()
                    .absolutePath

            withContext(Dispatchers.IO) {

                localLLM.loadModel(
                    modelPath
                )
            }

            modelLoaded = true

            status = "AI model ready"

        } catch (e: Exception) {

            status =
                "AI model loading failed: ${e.message}"

        } finally {

            modelLoading = false
        }
    }

    LaunchedEffect(Unit) {

        gmailAuth.authorize(
            activity = context as Activity,

            authorizationLauncher =
                gmailAuthorizationLauncher,

            onAuthorized = { result ->

                gmailAccessToken = gmailAuth.getAccessToken(result)

                if (gmailAccessToken != null) {

                    context
                        .getSharedPreferences(
                            "gmail_auth",
                            Context.MODE_PRIVATE
                        )
                        .edit {
                            putString(
                                "access_token",
                                gmailAccessToken
                            )
                        }
                    status = "Gmail authorization successful"

                    Toast.makeText(
                        context,
                        "Gmail authorization successful",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    status =
                        "No access token returned"
                }
            },

            onError = { exception ->

                status = "Gmail authorization failed: ${exception.message}"
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {

        /*
         * HEADER / STATUS
         */

        item {

            Text(
                text = status,
                modifier = Modifier.padding(
                    bottom = 8.dp
                )
            )
        }

        /*
         * LOAD GMAIL BUTTON
         */

        if (gmailAccessToken != null) {

            item {

                Button(
                    onClick = {

                        status = "Loading Gmail messages..."

                        scope.launch {

                            try {

                                val messages =
                                    gmailApi.listMessages(
                                        accessToken = gmailAccessToken!!,
                                        maxResults = 10,
                                        query = "newer_than:1d"
                                    )

                                val loadedEmails = messages.map { message ->

                                    val fullMessage = gmailApi.getMessage(
                                        accessToken = gmailAccessToken!!,
                                        messageId = message.id
                                    )

                                    GmailMessageParser.parse(fullMessage)
                                }

                                emails = loadedEmails

                                status = "${emails.size} emails loaded"

                            } catch (e: Exception) {

                                status = "Gmail API failed: ${e.message}"
                            }
                        }
                    }
                ) {

                    Text("Load Gmail")
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            /*
             * EMAIL LIST
             */

            items(1) { email ->

                if (emails.isNotEmpty()) {

                    val email = emails.first()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp,
                                horizontal = 8.dp
                            )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Text(
                                text = "EMAIL TO ANALYZE",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
//                            ----------------SAVE THE EMAIL------------------------------
                            Button(
                                enabled = emails.isNotEmpty(),
                                onClick = {

                                    scope.launch {

                                        try {

                                            val email = emails.first()

                                            val saved =
                                                emailRepository.saveIfNew(email)

                                            if (saved) {

                                                println(
                                                    "FIRST EMAIL SUCCESSFULLY SAVED"
                                                )

                                            } else {

                                                println(
                                                    "FIRST EMAIL WAS ALREADY IN DATABASE"
                                                )
                                            }

                                        } catch (e: Exception) {

                                            println(
                                                "EMAIL SAVE FAILED: ${e.message}"
                                            )
                                        }
                                    }
                                }
                            ) {

                                Text("Save Email")
                            }

                            Text(
                                text = email.subject,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "From: ${email.sender}"
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = email.body,
                                maxLines = 3
                            )
                        }
                    }
                }
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    enabled =
                        !modelLoading &&
                                modelLoaded,
                    onClick = {

                        scope.launch {

                            try {

                                val email = emails.first()

                                val analyzer =
                                    EmailAnalyzer(localLLM)

                                val analysis =
                                    analyzer.analyze(email)

                                println("========== EMAIL ANALYSIS ==========")
                                println("Priority: ${analysis.priority}")
                                println("Summary: ${analysis.summary}")
                                println("Action Items: ${analysis.actionItems}")
                                println("Deadlines: ${analysis.deadlines}")
                                println("Calendar Events: ${analysis.calendarEvents}")
                                println("====================================")

                            } catch (e: Exception) {

                                println(
                                    "EMAIL ANALYSIS FAILED: ${e.message}"
                                )
                            }
                        }
                    }
                ) {

                    Text(
                        if (modelLoading) {
                            "Loading AI..."
                        } else {
                            "Analyze Email"
                        }
                    )                }
            }
//            items(emails) { email ->
//
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(
//                            vertical = 4.dp,
//                            horizontal = 8.dp
//                        )
//                ) {
//
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp)
//                    ) {
//
//                        Text(
//                            text = email.subject,
//                            fontWeight = FontWeight.Bold
//                        )
//
//                        Spacer(
//                            modifier = Modifier.height(4.dp)
//                        )
//
//                        Text(
//                            text = email.sender
//                        )
//
//                        Spacer(
//                            modifier = Modifier.height(4.dp)
//                        )
//
//                        Text(
//                            text = email.body,
//                            maxLines = 2
//                        )
//                    }
//                }
//            }

        }
    }
}