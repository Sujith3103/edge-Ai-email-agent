package com.example.smartgmail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import com.example.smartgmail.ui.gmail.GmailScreen
import com.example.smartgmail.ui.llm.LLMScreen
import com.example.smartgmail.ui.navigation.SmartGmailHeader
import com.example.smartgmail.ui.tasks.TasksScreen
import com.example.smartgmail.worker.GmailSyncWorker

enum class SmartGmailPage {
    INBOX,
    ASSISTANT,
    TASKS
}


@Composable
fun SmartGmailScreen(
    modifier: Modifier = Modifier
) {

    var currentPage by remember {
        mutableStateOf(
            SmartGmailPage.INBOX
        )
    }

//    SmartGmailHeader(
//
//        onMenuClick = {
//            // menu logic
//        },
//
//        onRefreshClick = {
//            // refresh logic
//        }
//    )
    Scaffold(

        modifier = modifier.fillMaxSize(),
        topBar = {
            SmartGmailHeader()
        },
        // =====================================================
        // CUSTOM HEADER
        // ====================================================


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomBar = {

            NavigationBar {

                // =================================================
                // INBOX
                // =================================================

                NavigationBarItem(

                    selected =
                        currentPage ==
                                SmartGmailPage.INBOX,

                    onClick = {

                        currentPage =
                            SmartGmailPage.INBOX
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.Email,

                            contentDescription =
                                "Inbox"
                        )
                    },

                    label = {

                        Text("Inbox")
                    }
                )


                // =================================================
                // ASSISTANT
                // =================================================

                NavigationBarItem(

                    selected =
                        currentPage ==
                                SmartGmailPage.ASSISTANT,

                    onClick = {

                        currentPage =
                            SmartGmailPage.ASSISTANT
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.AutoAwesome,

                            contentDescription =
                                "Assistant"
                        )
                    },

                    label = {

                        Text("Assistant")
                    }
                )


                // =================================================
                // TASKS
                // =================================================

                NavigationBarItem(

                    selected =
                        currentPage ==
                                SmartGmailPage.TASKS,

                    onClick = {

                        currentPage =
                            SmartGmailPage.TASKS
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.Check,

                            contentDescription =
                                "Tasks"
                        )
                    },

                    label = {

                        Text("Tasks")
                    }
                )
            }
        }

    ) { innerPadding ->


        // =====================================================
        // SCREEN CONTENT
        // =====================================================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when (currentPage) {

                // =================================================
                // INBOX
                // =================================================

                SmartGmailPage.INBOX -> {

                    GmailScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }


                // =================================================
                // ASSISTANT
                // =================================================

                SmartGmailPage.ASSISTANT -> {

                    LLMScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }


                // =================================================
                // TASKS
                // ================================================

                SmartGmailPage.TASKS -> {

                    TasksScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}