package com.example.smartgmail.repository

import com.example.smartgmail.database.dao.EmailDao
import com.example.smartgmail.database.entity.EmailEntity
import com.example.smartgmail.database.entity.InboxEmail
import com.example.smartgmail.model.Email
import kotlinx.coroutines.flow.Flow

class EmailRepository(
    private val emailDao: EmailDao
) {

    suspend fun saveIfNew(
        email: Email
    ): Boolean {

        if (emailDao.emailExists(email.id)) {
            return false
        }

        val entity = EmailEntity(
            id = email.id,
            threadId = email.threadId,
            sender = email.sender,
            recipient = email.recipient,
            subject = email.subject,
            date = email.date,
            body = email.body
        )

        emailDao.insertEmail(entity)
        return true
    }

    fun getAllEmails(): Flow<List<EmailEntity>> = emailDao.getAllEmails()

    fun getInboxEmails(): Flow<List<InboxEmail>> = emailDao.getInboxEmails()

    fun getFailedEmails(): Flow<List<InboxEmail>> = emailDao.getFailedEmails()

    fun getDeletedEmails(): Flow<List<InboxEmail>> = emailDao.getDeletedEmails()

    suspend fun getEmail(emailId: String): EmailEntity? = emailDao.getEmail(emailId)

    suspend fun markAsDeleted(emailId: String) = emailDao.markAsDeleted(emailId)

    suspend fun restoreEmail(emailId: String) = emailDao.restoreEmail(emailId)

    suspend fun deletePermanently(emailId: String) = emailDao.deletePermanently(emailId)
}
