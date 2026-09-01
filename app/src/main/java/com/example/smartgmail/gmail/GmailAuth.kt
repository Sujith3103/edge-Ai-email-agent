package com.example.smartgmail.gmail

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

//This is responsible for getting permission from Google to access Gmail.

class GmailAuth {

    companion object {
        private const val GMAIL_READ_ONLY =
            "https://www.googleapis.com/auth/gmail.readonly"
    }

    fun createAuthorizationRequest(): AuthorizationRequest {
        val gmailScope = Scope(GMAIL_READ_ONLY)

        return AuthorizationRequest.builder()
            .setRequestedScopes(listOf(gmailScope))
            .build()
    }


    fun authorize(
        activity: Activity,
        authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onAuthorized: (AuthorizationResult) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val client = Identity.getAuthorizationClient(activity)

        client
            .authorize(createAuthorizationRequest())
            .addOnSuccessListener { result ->

                if (result.hasResolution()) {

                    val pendingIntent = result.pendingIntent

                    if (pendingIntent != null) {

                        val request =
                            IntentSenderRequest.Builder(
                                pendingIntent.intentSender
                            ).build()

                        authorizationLauncher.launch(request)

                    } else {
                        onError(
                            IllegalStateException(
                                "Authorization requires a resolution but no PendingIntent was provided"
                            )
                        )
                    }

                } else {

                    onAuthorized(result)
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun getAuthorizationResult(
        activity: Activity,
        data: Intent
    ): AuthorizationResult {

        return Identity
            .getAuthorizationClient(activity)
            .getAuthorizationResultFromIntent(data)
    }

    fun getAccessToken(
        result: AuthorizationResult
    ): String? {
        return result.accessToken
    }
}