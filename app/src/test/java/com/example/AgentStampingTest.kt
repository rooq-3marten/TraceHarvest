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
    private var signedIn: String? = "AGENT-A"
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
            crop = "Sesame", latitude = 12.0, longitude = 8.0, agentId = "AGENT-A"
        )
        val log = repo.logPractice(
            farmerCode = farmer.farmerCode, farmerName = farmer.fullName, crop = "Sesame", agentId = "AGENT-A"
        )
        val batch = repo.createHarvestBatch(
            farmerCode = farmer.farmerCode, farmerName = farmer.fullName, crop = "Sesame", region = "Kano",
            bagCount = 1, netWeightKg = 50.0, moisturePercent = 7.0, foreignMatterPercent = 1.0, agentId = "AGENT-A"
        )
        assertEquals("AGENT-A", farmer.agentId)
        assertEquals("AGENT-A", log.agentId)
        assertEquals("AGENT-A", batch.agentId)
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
        fun log(agent: String, code: String) = PracticeLogEntity(
            agentId = agent, farmerCode = code, farmerName = "N", crop = "Sesame", category = "Pesticide",
            practiceDetails = "x", productName = "p", activeIngredient = "a", dosage = "1 L",
            syncStatus = "pending_sync", isSynced = false
        )
        dao.insertPracticeLog(log("AGENT-A", "TH-A"))
        dao.insertPracticeLog(log("AGENT-B", "TH-B"))

        val result = repo.syncAllPending()

        assertEquals(1, result.otherAccountRecords)
        assertTrue(result.message.contains("belong"))
        val stillPending = dao.getPendingSyncPracticeLogs()
        assertEquals(1, stillPending.size)
        assertEquals("AGENT-B", stillPending.first().agentId)
        assertFalse(stillPending.first().isSynced)
    }

    @Test
    fun `sync with no signed in agent fails and changes nothing`() = runBlocking {
        signedIn = null
        val dao = db.traceHarvestDao()
        dao.insertPracticeLog(
            PracticeLogEntity(
                agentId = "AGENT-A", farmerCode = "TH-A", farmerName = "N", crop = "Sesame", category = "Pesticide",
                practiceDetails = "x", productName = "p", activeIngredient = "a", dosage = "1 L",
                syncStatus = "pending_sync", isSynced = false
            )
        )
        val result = repo.syncAllPending()
        assertFalse(result.isSuccess)
        assertEquals(1, dao.getPendingSyncPracticeLogs().size)
    }
}
