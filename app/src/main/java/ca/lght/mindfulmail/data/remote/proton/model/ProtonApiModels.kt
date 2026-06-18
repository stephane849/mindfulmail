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
