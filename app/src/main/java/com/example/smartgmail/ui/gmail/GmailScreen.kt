package com.example.smartgmail.ui.gmail

import android.app.Activity
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.ai.runLLMBenchmark
import com.example.smartgmail.database.entity.InboxEmail
import com.example.smartgmail.gmail.GmailApi
import com.example.smartgmail.repository.EmailRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun GmailScreen(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val gmailApi = remember {
        GmailApi()
    }

    val app =
        context.applicationContext as SmartGmailApplication


    // ============================================================
    // APP MANAGERS
    // ============================================================

    val aiManager =
        app.aiManager

    val gmailManager =
        app.gmailManager

    val gmailAuth =
        gmailManager.getAuth()


    // ============================================================
    // AI STATE
    // ============================================================

    val aiReady by
    aiManager.isReady.collectAsState()


    // ============================================================
    // DATABASE
    // ============================================================

    val database =
        app.database

    val emailRepository =
        remember {
            EmailRepository(
                database.emailDao()
            )
        }


    // ============================================================
    // INBOX VIEWMODEL
    // ============================================================

    val viewModel: InboxViewModel =
        viewModel(
            factory =
                InboxViewModel.Factory(
                    emailRepository
                )
        )

    val inboxEmails by
    viewModel.inboxEmails.collectAsState()


    // ============================================================
    // GMAIL AUTH STATE
    // ============================================================

    var gmailAccessToken by remember {

        mutableStateOf(
            gmailManager.getAccessToken()
        )
    }

    var status by remember {

        mutableStateOf(
            "Gmail not connected"
        )
    }


    // ============================================================
    // BENCHMARK STATE
    // ============================================================

    var benchmarkRunning by remember {

        mutableStateOf(false)
    }


    // ============================================================
    // GMAIL AUTHORIZATION LAUNCHER
    // ============================================================

    val gmailAuthorizationLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts
                    .StartIntentSenderForResult()

        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val data =
                    result.data

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

                            gmailManager.saveAccessToken(
                                token
                            )

                            gmailAccessToken =
                                token

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


    // ============================================================
    // GMAIL AUTHORIZATION
    // ============================================================

    LaunchedEffect(Unit) {

        gmailAuth.authorize(

            activity =
                context as Activity,

            authorizationLauncher =
                gmailAuthorizationLauncher,

            onAuthorized = { result ->

                val token =
                    gmailAuth.getAccessToken(
                        result
                    )

                if (token != null) {

                    gmailManager.saveAccessToken(
                        token
                    )

                    gmailAccessToken =
                        token

                    status =
                        "Gmail authorization successful"

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

                status =
                    "Gmail authorization failed: ${exception.message}"
            }
        )
    }


    // ============================================================
    // UI
    // ============================================================

    LazyColumn(

        modifier =
            modifier.fillMaxWidth(),

        contentPadding =
            PaddingValues(
                bottom = 16.dp
            )
    ) {


        // ========================================================
        // GMAIL STATUS
        // ========================================================

        item {

            Text(

                text = status,

                modifier =
                    Modifier.padding(
                        16.dp
                    ),

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .secondary
            )
        }


        // ========================================================
        // LLM BENCHMARK BUTTON
        // ========================================================

        item {

            Button(

                enabled =
                    aiReady &&
                            !benchmarkRunning,

                onClick = {

                    scope.launch {

                        benchmarkRunning =
                            true

                        try {

                            /*
                             * Get the already initialized
                             * LocalLLM instance.
                             */

                            val localLLM =
                                aiManager.getLLM()


                            /*
                             * Run the complete benchmark.
                             *
                             * This calls:
                             *
                             * 5000 PP
                             * 6000 PP
                             * 7000 PP
                             *
                             * with tg = 1.
                             */

                            withContext(
                                Dispatchers.Default
                            ) {

                                runLLMBenchmark(
                                    localLLM
                                )
                            }


                            Toast.makeText(
                                context,
                                "LLM benchmark complete. Check Logcat.",
                                Toast.LENGTH_LONG
                            ).show()


                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Benchmark failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                        } finally {

                            benchmarkRunning =
                                false
                        }
                    }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
            ) {

                Text(

                    text =
                        when {

                            benchmarkRunning ->
                                "Benchmark running..."

                            !aiReady ->
                                "AI Model Not Ready"

                            else ->
                                "Run LLM Benchmark"
                        }
                )
            }
        }


        // ========================================================
        // INBOX EMAILS
        // ========================================================

        items(

            items =
                inboxEmails,

            key = {
                it.id
            }

        ) { email ->

            GmailEmailCard(
                email = email
            )
        }
    }
}


// =================================================================
// EMAIL CARD
// =================================================================

@Composable
fun GmailEmailCard(

    email: InboxEmail,

    modifier: Modifier = Modifier

) {

    Card(

        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp,
                    horizontal = 8.dp
                )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {


            // ====================================================
            // SENDER + DATE
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(

                    text =
                        email.sender
                            .substringBefore(" <"),

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines = 1
                )

                Text(

                    text =
                        email.date
                            .substringBefore(" "),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            // ====================================================
            // SUBJECT
            // ====================================================

            Text(

                text =
                    email.subject,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                fontWeight =
                    FontWeight.Medium,

                maxLines = 2
            )


            // ====================================================
            // AI SUMMARY
            // ====================================================

            if (
                email.summary != null &&
                email.summary.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(

                    text =
                        email.summary,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines = 2
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            // ====================================================
            // PRIORITY
            // ====================================================

            val priorityText =

                when {

                    email.analysisStatus
                        ?.uppercase() ==
                            "FAILED" -> {

                        "Analysis failed"
                    }

                    email.priority != null -> {

                        email.priority
                            .uppercase()
                    }

                    else -> {

                        "Analyzing..."
                    }
                }


            val priorityColor =

                when (
                    email.priority
                        ?.uppercase()
                ) {

                    "HIGH" ->
                        Color(0xFFD32F2F)

                    "MEDIUM" ->
                        Color(0xFFF57C00)

                    "LOW" ->
                        Color(0xFF388E3C)

                    else ->
                        Color.Gray
                }


            Surface(

                color =
                    priorityColor.copy(
                        alpha = 0.1f
                    ),

                shape =
                    MaterialTheme
                        .shapes
                        .small
            ) {

                Text(

                    text =
                        priorityText,

                    modifier =
                        Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),

                    color =
                        priorityColor,

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}