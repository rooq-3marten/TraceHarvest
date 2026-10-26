package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.core.auth.SessionManager
import com.example.core.auth.sessionDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTokenStorageTest {

    private lateinit var ctx: Context
    private lateinit var manager: SessionManager

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        manager = SessionManager(ctx, FakeTokenCipher())
        runBlocking { manager.clearSession() }
    }

    @Test
    fun `a saved session round-trips through the cipher`() = runBlocking {
        manager.saveSession("access-1", "refresh-1", "TEST-AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        assertTrue(manager.hasValidSession())
        assertEquals("access-1", manager.getAuthToken())
        assertEquals("refresh-1", manager.getRefreshToken())
        assertEquals("TEST-AGENT-X", manager.currentAgentIdOrNull())
    }

    @Test
    fun `a signed out device has an empty profile and no agent id`() = runBlocking {
        assertEquals("", manager.getCachedProfile().agentId)
        assertNull(manager.currentAgentIdOrNull())
        assertFalse(manager.hasValidSession())
    }

    @Test
    fun `refreshed tokens replace the stored ones`() = runBlocking {
        manager.saveSession("access-1", "refresh-1", "TEST-AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        manager.saveRefreshedTokens("access-2", "refresh-2")
        assertEquals("access-2", manager.currentAccessToken())
        assertEquals("refresh-2", manager.currentRefreshToken())
    }

    @Test
    fun `expiring the session clears identity and leaves a one-time notice`() = runBlocking {
        manager.saveSession("access-1", "refresh-1", "TEST-AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        manager.expireSession()
        assertFalse(manager.hasValidSession())
        assertNull(manager.currentAgentIdOrNull())
        assertTrue(manager.consumeSessionExpiredNotice())
        assertFalse(manager.consumeSessionExpiredNotice())
    }

    @Test
    fun `legacy plain text tokens are migrated once to encrypted keys and deleted`() = runBlocking {
        ctx.sessionDataStore.edit { prefs ->
            prefs.clear()
            prefs[SessionManager.KEY_AUTH_TOKEN] = "legacy-plain-access"
            prefs[SessionManager.KEY_REFRESH_TOKEN] = "legacy-plain-refresh"
            prefs[SessionManager.KEY_AGENT_ID] = "TEST-AGENT-LEGACY"
            prefs[SessionManager.KEY_TOKEN_EXPIRY] = System.currentTimeMillis() + 60_000L
        }

        val freshManager = SessionManager(ctx, FakeTokenCipher())
        assertEquals("legacy-plain-access", freshManager.getAuthToken())
        assertEquals("legacy-plain-refresh", freshManager.getRefreshToken())

        val prefsAfter = ctx.sessionDataStore.data.first()
        assertNull("Plain access token must be deleted after migration", prefsAfter[SessionManager.KEY_AUTH_TOKEN])
        assertNull("Plain refresh token must be deleted after migration", prefsAfter[SessionManager.KEY_REFRESH_TOKEN])
        assertNotNull("Encrypted access token must be present", prefsAfter[SessionManager.KEY_AUTH_TOKEN_ENC])
        assertNotNull("Encrypted refresh token must be present", prefsAfter[SessionManager.KEY_REFRESH_TOKEN_ENC])
    }
}
