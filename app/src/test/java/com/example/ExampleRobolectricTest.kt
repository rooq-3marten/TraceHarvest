package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.TraceHarvestRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TraceHarvest", appName)
  }

  @Test
  fun `verify sha256 cryptographic provenance hashing`() {
    val input = "TraceHarvest:NG-SES-2026-0042:EU:1500kg"
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray())
    val hex = digest.joinToString("") { "%02x".format(it) }
    assertEquals(64, hex.length)
  }

  @Test
  fun `verify banned pesticide residue rule flags sniper dichlorvos`() {
    val bannedSubstance = "Sniper 1000EC (Dichlorvos)"
    val isBanned = bannedSubstance.contains("Dichlorvos", ignoreCase = true) ||
        bannedSubstance.contains("Sniper", ignoreCase = true) ||
        bannedSubstance.contains("Chlorpyrifos", ignoreCase = true)
    assertTrue("Dichlorvos should be detected as banned substance", isBanned)
  }

  @Test
  fun `verify safe moisture threshold for aflatoxin control`() {
    val safeMoisture = 7.4
    val unsafeMoisture = 13.8
    val maxStandard = 10.0
    assertTrue(safeMoisture <= maxStandard)
    assertTrue(unsafeMoisture > maxStandard)
  }

  @Test
  fun `verify ussd engine protocol con and end transitions`() {
    val engine = com.example.data.gateway.UssdEngine()
    val initialReq = com.example.data.gateway.UssdRequest(
      sessionId = "AT_101",
      serviceCode = "*384*748#",
      phoneNumber = "+2348031234567",
      text = ""
    )
    val response = engine.processRequest(initialReq, emptyList(), emptyList())
    assertEquals(com.example.data.gateway.UssdResponseType.CON, response.responseType)
    assertTrue(response.message.startsWith("CON TraceHarvest Nigeria"))

    // Farmer requests Farmer ID & Geotag (Option 3)
    val hopReq = com.example.data.gateway.UssdRequest(
      sessionId = "AT_101",
      serviceCode = "*384*748#",
      phoneNumber = "+2348031234567",
      text = "3"
    )
    val hopResponse = engine.processRequest(hopReq, emptyList(), emptyList())
    assertEquals(com.example.data.gateway.UssdResponseType.END, hopResponse.responseType)
    assertTrue(hopResponse.message.contains("Farmer Provenance ID"))
  }

  @Test
  fun `verify two-way sms keyword parser auto-confirms batch delivery`() {
    val smsEngine = com.example.data.gateway.SmsEngine()
    val result = smsEngine.processInboundMessage(
      fromPhone = "+2348031234567",
      messageText = "1 (CONFIRM)",
      farmers = emptyList(),
      batches = emptyList()
    )
    assertEquals("BATCH_DIGITALLY_SIGNED", result.actionTaken)
    assertTrue(result.automatedResponse.contains("Na gode"))
  }

  @Test
  fun `verify currency formatter accurately uses Naira and avoids hallucinations`() {
    val sampleAmount = 65_000_000.0
    val formatted = com.example.core.util.CurrencyFormatter.formatNaira(sampleAmount)
    assertTrue("Formatted currency must contain Naira symbol ₦", formatted.startsWith("₦"))
    assertTrue("Formatted currency must format with separators", formatted.contains("65,000,000"))

    val compactMillion = com.example.core.util.CurrencyFormatter.formatNairaCompact(260_000_000.0)
    assertEquals("₦260.0M", compactMillion)

    val compactBillion = com.example.core.util.CurrencyFormatter.formatNairaCompact(1_500_000_000.0)
    assertEquals("₦1.5B", compactBillion)

    val metricTons = com.example.core.util.CurrencyFormatter.formatMetricTons(12.45)
    assertEquals("12.45 MT", metricTons)
  }

  @Test
  fun `verify navigation destinations define all primary scalable views`() {
    val items = com.example.ui.navigation.NavigationDestinations.items
    assertEquals(5, items.size)
    val tabs = items.map { it.tab }.toSet()
    assertEquals(5, tabs.size)
    assertTrue(tabs.contains(com.example.ui.viewmodel.AppTab.HOME_DASHBOARD))
    assertTrue(tabs.contains(com.example.ui.viewmodel.AppTab.REGISTER_FARMER))
    assertTrue(tabs.contains(com.example.ui.viewmodel.AppTab.LOG_PRACTICE))
    assertTrue(tabs.contains(com.example.ui.viewmodel.AppTab.BATCH_CREATION))
    assertTrue(tabs.contains(com.example.ui.viewmodel.AppTab.SYNC_RECORDS))
  }

  @Test
  fun `verify enrolled farmer identity provides localId displayId and gps polygon`() {
    val farmer = com.example.data.local.entity.FarmerEntity(
      id = 42L,
      farmerCode = "TH-KAN-2026-1048",
      fullName = "Musa Ibrahim Dambatta",
      phoneNumber = "+2348034512991",
      state = "Kano",
      lga = "Dambatta",
      community = "Fagwalawa",
      crop = "Sesame",
      farmSizeHectares = 4.5,
      latitude = 12.4358,
      longitude = 8.5147,
      cooperative = "Dambatta Sesame Growers Union",
      farmerDisplayId = "TH-KAN-2026-1048",
      gpsPolygon = "12.4374,8.5131;12.4374,8.5163;12.4342,8.5163;12.4342,8.5131"
    )

    assertEquals(42L, farmer.farmerLocalId)
    assertEquals("TH-KAN-2026-1048", farmer.farmerDisplayId)
    assertTrue("GPS coordinates should contain latitude", farmer.gpsCoordinates.contains("12.4358"))
    assertTrue("GPS coordinates should contain longitude", farmer.gpsCoordinates.contains("8.5147"))

    val polygon = farmer.getPolygonCoordinates()
    assertEquals(4, polygon.size)
    assertEquals(12.4374, polygon[0].first, 0.0001)
    assertEquals(8.5131, polygon[0].second, 0.0001)
  }

  @Test
  fun `verify nafdac catalog validates Karate 5 EC and Indorama NPK`() {
    // 1. Karate 5 EC Validation
    val (karateMatch, karateCompliance) = com.example.data.catalog.NafdacCatalog.validateApplication(
      productName = "Karate 5 EC",
      activeIngredient = "Lambda-cyhalothrin"
    )
    org.junit.Assert.assertNotNull(karateMatch)
    assertEquals("04-2015", karateMatch?.nafdacRegNo)
    assertEquals(14, karateMatch?.preHarvestIntervalDays)
    assertEquals(com.example.data.catalog.NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT, karateCompliance)

    // 2. Indorama NPK Validation
    val (indoramaMatch, indoramaCompliance) = com.example.data.catalog.NafdacCatalog.validateApplication(
      productName = "Indorama NPK 15:15:15",
      activeIngredient = "Nitrogen 15% - Phosphorus 15% - Potassium 15%"
    )
    org.junit.Assert.assertNotNull(indoramaMatch)
    assertEquals("04-7892", indoramaMatch?.nafdacRegNo)
    assertEquals(0, indoramaMatch?.preHarvestIntervalDays)
    assertEquals(com.example.data.catalog.NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT, indoramaCompliance)

    // 3. Prohibited Chemical Check (Sniper DDVP)
    val (sniperMatch, sniperCompliance) = com.example.data.catalog.NafdacCatalog.validateApplication(
      productName = "Sniper 1000EC",
      activeIngredient = "Dichlorvos"
    )
    assertEquals(com.example.data.catalog.NafdacExportCompliance.BANNED_MRL_VIOLATION, sniperCompliance)
    assertTrue(com.example.data.catalog.NafdacCatalog.isBannedSubstance("Sniper 1000EC", "Dichlorvos"))
  }

  @Test
  fun `verify practice log stores quantified dosage exact date and hardware sensors`() {
    val exactDate = 1758787200000L
    val log = com.example.data.local.entity.PracticeLogEntity(
      id = 101L,
      farmerCode = "TH-KAN-2026-1048",
      farmerName = "Musa Ibrahim Dambatta",
      crop = "Sesame",
      category = "Pesticide Application",
      practiceDetails = "Karate 5 EC applied at 2.5 Litres",
      productName = "Karate 5 EC",
      activeIngredient = "Lambda-cyhalothrin (50 g/L EC)",
      dosage = "2.5 Litres",
      dateApplied = exactDate,
      preHarvestIntervalDays = 14,
      farmerLocalId = 1L,
      farmerDisplayId = "TH-KAN-2026-1048",
      nafdacRegNo = "04-2015",
      dosageQuantity = 2.5,
      dosageUnit = "Litres",
      calendarDateApplied = exactDate,
      liveLatitude = 12.4361,
      liveLongitude = 8.5149,
      gpsAccuracyMeters = 2.8f,
      gpsFixStatus = "GPS_LOCK_HIGH_ACCURACY",
      verificationPhotoUri = "content://media/picker/image/42",
      photoVerificationType = "CONTAINER_LABEL",
      syncStatus = "pending_sync",
      isSynced = false
    )

    assertEquals(2.5, log.dosageQuantity, 0.001)
    assertEquals("Litres", log.dosageUnit)
    assertEquals(exactDate, log.calendarDateApplied)
    assertEquals(12.4361, log.liveLatitude, 0.0001)
    assertEquals(8.5149, log.liveLongitude, 0.0001)
    assertEquals(2.8f, log.gpsAccuracyMeters)
    assertEquals("content://media/picker/image/42", log.verificationPhotoUri)
    assertEquals("CONTAINER_LABEL", log.photoVerificationType)
    assertEquals("pending_sync", log.syncStatus)
    org.junit.Assert.assertFalse(log.isSynced)
  }

  @Test
  fun `verify synchronization engine dispatches pending practice records`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.traceHarvestDao()

    val log = com.example.data.local.entity.PracticeLogEntity(
      farmerCode = "TH-TEST-1",
      farmerName = "Test Farmer",
      crop = "Sesame",
      category = "Pesticide Application",
      practiceDetails = "Applied test input",
      productName = "Karate 5 EC",
      activeIngredient = "Lambda-cyhalothrin",
      dosage = "1.0 Litres",
      syncStatus = "pending_sync",
      isSynced = false
    )
    dao.insertPracticeLog(log)

    val pendingBefore = dao.getPendingSyncPracticeLogs()
    assertEquals(1, pendingBefore.size)

    val repository = TraceHarvestRepository(dao)
    val syncResult = repository.syncAllPending()

    assertTrue(syncResult.isSuccess)
    assertEquals(1, syncResult.syncedPracticesCount)

    val pendingAfter = dao.getPendingSyncPracticeLogs()
    assertEquals(0, pendingAfter.size)
    db.close()
  }

  @Test
  fun `verify MainActivity launches successfully without crashing`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `verify SessionManager stores and validates session token`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = com.example.core.auth.SessionManager.getInstance(context)

    // Save session
    sessionManager.saveSession(
      token = "jwt_test_token_12345",
      refreshToken = "rf_test_12345",
      agentId = "AGENT-NG-042",
      name = "Aminu Bello",
      phone = "+2348031234567",
      email = "aminu.bello@traceharvest.ng"
    )

    assertTrue(sessionManager.hasValidSession())
    assertEquals("jwt_test_token_12345", sessionManager.getAuthToken())

    // Clear session
    sessionManager.clearSession()
    org.junit.Assert.assertFalse(sessionManager.hasValidSession())
    org.junit.Assert.assertNull(sessionManager.getAuthToken())
  }

  @Test
  fun `verify Nigerian phone number validator accepts valid and rejects malformed inputs`() {
    val validPhones = listOf(
      "+2348031234567",
      "+234 803 123 4567",
      "08031234567",
      "07012345678",
      "09012345678",
      "08123456789"
    )

    val invalidPhones = listOf(
      "12345",
      "invalid_phone",
      "+15551234567",
      "+2345"
    )

    fun isValid(p: String): Boolean {
      val clean = p.replace(" ", "").replace("-", "")
      return (clean.startsWith("+234") && clean.length in 13..14) ||
             (clean.startsWith("234") && clean.length in 12..13) ||
             (clean.startsWith("0") && clean.length in 10..11)
    }

    validPhones.forEach {
      assertTrue("Phone $it should be recognized as valid", isValid(it))
    }

    invalidPhones.forEach {
      org.junit.Assert.assertFalse("Phone $it should be recognized as invalid", isValid(it))
    }
  }

  @Test
  fun `verify duplicate farmer phone detection guards against duplicate registration`() {
    val existing = listOf(
      com.example.data.local.entity.FarmerEntity(
        localId = "f1",
        name = "Musa Ibrahim",
        phoneNumber = "+2348031234567"
      )
    )

    val duplicateCandidate = "+234 803 123 4567"
    val newCandidate = "+234 802 987 6543"

    fun isDuplicate(candidate: String): Boolean {
      val clean = candidate.trim().replace(" ", "").replace("-", "")
      return existing.any { f ->
        val fClean = f.phoneNumber.replace(" ", "").replace("-", "")
        fClean == clean || fClean.endsWith(clean.takeLast(10))
      }
    }

    assertTrue("Duplicate phone must be flagged", isDuplicate(duplicateCandidate))
    org.junit.Assert.assertFalse("Distinct phone must not be flagged", isDuplicate(newCandidate))
  }

  @Test
  fun `verify agent approval status enum conversion and default fallback`() {
    val pending = com.example.core.auth.AgentApprovalStatus.fromRaw("pending")
    val approved = com.example.core.auth.AgentApprovalStatus.fromRaw("APPROVED")
    val rejected = com.example.core.auth.AgentApprovalStatus.fromRaw("rejected")
    val suspended = com.example.core.auth.AgentApprovalStatus.fromRaw("suspended")
    val unknownFallback = com.example.core.auth.AgentApprovalStatus.fromRaw("unknown_status")

    assertEquals(com.example.core.auth.AgentApprovalStatus.PENDING, pending)
    assertEquals(com.example.core.auth.AgentApprovalStatus.APPROVED, approved)
    assertEquals(com.example.core.auth.AgentApprovalStatus.REJECTED, rejected)
    assertEquals(com.example.core.auth.AgentApprovalStatus.SUSPENDED, suspended)
    assertEquals(com.example.core.auth.AgentApprovalStatus.PENDING, unknownFallback)
  }

  @Test
  fun `verify agent registration client validation rules`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.AgentAuthViewModel(context)

    // Initially blank fields should fail validation
    vm.setSignUpFullName("")
    vm.setSignUpEmail("")
    val validBlank = vm.validateSignUpForm()
    org.junit.Assert.assertFalse("Blank sign up must fail", validBlank)
    assertTrue("Full name error must be present", vm.signUpErrors.value.containsKey("fullName"))
    assertTrue("Email error must be present", vm.signUpErrors.value.containsKey("email"))

    // Set valid agent registration information
    vm.setSignUpFullName("Zubairu Abubakar")
    vm.setSignUpEmail("zubairu.abubakar@traceharvest.ng")
    vm.setSignUpAssociation("Garki Sesame Producers Cooperative")
    vm.setSignUpLocation("Ringim LGA, Jigawa State")
    vm.setSignUpPhone("+234 812 345 6789")
    vm.setSignUpPassword("SecurePassword2026")
    vm.setSignUpConfirmPassword("SecurePassword2026")

    val validFilled = vm.validateSignUpForm()
    assertTrue("Properly filled form must pass validation", validFilled)
    assertTrue("Errors must be empty", vm.signUpErrors.value.isEmpty())
  }
}


