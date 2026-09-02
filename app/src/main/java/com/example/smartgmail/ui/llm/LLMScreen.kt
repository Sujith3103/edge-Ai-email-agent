package com.example.smartgmail.ui.llm

import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import com.example.smartgmail.ai.LocalLLM
import com.example.smartgmail.model.ModelManager
import com.example.smartgmail.ui.startup.StartupScreen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class ChatMessage(
    val isUser: Boolean,
    val text: String
)


@Composable
fun LLMScreen(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()


    // =========================================================
    // LOCAL MODEL
    // =========================================================

    val modelManager = remember {
        ModelManager(context)
    }

    val localLLM = remember {
        LocalLLM(context)
    }


    // =========================================================
    // MODEL STATE
    // =========================================================

    var modelInstalled by remember {
        mutableStateOf(
            modelManager.isModelInstalled()
        )
    }

    var modelLoaded by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var importing by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // STARTUP STATE
    // =========================================================

    var startupLoading by remember {
        mutableStateOf(true)
    }


    // =========================================================
    // STATUS
    // =========================================================

    var status by remember {

        mutableStateOf(
            if (modelInstalled) {
                "Model installed"
            } else {
                "Model not installed"
            }
        )
    }


    // =========================================================
    // CHAT STATE
    // =========================================================

    var input by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf(
            listOf<ChatMessage>()
        )
    }

    var generating by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // FILE PICKER
    // =========================================================

    val filePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            // User cancelled
            if (uri == null) {

                status =
                    "Model selection cancelled"

                return@rememberLauncherForActivityResult
            }


            importing = true

            modelLoaded = false

            status =
                "Importing model..."


            scope.launch {

                try {

                    // -----------------------------------------
                    // IMPORT
                    // -----------------------------------------

                    withContext(Dispatchers.IO) {

                        modelManager.importModel(uri)
                    }


                    modelInstalled = true

                    status =
                        "Model imported"


                    // -----------------------------------------
                    // LOAD
                    // -----------------------------------------

                    loading = true

                    status =
                        "Loading model..."


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

                    status =
                        "Ready"


                } catch (e: Exception) {

                    modelLoaded = false

                    status =
                        "Import failed: ${e.message}"

                } finally {

                    importing = false
                    loading = false
                }
            }
        }


    // =========================================================
    // LOAD EXISTING MODEL AT STARTUP
    // =========================================================

    LaunchedEffect(Unit) {

        if (!modelInstalled) {

            /*
             * No model exists.
             *
             * Stop startup loading and let the
             * Import Model button be displayed.
             */

            status =
                "Model not installed"

            startupLoading = false

            return@LaunchedEffect
        }


        // ---------------------------------------------
        // MODEL EXISTS
        // ---------------------------------------------

        loading = true

        status =
            "Loading model..."


        try {

            val modelPath =
                modelManager
                    .modelFile()
                    .absolutePath


            println(
                "========== MODEL STARTUP =========="
            )

            println(
                "Model path = $modelPath"
            )

            println(
                "Model exists = ${
                    modelManager.isModelInstalled()
                }"
            )


            withContext(Dispatchers.IO) {

                localLLM.loadModel(
                    modelPath
                )
            }


            modelLoaded = true

            status =
                "Ready"


            println(
                "MODEL LOADED SUCCESSFULLY"
            )


        } catch (e: Exception) {

            modelLoaded = false

            status =
                "Model loading failed: ${e.message}"


            println(
                "MODEL LOADING FAILED: ${e.message}"
            )


        } finally {

            loading = false
            startupLoading = false
        }
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    fun sendMessage() {

        val userMessage =
            input.trim()


        if (
            userMessage.isEmpty() ||
            generating ||
            !modelLoaded
        ) {

            return
        }


        input = ""


        // Add user message
        messages =
            messages +
                    ChatMessage(
                        isUser = true,
                        text = userMessage
                    )


        // Add empty assistant message
        messages =
            messages +
                    ChatMessage(
                        isUser = false,
                        text = ""
                    )


        generating = true

        status =
            "Generating..."


        scope.launch {

            try {

                // -----------------------------------------
                // BUILD CONVERSATION
                // -----------------------------------------

                val conversation =
                    messages
                        .dropLast(1)
                        .joinToString("\n\n") { message ->

                            if (message.isUser) {

                                "User: ${message.text}"

                            } else {

                                "Assistant: ${message.text}"
                            }
                        }


                val prompt = """
                    You are SmartGmail, a helpful AI assistant
                    running locally on an Android phone.

                    Conversation:

                    $conversation

                    Assistant:
                """.trimIndent()


                var response = ""


                // -----------------------------------------
                // GENERATE
                // -----------------------------------------

                localLLM
                    .generate(
                        prompt = prompt,
                        maxTokens = 256
                    )
                    .collect { token ->

                        response += token


                        messages =
                            messages.dropLast(1) +
                                    ChatMessage(
                                        isUser = false,
                                        text = response
                                    )
                    }


                status =
                    "Ready"


            } catch (e: Exception) {

                status =
                    "Generation failed: ${e.message}"


                messages =
                    messages.dropLast(1)


            } finally {

                generating = false
            }
        }
    }


    // =========================================================
    // UI
    // =========================================================

    Column(
        modifier =
            modifier.fillMaxSize()
    ) {


        // =====================================================
        // HEADER
        // =====================================================

        Text(
            text =
                "SmartGmail AI",

            modifier =
                Modifier.padding(
                    bottom = 8.dp
                )
        )


        // =====================================================
        // STATUS
        // =====================================================

        Text(
            text =
                when {

                    importing ->
                        "Importing model..."

                    loading ->
                        "Loading model..."

                    modelLoaded ->
                        "Qwen model ready"

                    modelInstalled ->
                        "Model installed"

                    else ->
                        "AI model not available"
                },

            modifier =
                Modifier.padding(
                    bottom = 12.dp
                )
        )


        // =====================================================
        // MODEL SETUP
        // =====================================================

        if (!modelInstalled) {

            /*
             * MODEL DOES NOT EXIST
             *
             * Show import button.
             */

            Text(
                text =
                    "Qwen model is not installed.",

                modifier =
                    Modifier.padding(
                        bottom = 8.dp
                    )
            )


            Button(

                enabled =
                    !importing &&
                            !loading,

                onClick = {

                    println(
                        "OPENING MODEL FILE PICKER"
                    )


                    filePicker.launch(
                        arrayOf(
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    if (importing) {
                        "Importing..."
                    } else {
                        "Import Qwen Model"
                    }
                )
            }


        } else {

            // =================================================
            // MODEL EXISTS
            // =================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {


                // ---------------------------------------------
                // REPLACE MODEL
                // ---------------------------------------------

                Button(

                    enabled =
                        !importing &&
                                !loading &&
                                !generating,

                    onClick = {

                        println(
                            "OPENING MODEL FILE PICKER"
                        )


                        filePicker.launch(
                            arrayOf(
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        "Replace Model"
                    )
                }


                // ---------------------------------------------
                // LOAD MODEL
                // ---------------------------------------------

                if (!modelLoaded) {

                    Button(

                        enabled =
                            !loading &&
                                    !importing,

                        onClick = {

                            loading = true

                            status =
                                "Loading model..."


                            scope.launch {

                                try {

                                    val modelPath =
                                        modelManager
                                            .modelFile()
                                            .absolutePath


                                    withContext(
                                        Dispatchers.IO
                                    ) {

                                        localLLM.loadModel(
                                            modelPath
                                        )
                                    }


                                    modelLoaded = true

                                    status =
                                        "Ready"


                                } catch (e: Exception) {

                                    modelLoaded = false

                                    status =
                                        "Model loading failed: ${e.message}"

                                } finally {

                                    loading = false
                                }
                            }
                        }
                    ) {

                        Text(
                            if (loading) {
                                "Loading..."
                            } else {
                                "Load Model"
                            }
                        )
                    }
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =====================================================
        // STARTUP SCREEN
        // =====================================================

        if (startupLoading) {

            StartupScreen()
        }


        // =====================================================
        // CHAT
        // =====================================================

        if (modelLoaded) {

            LazyColumn(

                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(messages) { message ->

                    Text(

                        text =
                            if (message.isUser) {

                                "You: ${message.text}"

                            } else {

                                "AI: ${message.text}"
                            },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            // =================================================
            // CHAT INPUT
            // =================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedTextField(

                    value =
                        input,

                    onValueChange = {
                        input = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    enabled =
                        !generating,

                    placeholder = {
                        Text(
                            "Ask something..."
                        )
                    }
                )


                Spacer(
                    modifier =
                        Modifier.padding(
                            horizontal = 4.dp
                        )
                )


                Button(

                    enabled =
                        input.isNotBlank() &&
                                !generating,

                    onClick = {
                        sendMessage()
                    }
                ) {

                    Text(
                        "Send"
                    )
                }
            }
        }
    }
}