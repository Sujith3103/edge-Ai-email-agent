package com.example.smartgmail.ui.llm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.repository.RetrievalRepository
import com.example.smartgmail.ui.components.GlassCard
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
                // Use the Global Analysis Mutex to prevent crashes and state corruption
                aiManager.runAnalysis { localLLM ->
                    val retrievalRepo = RetrievalRepository(
                        app.database.emailDao(), 
                        app.database.taskDao(),
                        app.database.eventDao()
                    )

                    // 1. Get Context
                    val dbContext = retrievalRepo.getRelevantContext(userMessage, localLLM)

                    // 2. Format Conversation
                    val conversation = messages.dropLast(1).joinToString("\n\n") { message ->
                        if (message.isUser) "User: ${message.text}" else "Assistant: ${message.text}"
                    }

                    val prompt = """
                        You are Brill Mail Assistant, an expert personal AI.
                        You have direct access to the user's local database of emails, tasks, and calendar events.
                        
                        CRITICAL INSTRUCTIONS:
                        1. Carefully examine the "DATABASE CONTEXT" section below. 
                        2. If the user's question relates to specific emails, deadlines, or events, use the data in the context to answer.
                        3. If the context contains relevant data, NEVER say you don't have access.
                        4. If the context is truly empty or unrelated, then answer based on your general knowledge.
                        
                        DATABASE CONTEXT:
                        $dbContext
                        
                        Conversation History:
                        $conversation
                        
                        Assistant:
                    """.trimIndent()

                    // Reset context for the final conversational response
                    localLLM.resetContext()

                    var responseText = ""
                    localLLM.generate(prompt = prompt, maxTokens = 512).collect { token ->
                        responseText += token
                        messages = messages.dropLast(1) + ChatMessage(isUser = false, text = responseText)
                    }
                }
            } catch (e: Exception) {
                messages = messages.dropLast(1) + ChatMessage(isUser = false, text = "Sorry, I encountered an error: ${e.message}")
            } finally {
                generating = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF4285F4))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Brill AI Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isAiReady) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("AI model is not ready.", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                // Chat Messages
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(messages) { message ->
                        ChatBubbleGlass(message)
                    }
                }

                // Input Area
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    cornerRadius = 32.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask anything...", color = Color.White.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { if (input.isNotBlank() && !generating) sendMessage() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4))
                        ) {
                            if (generating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleGlass(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser) Color(0xFF4285F4).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (message.isUser) Color(0xFF4285F4).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
    
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (message.isUser) 20.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 20.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
