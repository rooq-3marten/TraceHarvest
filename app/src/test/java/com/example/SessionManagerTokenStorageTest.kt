package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.auth.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTokenStorageTest {

    private lateinit var manager: SessionManager

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        manager = SessionManager(ctx, FakeTokenCipher())
        runBlocking { manager.clearSession() }
    }

    @Test
    fun `a saved session round-trips through the cipher`() = runBlocking {
        manager.saveSession("access-1", "refresh-1", "AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        assertTrue(manager.hasValidSession())
        assertEquals("access-1", manager.getAuthToken())
        assertEquals("refresh-1", manager.getRefreshToken())
        assertEquals("AGENT-X", manager.currentAgentIdOrNull())
    }

    @Test
    fun `a signed out device has an empty profile and no agent id`() = runBlocking {
        assertEquals("", manager.getCachedProfile().agentId)
        assertNull(manager.currentAgentIdOrNull())
        assertFalse(manager.hasValidSession())
    }

    @Test
    fun `refreshed tokens replace the stored ones`() = runBlocking {
        manager.saveSession("access-1", "refresh-1", "AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        manager.saveRefreshedTokens("access-2", "refresh-2")
        assertEquals("access-2", manager.currentAccessToken())
        assertEquals("refresh-2", manager.currentRefreshToken())
    }

    @Test
    fun `expiring the session clears identity and leaves a one-time notice`() = runBlocking {
        manager.saveSession("access-1", "refresh-1", "AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        manager.expireSession()
        assertFalse(manager.hasValidSession())
        assertNull(manager.currentAgentIdOrNull())
        assertTrue(manager.consumeSessionExpiredNotice())
        assertFalse(manager.consumeSessionExpiredNotice())
    }
}
