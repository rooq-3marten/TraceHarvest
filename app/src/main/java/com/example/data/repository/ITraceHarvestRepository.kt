package com.example.data.repository

import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.data.local.entity.SmsLogEntity
import kotlinx.coroutines.flow.Flow

data class SyncResult(
    val syncedFarmersCount: Int = 0,
    val syncedPracticesCount: Int = 0,
    val totalPendingRemaining: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val message: String = ""
)

/**
 * Repository interface for Field Agent workflows:
 * 1. Register New Farmer (Offline SQLite -> pending_sync -> Backend Sync -> Unique Farmer ID)
 * 2. Stage 2: Practice Logging (Icon-based menu, approved products, quantity, label photo, auto GPS & Agent ID)
 */
interface ITraceHarvestRepository {
    val allFarmers: Flow<List<FarmerEntity>>
    val allPracticeLogs: Flow<List<PracticeLogEntity>>
    val allBatches: Flow<List<HarvestBatchEntity>>
    val allSmsLogs: Flow<List<SmsLogEntity>>

    val totalBatchesCount: Flow<Int>
    val exportReadyBatchesCount: Flow<Int>
    val blockedBatchesCount: Flow<Int>
    val farmerCount: Flow<Int>
    val bannedViolationsCount: Flow<Int>

    val pendingFarmerSyncCount: Flow<Int>
    val pendingPracticeSyncCount: Flow<Int>

    fun getPracticeLogsForFarmer(farmerCode: String): Flow<List<PracticeLogEntity>>

    suspend fun checkAndSeedInitialData()

    suspend fun registerFarmer(
        fullName: String,
        phoneNumber: String,
        state: String,
        lga: String,
        community: String,
        crop: String,
        farmSize: Double = 1.0,
        farmSizeUnit: String = "hectares",
        farmSizeHectares: Double = farmSize,
        latitude: Double,
        longitude: Double,
        cooperative: String = "",
        agentId: String = "",
        isOfflineMode: Boolean = true
    ): FarmerEntity

    suspend fun logPractice(
        farmerCode: String,
        farmerName: String,
        crop: String,
        practiceType: String = "🧪 Pesticide application",
        category: String = practiceType,
        productName: String = "",
        activeIngredient: String = "",
        dosage: String = "",
        quantityUsed: Double = 1.0,
        quantityUnit: String = "Litres",
        calendarDateApplied: Long = System.currentTimeMillis(),
        verificationPhotoUri: String = "",
        liveLatitude: Double = 0.0,
        liveLongitude: Double = 0.0,
        gpsAccuracyMeters: Float = 0.0f,
        source: String = "Agent Mobile App",
        phiDays: Int = 0,
        farmerLocalId: Long = 0,
        farmerDisplayId: String = farmerCode,
        nafdacRegNo: String = "",
        gpsCoordinates: String = "",
        agentId: String = "",
        syncStatus: String = "pending_sync"
    ): PracticeLogEntity

    suspend fun syncAllPending(): SyncResult

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
    ): HarvestBatchEntity

    suspend fun toggleFlagBatch(batchId: Long, currentFlag: Boolean, reason: String?)

    suspend fun anchorBatch(batchId: Long)

    suspend fun simulateIncomingFarmerSms(
        phone: String,
        farmerName: String,
        replyText: String,
        messageType: String
    )
}
