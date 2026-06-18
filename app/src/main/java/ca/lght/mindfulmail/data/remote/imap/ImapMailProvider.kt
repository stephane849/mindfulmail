package ca.lght.mindfulmail.data.remote.imap

import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.provider.MailProvider
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.provider.SyncResult
import javax.inject.Inject

/**
 * IMAP/SMTP provider for MindfulMail.
 *
 * Full implementation is planned for Phase 2. This provider will use:
 * - Jakarta Mail (android-mail) for IMAP and SMTP connectivity
 * - Standard IMAP IDLE for push-like notification support
 * - SSL/TLS for both incoming (IMAP) and outgoing (SMTP) connections
 */
class ImapMailProvider @Inject constructor() : MailProvider {

    override suspend fun login(credentials: MailCredentials): Result<Account> =
        throw NotImplementedError("ImapMailProvider: login not yet implemented — see Phase 2")

    override suspend fun logout() =
        throw NotImplementedError("ImapMailProvider: logout not yet implemented — see Phase 2")

    override suspend fun isAuthenticated(): Boolean =
        throw NotImplementedError("ImapMailProvider: isAuthenticated not yet implemented — see Phase 2")

    override suspend fun getLabels(): Result<List<Label>> =
        throw NotImplementedError("ImapMailProvider: getLabels not yet implemented — see Phase 2")

    override suspend fun getConversations(labelId: String, page: Int): Result<List<Conversation>> =
        throw NotImplementedError("ImapMailProvider: getConversations not yet implemented — see Phase 2")

    override suspend fun getMessage(id: String): Result<Message> =
        throw NotImplementedError("ImapMailProvider: getMessage not yet implemented — see Phase 2")

    override suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>> =
        throw NotImplementedError("ImapMailProvider: getMessagesInConversation not yet implemented — see Phase 2")

    override suspend fun markAsRead(messageIds: List<String>): Result<Unit> =
        throw NotImplementedError("ImapMailProvider: markAsRead not yet implemented — see Phase 2")

    override suspend fun markAsUnread(messageIds: List<String>): Result<Unit> =
        throw NotImplementedError("ImapMailProvider: markAsUnread not yet implemented — see Phase 2")

    override suspend fun moveToLabel(messageIds: List<String>, labelId: String): Result<Unit> =
        throw NotImplementedError("ImapMailProvider: moveToLabel not yet implemented — see Phase 2")

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> =
        throw NotImplementedError("ImapMailProvider: deleteMessages not yet implemented — see Phase 2")

    override suspend fun sendMessage(draft: MessageDraft): Result<Unit> =
        throw NotImplementedError("ImapMailProvider: sendMessage not yet implemented — see Phase 2")

    override suspend fun sync(): Result<SyncResult> =
        throw NotImplementedError("ImapMailProvider: sync not yet implemented — see Phase 2")
}
