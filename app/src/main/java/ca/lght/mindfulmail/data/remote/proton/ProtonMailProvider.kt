package ca.lght.mindfulmail.data.remote.proton

import ca.lght.mindfulmail.data.remote.proton.api.ProtonApiClient
import ca.lght.mindfulmail.data.remote.proton.api.ProtonAuthException
import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSessionStore
import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSrp
import ca.lght.mindfulmail.data.remote.proton.model.AuthRequest
import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.AccountType
import ca.lght.mindfulmail.domain.model.Conversation
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
 * Phase 3 will implement message sync, conversation fetch, send, etc.
 *
 * References:
 * - Proton API: https://protonmail.com/blog/security-updates-2018/
 * - PGPainless: https://pgpainless.org
 */
class ProtonMailProvider @Inject constructor(
    private val apiClient: ProtonApiClient,
    private val sessionStore: ProtonSessionStore,
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

    // ── Phase 3 (not yet implemented) ────────────────────────────────────────

    override suspend fun getConversations(labelId: String, page: Int): Result<List<Conversation>> =
        Result.failure(NotImplementedError("ProtonMailProvider: getConversations not yet implemented — see Phase 3"))

    override suspend fun getMessage(id: String): Result<Message> =
        Result.failure(NotImplementedError("ProtonMailProvider: getMessage not yet implemented — see Phase 3"))

    override suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>> =
        Result.failure(NotImplementedError("ProtonMailProvider: getMessagesInConversation not yet implemented — see Phase 3"))

    override suspend fun markAsRead(messageIds: List<String>): Result<Unit> =
        Result.failure(NotImplementedError("ProtonMailProvider: markAsRead not yet implemented — see Phase 3"))

    override suspend fun markAsUnread(messageIds: List<String>): Result<Unit> =
        Result.failure(NotImplementedError("ProtonMailProvider: markAsUnread not yet implemented — see Phase 3"))

    override suspend fun moveToLabel(messageIds: List<String>, labelId: String): Result<Unit> =
        Result.failure(NotImplementedError("ProtonMailProvider: moveToLabel not yet implemented — see Phase 3"))

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> =
        Result.failure(NotImplementedError("ProtonMailProvider: deleteMessages not yet implemented — see Phase 3"))

    override suspend fun sendMessage(draft: MessageDraft): Result<Unit> =
        Result.failure(NotImplementedError("ProtonMailProvider: sendMessage not yet implemented — see Phase 3"))

    override suspend fun sync(): Result<SyncResult> =
        Result.failure(NotImplementedError("ProtonMailProvider: sync not yet implemented — see Phase 3"))

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
