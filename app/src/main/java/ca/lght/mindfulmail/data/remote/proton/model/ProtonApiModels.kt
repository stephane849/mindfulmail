package ca.lght.mindfulmail.data.remote.proton.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Auth step 1 — POST /auth/info
@Serializable
data class AuthInfoRequest(
    @SerialName("Username") val username: String,
)

@Serializable
data class AuthInfoResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Modulus") val modulus: String,
    @SerialName("ServerEphemeral") val serverEphemeral: String,
    @SerialName("Version") val version: Int,
    @SerialName("Salt") val salt: String,
    @SerialName("SRPSession") val srpSession: String,
)

// Auth step 2 — POST /auth
@Serializable
data class AuthRequest(
    @SerialName("Username") val username: String,
    @SerialName("ClientEphemeral") val clientEphemeral: String,
    @SerialName("ClientProof") val clientProof: String,
    @SerialName("SRPSession") val srpSession: String,
)

@Serializable
data class AuthResponse(
    @SerialName("Code") val code: Int,
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("TokenType") val tokenType: String,
    @SerialName("UID") val uid: String,
    @SerialName("RefreshToken") val refreshToken: String,
    @SerialName("ServerProof") val serverProof: String,
    @SerialName("UserID") val userId: String,
)

// User info — GET /core/v4/users
@Serializable
data class ProtonUserResponse(
    @SerialName("Code") val code: Int,
    @SerialName("User") val user: ProtonUser,
)

@Serializable
data class ProtonUser(
    @SerialName("ID") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("DisplayName") val displayName: String,
    @SerialName("Email") val email: String? = null,
)

// Labels — GET /core/v4/labels?Type=1 (folders) and ?Type=2 (labels)
@Serializable
data class ProtonLabelsResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Labels") val labels: List<ProtonLabel>,
)

@Serializable
data class ProtonLabel(
    @SerialName("ID") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Type") val type: Int,   // 1=folder, 2=label, 3=system
    @SerialName("Unread") val unread: Int = 0,
)

// Refresh token — POST /auth/refresh
@Serializable
data class RefreshRequest(
    @SerialName("ResponseType") val responseType: String = "token",
    @SerialName("GrantType") val grantType: String = "refresh_token",
    @SerialName("RefreshToken") val refreshToken: String,
    @SerialName("RedirectURI") val redirectUri: String = "https://protonmail.com",
)

@Serializable
data class RefreshResponse(
    @SerialName("Code") val code: Int,
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("RefreshToken") val refreshToken: String,
)

// Conversations — GET /mail/v4/conversations?LabelID=X&Page=N
@Serializable
data class ProtonConversationsResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Total") val total: Int,
    @SerialName("Conversations") val conversations: List<ProtonConversation>,
)

@Serializable
data class ProtonConversation(
    @SerialName("ID") val id: String,
    @SerialName("Subject") val subject: String,
    @SerialName("Senders") val senders: List<ProtonEmailAddress>,
    @SerialName("Recipients") val recipients: List<ProtonEmailAddress>,
    @SerialName("NumMessages") val numMessages: Int,
    @SerialName("NumUnread") val numUnread: Int,
    @SerialName("Time") val time: Long,
    @SerialName("LabelIDs") val labelIds: List<String> = emptyList(),
    @SerialName("Labels") val labels: List<ProtonConversationLabel> = emptyList(),
)

@Serializable
data class ProtonConversationLabel(
    @SerialName("ID") val id: String,
    @SerialName("ContextNumUnread") val contextNumUnread: Int = 0,
)

@Serializable
data class ProtonEmailAddress(
    @SerialName("Name") val name: String = "",
    @SerialName("Address") val address: String,
)

// Messages — GET /mail/v4/messages?ConversationID=X or GET /mail/v4/messages/ID
@Serializable
data class ProtonMessagesResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Total") val total: Int,
    @SerialName("Messages") val messages: List<ProtonMessageSummary>,
)

@Serializable
data class ProtonMessageSummary(
    @SerialName("ID") val id: String,
    @SerialName("ConversationID") val conversationId: String,
    @SerialName("Subject") val subject: String,
    @SerialName("Sender") val sender: ProtonEmailAddress,
    @SerialName("ToList") val toList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("CCList") val ccList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("BCCList") val bccList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("Time") val time: Long,
    @SerialName("Unread") val unread: Int,
    @SerialName("IsReplied") val isReplied: Int = 0,
    @SerialName("IsForwarded") val isForwarded: Int = 0,
    @SerialName("LabelIDs") val labelIds: List<String> = emptyList(),
    @SerialName("Starred") val starred: Int = 0,
    @SerialName("NumAttachments") val numAttachments: Int = 0,
    @SerialName("MIMEType") val mimeType: String = "text/plain",
)

