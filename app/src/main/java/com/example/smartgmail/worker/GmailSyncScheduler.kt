package com.example.smartgmail.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object GmailSyncScheduler {

    private const val WORK_NAME = "gmail_sync"

    fun schedule(
        context: Context
    ) {

        val request =
            PeriodicWorkRequestBuilder<GmailSyncWorker>(
                1,
                TimeUnit.HOURS
            )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(

                WORK_NAME,

                ExistingPeriodicWorkPolicy.KEEP,

                request
            )
    }

    fun refreshNow(context: Context) {

        val request =
            OneTimeWorkRequestBuilder<GmailSyncWorker>()
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(request)
    }
}