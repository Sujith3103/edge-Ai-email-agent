package com.example.smartgmail.gmail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GmailApi {

    suspend fun listMessages(
        accessToken: String,
        maxResults: Int = 10,
        query: String? = null
    ): List<GmailMessage> = withContext(Dispatchers.IO) {

        val queryParameter =
            if (query != null) {
                "&q=" + URLEncoder.encode(
                    query,
                    "UTF-8"
                )
            } else {
                ""
            }

        val url =
            URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages" +
                        "?maxResults=$maxResults" +
                        queryParameter
            )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                val error =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }

                throw GmailApiException(
                    responseCode,
                    error
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            val json =
                JSONObject(response)

            val messages =
                json.optJSONArray("messages")

            if (messages == null) {
                return@withContext emptyList()
            }

            val result =
                mutableListOf<GmailMessage>()

            for (i in 0 until messages.length()) {

                val message =
                    messages.getJSONObject(i)

                result.add(
                    GmailMessage(
                        id =
                            message.getString("id"),

                        threadId =
                            message.getString("threadId")
                    )
                )
            }

            result

        } finally {

            connection.disconnect()
        }
    }


    suspend fun getMessage(
        accessToken: String,
        messageId: String
    ): String = withContext(Dispatchers.IO) {

        val url =
            URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId" +
                        "?format=full"
            )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                val error =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }

                throw GmailApiException(
                    responseCode,
                    error
                )
            }

            connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        } finally {

            connection.disconnect()
        }
    }
}


data class GmailMessage(
    val id: String,
    val threadId: String
)


class GmailApiException(
    val code: Int,
    message: String?
) : Exception(
    "Gmail API error $code: $message"
)