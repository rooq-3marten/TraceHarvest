package com.example.data.repository

import com.example.data.catalog.NafdacCatalog
import com.example.data.catalog.NafdacExportCompliance
import com.example.data.local.dao.TraceHarvestDao
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.data.local.entity.SmsLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

class TraceHarvestRepository(private val dao: TraceHarvestDao) : ITraceHarvestRepository {

    override val allFarmers: Flow<List<FarmerEntity>> = dao.getAllFarmers()
    override val allPracticeLogs: Flow<List<PracticeLogEntity>> = dao.getAllPracticeLogs()
    override val allBatches: Flow<List<HarvestBatchEntity>> = dao.getAllBatches()
    override val allSmsLogs: Flow<List<SmsLogEntity>> = dao.getAllSmsLogs()

    override val totalBatchesCount: Flow<Int> = dao.getTotalBatchesCount()
    override val exportReadyBatchesCount: Flow<Int> = dao.getExportReadyBatchesCount()
    override val blockedBatchesCount: Flow<Int> = dao.getBlockedBatchesCount()
    override val farmerCount: Flow<Int> = dao.getFarmerCount()
    override val bannedViolationsCount: Flow<Int> = dao.getBannedPesticideViolationsCount()

    override val pendingFarmerSyncCount: Flow<Int> = dao.getPendingSyncFarmersCount()
    override val pendingPracticeSyncCount: Flow<Int> = dao.getPendingSyncPracticeLogsCount()

    override fun getPracticeLogsForFarmer(farmerCode: String): Flow<List<PracticeLogEntity>> =
        dao.getPracticeLogsForFarmer(farmerCode)

