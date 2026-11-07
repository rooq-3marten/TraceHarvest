package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.TraceHarvestRepository
import com.example.ui.components.FarmVertex
import com.example.ui.screens.toPolygonText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the agent captured is what gets stored. Nothing is invented, and one form makes one farmer. */
@RunWith(RobolectricTestRunner::class)
class FarmerEnrollmentTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TraceHarvestRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = TraceHarvestRepository(db.traceHarvestDao(), db.pendingSyncDao(), signedInAgentId = { "TEST-AGENT-A" })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun enrol(
        polygon: String = "",
        photoPath: String? = null,
        clientUuid: String? = null,
        offline: Boolean = true
    ) = repo.registerFarmer(
        fullName = "Farmer One", phoneNumber = "+2348030000041", state = "Kano", lga = "Dala", community = "",
        crop = "Sesame", latitude = 12.0, longitude = 8.0, agentId = "TEST-AGENT-A",
        isOfflineMode = offline, gpsPolygon = polygon, photoPath = photoPath, photoConsent = photoPath != null,
        clientUuid = clientUuid
    )

    @Test
    fun `a walked boundary is stored exactly as captured`() = runBlocking {
        val walked = "12.1,8.1;12.1,8.2;12.0,8.2"
        assertEquals(walked, enrol(polygon = walked).gpsPolygon)
    }

    @Test
    fun `no boundary means nothing is stored, not an invented square`() = runBlocking {
        assertEquals("", enrol().gpsPolygon)
    }

    @Test
    fun `a farmer is never marked synced before the server confirms it`() = runBlocking {
        val farmer = enrol(offline = false)
        assertFalse(farmer.isSynced)
        assertEquals(1, db.traceHarvestDao().getPendingSyncFarmers().size)
        assertTrue(farmer.farmerCode.contains("PENDING"))
    }

    @Test
    fun `saving the same form twice stores one farmer`() = runBlocking {
        val first = enrol(clientUuid = "form-1")
        val second = enrol(clientUuid = "form-1")
        assertEquals(first.localId, second.localId)
        assertEquals(1, db.traceHarvestDao().getPendingSyncFarmers().size)
    }

    @Test
    fun `the photo path and consent are kept together`() = runBlocking {
        val withPhoto = enrol(photoPath = "/data/photo.jpg")
        assertEquals("/data/photo.jpg", withPhoto.photoPath)
        assertTrue(withPhoto.photoConsent)
        val without = repo.registerFarmer(
            fullName = "Farmer Two", phoneNumber = "+2348030000042", state = "Kano", lga = "Dala", community = "",
            crop = "Sesame", latitude = 12.0, longitude = 8.0, agentId = "TEST-AGENT-A", photoConsent = true
        )
        assertNull(without.photoPath)
        assertFalse(without.photoConsent)
    }

    @Test
    fun `a boundary needs at least three points`() {
        assertEquals("", listOf(FarmVertex(1.0, 2.0), FarmVertex(3.0, 4.0)).toPolygonText())
        assertEquals("1.0,2.0;3.0,4.0;5.0,6.0",
            listOf(FarmVertex(1.0, 2.0), FarmVertex(3.0, 4.0), FarmVertex(5.0, 6.0)).toPolygonText())
    }
}
