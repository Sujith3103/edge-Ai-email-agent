package com.example.smartgmail.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object GmailSyncScheduler {

    private const val WORK_NAME = "gmail_sync"
    private const val SENT_WORK_NAME = "gmail_sent_sync"

    fun schedule(
        context: Context
    ) {

        val inboxRequest =
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
                inboxRequest
            )

        val sentRequest =
            PeriodicWorkRequestBuilder<GmailSentSyncWorker>(
                2,
                TimeUnit.HOURS
            )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                SENT_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                sentRequest
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

    fun refreshSentNow(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<GmailSentSyncWorker>()
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(request)
    }
}
