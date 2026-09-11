package com.example.smartgmail.ui.navigation

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.ui.components.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SystemStatusScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    val aiManager = app.aiManager
    val gmailManager = app.gmailManager
    val scope = rememberCoroutineScope()
    
    val totalProcessed by aiManager.totalProcessed.collectAsState()
    val totalFailed by aiManager.totalFailed.collectAsState()
    val avgTimeMs by aiManager.averageTimeMs.collectAsState()
    val isAiReady by aiManager.isReady.collectAsState()

    var gmailToken by remember { mutableStateOf(gmailManager.getAccessToken()) }
    val isGmailConnected = gmailToken != null

    var statusMessage by remember { mutableStateOf("") }
    var operationInProgress by remember { mutableStateOf(false) }

    // GMAIL AUTHORIZATION LAUNCHER
    val gmailAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val authResult = gmailManager.getAuth().getAuthorizationResult(context as Activity, result.data!!)
                val token = authResult.accessToken
                if (token != null) {
                    gmailManager.saveAccessToken(token)
                    gmailToken = token
                    Toast.makeText(context, "Gmail Connected Successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gmail Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                operationInProgress = true
                statusMessage = "Importing model..."
                try {
                    withContext(Dispatchers.IO) {
                        aiManager.modelManager.importModel(uri)
                    }
                    statusMessage = "Model imported. Initializing..."
                    aiManager.initialize()
                    statusMessage = "Model ready!"
                } catch (e: Exception) {
                    statusMessage = "Error: ${e.message}"
                } finally {
                    operationInProgress = false
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // GMAIL STATUS
            StatusCardGlass(
                title = "Gmail Service",
                icon = Icons.Default.Info,
                status = if (isGmailConnected) "Connected" else "Not Connected",
                color = Color(0xFF4285F4),
                active = isGmailConnected
            )

            if (!isGmailConnected) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Gmail Connection Required", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connect your Google account to allow Brill AI to analyze your emails locally.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                gmailManager.getAuth().authorize(
                                    context as Activity,
                                    gmailAuthLauncher,
                                    onAuthorized = { result ->
                                        val token = result.accessToken
                                        if (token != null) {
                                            gmailManager.saveAccessToken(token)
                                            gmailToken = token
                                        }
                                    },
                                    onError = { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("Connect Gmail")
                        }
                    }
                }
            }

            // AI MODEL STATUS
            StatusCardGlass(
                title = "Local AI Model",
                icon = Icons.Default.Memory,
                status = if (isAiReady) aiManager.modelManager.modelId else "Not Initialized",
                color = Color(0xFF9334E6),
                active = isAiReady
            )

            if (!isAiReady) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Model Action Required", style = MaterialTheme.typography.titleSmall, color = Color(0xFFEA4335))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("The local LLM is not loaded. You need to import a GGUF model.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            enabled = !operationInProgress,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Icon(Icons.Default.UploadFile, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Model")
                        }
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Model Management", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            enabled = !operationInProgress,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Replace Model")
                        }
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4285F4))
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("AI Processing Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MetricRowGlass("Processed", totalProcessed.toString())
                    MetricRowGlass("Failed", totalFailed.toString())
                    MetricRowGlass("Avg Processing Time", "${"%.2f".format(avgTimeMs / 1000.0)} sec")
                }
            }
        }
    }
}

@Composable
fun StatusCardGlass(title: String, icon: ImageVector, status: String, color: Color, active: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (active) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF34A853))
            }
        }
    }
}

@Composable
fun MetricRowGlass(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
