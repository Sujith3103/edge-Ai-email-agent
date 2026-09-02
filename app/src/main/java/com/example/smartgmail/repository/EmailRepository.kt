package com.example.smartgmail.repository

import com.example.smartgmail.database.dao.EmailDao
import com.example.smartgmail.database.entity.EmailEntity
import com.example.smartgmail.model.Email
import kotlinx.coroutines.flow.Flow

class EmailRepository(
    private val emailDao: EmailDao
) {

    suspend fun saveIfNew(
        email: Email
    ): Boolean {

        // Check whether Gmail message ID
        // already exists in our database.
        if (emailDao.emailExists(email.id)) {

            println(
                "EMAIL ALREADY EXISTS: ${email.id}"
            )

            return false
        }

        // Convert application model
        // into database entity.
        val entity = EmailEntity(
            id = email.id,
            threadId = email.threadId,
            sender = email.sender,
            recipient = email.recipient,
            subject = email.subject,
            date = email.date,
            body = email.body
        )

        // Store the new email.
        emailDao.insertEmail(entity)

        println(
            "NEW EMAIL SAVED: ${email.id}"
        )

        return true
    }


    fun getAllEmails():
            Flow<List<EmailEntity>> {

        return emailDao.getAllEmails()
    }


    suspend fun getEmail(
        emailId: String
    ): EmailEntity? {

        return emailDao.getEmail(emailId)
    }
}