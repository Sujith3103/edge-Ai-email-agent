package com.example.smartgmail.gmail

import android.text.Html
import android.util.Base64
import com.example.smartgmail.model.Email
//import android.provider.ContactsContract.CommonDataKinds.Email
import org.json.JSONArray
import org.json.JSONObject

object GmailMessageParser {

    fun parse(json: String): Email {

        val root = JSONObject(json)

        val id = root.optString("id")
        val threadId = root.optString("threadId")

        val payload = root.optJSONObject("payload")
            ?: throw IllegalStateException("Email payload is missing")

        val headers = payload.optJSONArray("headers")

        val sender = getHeader(headers, "From")
        val recipient = getHeader(headers, "To")
        val subject = getHeader(headers, "Subject")
        val date = getHeader(headers, "Date")

        val body = extractBody(payload)

        return Email(
            id = id,
            threadId = threadId,
            sender = sender,
            recipient = recipient,
            subject = subject,
            date = date,
            body = body
        )
    }

    private fun getHeader(
        headers: JSONArray?,
        name: String
    ): String {

        if (headers == null) {
            return ""
        }

        for (i in 0 until headers.length()) {

            val header = headers.optJSONObject(i)
                ?: continue

            if (
                header.optString("name")
                    .equals(name, ignoreCase = true)
            ) {
                return header.optString("value")
            }
        }

        return ""
    }

    private fun extractBody(
        part: JSONObject
    ): String {

        val mimeType = part.optString("mimeType")

        /*
         * First check whether this part directly contains
         * the email body.
         */
        val bodyObject =
            part.optJSONObject("body")

        if (bodyObject != null) {

            val encodedData =
                bodyObject.optString("data")

            if (encodedData.isNotEmpty()) {

                val decoded =
                    decodeBase64Url(encodedData)

                if (mimeType == "text/html") {

                    return Html
                        .fromHtml(
                            decoded,
                            Html.FROM_HTML_MODE_LEGACY
                        )
                        .toString()
                        .trim()
                }

                return decoded.trim()
            }
        }

        /*
         * If this is multipart email, the actual body
         * will be inside one of its parts.
         */
        val parts =
            part.optJSONArray("parts")

        if (parts != null) {

            /*
             * Prefer text/plain.
             */
            for (i in 0 until parts.length()) {

                val child =
                    parts.optJSONObject(i)
                        ?: continue

                if (
                    child.optString("mimeType")
                        .equals("text/plain", ignoreCase = true)
                ) {

                    val result =
                        extractBody(child)

                    if (result.isNotEmpty()) {
                        return result
                    }
                }
            }

            /*
             * If there is no text/plain, try HTML.
             */
            for (i in 0 until parts.length()) {

                val child =
                    parts.optJSONObject(i)
                        ?: continue

                if (
                    child.optString("mimeType")
                        .equals("text/html", ignoreCase = true)
                ) {

                    val result =
                        extractBody(child)

                    if (result.isNotEmpty()) {
                        return result
                    }
                }
            }

            /*
             * Some emails have nested multipart
             * structures, so recursively search them.
             */
            for (i in 0 until parts.length()) {

                val child =
                    parts.optJSONObject(i)
                        ?: continue

                val result =
                    extractBody(child)

                if (result.isNotEmpty()) {
                    return result
                }
            }
        }

        return ""
    }

    private fun decodeBase64Url(
        encoded: String
    ): String {

        val bytes =
            Base64.decode(
                encoded,
                Base64.URL_SAFE or Base64.NO_WRAP
            )

        return String(
            bytes,
            Charsets.UTF_8
        )
    }
}