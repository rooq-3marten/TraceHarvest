package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.gateway.SmsEngine
import com.example.data.gateway.UssdEngine
import com.example.data.gateway.UssdRequest
import com.example.data.gateway.UssdResponseType
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.repository.TraceHarvestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * TraceHarvest Comprehensive Stress & Resilience Test Suite
 *
 * Strains, fuzzes, and stress-tests:
 * 1. USSD Gateway (Concurrent sessions, malformed inputs, nested navigation stress)
 * 2. SMS Gateway (High volume inbound messages, dialect variants, unknown commands)
 * 3. Room Database & Repository (Concurrent writes, boundary values, zero/extreme metrics)
 * 4. MRL & SPS Quarantine Engine (Case-insensitive banned chemical evasion detection)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppStressTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TraceHarvestRepository
    private lateinit var ussdEngine: UssdEngine
    private lateinit var smsEngine: SmsEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TraceHarvestRepository(database.traceHarvestDao())
        ussdEngine = UssdEngine()
        smsEngine = SmsEngine()

        com.example.data.remote.NetworkClient.mockResponseForTesting = com.example.data.remote.model.AgentBatchSyncResponse(
            status = "success",
            syncedFarmersCount = 20,
            syncedPracticesCount = 0,
            assignedFarmerIds = emptyMap()
        )
    }

    @After
    fun tearDown() {
        com.example.data.remote.NetworkClient.mockResponseForTesting = null
        database.close()
    }

    // =========================================================================
    // 1. USSD GATEWAY PROTOCOL & NAVIGATION STRESS TESTING
    // =========================================================================

    @Test
    fun `stress test ussd engine with 500 concurrent sessions and random navigation hops`() {
        runBlocking {
        val sampleFarmer = FarmerEntity(
            id = 1,
            farmerCode = "TH-KAN-2026-1048",
            fullName = "Audu Abubakar",
            phoneNumber = "+2348031234567",
            state = "Kano",
            lga = "Dambatta",
            community = "Gwarabjawa",
            crop = "Sesame",
            farmSizeHectares = 2.5,
            latitude = 12.4382,
            longitude = 8.5147,
            cooperative = "Dambatta Sesame Growers",
            registrationTimestamp = System.currentTimeMillis()
        )

        val sampleBatch = HarvestBatchEntity(
            id = 1,
            batchCode = "NG-SES-2026-0042-EXP",
            crop = "Sesame",
            farmerCode = "TH-KAN-2026-1048",
            farmerName = "Audu Abubakar",
            region = "Kano (Dambatta)",
            harvestDate = System.currentTimeMillis(),
            bagCount = 30,
            netWeightKg = 1500.0,
            moisturePercent = 7.4,
            foreignMatterPercent = 1.2,
            grade = "Grade A Export Ready",
            mrlStatus = "PASSED_SPS",
            aflatoxinStatus = "SAFE (<4 ppb)",
            phiDaysObserved = 24,
            destinationMarket = "EU & United Kingdom",
            blockchainHash = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90",
            blockchainTxId = "0xa1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4",
            isBlockchainAnchored = true,
            isFlaggedForRejection = false,
            rejectionReason = null,
            qrPayload = "TRACEHARVEST://BATCH/NG-SES-2026-0042-EXP"
        )

        val farmers = listOf(sampleFarmer)
        val batches = listOf(sampleBatch)

        val testInputs = listOf(
            "",           // Root menu
            "1",          // Menu 1: Log practice input
            "1*1",        // Menu 1 -> Sub-option 1 (Lambda-Super)
            "1*2",        // Menu 1 -> Sub-option 2 (BioNeem)
            "1*3",        // Menu 1 -> Sub-option 3 (PICS Bags)
            "2",          // Menu 2: Check batch clearance
            "3",          // Menu 3: Farmer ID & Geotag
            "4",          // Menu 4: Export Price Premium
            "5",          // Menu 5: Report Banned Sniper Spray
            "99",         // Invalid root option
            "1*99",       // Invalid sub-option
            "abc",        // Non-numeric garbage
            "*#*#*#",     // Special characters
            "1*1*1*1*1"   // Over-nested navigation
        )

        // Launch 500 concurrent USSD requests using coroutines
        val jobs = (1..500).map { i ->
            async(Dispatchers.Default) {
                val input = testInputs[i % testInputs.size]
                val request = UssdRequest(
                    sessionId = "SESSION_BURST_$i",
                    serviceCode = "*384*748#",
                    phoneNumber = "+234803" + (1000000 + i),
                    text = input
                )
                val response = ussdEngine.processRequest(request, farmers, batches)
                assertNotNull("Response message cannot be null", response.message)
                assertTrue("Response must start with CON or END", response.message.startsWith("CON ") || response.message.startsWith("END "))
                if (response.responseType == UssdResponseType.CON) {
                    assertTrue(response.message.startsWith("CON "))
                } else {
                    assertTrue(response.message.startsWith("END "))
                }
            }
        }

        jobs.awaitAll()
        }
    }

    // =========================================================================
    // 3. TWO-WAY SMS PARSER STRESS TESTING
    // =========================================================================

    @Test
    fun `stress test sms engine with 1000 inbound messages and multilingual dialect inputs`() {
        val testMessages = listOf(
            "1",
            "CONFIRM",
            "confirm",
            "EEY",
            "eey",
            "YES",
            "yes",
            "2",
            "DISPUTE",
            "dispute",
            "AA",
            "aa",
            "NO",
            "no",
            "STATUS",
            "status",
            "REPORT SNIPER",
            "report sniper on my neighbor farm",
            "HELP",
            "help",
            "Barka da yamma",
            "Hello who is this",
            "1234567890",
            "????",
            "🔥🚨🌾",
            "   CONFIRM   ", // Extra padding
            "1 (CONFIRM)"   // Formatted option
        )

        for (i in 1..1000) {
            val msg = testMessages[i % testMessages.size]
            val result = smsEngine.processInboundMessage(
                fromPhone = "+234803" + (2000000 + i),
                messageText = msg,
                farmers = emptyList(),
                batches = emptyList()
            )

            assertNotNull(result.automatedResponse)
            assertTrue("Automated response must have content", result.automatedResponse.isNotBlank())
            assertNotNull(result.actionTaken)
        }
    }

    // =========================================================================
    // 4. ROOM DATABASE CONCURRENCY & BOUNDARY VALUE STRAIN TESTING
    // =========================================================================

    @Test
    fun `stress test room repository with concurrent bulk insertions`() {
        runBlocking {
            val initialFarmerCount = repository.farmerCount.first()
            assertEquals(0, initialFarmerCount)

            // Concurrently insert 50 farmers across multiple threads
            val farmerJobs = (1..50).map { i ->
                async(Dispatchers.IO) {
                    repository.registerFarmer(
                        fullName = "Stress Farmer #$i",
                        phoneNumber = "+234803" + (5000000 + i),
                        state = if (i % 2 == 0) "Kano" else "Jigawa",
                        lga = "LGA_$i",
                        community = "Community_$i",
                        crop = if (i % 3 == 0) "Sesame" else if (i % 3 == 1) "Ginger" else "Soybeans",
                        farmSizeHectares = 1.0 + (i % 10) * 0.5,
                        latitude = 12.0 + (i * 0.01),
                        longitude = 8.0 + (i * 0.01),
                        cooperative = "Coop_$i"
                    )
                }
            }
            val farmers = farmerJobs.awaitAll()
            assertEquals(50, farmers.size)

            val totalRegistered = repository.farmerCount.first()
            assertEquals(50, totalRegistered)

            // Concurrently insert 100 harvest batches linked to these farmers
            val batchJobs = (1..100).map { i ->
                async(Dispatchers.IO) {
                    val assignedFarmer = farmers[i % farmers.size]
                    repository.createHarvestBatch(
                        farmerCode = assignedFarmer.farmerCode,
                        farmerName = assignedFarmer.fullName,
                        crop = assignedFarmer.crop,
                        region = "${assignedFarmer.state} (${assignedFarmer.lga})",
                        bagCount = 10 + (i % 50),
                        netWeightKg = 500.0 + (i * 25.0),
                        moisturePercent = 6.0 + (i % 8) * 0.8,
                        foreignMatterPercent = 1.0 + (i % 3) * 0.5,
                        destinationMarket = "EU & United Kingdom"
                    )
                }
            }
            val batches = batchJobs.awaitAll()
            assertEquals(100, batches.size)

            val totalBatches = repository.totalBatchesCount.first()
            assertEquals(100, totalBatches)
        }
    }

    @Test
    fun `test extreme boundary values in harvest batch aggregation`() {
        runBlocking {
            // Zero bags and micro-weight (edge case)
            val minBatch = repository.createHarvestBatch(
                farmerCode = "TH-KAN-2026-9999",
                farmerName = "Micro Farmer",
                crop = "Sesame",
                region = "Kano",
                bagCount = 0,
                netWeightKg = 0.5,
                moisturePercent = 0.0,
                foreignMatterPercent = 0.0
            )
            assertNotNull(minBatch)
            assertEquals(0, minBatch.bagCount)
            assertEquals(0.5, minBatch.netWeightKg, 0.001)
            assertEquals("Grade A Export Ready", minBatch.grade) // 0% moisture is clean

            // High moisture aflatoxin violation (18.5% moisture)
            val wetBatch = repository.createHarvestBatch(
                farmerCode = "TH-JIG-2026-8888",
                farmerName = "High Moisture Farmer",
                crop = "Soybeans",
                region = "Jigawa",
                bagCount = 500,
                netWeightKg = 25000.0,
                moisturePercent = 18.5,
                foreignMatterPercent = 4.2
            )
            assertTrue("Batch with 18.5% moisture must be flagged", wetBatch.isFlaggedForRejection)
            assertEquals("UNSAFE (>10 ppb)", wetBatch.aflatoxinStatus)
            assertEquals("Non-Compliant High Risk", wetBatch.grade)
            assertTrue(wetBatch.rejectionReason?.contains("Aflatoxin Hazard") == true)
        }
    }

    // =========================================================================
    // 5. MRL DETECTION & QUARANTINE EVASION RESILIENCE
    // =========================================================================

    @Test
    fun `strain mrl detection engine to ensure banned sniper chemicals cannot evade filter`() {
        runBlocking {
            val bannedEvasionVariants = listOf(
                "Sniper 1000EC",
                "SNIPER",
                "sniper",
                "Dichlorvos DDVP",
                "ddvp",
                "DICHLORVOS 50%",
                "Chlorpyrifos-ethyl",
                "CHLORPYRIFOS",
                "Monocrotophos Azodrin"
            )

            for (variant in bannedEvasionVariants) {
                val log = repository.logPractice(
                    farmerCode = "TH-EVASION-TEST",
                    farmerName = "Evasion Tester",
                    crop = "Sesame",
                    category = "Pesticide",
                    productName = variant,
                    activeIngredient = variant,
                    dosage = "20ml / 15L water",
                    phiDays = 30
                )

                assertEquals(
                    "Chemical variant '$variant' must be flagged as BANNED_MRL_VIOLATION",
                    "BANNED_MRL_VIOLATION",
                    log.riskLevel
                )
                assertTrue(
                    "Risk notes must explain regulatory ban",
                    log.riskNotes.contains("BANNED") || log.riskNotes.contains("CRITICAL VIOLATION")
                )
            }

            // Verify that subsequent batch creation for this farmer is automatically blocked
            val batch = repository.createHarvestBatch(
                farmerCode = "TH-EVASION-TEST",
                farmerName = "Evasion Tester",
                crop = "Sesame",
                region = "Kano",
                bagCount = 50,
                netWeightKg = 2500.0,
                moisturePercent = 7.0,
                foreignMatterPercent = 1.0
            )

            assertTrue("Batch must be flagged due to prior banned chemical log", batch.isFlaggedForRejection)
            assertEquals("VIOLATION_BLOCKED", batch.mrlStatus)
            assertFalse("Flagged batch must NOT be automatically anchored", batch.isBlockchainAnchored)
        }
    }

    // =========================================================================
    // 6. IDEMPOTENT FASTAPI BATCH SYNC & CONCURRENCY STRESS
    // =========================================================================

    @Test
    fun `stress test idempotent batch sync with concurrent sync calls to verify zero duplicates`() {
        runBlocking {
            // Register 20 farmers in offline mode
            for (i in 1..20) {
                repository.registerFarmer(
                    fullName = "Stress Farmer $i",
                    phoneNumber = "+234803000${String.format(java.util.Locale.ROOT, "%04d", i)}",
                    state = if (i % 2 == 0) "Kano" else "Jigawa",
                    lga = "LGA $i",
                    community = "Village $i",
                    crop = "Sesame",
                    farmSize = 2.0 + i,
                    farmSizeUnit = "hectares",
                    farmSizeHectares = 2.0 + i,
                    latitude = 12.0 + (i * 0.01),
                    longitude = 8.5 + (i * 0.01),
                    cooperative = "Coop $i",
                    agentId = "AGENT-NG-042",
                    isOfflineMode = true
                )
            }

            val pendingCountBefore = repository.pendingFarmerSyncCount.first()
            assertEquals(20, pendingCountBefore)

            // Simulate 10 rapid concurrent taps on "Sync Now" across parallel coroutines
            val syncDeferreds = (1..10).map {
                async(Dispatchers.IO) {
                    repository.syncAllPending()
                }
            }
            val syncResults = syncDeferreds.awaitAll()

            // After concurrent sync, pending count must be 0
            val pendingCountAfter = repository.pendingFarmerSyncCount.first()
            assertEquals(0, pendingCountAfter)

            // Total farmers in database must remain exactly 20 (zero duplicate insertions)
            val totalFarmers = repository.farmerCount.first()
            assertEquals(20, totalFarmers)

            // Every farmer must have syncStatus = "synced"
            val allFarmers = repository.allFarmers.first()
            assertTrue(allFarmers.all { it.syncStatus == "synced" })
            assertTrue(allFarmers.all { it.isSynced })
            assertTrue(allFarmers.all { it.clientUuid.isNotBlank() })
        }
    }

    // =========================================================================
    // 7. OVERWORKED HIGH-VOLUME STRESS TESTING
    // =========================================================================

    @Test
    fun `stress test overworked application under heavy multi-threaded load`() {
        runBlocking {
            val totalOperations = 100
            val successCount = AtomicInteger(0)

            // Overwork repository with concurrent mixed operations: farmer registration + practice logging + batch queries
            val jobs = (1..totalOperations).map { index ->
                async(Dispatchers.IO) {
                    val code = "TH-LOAD-$index"
                    repository.registerFarmer(
                        fullName = "Overworked Farmer $index",
                        phoneNumber = "+2348000000$index",
                        state = "Kaduna",
                        lga = "Zaria",
                        community = "Samaru",
                        crop = "Soybeans",
                        farmSize = 3.5,
                        farmSizeUnit = "hectares",
                        farmSizeHectares = 3.5,
                        latitude = 11.0855,
                        longitude = 7.7199,
                        cooperative = "Zaria Soy",
                        agentId = "AGENT-LOAD",
                        isOfflineMode = index % 2 == 0
                    )

                    repository.logPractice(
                        farmerCode = code,
                        farmerName = "Overworked Farmer $index",
                        crop = "Soybeans",
                        category = "Fertilizer",
                        productName = "Indorama NPK 15:15:15",
                        activeIngredient = "NPK Compound",
                        dosage = "2 Bags",
                        phiDays = 0,
                        syncStatus = "synced"
                    )

                    successCount.incrementAndGet()
                }
            }

            jobs.awaitAll()
            assertEquals(totalOperations, successCount.get())

            // Verify database consistency under high load
            val finalFarmers = repository.allFarmers.first()
            assertTrue(finalFarmers.size >= totalOperations)
        }
    }

    // =========================================================================
    // 8. PHYSICAL LOCATION REVERSE GEOCODING & SPATIAL LANDMARK RESOLUTION
    // =========================================================================

    @Test
    fun `verify geographical coordinates resolve to physically named locations`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // Test Kano Dambatta Sesame hub coordinates (12.4382, 8.5147)
            val dambattaName = com.example.core.location.GeoLocationNamingService.resolveLocationName(
                context = context,
                latitude = 12.4382,
                longitude = 8.5147,
                fallbackState = "Kano",
                fallbackLga = "Dambatta",
                fallbackCommunity = "Gwarabjawa"
            )
            assertTrue("Should contain Dambatta or Kano", dambattaName.contains("Dambatta") || dambattaName.contains("Kano"))

            // Test Jigawa Maigatari export border hub coordinates (12.8622, 9.9078)
            val maigatariName = com.example.core.location.GeoLocationNamingService.resolveLocationName(
                context = context,
                latitude = 12.8622,
                longitude = 9.9078,
                fallbackState = "Jigawa",
                fallbackLga = "Maigatari"
            )
            assertTrue("Should contain Maigatari or Jigawa", maigatariName.contains("Maigatari") || maigatariName.contains("Jigawa"))

            // Test nearest hub calculation
            val nearest = com.example.core.location.GeoLocationNamingService.findNearestHub(11.0855, 7.7199)
            assertNotNull(nearest)
            assertEquals("Zaria Agricultural Research Basin", nearest!!.first.name)
            assertEquals("Kaduna", nearest.first.state)
        }
    }
}
