package com.example.smartgmail.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GmailMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {

        DropdownMenuItem(
            text = {
                Text("Sync Gmail")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onSyncClick()
            }
        )

        DropdownMenuItem(
            text = {
                Text("Settings")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onSettingsClick()
            }
        )

        DropdownMenuItem(
            text = {
                Text("About")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onAboutClick()
            }
        )
    }
}