package com.example.smartgmail.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartgmail.SmartGmailApplication
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
    val scope = rememberCoroutineScope()
    
    val totalProcessed by aiManager.totalProcessed.collectAsState()
    val totalFailed by aiManager.totalFailed.collectAsState()
    val avgTimeMs by aiManager.averageTimeMs.collectAsState()
    val isAiReady by aiManager.isReady.collectAsState()

    var statusMessage by remember { mutableStateOf("") }
    var operationInProgress by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "System Status",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        StatusCard(
            title = "Gmail Service",
            icon = Icons.Default.Info,
            status = "Connected",
            color = MaterialTheme.colorScheme.primary,
            active = true
        )

        StatusCard(
            title = "Local AI Model",
            icon = Icons.Default.Memory,
            status = if (isAiReady) aiManager.modelManager.modelId else "Not Initialized",
            color = MaterialTheme.colorScheme.secondary,
            active = isAiReady
        )

        if (!isAiReady) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model Action Required", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The local LLM is not loaded. You need to import a GGUF model to enable AI features.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !operationInProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Model")
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model Management", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !operationInProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replace Model")
                    }
                }
            }
        }

        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Processing Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                MetricRow("Processed", totalProcessed.toString())
                MetricRow("Failed", totalFailed.toString())
                MetricRow("Avg Processing Time", "${"%.2f".format(avgTimeMs / 1000.0)} sec")
            }
        }
    }
}

@Composable
fun StatusCard(title: String, icon: ImageVector, status: String, color: androidx.compose.ui.graphics.Color, active: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (active) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = androidx.compose.ui.graphics.Color(0xFF388E3C))
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
