package ca.lght.mindfulmail.data.repository

import ca.lght.mindfulmail.data.local.db.ConversationDao
import ca.lght.mindfulmail.data.local.db.LabelDao
import ca.lght.mindfulmail.data.local.db.MessageDao
import ca.lght.mindfulmail.data.local.entity.toConversation
import ca.lght.mindfulmail.data.local.entity.toEntity
import ca.lght.mindfulmail.data.local.entity.toLabel
import ca.lght.mindfulmail.data.local.entity.toMessage
import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.provider.MailProvider
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.provider.SyncResult
import ca.lght.mindfulmail.domain.repository.MailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val labelDao: LabelDao,
) : MailRepository {

    private val _activeAccount = MutableStateFlow<Account?>(null)
    override val activeAccount: Flow<Account?> = _activeAccount

    private var provider: MailProvider? = null

    override suspend fun setActiveProvider(provider: MailProvider) {
        this.provider = provider
    }

    override suspend fun login(credentials: MailCredentials): Result<Account> =
        withContext(Dispatchers.IO) {
            val p = provider ?: return@withContext Result.failure(IllegalStateException("No provider set"))
            p.login(credentials).onSuccess { account ->
                _activeAccount.value = account
            }
        }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        provider?.logout()
        _activeAccount.value = null
    }

    override fun getConversations(labelId: String): Flow<List<Conversation>> =
        conversationDao.getByLabel(labelId).map { entities ->
            entities.map { it.toConversation() }
        }

    override suspend fun getMessage(id: String): Result<Message> =
        withContext(Dispatchers.IO) {
            val cached = messageDao.getById(id)
            if (cached != null) return@withContext Result.success(cached.toMessage())
            val p = provider ?: return@withContext Result.failure(IllegalStateException("No provider set"))
            p.getMessage(id).onSuccess { message ->
                messageDao.upsert(listOf(message.toEntity()))
            }
        }

    override suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>> =
        withContext(Dispatchers.IO) {
            val p = provider ?: return@withContext Result.failure(IllegalStateException("No provider set"))
            p.getMessagesInConversation(conversationId).onSuccess { messages ->
                messageDao.upsert(messages.map { it.toEntity() })
            }
        }

    override suspend fun sendMessage(draft: MessageDraft): Result<Unit> =
        withContext(Dispatchers.IO) {
            val p = provider ?: return@withContext Result.failure(IllegalStateException("No provider set"))
            p.sendMessage(draft)
        }

    override suspend fun markAsRead(messageIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            messageDao.markAsRead(messageIds)
            provider?.markAsRead(messageIds) ?: Result.success(Unit)
        }

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            val p = provider ?: return@withContext Result.failure(IllegalStateException("No provider set"))
            p.deleteMessages(messageIds).onSuccess {
                messageDao.deleteByIds(messageIds)
            }
        }

    override suspend fun sync(): Result<SyncResult> =
        withContext(Dispatchers.IO) {
            val p = provider ?: return@withContext Result.failure(IllegalStateException("No provider set"))
            val labelsResult = p.getLabels()
            labelsResult.onSuccess { labels ->
                labelDao.upsert(labels.map { it.toEntity() })
            }
            p.sync()
        }

    override fun getLabels(): Flow<List<Label>> =
        labelDao.getAll().map { entities -> entities.map { it.toLabel() } }
}
