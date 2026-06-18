package ca.lght.mindfulmail.data.remote.proton

import ca.lght.mindfulmail.data.remote.proton.api.ProtonApiClient
import ca.lght.mindfulmail.data.remote.proton.api.ProtonAuthException
import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSessionStore
import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSrp
import ca.lght.mindfulmail.data.remote.proton.crypto.ProtonCryptoHelper
import ca.lght.mindfulmail.data.remote.proton.model.AuthRequest
import ca.lght.mindfulmail.data.remote.proton.model.ProtonAddressPackage
import ca.lght.mindfulmail.data.remote.proton.model.ProtonConversation
import ca.lght.mindfulmail.data.remote.proton.model.ProtonDraftBody
import ca.lght.mindfulmail.data.remote.proton.model.ProtonDraftRequest
import ca.lght.mindfulmail.data.remote.proton.model.ProtonEmailAddress
import ca.lght.mindfulmail.data.remote.proton.model.ProtonMessageDetail
import ca.lght.mindfulmail.data.remote.proton.model.ProtonMessageSummary
import ca.lght.mindfulmail.data.remote.proton.model.ProtonSendPackage
import ca.lght.mindfulmail.data.remote.proton.model.ProtonSendRequest
import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.AccountType
import ca.lght.mindfulmail.domain.model.Attachment
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.EmailAddress
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.LabelType
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.provider.MailProvider
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.provider.SyncResult
import javax.inject.Inject

/**
 * ProtonMail provider for MindfulMail.
 *
 * Phase 2 implements:
 * - SRP-based login (two-step: auth/info → auth) — note the BCrypt password hash step
 *   requires Phase 2 crypto integration and will return a descriptive error until then.
 * - Session persistence via [ProtonSessionStore].
 * - Labels/folders fetch so the inbox screen can populate its folder list.
 *
 * Phase 3 implements:
 * - Conversation fetch via event-loop-based sync.
 * - Full message fetch with PGP decryption via [ProtonCryptoHelper].
 * - Mark as read/unread, move to label, delete, send (simplified cleartext for Phase 3).
 *
 * References:
 * - Proton API: https://protonmail.com/blog/security-updates-2018/
 * - PGPainless: https://pgpainless.org
 */
