package com.example.smartgmail.ui.navigation

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartgmail.ui.components.GmailMenu
import com.example.smartgmail.worker.GmailSyncScheduler

@Composable
fun SmartGmailHeader() {

    val context = LocalContext.current

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // MENU
        IconButton(
            onClick = {
                menuExpanded = true
            }
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu"
            )
        }

        GmailMenu(
            expanded = menuExpanded,
            onDismiss = {
                menuExpanded = false
            },
            onSyncClick = {
                GmailSyncScheduler.refreshNow(context)
            },
            onSettingsClick = {
                // TODO: navigate to settings
            },
            onAboutClick = {
                // TODO: show about screen/dialog
            }
        )

        // TITLE
        Text(
            text = "Brill Mail",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge
        )

        // REFRESH
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