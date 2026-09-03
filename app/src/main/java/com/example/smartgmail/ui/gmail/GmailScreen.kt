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

import com.example.smartgmail.model.Email
import com.example.smartgmail.gmail.GmailApi

import com.example.smartgmail.ai.runLLMBenchmark

import com.example.smartgmail.repository.EmailRepository
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

    var gmailAccessToken by remember {
        mutableStateOf(
            gmailManager.getAccessToken()
        )
    }

    var status by remember {
        mutableStateOf("Gmail not connected")
    }

    var emails by remember {
        mutableStateOf<List<Email>>(emptyList())
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

    }
//    Button(
//        enabled = aiReady,
//        onClick = {
//            CoroutineScope(Dispatchers.IO).launch {
//                runLLMBenchmark(aiManager.getLLM())
//            }
//        }
//    ) {
//        Text("Run LLM Benchmark")
//    }
}