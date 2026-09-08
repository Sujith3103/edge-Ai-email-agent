package com.example.smartgmail.ui.llm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartgmail.SmartGmailApplication
import kotlinx.coroutines.launch

data class ChatMessage(
    val isUser: Boolean,
    val text: String
)

@Composable
fun LLMScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    val aiManager = app.aiManager
    val scope = rememberCoroutineScope()
    
    val isAiReady by aiManager.isReady.collectAsState()

    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var generating by remember { mutableStateOf(false) }

    fun sendMessage() {
        val userMessage = input.trim()
        if (userMessage.isEmpty() || generating || !isAiReady) return

        input = ""
        messages = messages + ChatMessage(isUser = true, text = userMessage)
        messages = messages + ChatMessage(isUser = false, text = "")
        generating = true

        scope.launch {
            try {
                val conversation = messages.dropLast(1).joinToString("\n\n") { message ->
                    if (message.isUser) "User: ${message.text}" else "Assistant: ${message.text}"
                }

                val prompt = """
                    You are Brill Mail Assistant, a helpful AI running locally.
                    
                    Conversation:
                    $conversation
                    
                    Assistant:
                """.trimIndent()

                var responseText = ""
                aiManager.getLLM().generate(prompt = prompt, maxTokens = 512).collect { token ->
                    responseText += token
                    messages = messages.dropLast(1) + ChatMessage(isUser = false, text = responseText)
                }
            } catch (e: Exception) {
                messages = messages.dropLast(1) + ChatMessage(isUser = false, text = "Sorry, I encountered an error: ${e.message}")
            } finally {
                generating = false
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Chat Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Brill AI Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isAiReady) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Model Not Loaded", style = MaterialTheme.typography.titleMedium)
                    Text("Please go to System Status to import a model.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            // Chat Messages
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }

            // Input Area
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Brill AI...") },
                    enabled = !generating,
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                SmallFloatingActionButton(
                    onClick = { if (input.isNotBlank() && !generating) sendMessage() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (generating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isUser) 16.dp else 0.dp,
        bottomEnd = if (message.isUser) 0.dp else 16.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = contentColor,
                fontSize = 15.sp
            )
        }
    }
}
