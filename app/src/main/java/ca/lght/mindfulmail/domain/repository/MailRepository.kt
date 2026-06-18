package ca.lght.mindfulmail.domain.repository

import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.provider.MailProvider
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.provider.SyncResult
import kotlinx.coroutines.flow.Flow

interface MailRepository {
    val activeAccount: Flow<Account?>

    suspend fun setActiveProvider(provider: MailProvider)
    suspend fun login(credentials: MailCredentials): Result<Account>
    suspend fun logout()

    fun getConversations(labelId: String): Flow<List<Conversation>>
    suspend fun getMessage(id: String): Result<Message>
    suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>>
    suspend fun sendMessage(draft: MessageDraft): Result<Unit>
    suspend fun markAsRead(messageIds: List<String>): Result<Unit>
    suspend fun deleteMessages(messageIds: List<String>): Result<Unit>
    suspend fun sync(): Result<SyncResult>
    fun getLabels(): Flow<List<Label>>
}
