package com.example.smartgmail.worker

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.ai.EmailAnalyzer
import com.example.smartgmail.gmail.GmailApi
import com.example.smartgmail.gmail.GmailApiException
import com.example.smartgmail.gmail.GmailMessageParser
import com.example.smartgmail.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class GmailSentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val gmailApi = GmailApi()

    override suspend fun doWork(): Result {
        val app = applicationContext as SmartGmailApplication
        return app.syncMutex.withLock {
            syncInternal(app)
        }
    }

    private suspend fun syncInternal(app: SmartGmailApplication): Result {
        println("========== GMAIL SENT SYNC STARTED ==========")

        val gmailManager = app.gmailManager
        val taskRepository = TaskRepository(app.database.taskDao())

        try {
            var accessToken = gmailManager.getAccessToken()

            if (accessToken == null) {
                accessToken = gmailManager.getAuth().authorizeSilently(applicationContext)
                if (accessToken != null) {
                    gmailManager.saveAccessToken(accessToken)
                }
            }

            if (accessToken == null) {
                return Result.failure()
            }

            val syncPreferences = applicationContext.getSharedPreferences("gmail_sent_sync", Context.MODE_PRIVATE)
            val lastSyncTime = syncPreferences.getLong("last_sync_time", 0L)

            val query = if (lastSyncTime == 0L) "in:sent" else "in:sent after:${lastSyncTime / 1000}"

            val messageReferences = gmailApi.listMessages(
                accessToken = accessToken,
                maxResults = 50,
                query = query
            )

            if (messageReferences.isEmpty()) {
                println("No new sent emails found.")
                return Result.success()
            }

            // Get pending tasks to check against
            val pendingTasks = taskRepository.getIncompleteTasks().first()
            if (pendingTasks.isEmpty()) {
                println("No pending tasks to check against.")
                // Still update checkpoint so we don't re-process old sent mails later
                updateCheckpoint(syncPreferences, messageReferences, accessToken)
                return Result.success()
            }

            var newestTime = lastSyncTime

            for (ref in messageReferences) {
                try {
                    val rawMessage = gmailApi.getMessage(accessToken, ref.id)
                    val json = JSONObject(rawMessage)
                    val internalDate = json.optLong("internalDate", 0L)
                    if (internalDate > newestTime) newestTime = internalDate

                    val email = GmailMessageParser.parse(rawMessage)

                    // AI Workflow to check task completion
                    val completedTaskIds = app.aiManager.runAnalysis { llm ->
                        val analyzer = EmailAnalyzer(llm)
                        analyzer.analyzeSentEmail(email, pendingTasks)
                    }

                    if (completedTaskIds.isNotEmpty()) {
                        println("SENT EMAIL ${email.id} COMPLETED TASKS: $completedTaskIds")
                        completedTaskIds.forEach { taskId ->
                            val task = pendingTasks.find { it.id == taskId }
                            if (task != null) {
                                taskRepository.updateTask(task.copy(completed = true))
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("sent_sync", "Failed to process sent message ${ref.id}", e)
                }
            }

            if (newestTime > lastSyncTime) {
                syncPreferences.edit { putLong("last_sync_time", newestTime) }
            }

            println("========== GMAIL SENT SYNC FINISHED ==========")
            return Result.success()

        } catch (e: GmailApiException) {
            if (e.code == 401) {
                gmailManager.clearAccessToken()
                return Result.retry()
            }
            return Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun updateCheckpoint(prefs: android.content.SharedPreferences, refs: List<com.example.smartgmail.gmail.GmailMessage>, token: String) {
        // Just a helper to update newest time if we skipped analysis
        var maxTime = prefs.getLong("last_sync_time", 0L)
        for (ref in refs.take(5)) { // Just check a few to get latest
             try {
                 val raw = gmailApi.getMessage(token, ref.id)
                 val time = JSONObject(raw).optLong("internalDate", 0L)
                 if (time > maxTime) maxTime = time
             } catch (e: Exception) {}
        }
        if (maxTime > prefs.getLong("last_sync_time", 0L)) {
            prefs.edit { putLong("last_sync_time", maxTime) }
        }
    }
}
