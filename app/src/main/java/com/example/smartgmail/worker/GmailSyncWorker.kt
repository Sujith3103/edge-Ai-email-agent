package com.example.smartgmail.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.content.edit

class GmailSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        println("========== GMAIL SYNC STARTED ==========")

        try {

            val preferences =
                applicationContext.getSharedPreferences(
                    "gmail_sync",
                    Context.MODE_PRIVATE
                )

            val lastSyncTime =
                preferences.getLong(
                    "last_sync_time",
                    0L
                )

            val currentTime = System.currentTimeMillis()

            println("Last sync = $lastSyncTime")

            println("Current time = $currentTime")

            /*
             * Gmail API call will go here.
             *
             * For now, we're only testing
             * the checkpoint mechanism.
             */

            preferences
                .edit {
                    putLong(
                        "last_sync_time",
                        currentTime
                    )
                }

            println("Checkpoint updated = $currentTime")

            println("========== GMAIL SYNC FINISHED ==========")

            return Result.success()

        } catch (e: Exception) {

            println("GMAIL SYNC FAILED: ${e.message}")

            return Result.retry()
        }
    }
}