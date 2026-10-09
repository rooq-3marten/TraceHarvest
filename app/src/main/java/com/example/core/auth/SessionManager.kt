package com.example.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    val agentId: String = "AGENT-NG-042",
    val name: String = "Aminu Bello",
    val phone: String = "+2348031234567",
    val email: String = "aminu.bello@traceharvest.ng",
    val association: String = "Kano Rice & Grains Cooperative",
    val location: String = "Dambatta, Kano State",
    val status: AgentApprovalStatus = AgentApprovalStatus.PENDING,
    val rejectionReason: String? = null,
    val role: String = "FIELD_AGENT",
    val zoneCode: String = "NW",
    val tokenExpiry: Long = 0L
)

class SessionManager(private val context: Context) {

    companion object {
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

        // 30 days token expiry
        const val TOKEN_VALIDITY_MS = 30L * 24L * 60L * 60L * 1000L
        // 7 days refresh threshold
        const val REFRESH_THRESHOLD_MS = 7L * 24L * 60L * 60L * 1000L

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val dataStore = context.sessionDataStore

    val isLoggedInFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val token = prefs[KEY_AUTH_TOKEN]
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            val now = System.currentTimeMillis()
            !token.isNullOrBlank() && expiry > now
        }

    val agentProfileFlow: Flow<AgentProfile> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AgentProfile(
                agentId = prefs[KEY_AGENT_ID] ?: "AGENT-NG-042",
                name = prefs[KEY_AGENT_NAME] ?: "Aminu Bello",
                phone = prefs[KEY_AGENT_PHONE] ?: "+2348031234567",
                email = prefs[KEY_AGENT_EMAIL] ?: "aminu.bello@traceharvest.ng",
                association = prefs[KEY_AGENT_ASSOCIATION] ?: "Kano Rice & Grains Cooperative",
                location = prefs[KEY_AGENT_LOCATION] ?: "Dambatta, Kano State",
                status = AgentApprovalStatus.fromRaw(prefs[KEY_AGENT_STATUS] ?: "pending"),
                rejectionReason = prefs[KEY_REJECTION_REASON],
                role = prefs[KEY_AGENT_ROLE] ?: "FIELD_AGENT",
                tokenExpiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            )
        }

    suspend fun saveSession(
        token: String,
        refreshToken: String?,
        agentId: String,
        name: String,
        phone: String,
        email: String,
        association: String = "Kano Rice & Grains Cooperative",
        location: String = "Dambatta, Kano State",
        status: AgentApprovalStatus = AgentApprovalStatus.APPROVED,
        rejectionReason: String? = null,
        keepSignedIn: Boolean = true,
        expiryMs: Long = System.currentTimeMillis() + TOKEN_VALIDITY_MS
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            if (refreshToken != null) {
                prefs[KEY_REFRESH_TOKEN] = refreshToken
            }
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

    suspend fun getCachedProfile(): AgentProfile {
        return try {
            val prefs = dataStore.data.first()
            AgentProfile(
                agentId = prefs[KEY_AGENT_ID] ?: "AGENT-NG-042",
                name = prefs[KEY_AGENT_NAME] ?: "Aminu Bello",
                phone = prefs[KEY_AGENT_PHONE] ?: "+2348031234567",
                email = prefs[KEY_AGENT_EMAIL] ?: "aminu.bello@traceharvest.ng",
                association = prefs[KEY_AGENT_ASSOCIATION] ?: "Kano Rice & Grains Cooperative",
                location = prefs[KEY_AGENT_LOCATION] ?: "Dambatta, Kano State",
                status = AgentApprovalStatus.fromRaw(prefs[KEY_AGENT_STATUS] ?: "pending"),
                rejectionReason = prefs[KEY_REJECTION_REASON],
                role = prefs[KEY_AGENT_ROLE] ?: "FIELD_AGENT",
                tokenExpiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            )
        } catch (e: Exception) {
            AgentProfile()
        }
    }

    suspend fun hasValidSession(): Boolean {
        return try {
            val prefs = dataStore.data.first()
            val token = prefs[KEY_AUTH_TOKEN]
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            !token.isNullOrBlank() && expiry > System.currentTimeMillis()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAuthToken(): String? {
        return try {
            val prefs = dataStore.data.first()
            val token = prefs[KEY_AUTH_TOKEN]
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            if (!token.isNullOrBlank() && expiry > System.currentTimeMillis()) token else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRefreshToken(): String? {
        return try {
            val prefs = dataStore.data.first()
            prefs[KEY_REFRESH_TOKEN]
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

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_TOKEN_EXPIRY)
            prefs.remove(KEY_AGENT_STATUS)
            prefs.remove(KEY_REJECTION_REASON)
        }
    }
}
