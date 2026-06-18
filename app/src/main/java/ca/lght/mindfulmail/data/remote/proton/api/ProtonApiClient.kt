package ca.lght.mindfulmail.data.remote.proton.api

import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSession
import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSessionStore
import ca.lght.mindfulmail.data.remote.proton.model.AuthInfoRequest
import ca.lght.mindfulmail.data.remote.proton.model.AuthInfoResponse
import ca.lght.mindfulmail.data.remote.proton.model.AuthRequest
import ca.lght.mindfulmail.data.remote.proton.model.AuthResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonConversationsResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonDraftRequest
import ca.lght.mindfulmail.data.remote.proton.model.ProtonDraftResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonEventResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonLabelMessagesRequest
import ca.lght.mindfulmail.data.remote.proton.model.ProtonLabelsResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonLatestEventResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonMessageIdsRequest
import ca.lght.mindfulmail.data.remote.proton.model.ProtonMessageResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonMessagesResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonSendRequest
import ca.lght.mindfulmail.data.remote.proton.model.ProtonResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonStatusResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonUserKeysResponse
import ca.lght.mindfulmail.data.remote.proton.model.ProtonUserResponse
import ca.lght.mindfulmail.data.remote.proton.model.RefreshRequest
import ca.lght.mindfulmail.data.remote.proton.model.RefreshResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown when Proton API authentication fails and cannot be recovered via token refresh. */
class ProtonAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when the Proton API returns a non-1000 Code in the response body. */
class ProtonApiException(val code: Int, message: String) : Exception("Proton API error $code: $message")

/** Validate that a Proton API response carries Code=1000; throw [ProtonApiException] otherwise. */
fun <T : ProtonResponse> T.validate(): T = apply {
    if (code != 1000) throw ProtonApiException(code, error ?: "unknown error")
}

/**
 * Ktor-based HTTP client wrapper for the Proton Mail REST API.
 *
 * Handles:
 * - Auth header injection (Bearer token + x-pm-uid) for authenticated endpoints.
 * - Transparent token refresh on HTTP 401, with one retry.
 * - Login (two-step SRP: getAuthInfo + authenticate).
 * - Label/folder fetch.
 */
