package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.PracticeLogEntity
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

@RunWith(RobolectricTestRunner::class)
class AgentStampingTest {

    private lateinit var db: AppDatabase
    private var signedIn: String? = "TEST-AGENT-A"
    private lateinit var repo: TraceHarvestRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = TraceHarvestRepository(db.traceHarvestDao(), signedInAgentId = { signedIn })
        NetworkClient.mockResponseForTesting = AgentBatchSyncResponse(status = "success")
    }

    @After
    fun tearDown() {
        NetworkClient.mockResponseForTesting = null
        db.close()
    }

    @Test
    fun `records are stamped with the signed in agent`() = runBlocking {
        val farmer = repo.registerFarmer(
            fullName = "F One", phoneNumber = "+2348030000001", state = "Kano", lga = "L", community = "C",
            crop = "Sesame", latitude = 12.0, longitude = 8.0, agentId = "TEST-AGENT-A"
        )
        val log = repo.logPractice(
            farmerCode = farmer.farmerCode, farmerName = farmer.fullName, crop = "Sesame", agentId = "TEST-AGENT-A"
        )
        val batch = repo.createBatch(
            farmerCode = farmer.farmerCode, farmerName = farmer.fullName, crop = "Sesame", region = "Kano",
            bagCount = 1, netWeightKg = 50.0, moisturePercent = 7.0, foreignMatterPercent = 1.0, agentId = "TEST-AGENT-A"
        )
        assertEquals("TEST-AGENT-A", farmer.agentId)
        assertEquals("TEST-AGENT-A", log.agentId)
        assertEquals("TEST-AGENT-A", batch.agentId)
        assertFalse("Locally created batch must not claim blockchain anchoring", batch.isBlockchainAnchored)
    }

    @Test
    fun `a blank agent id is refused`() {
        val failed = try {
            runBlocking {
                repo.registerFarmer(
                    fullName = "F", phoneNumber = "+2348030000002", state = "Kano", lga = "L", community = "C",
                    crop = "Sesame", latitude = 1.0, longitude = 1.0, agentId = ""
                )
            }
            false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue(failed)
    }

    @Test
    fun `sync sends only the signed in agents records and leaves the others untouched`() = runBlocking {
        val dao = db.traceHarvestDao()
        val fA1 = repo.registerFarmer(
            fullName = "Farmer A1", phoneNumber = "+2348030000011", state = "Kano", lga = "L1", community = "C1",
            crop = "Sesame", latitude = 12.1, longitude = 8.1, agentId = "TEST-AGENT-A", isOfflineMode = true
        )
        val fA2 = repo.registerFarmer(
            fullName = "Farmer A2", phoneNumber = "+2348030000012", state = "Kano", lga = "L2", community = "C2",
            crop = "Sesame", latitude = 12.2, longitude = 8.2, agentId = "TEST-AGENT-A", isOfflineMode = true
        )
        val fB1 = repo.registerFarmer(
            fullName = "Farmer B1", phoneNumber = "+2348030000013", state = "Kano", lga = "L3", community = "C3",
            crop = "Sesame", latitude = 12.3, longitude = 8.3, agentId = "TEST-AGENT-B", isOfflineMode = true
        )
        repo.logPractice(
            farmerCode = fA1.farmerCode, farmerName = fA1.fullName, crop = "Sesame", agentId = "TEST-AGENT-A"
        )
        repo.logPractice(
            farmerCode = fB1.farmerCode, farmerName = fB1.fullName, crop = "Sesame", agentId = "TEST-AGENT-B"
        )
        repo.createBatch(
            farmerCode = fB1.farmerCode, farmerName = fB1.fullName, crop = "Sesame", region = "Kano",
            bagCount = 2, netWeightKg = 100.0, moisturePercent = 7.2, foreignMatterPercent = 0.5, agentId = "TEST-AGENT-B"
        )

        NetworkClient.mockResponseForTesting = AgentBatchSyncResponse(
            status = "success",
            syncedFarmersCount = 2,
            syncedPracticesCount = 1,
            assignedFarmerIds = mapOf(
                fA1.clientUuid to "TH-SRV-001",
                fA2.clientUuid to "TH-SRV-002"
            )
        )

        val result = repo.syncAllPending()

        assertTrue(result.isSuccess)
        assertEquals(3, result.otherAccountRecords)
        assertTrue(result.message.contains("3 records belong to another account"))

        // Verify each farmer of TEST-AGENT-A received its distinct server code without clobbering the other
        val updatedA1 = dao.getFarmerByLocalId(fA1.localId)!!
        val updatedA2 = dao.getFarmerByLocalId(fA2.localId)!!
        val untouchedB1 = dao.getFarmerByLocalId(fB1.localId)!!
        assertEquals("TH-SRV-001", updatedA1.farmerCode)
        assertEquals("TH-SRV-002", updatedA2.farmerCode)
        assertTrue(updatedA1.isSynced)
        assertTrue(updatedA2.isSynced)

        // TEST-AGENT-B records stay pending and stamped with TEST-AGENT-B
        assertFalse(untouchedB1.isSynced)
        assertEquals("TEST-AGENT-B", untouchedB1.agentId)
        val stillPendingLogs = dao.getPendingSyncPracticeLogs()
        assertEquals(1, stillPendingLogs.size)
        assertEquals("TEST-AGENT-B", stillPendingLogs.first().agentId)
        val stillPendingBatches = dao.getPendingSyncBatches()
        assertEquals(1, stillPendingBatches.size)
        assertEquals("TEST-AGENT-B", stillPendingBatches.first().agentId)
    }

    @Test
    fun `createBatch enforces real pre harvest interval instead of bypassing it`() = runBlocking {
        val now = System.currentTimeMillis()
        repo.logPractice(
            farmerCode = "TH-PHI-1",
            farmerName = "PHI Farmer",
            crop = "Sesame",
            productName = "Karate 5 EC",
            activeIngredient = "Lambda-cyhalothrin",
            calendarDateApplied = now,
            phiDays = 14,
            agentId = "TEST-AGENT-A"
        )
        val tooEarlyBatch = repo.createBatch(
            farmerCode = "TH-PHI-1",
            farmerName = "PHI Farmer",
            crop = "Sesame",
            region = "Kano",
            bagCount = 10,
            netWeightKg = 500.0,
            moisturePercent = 7.4,
            foreignMatterPercent = 0.5,
            agentId = "TEST-AGENT-A"
        )
        assertTrue("Batch harvested same day as 14-day PHI spray must be flagged", tooEarlyBatch.isFlaggedForRejection)
        assertEquals("PENDING_PHI", tooEarlyBatch.mrlStatus)

        val fifteenDaysAgo = now - (15L * 24L * 60L * 60L * 1000L)
        repo.logPractice(
            farmerCode = "TH-PHI-2",
            farmerName = "Safe PHI Farmer",
            crop = "Sesame",
            productName = "Karate 5 EC",
            activeIngredient = "Lambda-cyhalothrin",
            calendarDateApplied = fifteenDaysAgo,
            phiDays = 14,
            agentId = "TEST-AGENT-A"
        )
        val safeBatch = repo.createBatch(
            farmerCode = "TH-PHI-2",
            farmerName = "Safe PHI Farmer",
            crop = "Sesame",
            region = "Kano",
            bagCount = 10,
            netWeightKg = 500.0,
            moisturePercent = 7.4,
            foreignMatterPercent = 0.5,
            agentId = "TEST-AGENT-A"
        )
        assertFalse("Batch harvested after 15 days should pass 14-day PHI", safeBatch.isFlaggedForRejection)
        assertEquals("PASSED_SPS", safeBatch.mrlStatus)
    }

    @Test
    fun `sync with no signed in agent fails and changes nothing`() = runBlocking {
        signedIn = null
        val dao = db.traceHarvestDao()
        dao.insertPracticeLog(
            PracticeLogEntity(
                agentId = "TEST-AGENT-A", farmerCode = "TH-A", farmerName = "N", crop = "Sesame", category = "Pesticide",
                practiceDetails = "x", productName = "p", activeIngredient = "a", dosage = "1 L",
                syncStatus = "pending_sync", isSynced = false
            )
        )
        val result = repo.syncAllPending()
        assertFalse(result.isSuccess)
        assertEquals(1, dao.getPendingSyncPracticeLogs().size)
    }
}
