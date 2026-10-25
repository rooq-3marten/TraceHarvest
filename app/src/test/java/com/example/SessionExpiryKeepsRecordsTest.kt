package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.auth.SessionManager
import com.example.data.local.AppDatabase
import com.example.data.repository.TraceHarvestRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Signing out because the session expired must never delete what the agent already collected. */
@RunWith(RobolectricTestRunner::class)
class SessionExpiryKeepsRecordsTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `expiring the session keeps every local record`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val sessions = SessionManager(ctx, FakeTokenCipher())
        sessions.saveSession("a", "r", "AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        val repo = TraceHarvestRepository(db.traceHarvestDao(), signedInAgentId = { sessions.currentAgentIdOrNull() })

        repo.registerFarmer(
            fullName = "Kept Farmer", phoneNumber = "+2348030000009", state = "Kano", lga = "L", community = "C",
            crop = "Sesame", latitude = 1.0, longitude = 1.0, agentId = "AGENT-X"
        )
        sessions.expireSession()

        assertFalse(sessions.hasValidSession())
        assertEquals(1, db.traceHarvestDao().getPendingSyncFarmers().size)
    }
}