@Singleton
class ProtonApiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: ProtonSessionStore,
) {
    private val baseUrl = "https://mail.proton.me/api"

    // ── Auth endpoints ───────────────────────────────────────────────────────

    /**
     * Step 1 of SRP auth — POST /auth/info.
     * Returns server modulus, ephemeral, and SRP session ID.
     */
    suspend fun getAuthInfo(username: String): AuthInfoResponse =
        httpClient.post("$baseUrl/auth/info") {
            contentType(ContentType.Application.Json)
            setBody(AuthInfoRequest(username))
        }.body<AuthInfoResponse>().validate()

    /**
     * Step 2 of SRP auth — POST /auth.
     * Sends client ephemeral and proof; receives access/refresh tokens.
     */
    suspend fun authenticate(request: AuthRequest): AuthResponse =
        httpClient.post("$baseUrl/auth") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<AuthResponse>().validate()

    /**
     * Refresh the access token using the stored refresh token — POST /auth/refresh.
     * Saves the new session and returns the response.
     *
     * @throws ProtonAuthException if no session exists or the refresh request fails.
     */
    suspend fun refreshToken(): RefreshResponse {
        val session = sessionStore.getSession()
            ?: throw ProtonAuthException("No active session to refresh")

        val response: RefreshResponse = httpClient.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            header("x-pm-uid", session.uid)
            setBody(RefreshRequest(refreshToken = session.refreshToken))
        }.body<RefreshResponse>().validate()

        // Persist the new tokens
        sessionStore.saveSession(
            uid = session.uid,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = session.userId,
        )
        return response
    }

    // ── Authenticated endpoints ──────────────────────────────────────────────

    /** GET /core/v4/users — returns the logged-in Proton user's profile. */
    suspend fun getUser(): ProtonUserResponse =
        authenticatedGet<ProtonUserResponse>("$baseUrl/core/v4/users").validate()

    /**
     * GET /core/v4/labels?Type=[type]
     *
     * @param type 1 = folders, 2 = user labels, 3 = system labels.
     */
    suspend fun getLabels(type: Int): ProtonLabelsResponse =
        authenticatedGet<ProtonLabelsResponse>("$baseUrl/core/v4/labels") {
            parameter("Type", type)
        }.validate()

    // ── Conversations ────────────────────────────────────────────────────────

    suspend fun getConversations(labelId: String, page: Int = 0): ProtonConversationsResponse =
        authenticatedGet<ProtonConversationsResponse>("$baseUrl/mail/v4/conversations") {
            parameter("LabelID", labelId)
            parameter("Page", page)
            parameter("PageSize", 50)
        }.validate()

    // ── Messages ─────────────────────────────────────────────────────────────

    suspend fun getMessagesInConversation(conversationId: String): ProtonMessagesResponse =
        authenticatedGet<ProtonMessagesResponse>("$baseUrl/mail/v4/messages") {
            parameter("ConversationID", conversationId)
        }.validate()

    suspend fun getMessageDetail(messageId: String): ProtonMessageResponse =
        authenticatedGet<ProtonMessageResponse>("$baseUrl/mail/v4/messages/$messageId").validate()

    // ── Event loop ───────────────────────────────────────────────────────────

    suspend fun getLatestEventId(): ProtonLatestEventResponse =
        authenticatedGet<ProtonLatestEventResponse>("$baseUrl/core/v4/events/latest").validate()

    suspend fun getEvents(eventId: String): ProtonEventResponse =
        authenticatedGet<ProtonEventResponse>("$baseUrl/core/v4/events/$eventId").validate()

    // ── Keys ─────────────────────────────────────────────────────────────────

    suspend fun getUserKeys(): ProtonUserKeysResponse =
        authenticatedGet<ProtonUserKeysResponse>("$baseUrl/core/v4/keys/user").validate()

    // ── Actions ──────────────────────────────────────────────────────────────

    suspend fun markMessagesRead(ids: List<String>) {
        authenticatedPut<ProtonStatusResponse>("$baseUrl/mail/v4/messages/read", ProtonMessageIdsRequest(ids)).validate()
    }

    suspend fun markMessagesUnread(ids: List<String>) {
        authenticatedPut<ProtonStatusResponse>("$baseUrl/mail/v4/messages/unread", ProtonMessageIdsRequest(ids)).validate()
    }

    suspend fun labelMessages(labelId: String, ids: List<String>) {
        authenticatedPut<ProtonStatusResponse>("$baseUrl/mail/v4/messages/label", ProtonLabelMessagesRequest(labelId, ids)).validate()
    }

    suspend fun deleteMessages(ids: List<String>) {
        authenticatedPut<ProtonStatusResponse>("$baseUrl/mail/v4/messages/delete", ProtonMessageIdsRequest(ids)).validate()
    }

    // ── Draft + send ─────────────────────────────────────────────────────────

    suspend fun createDraft(request: ProtonDraftRequest): ProtonDraftResponse =
        authenticatedPost<ProtonDraftResponse>("$baseUrl/mail/v4/messages", request).validate()

    suspend fun sendDraft(draftId: String, request: ProtonSendRequest) {
        authenticatedPost<ProtonStatusResponse>("$baseUrl/mail/v4/messages/$draftId", request).validate()
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private suspend inline fun <reified T> authenticatedGet(
        url: String,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val session = requireSession()
        val response = httpClient.get(url) {
            applyAuthHeaders(session)
            block()
        }
        return handleAuthResponse(response) {
            val newSession = requireSession()
            httpClient.get(url) {
                applyAuthHeaders(newSession)
                block()
            }.body()
        }
    }

    @Suppress("unused")
    private suspend inline fun <reified T> authenticatedPost(
        url: String,
        body: Any,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val session = requireSession()
        val response = httpClient.post(url) {
            applyAuthHeaders(session)
            contentType(ContentType.Application.Json)
            setBody(body)
            block()
        }
        return handleAuthResponse(response) {
            val newSession = requireSession()
            httpClient.post(url) {
                applyAuthHeaders(newSession)
                contentType(ContentType.Application.Json)
                setBody(body)
                block()
            }.body()
        }
    }

    @Suppress("unused")
    private suspend inline fun <reified T> authenticatedPut(
        url: String,
        body: Any,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val session = requireSession()
        val response = httpClient.put(url) {
            applyAuthHeaders(session)
            contentType(ContentType.Application.Json)
            setBody(body)
            block()
        }
        return handleAuthResponse(response) {
            val newSession = requireSession()
            httpClient.put(url) {
                applyAuthHeaders(newSession)
                contentType(ContentType.Application.Json)
                setBody(body)
                block()
            }.body()
        }
    }

    /**
     * Inspect [response]. If 401, refresh the token and invoke [retryBlock].
     * Otherwise return the body directly.
     */
    private suspend inline fun <reified T> handleAuthResponse(
        response: HttpResponse,
        retryBlock: () -> T,
    ): T {
        if (response.status == HttpStatusCode.Unauthorized) {
            try {
                refreshToken()
            } catch (e: Exception) {
                throw ProtonAuthException("Token refresh failed: ${e.message}", e)
            }
            return retryBlock()
        }
        return response.body()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuthHeaders(session: ProtonSession) {
        header("Authorization", "Bearer ${session.accessToken}")
        header("x-pm-uid", session.uid)
    }

    private fun requireSession(): ProtonSession =
        sessionStore.getSession()
            ?: throw ProtonAuthException("Not authenticated — please log in first")
}
