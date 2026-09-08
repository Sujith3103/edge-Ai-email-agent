package com.example.smartgmail.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.ai.runLLMBenchmark
import com.example.smartgmail.database.entity.InboxEmail

import com.example.smartgmail.ui.gmail.GmailScreen
import com.example.smartgmail.ui.gmail.EmailDetailScreen
import com.example.smartgmail.ui.llm.LLMScreen
import com.example.smartgmail.ui.navigation.SmartGmailHeader
import com.example.smartgmail.ui.navigation.SystemStatusScreen
import com.example.smartgmail.ui.tasks.TasksScreen
import com.example.smartgmail.ui.tasks.TimelineScreen
import com.example.smartgmail.ui.home.DailyBriefScreen
import com.example.smartgmail.worker.GmailSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SmartGmailPage {
    DAILY_BRIEF,
    INBOX,
    FAILED,
    ASSISTANT,
    TASKS,
    COMPLETED_TASKS,
    TIMELINE,
    DELETED,
    SYSTEM_STATUS
}


@Composable
fun SmartGmailScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmartGmailApplication
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentPage by remember {
        mutableStateOf(SmartGmailPage.DAILY_BRIEF)
    }

    var selectedEmail by remember { mutableStateOf<InboxEmail?>(null) }
    var benchmarkRunning by remember { mutableStateOf(false) }

    if (selectedEmail != null) {
        EmailDetailScreen(
            email = selectedEmail!!,
            onBack = { selectedEmail = null }
        )
        BackHandler {
            selectedEmail = null
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Brill Mail",
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Dashboard, null) },
                        label = { Text("Daily Brief") },
                        selected = currentPage == SmartGmailPage.DAILY_BRIEF,
                        onClick = {
                            currentPage = SmartGmailPage.DAILY_BRIEF
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Email, null) },
                        label = { Text("Inbox") },
                        selected = currentPage == SmartGmailPage.INBOX,
                        onClick = {
                            currentPage = SmartGmailPage.INBOX
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Error, null) },
                        label = { Text("Failed") },
                        selected = currentPage == SmartGmailPage.FAILED,
                        onClick = {
                            currentPage = SmartGmailPage.FAILED
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, null) },
                        label = { Text("Trash") },
                        selected = currentPage == SmartGmailPage.DELETED,
                        onClick = {
                            currentPage = SmartGmailPage.DELETED
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 28.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Schedule, null) },
                        label = { Text("Timeline") },
                        selected = currentPage == SmartGmailPage.TIMELINE,
                        onClick = {
                            currentPage = SmartGmailPage.TIMELINE
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Check, null) },
                        label = { Text("Tasks") },
                        selected = currentPage == SmartGmailPage.TASKS,
                        onClick = {
                            currentPage = SmartGmailPage.TASKS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.TaskAlt, null) },
                        label = { Text("Completed Tasks") },
                        selected = currentPage == SmartGmailPage.COMPLETED_TASKS,
                        onClick = {
                            currentPage = SmartGmailPage.COMPLETED_TASKS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 28.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoAwesome, null) },
                        label = { Text("Assistant") },
                        selected = currentPage == SmartGmailPage.ASSISTANT,
                        onClick = {
                            currentPage = SmartGmailPage.ASSISTANT
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.MonitorHeart, null) },
                        label = { Text("System Status") },
                        selected = currentPage == SmartGmailPage.SYSTEM_STATUS,
                        onClick = {
                            currentPage = SmartGmailPage.SYSTEM_STATUS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { 
                            if (benchmarkRunning) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Speed, null) 
                        },
                        label = { Text("Run LLM Benchmark") },
                        selected = false,
                        onClick = {
                            if (!benchmarkRunning) {
                                scope.launch {
                                    drawerState.close()
                                    benchmarkRunning = true
                                    try {
                                        val localLLM = app.aiManager.getLLM()
                                        withContext(Dispatchers.Default) {
                                            runLLMBenchmark(localLLM)
                                        }
                                        Toast.makeText(context, "Benchmark complete! Check Logcat.", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Benchmark failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        benchmarkRunning = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        ) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    SmartGmailHeader(
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentPage == SmartGmailPage.DAILY_BRIEF,
                            onClick = { currentPage = SmartGmailPage.DAILY_BRIEF },
                            icon = { Icon(Icons.Default.Dashboard, "Brief") },
                            label = { Text("Brief") }
                        )
                        NavigationBarItem(
                            selected = currentPage == SmartGmailPage.INBOX,
                            onClick = { currentPage = SmartGmailPage.INBOX },
                            icon = { Icon(Icons.Default.Email, "Inbox") },
                            label = { Text("Inbox") }
                        )
                        NavigationBarItem(
                            selected = currentPage == SmartGmailPage.TASKS,
                            onClick = { currentPage = SmartGmailPage.TASKS },
                            icon = { Icon(Icons.Default.Check, "Tasks") },
                            label = { Text("Tasks") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentPage) {
                        SmartGmailPage.DAILY_BRIEF -> {
                            DailyBriefScreen(
                                modifier = Modifier.fillMaxSize(),
                                onViewInbox = { currentPage = SmartGmailPage.INBOX }
                            )
                        }
                        SmartGmailPage.INBOX -> {
                            GmailScreen(
                                modifier = Modifier.fillMaxSize(),
                                mode = GmailScreenMode.INBOX,
                                onEmailClick = { selectedEmail = it }
                            )
                        }
                        SmartGmailPage.FAILED -> {
                            GmailScreen(
                                modifier = Modifier.fillMaxSize(),
                                mode = GmailScreenMode.FAILED,
                                onEmailClick = { selectedEmail = it }
                            )
                        }
                        SmartGmailPage.DELETED -> {
                            GmailScreen(
                                modifier = Modifier.fillMaxSize(),
                                mode = GmailScreenMode.DELETED,
                                onEmailClick = { selectedEmail = it }
                            )
                        }
                        SmartGmailPage.ASSISTANT -> {
                            LLMScreen(modifier = Modifier.fillMaxSize())
                        }
                        SmartGmailPage.TASKS -> {
                            TasksScreen(
                                modifier = Modifier.fillMaxSize(),
                                showCompletedOnly = false
                            )
                        }
                        SmartGmailPage.COMPLETED_TASKS -> {
                            TasksScreen(
                                modifier = Modifier.fillMaxSize(),
                                showCompletedOnly = true,
                                onRefreshSent = { GmailSyncScheduler.refreshSentNow(context) }
                            )
                        }
                        SmartGmailPage.TIMELINE -> {
                            TimelineScreen(modifier = Modifier.fillMaxSize())
                        }
                        SmartGmailPage.SYSTEM_STATUS -> {
                            SystemStatusScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

enum class GmailScreenMode {
    INBOX,
    FAILED,
    DELETED
}
