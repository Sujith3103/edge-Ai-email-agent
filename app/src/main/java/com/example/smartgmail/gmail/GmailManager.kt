package com.example.smartgmail.gmail

import android.content.Context
import androidx.core.content.edit

class GmailManager(
    private val context: Context
) {

    private val gmailAuth = GmailAuth()

    companion object {
        private const val PREFS_NAME = "gmail_auth"
        private const val ACCESS_TOKEN = "access_token"
    }

    fun getAccessToken(): String? {
        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(ACCESS_TOKEN, null)
    }

    fun saveAccessToken(token: String) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit {
                putString(ACCESS_TOKEN, token)
            }
    }

    fun getAuth(): GmailAuth {
        return gmailAuth
    }
}
