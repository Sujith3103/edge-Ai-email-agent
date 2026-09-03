package com.example.smartgmail

import android.app.Application
import com.example.smartgmail.worker.GmailSyncScheduler
import kotlin.getValue

import com.example.smartgmail.ai.AIManager
import com.example.smartgmail.database.DatabaseProvider
import com.example.smartgmail.gmail.GmailManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmartGmailApplication : Application() {

    val aiManager by lazy {
        AIManager(this)
    }

    val gmailManager by lazy {
        GmailManager(this)
    }

    val database by lazy {
        DatabaseProvider.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        GmailSyncScheduler.schedule(this)

        CoroutineScope(Dispatchers.IO).launch {
            aiManager.initialize()
        }
    }
}