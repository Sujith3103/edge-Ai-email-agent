package com.example.smartgmail.worker

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartgmail.SmartGmailApplication
import com.example.smartgmail.ai.EmailAnalyzer
import com.example.smartgmail.database.DatabaseProvider
import com.example.smartgmail.database.entity.EmailEntity
import com.example.smartgmail.gmail.GmailApi
import com.example.smartgmail.gmail.GmailApiException
import com.example.smartgmail.context.EmailContextBuilder
import com.example.smartgmail.gmail.GmailMessageParser
import com.example.smartgmail.model.Email
import com.example.smartgmail.repository.EmailRepository
import com.example.smartgmail.repository.EmailAnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
class GmailSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    private val gmailApi = GmailApi()

    override suspend fun doWork(): Result {

        println("========== GMAIL SYNC STARTED ==========")

        try {

            // =================================================
            // APPLICATION
            // =================================================



            val app =
                applicationContext as SmartGmailApplication

            val gmailManager =
                app.gmailManager

            val aiManager = app.aiManager

            val localLLM = aiManager.getLLM()

            val database =
                app.database

            val emailRepository =
                EmailRepository(
                    database.emailDao()
                )

            val emailAnalysisRepository =
                EmailAnalysisRepository(
                    database
                )

            val emailAnalyzer = EmailAnalyzer(localLLM)

            // =================================================
            // AUTHENTICATION
            // =================================================

            val accessToken =
                gmailManager.getAccessToken()

            if (accessToken == null) {

                println(
                    "No Gmail access token available"
                )

                return Result.failure()
            }


            // =================================================
            // LAST SYNC
            // =================================================

            val syncPreferences =
                applicationContext.getSharedPreferences(
                    "gmail_sync",
                    Context.MODE_PRIVATE
                )

            val lastSyncTime =
                syncPreferences.getLong(
                    "last_sync_time",
                    0L
                )

            println(
                "Last sync = $lastSyncTime"
            )


            // =================================================
            // GMAIL QUERY
            // =================================================

            val query =
                if (lastSyncTime == 0L) {
                    "in:anywhere"
                } else {
                    "after:${lastSyncTime / 1000}"
                }

            println(
                "Gmail query = $query"
            )


            // =================================================
            // MESSAGE REFERENCES
            // =================================================

            val messageReferences =
                gmailApi.listMessages(
                    accessToken = accessToken,
                    maxResults = 100,
                    query = query
                )

            println(
                "Messages found = " +
                        messageReferences.size
            )


            if (messageReferences.isEmpty()) {

                println(
                    "No new emails found."
                )

                return Result.success()
            }


            // =================================================
            // PROCESS EMAILS
            // =================================================

            var newestEmailTime =
                lastSyncTime

            for (messageReference in messageReferences) {

                try {

                    // -----------------------------------------
                    // GET FULL MESSAGE
                    // -----------------------------------------

                    val rawMessage =
                        gmailApi.getMessage(
                            accessToken = accessToken,
                            messageId = messageReference.id
                        )


                    // -----------------------------------------
                    // CHECK INTERNAL DATE
                    // -----------------------------------------

                    val json =
                        JSONObject(rawMessage)

                    val internalDate =
                        json.optLong(
                            "internalDate",
                            0L
                        )

                    if (internalDate > newestEmailTime) {

                        newestEmailTime =
                            internalDate
                    }


                    // -----------------------------------------
                    // PARSE GMAIL MESSAGE
                    // -----------------------------------------

                    val email =
                        GmailMessageParser.parse(
                            rawMessage
                        )


                    // -----------------------------------------
                    // SAVE EMAIL
                    // -----------------------------------------

                    val saved =
                        emailRepository.saveIfNew(
                            email
                        )


                    // -----------------------------------------
                    // SKIP EXISTING EMAILS
                    // -----------------------------------------

                    if (!saved) {

                        println(
                            "EMAIL ALREADY EXISTS: ${email.id}"
                        )

                        continue
                    }


                    println(
                        "NEW EMAIL SAVED: ${email.id}"
                    )


                    // -----------------------------------------
                    // AI WORKFLOW
                    // -----------------------------------------

                    try {
                        val analyzedEmail =
                            emailAnalyzer.analyze(email)

                        emailAnalysisRepository.saveAnalysis(
                            analyzedEmail
                        )

                        Log.d(
                            "workflow",
                            "ANALYSIS SAVED: ${email.id}"
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "workflow",
                            "ANALYSIS FAILED: ${email.id}",
                            e
                        )

                        try {
                            emailAnalysisRepository.saveFailedAnalysis(email.id)
                        } catch (inner: Exception) {
                            Log.e("workflow", "FAILED to save failure status", inner)
                        }
                    }

                } catch (e: Exception) {

                    println(
                        "Failed to process message " +
                                "${messageReference.id}: " +
                                e.message
                    )
                }
            }

            // =================================================
            // UPDATE CHECKPOINT
            // =================================================

            if (
                newestEmailTime >
                lastSyncTime
            ) {

                syncPreferences.edit {

                    putLong(
                        "last_sync_time",
                        newestEmailTime
                    )
                }

                println(
                    "Checkpoint updated = " +
                            newestEmailTime
                )
            }


            println(
                "========== GMAIL SYNC FINISHED =========="
            )

            return Result.success()

        } catch (e: GmailApiException) {

            println(
                "GMAIL API FAILED: ${e.message}"
            )

            if (e.code == 401) {

                println(
                    "Gmail access token is invalid or expired."
                )

                return Result.failure()
            }

            return Result.retry()

        } catch (e: Exception) {

            println(
                "GMAIL SYNC FAILED: ${e.message}"
            )

            e.printStackTrace()

            return Result.retry()
        }
    }
}