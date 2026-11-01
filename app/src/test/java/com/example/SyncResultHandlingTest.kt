package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.catalog.DashboardRisk
import com.example.data.remote.NetworkClient
import com.example.data.remote.model.AgentBatchSyncResponse
import com.example.data.repository.TraceHarvestRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A record is "synced" only when the server says it saved it. */
@RunWith(RobolectricTestRunner::class)
class SyncResultHandlingTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TraceHarvestRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = TraceHarvestRepository(db.traceHarvestDao(), signedInAgentId = { "TEST-AGENT-A" })
    }

    @After
    fun tearDown() {
        NetworkClient.mockResponderForTesting = null
        NetworkClient.mockResponseForTesting = null
        db.close()
    }

    private suspend fun farmerWithPractice() = repo.registerFarmer(
        fullName = "Farmer One", phoneNumber = "+2348030000021", state = "Kano", lga = "L", community = "C",
        crop = "Sesame", latitude = 12.0, longitude = 8.0, agentId = "TEST-AGENT-A", isOfflineMode = true
    ).also {
        repo.logPractice(farmerCode = it.farmerCode, farmerName = it.fullName, crop = "Sesame", agentId = "TEST-AGENT-A")
    }

    @Test
    fun `a record the server rejects stays pending and the reason is shown`() = runBlocking {
        val dao = db.traceHarvestDao()
        val farmer = farmerWithPractice()
        val practice = dao.getPendingSyncPracticeLogs().single()

        FakeSyncServer.install(rejected = mapOf(practice.clientUuid to "The farmer is not on the server yet."))
        val result = repo.syncAllPending()

        assertFalse(result.isSuccess)
        assertEquals(1, result.syncedFarmersCount)
        assertEquals(0, result.syncedPracticesCount)
        assertEquals(1, result.totalPendingRemaining)
        assertTrue(result.message.contains("The farmer is not on the server yet."))
        assertEquals(1, dao.getPendingSyncPracticeLogs().size)
        assertTrue(dao.getFarmerByLocalId(farmer.localId)!!.isSynced)
    }

    @Test
    fun `a record the server does not mention stays pending`() = runBlocking {
        val dao = db.traceHarvestDao()
        farmerWithPractice()

        NetworkClient.mockResponseForTesting = AgentBatchSyncResponse(status = "success")
        val result = repo.syncAllPending()

        assertFalse(result.isSuccess)
        assertEquals(0, result.syncedFarmersCount)
        assertEquals(1, dao.getPendingSyncFarmers().size)
        assertEquals(1, dao.getPendingSyncPracticeLogs().size)
    }

    @Test
    fun `a rejected record is sent again next time and saved once the server accepts it`() = runBlocking {
        val dao = db.traceHarvestDao()
        farmerWithPractice()
        val practice = dao.getPendingSyncPracticeLogs().single()

        FakeSyncServer.install(rejected = mapOf(practice.clientUuid to "Not saved."))
        repo.syncAllPending()
        assertEquals(1, dao.getPendingSyncPracticeLogs().size)

        FakeSyncServer.install()
        val second = repo.syncAllPending()

        assertTrue(second.isSuccess)
        assertEquals(0, dao.getPendingSyncPracticeLogs().size)
    }
}

/** The server and dashboard only know three risk words. */
class DashboardRiskTest {
    @Test
    fun `NAFDAC states map to the dashboard risk words`() {
        assertEquals("COMPLIANT", DashboardRisk.from("APPROVED_EXPORT_COMPLIANT"))
        assertEquals("COMPLIANT", DashboardRisk.from("APPROVED_ORGANIC_PREMIUM"))
        assertEquals("FLAGGED_HIGH_RISK", DashboardRisk.from("BANNED_MRL_VIOLATION"))
        assertEquals("UNDER_REVIEW", DashboardRisk.from("RESTRICTED_PHI_MONITORED"))
        assertEquals("UNDER_REVIEW", DashboardRisk.from("UNREGISTERED_UNKNOWN"))
        assertEquals("UNDER_REVIEW", DashboardRisk.from(""))
    }
}
