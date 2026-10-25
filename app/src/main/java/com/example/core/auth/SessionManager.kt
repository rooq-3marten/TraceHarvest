package com.example.core.auth

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "traceharvest_agent_session")

enum class AgentApprovalStatus(val rawValue: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    SUSPENDED("suspended");

    companion object {
        fun fromRaw(value: String?): AgentApprovalStatus {
            return entries.find { it.rawValue.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

data class AgentProfile(
    val agentId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val association: String = "",
    val location: String = "",
    val status: AgentApprovalStatus = AgentApprovalStatus.PENDING,
    val rejectionReason: String? = null,
    val role: String = "FIELD_AGENT",
    val zoneCode: String = "NW",
    val tokenExpiry: Long = 0L
)

/**
 * Holds the signed-in agent's session.
 *
 * - Access and refresh tokens are encrypted (AES-GCM, key in the Android Keystore) before they reach disk.
 *   Tokens saved in plain text by earlier app versions are migrated once and then deleted.
 * - Nothing here ever invents an identity: with no stored session the profile is empty.
 */
class SessionManager(
    private val context: Context,
    private val cipher: TokenCipher = KeystoreTokenCipher()
) : AuthTokenStore {

    companion object {
        private const val TAG = "SessionManager"

        // Encrypted token storage
        val KEY_AUTH_TOKEN_ENC = stringPreferencesKey("auth_token_enc")
        val KEY_REFRESH_TOKEN_ENC = stringPreferencesKey("refresh_token_enc")

        // Plain-text keys written by earlier versions. Read only for the one-time migration.
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")

        val KEY_TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        val KEY_LAST_LOGIN = longPreferencesKey("last_login_epoch")
        val KEY_AGENT_ID = stringPreferencesKey("agent_id")
        val KEY_AGENT_NAME = stringPreferencesKey("agent_name")
        val KEY_AGENT_PHONE = stringPreferencesKey("agent_phone")
        val KEY_AGENT_EMAIL = stringPreferencesKey("agent_email")
        val KEY_AGENT_ASSOCIATION = stringPreferencesKey("agent_association")
        val KEY_AGENT_LOCATION = stringPreferencesKey("agent_location")
        val KEY_AGENT_STATUS = stringPreferencesKey("agent_approval_status")
        val KEY_REJECTION_REASON = stringPreferencesKey("agent_rejection_reason")
        val KEY_AGENT_ROLE = stringPreferencesKey("agent_role")
        val KEY_KEEP_SIGNED_IN = booleanPreferencesKey("keep_signed_in")
        val KEY_SESSION_EXPIRED_NOTICE = booleanPreferencesKey("session_expired_notice")

        // 30 days: the sliding window the server grants to each refresh token
        const val TOKEN_VALIDITY_MS = 30L * 24L * 60L * 60L * 1000L
        // 7 days refresh threshold
        const val REFRESH_THRESHOLD_MS = 7L * 24L * 60L * 60L * 1000L

        private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /** Emits when a silent token refresh was rejected by the server and the session was cleared. */
        val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val dataStore = context.sessionDataStore

    private val migrationMutex = Mutex()

    @Volatile
    private var legacyTokensMigrated = false

    private fun safeData(): Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    /** True when a token (encrypted, or not yet migrated) is stored. Does not decrypt anything. */
    private fun Preferences.hasStoredAccessToken(): Boolean =
        !this[KEY_AUTH_TOKEN_ENC].isNullOrBlank() || !this[KEY_AUTH_TOKEN].isNullOrBlank()

    private fun Preferences.toProfile(): AgentProfile {
        val agentId = this[KEY_AGENT_ID].orEmpty()
        // No stored identity means no profile. Never substitute a made-up agent.
        if (agentId.isBlank()) return AgentProfile()
        return AgentProfile(
            agentId = agentId,
            name = this[KEY_AGENT_NAME].orEmpty(),
            phone = this[KEY_AGENT_PHONE].orEmpty(),
            email = this[KEY_AGENT_EMAIL].orEmpty(),
            association = this[KEY_AGENT_ASSOCIATION].orEmpty(),
            location = this[KEY_AGENT_LOCATION].orEmpty(),
            status = AgentApprovalStatus.fromRaw(this[KEY_AGENT_STATUS] ?: "pending"),
            rejectionReason = this[KEY_REJECTION_REASON],
            role = this[KEY_AGENT_ROLE] ?: "FIELD_AGENT",
            tokenExpiry = this[KEY_TOKEN_EXPIRY] ?: 0L
        )
    }

    /** Decrypts a stored token, falling back to a not-yet-migrated plain value. */
    private fun Preferences.readToken(encKey: Preferences.Key<String>, legacyKey: Preferences.Key<String>): String? {
        val encrypted = this[encKey]
        if (!encrypted.isNullOrBlank()) {
            // If it cannot be decrypted (lost key) the session is treated as absent.
            return cipher.decrypt(encrypted)?.takeIf { it.isNotBlank() }
        }
        return this[legacyKey]?.takeIf { it.isNotBlank() }
    }

    /**
     * One-time migration: encrypt tokens that earlier versions saved in plain text, then delete the plain copies.
     * Safe to call repeatedly. If encryption is unavailable the plain values are left alone and retried later.
     */
    private suspend fun migrateLegacyTokens() {
        if (legacyTokensMigrated) return
        migrationMutex.withLock {
            if (legacyTokensMigrated) return
            try {
                dataStore.edit { prefs ->
                    val legacyAccess = prefs[KEY_AUTH_TOKEN]
                    if (legacyAccess != null) {
                        if (prefs[KEY_AUTH_TOKEN_ENC].isNullOrBlank() && legacyAccess.isNotBlank()) {
                            prefs[KEY_AUTH_TOKEN_ENC] = cipher.encrypt(legacyAccess)
                        }
                        prefs.remove(KEY_AUTH_TOKEN)
                    }
                    val legacyRefresh = prefs[KEY_REFRESH_TOKEN]
                    if (legacyRefresh != null) {
                        if (prefs[KEY_REFRESH_TOKEN_ENC].isNullOrBlank() && legacyRefresh.isNotBlank()) {
                            prefs[KEY_REFRESH_TOKEN_ENC] = cipher.encrypt(legacyRefresh)
                        }
                        prefs.remove(KEY_REFRESH_TOKEN)
                    }
                }
                legacyTokensMigrated = true
            } catch (e: Exception) {
                Log.w(TAG, "Token migration deferred: ${e.message}")
            }
        }
    }

    val isLoggedInFlow: Flow<Boolean> = safeData().map { prefs ->
        val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
        prefs.hasStoredAccessToken() && expiry > System.currentTimeMillis()
    }

    val agentProfileFlow: Flow<AgentProfile> = safeData().map { prefs -> prefs.toProfile() }

    suspend fun saveSession(
        token: String,
        refreshToken: String?,
        agentId: String,
        name: String,
        phone: String,
        email: String,
        association: String = "",
        location: String = "",
        status: AgentApprovalStatus = AgentApprovalStatus.APPROVED,
        rejectionReason: String? = null,
        keepSignedIn: Boolean = true,
        expiryMs: Long = System.currentTimeMillis() + TOKEN_VALIDITY_MS
    ) {
        // Encrypt before the edit so a Keystore failure aborts the save instead of writing plain text.
        val encryptedAccess = cipher.encrypt(token)
        val encryptedRefresh = refreshToken?.let { cipher.encrypt(it) }
        dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN_ENC] = encryptedAccess
            if (encryptedRefresh != null) {
                prefs[KEY_REFRESH_TOKEN_ENC] = encryptedRefresh
            } else {
                prefs.remove(KEY_REFRESH_TOKEN_ENC)
            }
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_SESSION_EXPIRED_NOTICE)
            prefs[KEY_TOKEN_EXPIRY] = expiryMs
            prefs[KEY_LAST_LOGIN] = System.currentTimeMillis()
            prefs[KEY_AGENT_ID] = agentId
            prefs[KEY_AGENT_NAME] = name
            prefs[KEY_AGENT_PHONE] = phone
            prefs[KEY_AGENT_EMAIL] = email
            prefs[KEY_AGENT_ASSOCIATION] = association
            prefs[KEY_AGENT_LOCATION] = location
            prefs[KEY_AGENT_STATUS] = status.rawValue
            if (rejectionReason != null) {
                prefs[KEY_REJECTION_REASON] = rejectionReason
            } else {
                prefs.remove(KEY_REJECTION_REASON)
            }
            prefs[KEY_AGENT_ROLE] = if (email.contains("supervisor", ignoreCase = true)) "SUPERVISOR" else "FIELD_AGENT"
            prefs[KEY_KEEP_SIGNED_IN] = keepSignedIn
        }
        legacyTokensMigrated = true
    }

    suspend fun updateAgentStatus(
        status: AgentApprovalStatus,
        rejectionReason: String? = null,
        updatedName: String? = null,
        updatedAssociation: String? = null,
        updatedLocation: String? = null,
        updatedPhone: String? = null
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_AGENT_STATUS] = status.rawValue
            if (rejectionReason != null) {
                prefs[KEY_REJECTION_REASON] = rejectionReason
            } else {
                prefs.remove(KEY_REJECTION_REASON)
            }
            if (updatedName != null) prefs[KEY_AGENT_NAME] = updatedName
            if (updatedAssociation != null) prefs[KEY_AGENT_ASSOCIATION] = updatedAssociation
            if (updatedLocation != null) prefs[KEY_AGENT_LOCATION] = updatedLocation
            if (updatedPhone != null) prefs[KEY_AGENT_PHONE] = updatedPhone
        }
    }

    /**
     * The stored profile. With nothing stored this is an EMPTY profile (blank agentId), never a made-up agent,
     * and the Splash screen routes to Login.
     */
    suspend fun getCachedProfile(): AgentProfile {
        return try {
            dataStore.data.first().toProfile()
        } catch (e: Exception) {
            AgentProfile()
        }
    }

    /** The signed-in agent's ID, or null when there is no valid session or no stored identity. */
    suspend fun currentAgentIdOrNull(): String? {
        if (!hasValidSession()) return null
        return getCachedProfile().agentId.takeIf { it.isNotBlank() }
    }

    suspend fun hasValidSession(): Boolean {
        return try {
            migrateLegacyTokens()
            val prefs = dataStore.data.first()
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            prefs.readToken(KEY_AUTH_TOKEN_ENC, KEY_AUTH_TOKEN) != null && expiry > System.currentTimeMillis()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAuthToken(): String? {
        return try {
            migrateLegacyTokens()
            val prefs = dataStore.data.first()
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            if (expiry > System.currentTimeMillis()) prefs.readToken(KEY_AUTH_TOKEN_ENC, KEY_AUTH_TOKEN) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRefreshToken(): String? {
        return try {
            migrateLegacyTokens()
            dataStore.data.first().readToken(KEY_REFRESH_TOKEN_ENC, KEY_REFRESH_TOKEN)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun shouldRefreshToken(): Boolean {
        return try {
            val prefs = dataStore.data.first()
            val lastLogin = prefs[KEY_LAST_LOGIN] ?: 0L
            val elapsed = System.currentTimeMillis() - lastLogin
            elapsed > REFRESH_THRESHOLD_MS
        } catch (e: Exception) {
            false
        }
    }

    // ---- AuthTokenStore: used by the silent token refresh ----

    override suspend fun currentAccessToken(): String? {
        return try {
            migrateLegacyTokens()
            dataStore.data.first().readToken(KEY_AUTH_TOKEN_ENC, KEY_AUTH_TOKEN)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun currentRefreshToken(): String? = getRefreshToken()

    override suspend fun saveRefreshedTokens(accessToken: String, refreshToken: String?) {
        val encryptedAccess = cipher.encrypt(accessToken)
        val encryptedRefresh = refreshToken?.takeIf { it.isNotBlank() }?.let { cipher.encrypt(it) }
        dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN_ENC] = encryptedAccess
            if (encryptedRefresh != null) prefs[KEY_REFRESH_TOKEN_ENC] = encryptedRefresh
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            val now = System.currentTimeMillis()
            prefs[KEY_TOKEN_EXPIRY] = now + TOKEN_VALIDITY_MS
            prefs[KEY_LAST_LOGIN] = now
        }
    }

    override suspend fun expireSession() {
        clearSession()
        dataStore.edit { prefs -> prefs[KEY_SESSION_EXPIRED_NOTICE] = true }
        _sessionExpiredEvents.tryEmit(Unit)
    }

    /** True once after [expireSession]; the Login screen uses it to say "Session expired, please sign in". */
    suspend fun consumeSessionExpiredNotice(): Boolean {
        return try {
            var had = false
            dataStore.edit { prefs ->
                had = prefs[KEY_SESSION_EXPIRED_NOTICE] == true
                prefs.remove(KEY_SESSION_EXPIRED_NOTICE)
            }
            had
        } catch (e: Exception) {
            false
        }
    }

    /** Signs out: removes the tokens and the signed-in identity. Records in the local database are untouched. */
    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN_ENC)
            prefs.remove(KEY_REFRESH_TOKEN_ENC)
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_TOKEN_EXPIRY)
            prefs.remove(KEY_LAST_LOGIN)
            prefs.remove(KEY_AGENT_STATUS)
            prefs.remove(KEY_REJECTION_REASON)
            prefs.remove(KEY_AGENT_ID)
            prefs.remove(KEY_AGENT_NAME)
            prefs.remove(KEY_AGENT_PHONE)
            prefs.remove(KEY_AGENT_EMAIL)
            prefs.remove(KEY_AGENT_ASSOCIATION)
            prefs.remove(KEY_AGENT_LOCATION)
            prefs.remove(KEY_AGENT_ROLE)
        }
    }
}
