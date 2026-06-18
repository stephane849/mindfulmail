package ca.lght.mindfulmail.data.remote.proton.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted persistent storage for a Proton session (access token, refresh token, UID, user ID).
 *
 * Uses Jetpack Security's [EncryptedSharedPreferences] backed by an AES-256 master key
 * in the Android KeyStore, so credentials are at rest-encrypted and survive app restarts.
 */
@Singleton
class ProtonSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "proton_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveSession(uid: String, accessToken: String, refreshToken: String, userId: String) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getSession(): ProtonSession? {
        val uid = prefs.getString(KEY_UID, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return ProtonSession(uid, accessToken, refreshToken, userId)
    }

    fun saveEventId(eventId: String) {
        prefs.edit().putString(KEY_EVENT_ID, eventId).apply()
    }

    fun getEventId(): String? = prefs.getString(KEY_EVENT_ID, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EVENT_ID = "event_id"
    }
}

data class ProtonSession(
    val uid: String,
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
)