class ProtonMailProvider @Inject constructor(
    private val apiClient: ProtonApiClient,
    private val sessionStore: ProtonSessionStore,
    private val cryptoHelper: ProtonCryptoHelper,
) : MailProvider {

    // ── Auth ─────────────────────────────────────────────────────────────────

    /**
     * Log in with Proton SRP authentication.
     *
     * Flow:
     * 1. POST /auth/info — retrieve server ephemeral and SRP session.
     * 2. Compute client ephemeral and proof via [ProtonSrp.generateProofs].
     *    (Will return [Result.failure] with a descriptive message until BCrypt integration
     *    is complete — see [ProtonSrp.hashPassword].)
     * 3. POST /auth — exchange proofs for access/refresh tokens.
     * 4. Verify the server proof matches our expected value.
     * 5. Persist the session and return the [Account].
     * 6. Fetch user keys and load the primary private key into [ProtonCryptoHelper].
     */
    override suspend fun login(credentials: MailCredentials): Result<Account> {
        if (credentials !is MailCredentials.ProtonCredentials) {
            return Result.failure(IllegalArgumentException("ProtonMailProvider requires ProtonCredentials"))
        }

        return runCatching {
            // Step 1: auth info
            val authInfo = apiClient.getAuthInfo(credentials.username)

            // Step 2: SRP proofs — may throw NotImplementedError until BCrypt integration
            val proofs = try {
                // Strip PGP armor from the modulus if present
                val rawModulus = stripPgpArmor(authInfo.modulus)
                ProtonSrp.generateProofs(
                    username = credentials.username,
                    password = credentials.password,
                    version = authInfo.version,
                    salt = authInfo.salt,
                    modulus = rawModulus,
                    serverEphemeral = authInfo.serverEphemeral,
                )
            } catch (e: NotImplementedError) {
                throw IllegalStateException(
                    "SRP BCrypt password hashing is not yet integrated. " +
                    "Add the proton-crypto library to complete Phase 2. Details: ${e.message}",
                    e,
                )
            }

            // Step 3: authenticate
            val authResponse = apiClient.authenticate(
                AuthRequest(
                    username = credentials.username,
                    clientEphemeral = proofs.clientEphemeral,
                    clientProof = proofs.clientProof,
                    srpSession = authInfo.srpSession,
                ),
            )

            // Step 4: verify server proof
            if (authResponse.serverProof != proofs.expectedServerProof) {
                throw ProtonAuthException(
                    "Server proof verification failed — possible MITM attack. Aborting login.",
                )
            }

            // Step 5: persist session
            sessionStore.saveSession(
                uid = authResponse.uid,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                userId = authResponse.userId,
            )

            // Step 6: load user keys into crypto helper
            val keysResponse = apiClient.getUserKeys()
            val primaryKey = keysResponse.keys.firstOrNull { it.primary == 1 }
                ?: keysResponse.keys.firstOrNull { it.active == 1 }
            primaryKey?.let { key ->
                runCatching {
                    cryptoHelper.loadPrivateKey(key.privateKey, credentials.password)
                }
                // Non-fatal if key loading fails — decryption will fall back to returning raw body
            }

            // Fetch user profile for the Account
            val userResponse = apiClient.getUser()
            val user = userResponse.user
            Account(
                id = user.id,
                displayName = user.displayName.ifBlank { user.name },
                email = user.email ?: "${user.name}@proton.me",
                type = AccountType.PROTON,
            )
        }
    }

    override suspend fun logout() {
        cryptoHelper.clearKey()
        sessionStore.clearSession()
    }

    override suspend fun isAuthenticated(): Boolean =
        sessionStore.getSession() != null

    // ── Labels / Folders ─────────────────────────────────────────────────────

    /**
     * Fetch folders (type=1) and system labels (type=3), combine them, and map to
     * the domain [Label] model.
     *
     * Proton system label IDs:
     * - "0"  = Inbox
     * - "1"  = AllDrafts
     * - "2"  = AllSent
     * - "3"  = Trash
     * - "4"  = Spam
     * - "5"  = AllMail
     * - "6"  = Starred
     * - "10" = Archive
     */
    override suspend fun getLabels(): Result<List<Label>> = runCatching {
        val foldersResponse = apiClient.getLabels(type = 1)
        val systemResponse = apiClient.getLabels(type = 3)

        val protonLabels = foldersResponse.labels + systemResponse.labels

        protonLabels.map { protonLabel ->
            Label(
                id = protonLabel.id,
                name = protonLabel.name,
                type = when (protonLabel.type) {
                    1 -> LabelType.FOLDER
                    2 -> LabelType.LABEL
                    3 -> LabelType.SYSTEM
                    else -> LabelType.LABEL
                },
                unreadCount = protonLabel.unread,
            )
        }
    }

    // ── Phase 3 ──────────────────────────────────────────────────────────────

    override suspend fun getConversations(labelId: String, page: Int): Result<List<Conversation>> =
        runCatching {
            val response = apiClient.getConversations(labelId, page)
            response.conversations.map { it.toDomain() }
        }

    override suspend fun getMessage(id: String): Result<Message> =
        runCatching {
            val response = apiClient.getMessageDetail(id)
            val decryptedBody = cryptoHelper.decryptBody(response.message.body)
            response.message.toDomain(decryptedBody)
        }

    override suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>> =
        runCatching {
            val response = apiClient.getMessagesInConversation(conversationId)
            // Summaries don't contain body — return without decryption
            response.messages.map { it.toSummaryDomain() }
        }

    override suspend fun markAsRead(messageIds: List<String>): Result<Unit> =
        runCatching { apiClient.markMessagesRead(messageIds) }

    override suspend fun markAsUnread(messageIds: List<String>): Result<Unit> =
        runCatching { apiClient.markMessagesUnread(messageIds) }

    override suspend fun moveToLabel(messageIds: List<String>, labelId: String): Result<Unit> =
        runCatching { apiClient.labelMessages(labelId, messageIds) }

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> =
        runCatching { apiClient.deleteMessages(messageIds) }

    override suspend fun sendMessage(draft: MessageDraft): Result<Unit> =
        runCatching {
            // Phase 3: simplified send — create draft then send without per-recipient encryption.
            // Full per-recipient PGP key lookup and encryption will be added in Phase 4.
            val user = apiClient.getUser().user
            val draftRequest = ProtonDraftRequest(
                message = ProtonDraftBody(
                    subject = draft.subject,
                    sender = ProtonEmailAddress(
                        name = user.displayName,
                        address = user.email ?: "${user.name}@proton.me",
                    ),
                    toList = draft.to.map { ProtonEmailAddress(it.name ?: "", it.address) },
                    ccList = draft.cc.map { ProtonEmailAddress(it.name ?: "", it.address) },
                    bccList = draft.bcc.map { ProtonEmailAddress(it.name ?: "", it.address) },
                    body = draft.body,
                    mimeType = "text/plain",
                ),
            )
            val draftResponse = apiClient.createDraft(draftRequest)
            val draftId = draftResponse.message.id

            // Send with a single cleartext package (Type=2) — works for external recipients.
            // For internal Proton recipients, Type=1 with encrypted body key packet is needed (Phase 4).
            val sendRequest = ProtonSendRequest(
                packages = listOf(
                    ProtonSendPackage(
                        addresses = draft.to.associate { recipient ->
                            recipient.address to ProtonAddressPackage(type = 2)
                        },
                        mimeType = "text/plain",
                        body = draft.body,
                    )
                )
            )
            apiClient.sendDraft(draftId, sendRequest)
        }

    override suspend fun sync(): Result<SyncResult> =
        runCatching {
            // Event-loop based sync using Proton's /core/v4/events/{eventId} endpoint.
            // Fetches the latest event ID on first sync, then processes deltas.
            val latestEventId = apiClient.getLatestEventId().eventId
            var currentEventId = latestEventId
            var newMessages = 0
            var updatedMessages = 0
            var more = true

            while (more) {
                val eventResponse = apiClient.getEvents(currentEventId)

                eventResponse.messages?.forEach { event ->
                    when (event.action) {
                        0 -> { /* delete — handled by repository */ }
                        1 -> newMessages++
                        2, 3 -> updatedMessages++
                    }
                }

                currentEventId = eventResponse.eventId
                more = eventResponse.more == 1
            }

            SyncResult(newMessages = newMessages, updatedMessages = updatedMessages)
        }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Strip PGP armor headers/footers from a modulus string if present.
     * Proton occasionally wraps the modulus in a PGP cleartext signature block.
     */
    private fun stripPgpArmor(modulus: String): String {
        val trimmed = modulus.trim()
        if (!trimmed.startsWith("-----")) return trimmed

        // Find the blank line that separates headers from body in PGP armor
        val lines = trimmed.lines()
        var bodyStart = -1
        for (i in lines.indices) {
            if (lines[i].isBlank()) {
                bodyStart = i + 1
                break
            }
        }
        if (bodyStart < 0) return trimmed

        // Collect body lines until the next armor marker
        val bodyLines = lines.drop(bodyStart).takeWhile { !it.startsWith("-----") }
        return bodyLines.joinToString("").trim()
    }
}