// Full message — GET /mail/v4/messages/{id}
@Serializable
data class ProtonMessageResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Message") val message: ProtonMessageDetail,
)

@Serializable
data class ProtonMessageDetail(
    @SerialName("ID") val id: String,
    @SerialName("ConversationID") val conversationId: String,
    @SerialName("Subject") val subject: String,
    @SerialName("Sender") val sender: ProtonEmailAddress,
    @SerialName("ToList") val toList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("CCList") val ccList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("BCCList") val bccList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("Time") val time: Long,
    @SerialName("Unread") val unread: Int,
    @SerialName("Body") val body: String,   // encrypted PGP body
    @SerialName("MIMEType") val mimeType: String = "text/plain",
    @SerialName("LabelIDs") val labelIds: List<String> = emptyList(),
    @SerialName("Starred") val starred: Int = 0,
    @SerialName("Attachments") val attachments: List<ProtonAttachment> = emptyList(),
    @SerialName("Header") val header: String = "",
)

@Serializable
data class ProtonAttachment(
    @SerialName("ID") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("MIMEType") val mimeType: String,
    @SerialName("Size") val size: Long,
    @SerialName("KeyPackets") val keyPackets: String = "",
)

// Event loop — GET /core/v4/events/{eventId}
@Serializable
data class ProtonEventResponse(
    @SerialName("Code") val code: Int,
    @SerialName("EventID") val eventId: String,
    @SerialName("More") val more: Int = 0,
    @SerialName("Messages") val messages: List<ProtonEventMessage>? = null,
    @SerialName("Conversations") val conversations: List<ProtonEventConversation>? = null,
)

@Serializable
data class ProtonEventMessage(
    @SerialName("ID") val id: String,
    @SerialName("Action") val action: Int,  // 0=delete, 1=create, 2=update flags, 3=update draft
    @SerialName("Message") val message: ProtonMessageSummary? = null,
)

@Serializable
data class ProtonEventConversation(
    @SerialName("ID") val id: String,
    @SerialName("Action") val action: Int,
    @SerialName("Conversation") val conversation: ProtonConversation? = null,
)

// Outgoing message — POST /mail/v4/messages
@Serializable
data class ProtonDraftRequest(
    @SerialName("Message") val message: ProtonDraftBody,
)

@Serializable
data class ProtonDraftBody(
    @SerialName("Subject") val subject: String,
    @SerialName("Sender") val sender: ProtonEmailAddress,
    @SerialName("ToList") val toList: List<ProtonEmailAddress>,
    @SerialName("CCList") val ccList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("BCCList") val bccList: List<ProtonEmailAddress> = emptyList(),
    @SerialName("Body") val body: String,           // PGP-encrypted body
    @SerialName("MIMEType") val mimeType: String = "text/plain",
)

@Serializable
data class ProtonDraftResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Message") val message: ProtonMessageDetail,
)

// Send — POST /mail/v4/messages/{draftId}
@Serializable
data class ProtonSendRequest(
    @SerialName("Packages") val packages: List<ProtonSendPackage>,
)

@Serializable
data class ProtonSendPackage(
    @SerialName("Addresses") val addresses: Map<String, ProtonAddressPackage>,
    @SerialName("MIMEType") val mimeType: String,
    @SerialName("Body") val body: String,
)

@Serializable
data class ProtonAddressPackage(
    @SerialName("Type") val type: Int,  // 1=internal Proton, 2=external cleartext, 4=external PGP
    @SerialName("BodyKeyPacket") val bodyKeyPacket: String = "",
    @SerialName("PublicKey") val publicKey: String = "",
)

// Mark as read/unread — PUT /mail/v4/messages/read and /unread
@Serializable
data class ProtonMessageIdsRequest(
    @SerialName("IDs") val ids: List<String>,
)

// Move to label — PUT /mail/v4/messages/label
@Serializable
data class ProtonLabelMessagesRequest(
    @SerialName("LabelID") val labelId: String,
    @SerialName("IDs") val ids: List<String>,
)

// Initial event ID — GET /core/v4/events/latest
@Serializable
data class ProtonLatestEventResponse(
    @SerialName("Code") val code: Int,
    @SerialName("EventID") val eventId: String,
)

// User keys — GET /core/v4/keys/user (needed to find the private key for decryption)
@Serializable
data class ProtonUserKeysResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Keys") val keys: List<ProtonUserKey>,
)

@Serializable
data class ProtonUserKey(
    @SerialName("ID") val id: String,
    @SerialName("Version") val version: Int,
    @SerialName("PrivateKey") val privateKey: String,   // armored PGP private key
    @SerialName("Token") val token: String? = null,
    @SerialName("Active") val active: Int = 1,
    @SerialName("Primary") val primary: Int = 0,
)
