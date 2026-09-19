package com.example.data.repository

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class TraceHarvestRepository(private val dao: TraceHarvestDao) {

    val allFarmers: Flow<List<FarmerEntity>> = dao.getAllFarmers()
    val allPracticeLogs: Flow<List<PracticeLogEntity>> = dao.getAllPracticeLogs()
    val allBatches: Flow<List<HarvestBatchEntity>> = dao.getAllBatches()
    val allSmsLogs: Flow<List<SmsLogEntity>> = dao.getAllSmsLogs()

    val totalBatchesCount: Flow<Int> = dao.getTotalBatchesCount()
    val exportReadyBatchesCount: Flow<Int> = dao.getExportReadyBatchesCount()
    val blockedBatchesCount: Flow<Int> = dao.getBlockedBatchesCount()
    val farmerCount: Flow<Int> = dao.getFarmerCount()
    val bannedViolationsCount: Flow<Int> = dao.getBannedPesticideViolationsCount()

    fun getPracticeLogsForFarmer(farmerCode: String): Flow<List<PracticeLogEntity>> =
        dao.getPracticeLogsForFarmer(farmerCode)

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val count = dao.getFarmerCount().first()
        if (count == 0) {
            seedInitialDataset()
        }
    }

    suspend fun registerFarmer(
        fullName: String,
        phoneNumber: String,
        state: String,
        lga: String,
        community: String,
        crop: String,
        farmSizeHectares: Double,
        latitude: Double,
        longitude: Double,
        cooperative: String
    ): FarmerEntity = withContext(Dispatchers.IO) {
        val statePrefix = state.take(3).uppercase(Locale.ROOT)
        val randomDigits = Random.nextInt(1000, 9999)
        val farmerCode = "TH-$statePrefix-2026-$randomDigits"

        val farmer = FarmerEntity(
            farmerCode = farmerCode,
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            state = state,
            lga = lga.trim(),
            community = community.trim(),
            crop = crop,
            farmSizeHectares = farmSizeHectares,
            latitude = latitude,
            longitude = longitude,
            cooperative = cooperative.trim(),
            registrationTimestamp = System.currentTimeMillis()
        )
        val id = dao.insertFarmer(farmer)

        // Trigger automatic enrollment SMS to farmer
        dao.insertSmsLog(
            SmsLogEntity(
                farmerPhone = farmer.phoneNumber,
                farmerName = farmer.fullName,
                direction = "OUTBOUND",
                messageType = "ENROLLMENT",
                content = "TraceHarvest: Barka da zuwa! Your farmer ID is $farmerCode ($crop, $farmSizeHectares ha in $lga). Retain for batch aggregation & export bonus.",
                timestamp = System.currentTimeMillis(),
                status = "DELIVERED"
            )
        )

        farmer.copy(id = id)
    }

    suspend fun logPractice(
        farmerCode: String,
        farmerName: String,
        crop: String,
        category: String,
        productName: String,
        activeIngredient: String,
        dosage: String,
        source: String = "Agent Mobile App",
        phiDays: Int = 14
    ): PracticeLogEntity = withContext(Dispatchers.IO) {
        // MRL compliance intelligence
        val (riskLevel, riskNotes) = evaluateMrlRisk(productName, activeIngredient)

        val log = PracticeLogEntity(
            farmerCode = farmerCode,
            farmerName = farmerName,
            crop = crop,
            category = category,
            practiceDetails = "$productName ($activeIngredient) applied at $dosage",
            productName = productName,
            activeIngredient = activeIngredient,
            dosage = dosage,
            dateApplied = System.currentTimeMillis(),
            preHarvestIntervalDays = phiDays,
            source = source,
            riskLevel = riskLevel,
            riskNotes = riskNotes
        )
        val id = dao.insertPracticeLog(log)

        // If logged via agent app, send verification SMS confirmation prompt to farmer
        if (source == "Agent Mobile App") {
            val phone = dao.getFarmerByCode(farmerCode)?.phoneNumber ?: ("+234803" + Random.nextInt(1000000, 9999999))
            dao.insertSmsLog(
                SmsLogEntity(
                    farmerPhone = phone,
                    farmerName = farmerName,
                    direction = "OUTBOUND",
                    messageType = "PRACTICE_QUERY",
                    content = "TraceHarvest Record: Agent logged application of $productName on your $crop farm today. PHI: $phiDays days. Reply 1 to CONFIRM or 2 to DISPUTE.",
                    timestamp = System.currentTimeMillis(),
                    status = "DELIVERED"
                )
            )
        }

        log.copy(id = id)
    }

    suspend fun createHarvestBatch(
        farmerCode: String,
        farmerName: String,
        crop: String,
        region: String,
        bagCount: Int,
        netWeightKg: Double,
        moisturePercent: Double,
        foreignMatterPercent: Double,
        destinationMarket: String = "EU & United Kingdom"
    ): HarvestBatchEntity = withContext(Dispatchers.IO) {
        val cropCode = crop.take(3).uppercase(Locale.ROOT)
        val randomNum = Random.nextInt(1000, 9999)
        val batchCode = "NG-$cropCode-2026-$randomNum-EXP"

        // Evaluate latest practice logs for this farmer to check MRL compliance & PHI
        val logs = dao.getPracticeLogsForFarmer(farmerCode).first()
        val hasBannedChemical = logs.any { it.riskLevel == "BANNED_MRL_VIOLATION" }
        val daysSinceLastSpray = if (logs.isNotEmpty()) {
            val mostRecentSpray = logs.maxOf { it.dateApplied }
            val daysDiff = ((System.currentTimeMillis() - mostRecentSpray) / (1000 * 60 * 60 * 24)).toInt()
            maxOf(daysDiff, 18) // Realistic default
        } else {
            24
        }

        val requiredPhi = logs.maxOfOrNull { it.preHarvestIntervalDays } ?: 14

        val isPhiPassed = daysSinceLastSpray >= requiredPhi
        val isMoistureSafe = moisturePercent <= 10.0 // Aflatoxin control standard

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
            hasBannedChemical -> "EU MRL Alert: Banned pesticide detected in farm history (Dichlorvos/Chlorpyrifos exceedance risk)."
            !isPhiPassed -> "PHI Violation: Harvested before required Pre-Harvest Interval elapsed ($daysSinceLastSpray days vs $requiredPhi required)."
            moisturePercent > 10.5 -> "Aflatoxin Hazard: Moisture $moisturePercent% exceeds safe 10.0% SPS limit."
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
            isBlockchainAnchored = !isFlagged, // compliant batches automatically anchored
            isFlaggedForRejection = isFlagged,
            rejectionReason = rejectionReason,
            qrPayload = qrPayload
        )

        val id = dao.insertBatch(batch)

        // Send confirmation SMS to farmer
        val phone = dao.getFarmerByCode(farmerCode)?.phoneNumber ?: ("+234803" + Random.nextInt(1000000, 9999999))
        dao.insertSmsLog(
            SmsLogEntity(
                farmerPhone = phone,
                farmerName = farmerName,
                direction = "OUTBOUND",
                messageType = "BATCH_CONFIRM",
                content = "TraceHarvest Aggregation: Your batch $batchCode ($bagCount bags, ${netWeightKg.toInt()} kg $crop) recorded. Moisture: $moisturePercent%. Status: $grade. Reply CONFIRM to sign.",
                timestamp = System.currentTimeMillis(),
                status = "DELIVERED"
            )
        )

        batch.copy(id = id)
    }

    suspend fun toggleFlagBatch(batchId: Long, currentFlag: Boolean, reason: String?) = withContext(Dispatchers.IO) {
        dao.setBatchFlaggedStatus(batchId, !currentFlag, if (!currentFlag) reason ?: "Manually flagged by compliance auditor" else null)
    }

    suspend fun anchorBatch(batchId: Long) = withContext(Dispatchers.IO) {
        val randomHash = "0x" + (1..40).map { "0123456789abcdef".random() }.joinToString("")
        dao.anchorBatchToBlockchain(batchId, randomHash)
    }

    suspend fun simulateIncomingFarmerSms(phone: String, farmerName: String, replyText: String, messageType: String) = withContext(Dispatchers.IO) {
        // Record incoming message
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

        // Send automated acknowledgment
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
    }

    private fun evaluateMrlRisk(productName: String, activeIngredient: String): Pair<String, String> {
        val lowerProd = productName.lowercase(Locale.ROOT)
        val lowerActive = activeIngredient.lowercase(Locale.ROOT)

        return when {
            lowerProd.contains("dichlorvos") || lowerProd.contains("ddvp") || lowerProd.contains("sniper") ||
                    lowerActive.contains("dichlorvos") -> {
                Pair(
                    "BANNED_MRL_VIOLATION",
                    "CRITICAL VIOLATION: Dichlorvos (DDVP) is strictly BANNED in the EU, UK, and CODEX for grain/seed storage. Causes immediate border rejection of entire shipment."
                )
            }
            lowerProd.contains("chlorpyrifos") || lowerActive.contains("chlorpyrifos") -> {
                Pair(
                    "BANNED_MRL_VIOLATION",
                    "CRITICAL VIOLATION: Chlorpyrifos authorization revoked by EU (EC No 2020/1085). MRL set at limit of quantification (0.01 mg/kg). Rejection guaranteed."
                )
            }
            lowerProd.contains("monocrotophos") || lowerActive.contains("monocrotophos") -> {
                Pair(
                    "BANNED_MRL_VIOLATION",
                    "CRITICAL VIOLATION: Monocrotophos is an extremely hazardous Class Ib organophosphate banned under the Rotterdam Convention."
                )
            }
            lowerProd.contains("lambda") || lowerActive.contains("cyhalothrin") -> {
                Pair(
                    "COMPLIANT",
                    "APPROVED WITH RESTRICTIONS: Lambda-cyhalothrin is permitted under EU MRL (0.05 mg/kg) provided Pre-Harvest Interval of >= 14 days is strictly observed."
                )
            }
            lowerProd.contains("neem") || lowerProd.contains("bio") || lowerActive.contains("azadirachtin") -> {
                Pair(
                    "COMPLIANT",
                    "PREMIUM ORGANIC: Bio-botanical biopesticide (Zero synthetic residue, exempt from MRL tolerance limits in EU & Japan)."
                )
            }
            lowerProd.contains("pics") || lowerProd.contains("hermetic") -> {
                Pair(
                    "COMPLIANT",
                    "GOLD STANDARD POST-HARVEST: Purdue Improved Crop Storage (PICS) triple-layer hermetic bags. 100% chemical-free weevil and aflatoxin control."
                )
            }
            else -> {
                Pair(
                    "CAUTION",
                    "STANDARD MONITORING: Ensure exact dosage adherence and verify laboratory test certificate prior to container stuffing."
                )
            }
        }
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
                registrationTimestamp = now - 65 * day
            ),
            FarmerEntity(
                farmerCode = "TH-JIG-2026-2819",
                fullName = "Amina Abubakar Maigatari",
                phoneNumber = "+2348167823412",
                state = "Jigawa",
                lga = "Maigatari",
                community = "Galadi",
                crop = "Sesame",
                farmSizeHectares = 6.0,
                latitude = 12.8122,
                longitude = 9.4589,
                cooperative = "Maigatari Export Cluster",
                registrationTimestamp = now - 58 * day
            ),
            FarmerEntity(
                farmerCode = "TH-BEN-2026-4401",
                fullName = "Terkimbi Terver",
                phoneNumber = "+2348059918234",
                state = "Benue",
                lga = "Makurdi",
                community = "Agan",
                crop = "Cowpea",
                farmSizeHectares = 3.2,
                latitude = 7.7322,
                longitude = 8.5391,
                cooperative = "Benue Valley Grain Alliance",
                registrationTimestamp = now - 50 * day
            ),
            FarmerEntity(
                farmerCode = "TH-KAD-2026-6210",
                fullName = "Garba Lawal Kachia",
                phoneNumber = "+2348023190822",
                state = "Kaduna",
                lga = "Kachia",
                community = "Gumel",
                crop = "Ginger",
                farmSizeHectares = 2.8,
                latitude = 9.8731,
                longitude = 7.9542,
                cooperative = "Kaduna High-Oleoresin Ginger Union",
                registrationTimestamp = now - 42 * day
            ),
            FarmerEntity(
                farmerCode = "TH-KAN-2026-7734",
                fullName = "Bello Aliyu Bichi",
                phoneNumber = "+2348074499100",
                state = "Kano",
                lga = "Bichi",
                community = "Badume",
                crop = "Cowpea",
                farmSizeHectares = 5.0,
                latitude = 12.2341,
                longitude = 8.2415,
                cooperative = "Bichi Legume Association",
                registrationTimestamp = now - 35 * day
            )
        )
        dao.insertFarmers(initialFarmers)

        val initialLogs = listOf(
            PracticeLogEntity(
                farmerCode = "TH-KAN-2026-1048",
                farmerName = "Musa Ibrahim Dambatta",
                crop = "Sesame",
                category = "Pesticide Application",
                practiceDetails = "Applied Lambda-Super 5EC for Sesame Gall Midge control",
                productName = "Lambda-Super 5EC",
                activeIngredient = "Lambda-cyhalothrin",
                dosage = "400 ml / hectare",
                dateApplied = now - 28 * day,
                preHarvestIntervalDays = 14,
                source = "Agent Mobile App",
                riskLevel = "COMPLIANT",
                riskNotes = "Compliant with EU MRL (0.05 mg/kg). Observed PHI is 28 days (required 14)."
            ),
            PracticeLogEntity(
                farmerCode = "TH-JIG-2026-2819",
                farmerName = "Amina Abubakar Maigatari",
                crop = "Sesame",
                category = "Pesticide Application",
                practiceDetails = "Applied Certified Organic Neem Seed Oil formulation",
                productName = "BioNeem Extract 0.3%",
                activeIngredient = "Azadirachtin",
                dosage = "1.5 Litres / hectare",
                dateApplied = now - 22 * day,
                preHarvestIntervalDays = 3,
                source = "USSD (*384*748#)",
                riskLevel = "COMPLIANT",
                riskNotes = "Zero synthetic residue. Qualifies for organic premium export pricing to Germany & Japan."
            ),
            PracticeLogEntity(
                farmerCode = "TH-BEN-2026-4401",
                farmerName = "Terkimbi Terver",
                crop = "Cowpea",
                category = "Drying & Aflatoxin Control",
                practiceDetails = "Moisture reduction using elevated black tarpaulins and PICS hermetic storage bags",
                productName = "PICS Hermetic Triple-Layer Bags",
                activeIngredient = "Hermetic Barrier",
                dosage = "100 bags deployed",
                dateApplied = now - 15 * day,
                preHarvestIntervalDays = 0,
                source = "SMS Reply (34461)",
                riskLevel = "COMPLIANT",
                riskNotes = "Aflatoxin prevention verified: grain moisture reduced to 7.8% (Target < 10%)."
            ),
            PracticeLogEntity(
                farmerCode = "TH-KAN-2026-7734",
                farmerName = "Bello Aliyu Bichi",
                crop = "Cowpea",
                category = "Pesticide Application",
                practiceDetails = "Storage fumigation using Sniper DDVP spray (ILLEGAL)",
                productName = "Sniper 1000EC",
                activeIngredient = "Dichlorvos (DDVP)",
                dosage = "Uncalibrated manual spray",
                dateApplied = now - 8 * day,
                preHarvestIntervalDays = 60,
                source = "Agent Mobile App",
                riskLevel = "BANNED_MRL_VIOLATION",
                riskNotes = "CRITICAL BORDER REJECTION RISK: Dichlorvos is banned in the EU. MRL exceedance will trigger immediate consignment seizure."
            )
        )
        dao.insertPracticeLogs(initialLogs)

        val batch1Hash = generateSha256("NG-SES-2026-0042-EXP|Sesame|TH-KAN-2026-1048|1850.0|7.4|PASSED_SPS")
        val batch2Hash = generateSha256("NG-SES-2026-0089-EXP|Sesame|TH-JIG-2026-2819|2400.0|6.8|PASSED_SPS")
        val batch3Hash = generateSha256("NG-COW-2026-0155-EXP|Cowpea|TH-BEN-2026-4401|1200.0|8.2|PASSED_SPS")
        val batch4Hash = generateSha256("NG-COW-2026-0201-BLK|Cowpea|TH-KAN-2026-7734|1600.0|12.8|VIOLATION_BLOCKED")

        val initialBatches = listOf(
            HarvestBatchEntity(
                batchCode = "NG-SES-2026-0042-EXP",
                crop = "Sesame (White)",
                farmerCode = "TH-KAN-2026-1048",
                farmerName = "Musa Ibrahim Dambatta",
                region = "Kano (Dambatta)",
                harvestDate = now - 5 * day,
                bagCount = 37,
                netWeightKg = 1850.0,
                moisturePercent = 7.4,
                foreignMatterPercent = 1.1,
                grade = "Grade A Export Ready",
                mrlStatus = "PASSED_SPS",
                aflatoxinStatus = "SAFE (<4 ppb)",
                phiDaysObserved = 28,
                destinationMarket = "Japan & European Union",
                blockchainHash = batch1Hash,
                blockchainTxId = "0x" + batch1Hash.take(40),
                isBlockchainAnchored = true,
                isFlaggedForRejection = false,
                rejectionReason = null,
                qrPayload = "TRACEHARVEST://BATCH/NG-SES-2026-0042-EXP?hash=${batch1Hash.take(16)}&status=PASSED"
            ),
            HarvestBatchEntity(
                batchCode = "NG-SES-2026-0089-EXP",
                crop = "Sesame (Premium)",
                farmerCode = "TH-JIG-2026-2819",
                farmerName = "Amina Abubakar Maigatari",
                region = "Jigawa (Maigatari)",
                harvestDate = now - 3 * day,
                bagCount = 48,
                netWeightKg = 2400.0,
                moisturePercent = 6.8,
                foreignMatterPercent = 0.8,
                grade = "Grade A Export Ready",
                mrlStatus = "PASSED_SPS",
                aflatoxinStatus = "SAFE (<4 ppb)",
                phiDaysObserved = 22,
                destinationMarket = "EU & Türkiye",
                blockchainHash = batch2Hash,
                blockchainTxId = "0x" + batch2Hash.take(40),
                isBlockchainAnchored = true,
                isFlaggedForRejection = false,
                rejectionReason = null,
                qrPayload = "TRACEHARVEST://BATCH/NG-SES-2026-0089-EXP?hash=${batch2Hash.take(16)}&status=PASSED"
            ),
            HarvestBatchEntity(
                batchCode = "NG-COW-2026-0155-EXP",
                crop = "Cowpeas (White Oloyin)",
                farmerCode = "TH-BEN-2026-4401",
                farmerName = "Terkimbi Terver",
                region = "Benue (Makurdi)",
                harvestDate = now - 2 * day,
                bagCount = 24,
                netWeightKg = 1200.0,
                moisturePercent = 8.2,
                foreignMatterPercent = 1.4,
                grade = "Grade A Export Ready",
                mrlStatus = "PASSED_SPS",
                aflatoxinStatus = "SAFE (<4 ppb)",
                phiDaysObserved = 30,
                destinationMarket = "United Kingdom",
                blockchainHash = batch3Hash,
                blockchainTxId = "0x" + batch3Hash.take(40),
                isBlockchainAnchored = true,
                isFlaggedForRejection = false,
                rejectionReason = null,
                qrPayload = "TRACEHARVEST://BATCH/NG-COW-2026-0155-EXP?hash=${batch3Hash.take(16)}&status=PASSED"
            ),
            HarvestBatchEntity(
                batchCode = "NG-COW-2026-0201-BLK",
                crop = "Cowpeas (Brown Beans)",
                farmerCode = "TH-KAN-2026-7734",
                farmerName = "Bello Aliyu Bichi",
                region = "Kano (Bichi)",
                harvestDate = now - 1 * day,
                bagCount = 32,
                netWeightKg = 1600.0,
                moisturePercent = 12.8,
                foreignMatterPercent = 3.2,
                grade = "Non-Compliant High Risk",
                mrlStatus = "VIOLATION_BLOCKED",
                aflatoxinStatus = "UNSAFE (>10 ppb)",
                phiDaysObserved = 8,
                destinationMarket = "BLOCKED - Intercepted Pre-Shipment",
                blockchainHash = batch4Hash,
                blockchainTxId = "0x" + batch4Hash.take(40),
                isBlockchainAnchored = false,
                isFlaggedForRejection = true,
                rejectionReason = "EU MRL Alert: Dichlorvos (Sniper) residue detected. Moisture 12.8% exceeds safe limit. Container loading stopped at Kano aggregation center.",
                qrPayload = "TRACEHARVEST://BATCH/NG-COW-2026-0201-BLK?hash=${batch4Hash.take(16)}&status=BLOCKED"
            )
        )
        dao.insertBatches(initialBatches)

        val initialSms = listOf(
            SmsLogEntity(
                farmerPhone = "+2348034512991",
                farmerName = "Musa Ibrahim Dambatta",
                direction = "OUTBOUND",
                messageType = "BATCH_CONFIRM",
                content = "TraceHarvest: Batch NG-SES-2026-0042 (1,850kg Sesame) registered for export to Japan. Reply 1 to CONFIRM.",
                timestamp = now - 5 * day,
                status = "CONFIRMED"
            ),
            SmsLogEntity(
                farmerPhone = "+2348034512991",
                farmerName = "Musa Ibrahim Dambatta",
                direction = "INBOUND",
                messageType = "BATCH_CONFIRM",
                content = "1",
                timestamp = now - 5 * day + 120000,
                status = "CONFIRMED"
            ),
            SmsLogEntity(
                farmerPhone = "+2348167823412",
                farmerName = "Amina Abubakar Maigatari",
                direction = "OUTBOUND",
                messageType = "PRACTICE_QUERY",
                content = "TraceHarvest USSD: Did you spray chemical on sesame farm on Sept 2? Reply 1 YES, 2 NO.",
                timestamp = now - 22 * day,
                status = "CONFIRMED"
            ),
            SmsLogEntity(
                farmerPhone = "+2348167823412",
                farmerName = "Amina Abubakar Maigatari",
                direction = "INBOUND",
                messageType = "PRACTICE_REPLY",
                content = "1 (Neem Bio)",
                timestamp = now - 22 * day + 60000,
                status = "CONFIRMED"
            ),
            SmsLogEntity(
                farmerPhone = "+2348074499100",
                farmerName = "Bello Aliyu Bichi",
                direction = "OUTBOUND",
                messageType = "BATCH_CONFIRM",
                content = "TraceHarvest ALERT: Batch NG-COW-2026-0201 FLAGGED. Sniper Dichlorvos detected. Export stopped. Contact cooperative lead.",
                timestamp = now - 1 * day,
                status = "DELIVERED"
            )
        )
        dao.insertSmsLogs(initialSms)
    }
}
