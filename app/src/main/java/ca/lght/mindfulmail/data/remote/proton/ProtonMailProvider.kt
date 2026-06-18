package ca.lght.mindfulmail.data.remote.proton

import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.provider.MailProvider
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.provider.SyncResult
import io.ktor.client.HttpClient
import javax.inject.Inject

/**
 * ProtonMail provider for MindfulMail.
 *
 * Full implementation is planned for Phase 2. This provider will use:
 * - SRP (Secure Remote Password) authentication via the Proton REST API
 *   at https://mail.proton.me/api
 * - PGPainless for OpenPGP end-to-end encryption / decryption of message bodies
 * - Ktor HttpClient for all HTTP communication
 *
 * References:
 * - Proton API: https://protonmail.com/blog/security-updates-2018/
 * - PGPainless: https://pgpainless.org
 */
class ProtonMailProvider @Inject constructor(
    private val httpClient: HttpClient,
) : MailProvider {

    override suspend fun login(credentials: MailCredentials): Result<Account> =
        throw NotImplementedError("ProtonMailProvider: login not yet implemented — see Phase 2")

    override suspend fun logout() =
        throw NotImplementedError("ProtonMailProvider: logout not yet implemented — see Phase 2")

    override suspend fun isAuthenticated(): Boolean =
        throw NotImplementedError("ProtonMailProvider: isAuthenticated not yet implemented — see Phase 2")

    override suspend fun getLabels(): Result<List<Label>> =
        throw NotImplementedError("ProtonMailProvider: getLabels not yet implemented — see Phase 2")

    override suspend fun getConversations(labelId: String, page: Int): Result<List<Conversation>> =
        throw NotImplementedError("ProtonMailProvider: getConversations not yet implemented — see Phase 2")

    override suspend fun getMessage(id: String): Result<Message> =
        throw NotImplementedError("ProtonMailProvider: getMessage not yet implemented — see Phase 2")

    override suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>> =
        throw NotImplementedError("ProtonMailProvider: getMessagesInConversation not yet implemented — see Phase 2")

    override suspend fun markAsRead(messageIds: List<String>): Result<Unit> =
        throw NotImplementedError("ProtonMailProvider: markAsRead not yet implemented — see Phase 2")

    override suspend fun markAsUnread(messageIds: List<String>): Result<Unit> =
        throw NotImplementedError("ProtonMailProvider: markAsUnread not yet implemented — see Phase 2")

    override suspend fun moveToLabel(messageIds: List<String>, labelId: String): Result<Unit> =
        throw NotImplementedError("ProtonMailProvider: moveToLabel not yet implemented — see Phase 2")

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> =
        throw NotImplementedError("ProtonMailProvider: deleteMessages not yet implemented — see Phase 2")

    override suspend fun sendMessage(draft: MessageDraft): Result<Unit> =
        throw NotImplementedError("ProtonMailProvider: sendMessage not yet implemented — see Phase 2")

    override suspend fun sync(): Result<SyncResult> =
        throw NotImplementedError("ProtonMailProvider: sync not yet implemented — see Phase 2")
}
