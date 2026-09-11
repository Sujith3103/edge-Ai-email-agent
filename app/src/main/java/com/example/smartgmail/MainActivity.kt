package com.example.smartgmail

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Scaffold

import androidx.compose.ui.Modifier

import com.example.smartgmail.ui.SmartGmailScreen
import com.example.smartgmail.ui.theme.SmartGmailTheme


class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {

            SmartGmailTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    SmartGmailScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}