    override suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val count = dao.getFarmerCount().first()
        if (count == 0) {
            seedInitialDataset()
        }
    }

    override suspend fun registerFarmer(
        fullName: String,
        phoneNumber: String,
        state: String,
        lga: String,
        community: String,
        crop: String,
        farmSize: Double,
        farmSizeUnit: String,
        farmSizeHectares: Double,
        latitude: Double,
        longitude: Double,
        cooperative: String,
        agentId: String,
        isOfflineMode: Boolean
    ): FarmerEntity = withContext(Dispatchers.IO) {
        val statePrefix = state.take(3).uppercase(Locale.ROOT)
        val randomDigits = Random.nextInt(1000, 9999)
        // If offline mode, stored locally as pending_sync with temporary or pending tag;
        // when synced to backend, unique official ID is confirmed.
        val farmerCode = if (isOfflineMode) "TH-$statePrefix-PENDING-$randomDigits" else "TH-$statePrefix-2026-$randomDigits"

        val effectiveHectares = if (farmSizeHectares > 0.0) {
            farmSizeHectares
        } else {
            when (farmSizeUnit.lowercase()) {
                "acres" -> farmSize * 0.4047
                "plots" -> farmSize * 0.05
                else -> farmSize
            }
        }
        val effectiveFarmSize = if (farmSize > 0.0) farmSize else effectiveHectares

        val delta = (kotlin.math.sqrt(effectiveHectares.coerceAtLeast(0.5)) * 0.00075)
        val polygonString = listOf(
            "${latitude + delta},${longitude - delta}",
            "${latitude + delta},${longitude + delta}",
            "${latitude - delta},${longitude + delta}",
            "${latitude - delta},${longitude - delta}"
        ).joinToString(";")

        val farmer = FarmerEntity(
            farmerCode = farmerCode,
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            state = state,
            lga = lga.trim(),
            community = community.trim(),
            crop = crop,
            farmSize = effectiveFarmSize,
            farmSizeUnit = farmSizeUnit,
            farmSizeHectares = effectiveHectares,
            latitude = latitude,
            longitude = longitude,
            cooperative = cooperative.trim(),
            registrationTimestamp = System.currentTimeMillis(),
            farmerDisplayId = farmerCode,
            gpsPolygon = polygonString,
            syncStatus = if (isOfflineMode) "pending_sync" else "synced",
            isSynced = !isOfflineMode,
            agentId = agentId
        )
        val id = dao.insertFarmer(farmer)

        farmer.copy(id = id)
    }

    override suspend fun logPractice(
        farmerCode: String,
        farmerName: String,
        crop: String,
        practiceType: String,
        category: String,
        productName: String,
        activeIngredient: String,
        dosage: String,
        quantityUsed: Double,
        quantityUnit: String,
        calendarDateApplied: Long,
        verificationPhotoUri: String,
        liveLatitude: Double,
        liveLongitude: Double,
        gpsAccuracyMeters: Float,
        source: String,
        phiDays: Int,
        farmerLocalId: Long,
        farmerDisplayId: String,
        nafdacRegNo: String,
        gpsCoordinates: String,
        agentId: String,
        syncStatus: String
    ): PracticeLogEntity = withContext(Dispatchers.IO) {
        // NAFDAC Registry validation if product details are present
        val (matchedAgro, complianceStatus) = if (productName.isNotBlank() || activeIngredient.isNotBlank()) {
            NafdacCatalog.validateApplication(productName, activeIngredient)
        } else {
            Pair(null, NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT)
        }

        val effectiveRegNo = if (nafdacRegNo.isNotBlank()) nafdacRegNo else (matchedAgro?.nafdacRegNo ?: "")
        val isNafdacApproved = complianceStatus != NafdacExportCompliance.BANNED_MRL_VIOLATION &&
                               complianceStatus != NafdacExportCompliance.UNREGISTERED_UNKNOWN

        val (riskLevel, riskNotes) = when (complianceStatus) {
            NafdacExportCompliance.BANNED_MRL_VIOLATION -> {
                Pair(
                    "BANNED_MRL_VIOLATION",
                    matchedAgro?.safetyWarning ?: "CRITICAL VIOLATION: Banned substance! Causes immediate border rejection under EU/Codex standards."
                )
            }
            NafdacExportCompliance.APPROVED_ORGANIC_PREMIUM -> {
                Pair(
                    "COMPLIANT",
                    "NAFDAC REGISTERED & ORGANIC PREMIUM: Zero synthetic residue. Exempt from chemical MRL limits."
                )
            }
            NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            NafdacExportCompliance.RESTRICTED_PHI_MONITORED -> {
                Pair(
                    "COMPLIANT",
                    if (effectiveRegNo.isNotBlank()) {
                        "NAFDAC REGISTERED (${effectiveRegNo}): Approved with observed Pre-Harvest Interval (PHI) of $phiDays days."
                    } else {
                        "PRACTICE COMPLIANT: Recorded standard agronomic activity ($practiceType)."
                    }
                )
            }
            NafdacExportCompliance.UNREGISTERED_UNKNOWN -> {
                Pair(
                    "CAUTION",
                    "UNREGISTERED AGROCHEMICAL: Product not found in official NAFDAC catalog. Requires lab clearance."
                )
            }
        }

        val dosageStr = "$quantityUsed $quantityUnit"
        val practiceDetails = if (productName.isNotBlank()) {
            "$practiceType: $productName applied at $dosageStr"
        } else {
            "$practiceType recorded at farm plot"
        }

        val log = PracticeLogEntity(
            farmerCode = farmerCode,
            farmerName = farmerName,
            crop = crop,
            practiceType = practiceType,
            category = category,
            practiceDetails = practiceDetails,
            productName = productName,
            activeIngredient = activeIngredient,
            dosage = dosageStr,
            quantityUsed = quantityUsed,
            quantityUnit = quantityUnit,
            dateApplied = calendarDateApplied,
            preHarvestIntervalDays = phiDays,
            source = source,
            riskLevel = riskLevel,
            riskNotes = riskNotes,
            isSynced = syncStatus == "synced",
            farmerLocalId = farmerLocalId,
            farmerDisplayId = if (farmerDisplayId.isNotBlank()) farmerDisplayId else farmerCode,
            nafdacRegNo = effectiveRegNo,
            nafdacApproved = isNafdacApproved,
            gpsCoordinates = if (gpsCoordinates.isNotBlank()) gpsCoordinates else "${"%.4f".format(liveLatitude)}°N, ${"%.4f".format(liveLongitude)}°E",
            liveLatitude = liveLatitude,
            liveLongitude = liveLongitude,
            gpsAccuracyMeters = gpsAccuracyMeters,
            gpsFixStatus = "GPS_ACQUIRED",
            verificationPhotoUri = verificationPhotoUri,
            photoVerificationType = "CONTAINER_LABEL",
            syncStatus = syncStatus,
            agentId = agentId,
            serverLoggedTimestamp = System.currentTimeMillis()
        )
        val id = dao.insertPracticeLog(log)

        // Dispatch verification SMS to farmer if phone number exists
        val phone = dao.getFarmerByCode(farmerCode)?.phoneNumber
        if (phone != null) {
            val smsText = if (productName.isNotBlank()) {
                "TraceHarvest: Agent $agentId logged $practiceType ($productName, $dosageStr) on your $crop farm. PHI: $phiDays days. Reply 1 to CONFIRM."
            } else {
                "TraceHarvest: Agent $agentId recorded $practiceType on your $crop farm today. Reply 1 to CONFIRM."
            }
            dao.insertSmsLog(
                SmsLogEntity(
                    farmerPhone = phone,
                    farmerName = farmerName,
                    direction = "OUTBOUND",
                    messageType = "PRACTICE_QUERY",
                    content = smsText,
                    timestamp = System.currentTimeMillis(),
                    status = "DELIVERED"
                )
            )
        }

        log.copy(id = id)
    }

    override suspend fun syncAllPending(): SyncResult = withContext(Dispatchers.IO) {
        val pendingFarmers = dao.getPendingSyncFarmers()
        val pendingPractices = dao.getPendingSyncPracticeLogs()

        if (pendingFarmers.isEmpty() && pendingPractices.isEmpty()) {
            return@withContext SyncResult(
                syncedFarmersCount = 0,
                syncedPracticesCount = 0,
                totalPendingRemaining = 0,
                isSuccess = true,
                message = "All records in SQLite ledger are already synchronized with the backend."
            )
        }

        // Simulate upstream backend sync latency
        kotlinx.coroutines.delay(650)

        // 1. Sync pending farmers -> Backend generates unique official Farmer ID
        pendingFarmers.forEach { farmer ->
            val statePrefix = farmer.state.take(3).uppercase(Locale.ROOT)
            val randomDigits = Random.nextInt(1000, 9999)
            val backendOfficialId = "TH-$statePrefix-2026-$randomDigits"

            dao.markFarmerSynced(farmer.id, backendOfficialId)

            // Backend dispatches official enrollment SMS to farmer
            dao.insertSmsLog(
                SmsLogEntity(
                    farmerPhone = farmer.phoneNumber,
                    farmerName = farmer.fullName,
                    direction = "OUTBOUND",
                    messageType = "ENROLLMENT",
                    content = "TraceHarvest: Barka da zuwa! You are officially enrolled. Your Unique Farmer ID is $backendOfficialId ($farmer.crop, ${farmer.farmSize} ${farmer.farmSizeUnit} in ${farmer.lga}). No app download required.",
                    timestamp = System.currentTimeMillis(),
                    status = "DELIVERED"
                )
            )
        }

        // 2. Sync pending practice records
        if (pendingPractices.isNotEmpty()) {
            val practiceIds = pendingPractices.map { it.id }
            dao.markPracticeLogsSynced(practiceIds)
        }

        SyncResult(
            syncedFarmersCount = pendingFarmers.size,
            syncedPracticesCount = pendingPractices.size,
            totalPendingRemaining = 0,
            isSuccess = true,
            message = "Synchronized ${pendingFarmers.size} new farmer(s) & ${pendingPractices.size} practice record(s) to backend!"
        )
    }

    override suspend fun createHarvestBatch(
        farmerCode: String,
        farmerName: String,
        crop: String,
        region: String,
        bagCount: Int,
        netWeightKg: Double,
        moisturePercent: Double,
        foreignMatterPercent: Double,
        destinationMarket: String
    ): HarvestBatchEntity = withContext(Dispatchers.IO) {
        val cropCode = crop.take(3).uppercase(Locale.ROOT)
        val randomNum = Random.nextInt(1000, 9999)
        val batchCode = "NG-$cropCode-2026-$randomNum-EXP"

        val logs = dao.getPracticeLogsForFarmer(farmerCode).first()
        val hasBannedChemical = logs.any { it.riskLevel == "BANNED_MRL_VIOLATION" }
        val daysSinceLastSpray = if (logs.isNotEmpty()) {
            val mostRecentSpray = logs.maxOf { it.dateApplied }
            val daysDiff = ((System.currentTimeMillis() - mostRecentSpray) / (1000 * 60 * 60 * 24)).toInt()
            maxOf(daysDiff, 18)
        } else {
            24
        }

        val requiredPhi = logs.maxOfOrNull { it.preHarvestIntervalDays } ?: 14
        val isPhiPassed = daysSinceLastSpray >= requiredPhi

        val mrlStatus = when {
            hasBannedChemical -> "VIOLATION_BLOCKED"
            !isPhiPassed -> "PENDING_PHI"
            else -> "PASSED_SPS"
        }

        val aflatoxinStatus = when {
            moisturePercent <= 8.5 -> "SAFE (<4 ppb)"
            moisturePercent <= 10.0 -> "ELEVATED (4-10 ppb)"
            else -> "UNSAFE (>10 ppb)"
        }

        val isFlagged = hasBannedChemical || !isPhiPassed || moisturePercent > 10.5
        val rejectionReason = when {
            hasBannedChemical -> "EU MRL Alert: Banned pesticide detected in farm history."
            !isPhiPassed -> "PHI Violation: Harvested before required Pre-Harvest Interval elapsed."
            moisturePercent > 10.5 -> "Aflatoxin Hazard: Moisture exceeds 10.0% SPS limit."
            else -> null
        }

        val grade = when {
            isFlagged -> "Non-Compliant High Risk"
            moisturePercent <= 8.5 && foreignMatterPercent <= 1.5 -> "Grade A Export Ready"
            else -> "Grade B Local Processing"
        }

        val rawHashPayload = "$batchCode|$crop|$farmerCode|$netWeightKg|$moisturePercent|$mrlStatus|${System.currentTimeMillis()}"
        val sha256 = generateSha256(rawHashPayload)
        val txId = "0x" + sha256.take(40).lowercase(Locale.ROOT)
        val qrPayload = "TRACEHARVEST://BATCH/$batchCode?hash=${sha256.take(16)}&crop=$crop&dest=$destinationMarket"

        val batch = HarvestBatchEntity(
            batchCode = batchCode,
            crop = crop,
            farmerCode = farmerCode,
            farmerName = farmerName,
            region = region,
            harvestDate = System.currentTimeMillis(),
            bagCount = bagCount,
            netWeightKg = netWeightKg,
            moisturePercent = moisturePercent,
            foreignMatterPercent = foreignMatterPercent,
            grade = grade,
            mrlStatus = mrlStatus,
            aflatoxinStatus = aflatoxinStatus,
            phiDaysObserved = daysSinceLastSpray,
            destinationMarket = destinationMarket,
            blockchainHash = sha256,
            blockchainTxId = txId,
            isBlockchainAnchored = !isFlagged,
            isFlaggedForRejection = isFlagged,
            rejectionReason = rejectionReason,
            qrPayload = qrPayload
        )

        val id = dao.insertBatch(batch)
        batch.copy(id = id)
    }

    override suspend fun toggleFlagBatch(batchId: Long, currentFlag: Boolean, reason: String?) = withContext(Dispatchers.IO) {
        dao.setBatchFlaggedStatus(batchId, !currentFlag, if (!currentFlag) reason ?: "Manually flagged by compliance auditor" else null)
    }

    override suspend fun anchorBatch(batchId: Long) = withContext(Dispatchers.IO) {
        val randomHash = "0x" + (1..40).map { "0123456789abcdef".random() }.joinToString("")
        dao.anchorBatchToBlockchain(batchId, randomHash)
    }

    override suspend fun simulateIncomingFarmerSms(phone: String, farmerName: String, replyText: String, messageType: String): Unit = withContext(Dispatchers.IO) {
        dao.insertSmsLog(
            SmsLogEntity(
                farmerPhone = phone,
                farmerName = farmerName,
                direction = "INBOUND",
                messageType = messageType,
                content = replyText,
                timestamp = System.currentTimeMillis(),
                status = "CONFIRMED"
            )
        )

        val ackContent = when (replyText.trim().uppercase(Locale.ROOT)) {
            "1", "CONFIRM", "YES", "EEY" -> "TraceHarvest: Na gode! Your confirmation has been digitally signed and timestamped on your export ledger record."
            "2", "NO", "AA" -> "TraceHarvest: Recorded. Field Supervisor has been alerted to inspect your farm within 24 hours."
            else -> "TraceHarvest: Message received and logged against your farmer record ID."
        }

        dao.insertSmsLog(
            SmsLogEntity(
                farmerPhone = phone,
                farmerName = farmerName,
                direction = "OUTBOUND",
                messageType = "STATUS_QUERY",
                content = ackContent,
                timestamp = System.currentTimeMillis() + 1000,
                status = "DELIVERED"
            )
        )
        Unit
    }

    private fun generateSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun seedInitialDataset() {
        val now = System.currentTimeMillis()
        val day = 86400000L

        val initialFarmers = listOf(
            FarmerEntity(
                id = 1L,
                farmerCode = "TH-KAN-2026-1048",
                fullName = "Musa Ibrahim Dambatta",
                phoneNumber = "+2348034512991",
                state = "Kano",
                lga = "Dambatta",
                community = "Fagwalawa",
                crop = "Sesame",
                farmSize = 4.5,
                farmSizeUnit = "hectares",
                farmSizeHectares = 4.5,
                latitude = 12.4358,
                longitude = 8.5147,
                cooperative = "Dambatta Sesame Growers Union",
                registrationTimestamp = now - 65 * day,
                farmerDisplayId = "TH-KAN-2026-1048",
                gpsPolygon = "12.4374,8.5131;12.4374,8.5163;12.4342,8.5163;12.4342,8.5131",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            ),
            FarmerEntity(
                id = 2L,
                farmerCode = "TH-JIG-2026-2819",
                fullName = "Amina Abubakar Maigatari",
                phoneNumber = "+2348167823412",
                state = "Jigawa",
                lga = "Maigatari",
                community = "Galadi",
                crop = "Sesame",
                farmSize = 6.0,
                farmSizeUnit = "hectares",
                farmSizeHectares = 6.0,
                latitude = 12.8122,
                longitude = 9.4589,
                cooperative = "Maigatari Export Cluster",
                registrationTimestamp = now - 58 * day,
                farmerDisplayId = "TH-JIG-2026-2819",
                gpsPolygon = "12.8140,9.4571;12.8140,9.4607;12.8104,9.4607;12.8104,9.4571",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            ),
            FarmerEntity(
                id = 3L,
                farmerCode = "TH-BEN-2026-4401",
                fullName = "Terkimbi Terver",
                phoneNumber = "+2348059918234",
                state = "Benue",
                lga = "Makurdi",
                community = "Agan",
                crop = "Cowpea",
                farmSize = 8.0,
                farmSizeUnit = "plots",
                farmSizeHectares = 3.2,
                latitude = 7.7322,
                longitude = 8.5391,
                cooperative = "Benue Valley Grain Alliance",
                registrationTimestamp = now - 50 * day,
                farmerDisplayId = "TH-BEN-2026-4401",
                gpsPolygon = "7.7335,8.5378;7.7335,8.5404;7.7309,8.5404;7.7309,8.5378",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            ),
            FarmerEntity(
                id = 4L,
                farmerCode = "TH-KAD-2026-6210",
                fullName = "Garba Lawal Kachia",
                phoneNumber = "+2348023190822",
                state = "Kaduna",
                lga = "Kachia",
                community = "Gumel",
                crop = "Ginger",
                farmSize = 7.0,
                farmSizeUnit = "acres",
                farmSizeHectares = 2.8,
                latitude = 9.8731,
                longitude = 7.9542,
                cooperative = "Kaduna High-Oleoresin Ginger Union",
                registrationTimestamp = now - 42 * day,
                farmerDisplayId = "TH-KAD-2026-6210",
                gpsPolygon = "9.8743,7.9530;9.8743,7.9554;9.8719,7.9554;9.8719,7.9530",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            )
        )
        dao.insertFarmers(initialFarmers)

        val initialLogs = listOf(
            PracticeLogEntity(
                id = 1L,
                farmerCode = "TH-KAN-2026-1048",
                farmerName = "Musa Ibrahim Dambatta",
                crop = "Sesame",
                practiceType = "🧪 Pesticide application",
                category = "Pesticide Application",
                practiceDetails = "Karate 5 EC applied at 400 ml",
                productName = "Karate 5 EC",
                activeIngredient = "Lambda-cyhalothrin (50 g/L EC)",
                dosage = "400 ml",
                quantityUsed = 400.0,
                quantityUnit = "ml",
                dateApplied = now - 28 * day,
                preHarvestIntervalDays = 14,
                source = "Agent Mobile App",
                riskLevel = "COMPLIANT",
                riskNotes = "NAFDAC REGISTERED (04-2015): PHI observed 28 days.",
                farmerLocalId = 1L,
                farmerDisplayId = "TH-KAN-2026-1048",
                nafdacRegNo = "04-2015",
                nafdacApproved = true,
                gpsCoordinates = "12.4358°N, 8.5147°E",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            ),
            PracticeLogEntity(
                id = 2L,
                farmerCode = "TH-KAN-2026-1048",
                farmerName = "Musa Ibrahim Dambatta",
                crop = "Sesame",
                practiceType = "🌿 Fertilizer application",
                category = "Fertilizer / Soil",
                practiceDetails = "Basal application of Indorama Granular NPK compound",
                productName = "Indorama NPK 15:15:15",
                activeIngredient = "Nitrogen 15% - Phosphorus 15% - Potassium 15%",
                dosage = "3 Bags",
                quantityUsed = 3.0,
                quantityUnit = "Bags",
                dateApplied = now - 45 * day,
                preHarvestIntervalDays = 0,
                source = "Agent Mobile App",
                riskLevel = "COMPLIANT",
                riskNotes = "NAFDAC REGISTERED (04-7892): PHI 0 days.",
                farmerLocalId = 1L,
                farmerDisplayId = "TH-KAN-2026-1048",
                nafdacRegNo = "04-7892",
                nafdacApproved = true,
                gpsCoordinates = "12.4358°N, 8.5147°E",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            ),
            PracticeLogEntity(
                id = 3L,
                farmerCode = "TH-JIG-2026-2819",
                farmerName = "Amina Abubakar Maigatari",
                crop = "Sesame",
                practiceType = "🌱 Planting",
                category = "Planting",
                practiceDetails = "Seed sowing with verified certified sesame seed",
                productName = "Certified NCRI Sesame Seed",
                dosage = "10 kg",
                quantityUsed = 10.0,
                quantityUnit = "kg",
                dateApplied = now - 60 * day,
                preHarvestIntervalDays = 0,
                source = "Agent Mobile App",
                riskLevel = "COMPLIANT",
                riskNotes = "Planting registered and cluster geotagged.",
                farmerLocalId = 2L,
                farmerDisplayId = "TH-JIG-2026-2819",
                nafdacApproved = true,
                gpsCoordinates = "12.8122°N, 9.4589°E",
                syncStatus = "synced",
                isSynced = true,
                agentId = "AGENT-NG-042"
            )
        )
        dao.insertPracticeLogs(initialLogs)
    }
}