// ── Private extension functions ───────────────────────────────────────────────

private fun ProtonConversation.toDomain() = Conversation(
    id = id,
    subject = subject,
    senders = senders.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    numMessages = numMessages,
    numUnread = numUnread,
    latestTimestamp = time,
    labelIds = labelIds,
    isStarred = labelIds.contains("10"),  // Proton Starred label ID
)

private fun ProtonMessageDetail.toDomain(decryptedBody: String) = Message(
    id = id,
    conversationId = conversationId,
    subject = subject,
    sender = EmailAddress(sender.name.ifBlank { null }, sender.address),
    recipients = toList.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    ccRecipients = ccList.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    bccRecipients = bccList.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    body = decryptedBody,
    bodyMimeType = mimeType,
    timestamp = time,
    isRead = unread == 0,
    isStarred = starred == 1,
    labelIds = labelIds,
    attachments = attachments.map { Attachment(it.id, it.name, it.mimeType, it.size, true) },
    isEncrypted = true,
)

private fun ProtonMessageSummary.toSummaryDomain() = Message(
    id = id,
    conversationId = conversationId,
    subject = subject,
    sender = EmailAddress(sender.name.ifBlank { null }, sender.address),
    recipients = toList.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    ccRecipients = ccList.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    bccRecipients = bccList.map { EmailAddress(it.name.ifBlank { null }, it.address) },
    body = "",  // not available in summary — call getMessage() for full body
    bodyMimeType = mimeType,
    timestamp = time,
    isRead = unread == 0,
    isStarred = starred == 1,
    labelIds = labelIds,
    isEncrypted = true,
)
