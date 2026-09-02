package com.example.smartgmail.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartgmail.worker.GmailSyncScheduler

import com.example.smartgmail.worker.GmailSyncWorker

@Composable
fun SmartGmailHeader(
    onMenuClick: () -> Unit,
//    onRefreshClick: () -> Unit
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onMenuClick
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu"
            )
        }

        Text(
            text = "Gmail Agent",

            modifier = Modifier.weight(1f),

            style = MaterialTheme.typography.titleLarge
        )

        IconButton(
            onClick = {
                GmailSyncScheduler.refreshNow(context)
            }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }
    }
}