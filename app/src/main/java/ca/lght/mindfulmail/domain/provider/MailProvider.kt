package ca.lght.mindfulmail.domain.provider

import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.Attachment
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.EmailAddress
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.Message

interface MailProvider {
    // Auth
    suspend fun login(credentials: MailCredentials): Result<Account>
    suspend fun logout()
    suspend fun isAuthenticated(): Boolean

    // Labels / Folders
    suspend fun getLabels(): Result<List<Label>>

    // Conversations
    suspend fun getConversations(labelId: String, page: Int = 0): Result<List<Conversation>>

    // Messages
    suspend fun getMessage(id: String): Result<Message>
    suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>>
    suspend fun markAsRead(messageIds: List<String>): Result<Unit>
    suspend fun markAsUnread(messageIds: List<String>): Result<Unit>
    suspend fun moveToLabel(messageIds: List<String>, labelId: String): Result<Unit>
    suspend fun deleteMessages(messageIds: List<String>): Result<Unit>
    suspend fun sendMessage(draft: MessageDraft): Result<Unit>

    // Sync
    suspend fun sync(): Result<SyncResult>
}

sealed class MailCredentials {
    data class ProtonCredentials(
        val username: String,
        val password: String,
    ) : MailCredentials()

    data class ImapCredentials(
        val email: String,
        val password: String,
        val imapHost: String,
        val imapPort: Int = 993,
        val smtpHost: String,
        val smtpPort: Int = 587,
        val useSSL: Boolean = true,
    ) : MailCredentials()
}

data class MessageDraft(
    val to: List<EmailAddress>,
    val cc: List<EmailAddress> = emptyList(),
    val bcc: List<EmailAddress> = emptyList(),
    val subject: String,
    val body: String,
    val replyToMessageId: String? = null,
    val attachments: List<Attachment> = emptyList(),
)

data class SyncResult(val newMessages: Int, val updatedMessages: Int)
