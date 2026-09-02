package com.example.smartgmail

import android.app.Application
import com.example.smartgmail.worker.GmailSyncScheduler

class SmartGmailApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        GmailSyncScheduler.schedule(this)

        // App-wide initialization goes here
    }
}