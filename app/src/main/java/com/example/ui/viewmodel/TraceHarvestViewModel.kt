package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.auth.SessionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.data.local.entity.SmsLogEntity
import com.example.data.repository.ITraceHarvestRepository
import com.example.data.repository.TraceHarvestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.ui.screens.FarmerEnrollment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Same statuses the DAO treats as "waiting to sync"
private val PENDING_STATUSES = setOf("PENDING", "pending_sync", "FAILED")

private const val SIGNED_OUT_MESSAGE = "You are signed out. Sign in again to save records."

enum class AppTab(val title: String) {
    HOME_DASHBOARD("Field Home"),
    REGISTER_FARMER("Register New Farmer"),
    LOG_PRACTICE("Practice Logging"),
    BATCH_CREATION("Create Batch"),
    SYNC_RECORDS("Sync & Records"),
    SETTINGS("Settings"),
    ZONE_SELECTION("Select Region")
}

data class DashboardMetrics(
    val totalFarmers: Int = 0,
    val pendingFarmersSync: Int = 0,
    val totalPractices: Int = 0,
    val pendingPracticesSync: Int = 0,
    val totalPending: Int = 0
)

class TraceHarvestViewModel @JvmOverloads constructor(
    application: Application,
    repositoryInstance: ITraceHarvestRepository? = null
) : AndroidViewModel(application) {

    // Set from the signed-in agent's profile (see TraceHarvestApp); never hardcoded.
    private val _currentAgentId = MutableStateFlow("")
    var currentAgentId: String
        get() = _currentAgentId.value
        set(value) {
            _currentAgentId.value = value
        }

    private val repository: ITraceHarvestRepository = repositoryInstance 
        ?: run {
            val db = AppDatabase.getDatabase(application)
            TraceHarvestRepository(
                db.traceHarvestDao(),
                db.pendingSyncDao(),
                // Sync uses the agent from the stored session, not from whichever record happens to be first
                signedInAgentId = { SessionManager.getInstance(application).currentAgentIdOrNull() }
            )
        }

    val allFarmers: StateFlow<List<FarmerEntity>>
    val allPracticeLogs: StateFlow<List<PracticeLogEntity>>
    val allBatches: StateFlow<List<HarvestBatchEntity>>
    val allSmsLogs: StateFlow<List<SmsLogEntity>>

    val pendingFarmerSyncCount: StateFlow<Int>
    val pendingPracticeSyncCount: StateFlow<Int>
    val totalPendingSyncCount: StateFlow<Int>

    private val farmerSaveLock = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val zoneManager = com.example.core.zone.ZonePreferenceManager(application)
    val currentZone: StateFlow<com.example.core.zone.GeopoliticalZone> = zoneManager.currentZone

    private val _selectedTab = MutableStateFlow(
        if (!zoneManager.isZoneConfigured()) AppTab.ZONE_SELECTION else AppTab.HOME_DASHBOARD
    )
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setZone(zone: com.example.core.zone.GeopoliticalZone) {
        zoneManager.setZone(zone)
        _selectedTab.value = AppTab.HOME_DASHBOARD
    }

    fun createConsignmentBatch(
        batchCode: String,
        crop: String,
        quantityKg: Double,
        moisturePercent: Double,
        foreignMatterPercent: Double,
        hubName: String,
        contributingFarmerIds: List<String>
    ) {
        if (currentAgentId.isBlank()) {
            _userMessage.value = SIGNED_OUT_MESSAGE
            return
        }
        viewModelScope.launch {
            try {
                val selectedFarmers = allFarmers.value.filter {
                    it.localId in contributingFarmerIds || it.farmerCode in contributingFarmerIds
                }
                val farmerCodes = selectedFarmers.map { it.farmerCode }.filter { it.isNotBlank() }
                val primaryFarmerCode = farmerCodes.firstOrNull()
                    ?: contributingFarmerIds.firstOrNull()?.takeIf { !it.contains("-") || it.startsWith("TH-") }
                    ?: "UNLINKED"
                val farmerLabel = when {
                    selectedFarmers.size == 1 -> selectedFarmers.first().fullName
                    selectedFarmers.size > 1 -> "${selectedFarmers.size} Smallholders"
                    contributingFarmerIds.isNotEmpty() -> "${contributingFarmerIds.size} Smallholders"
                    else -> "Unlinked Consignment"
                }
                repository.createBatch(
                    farmerCode = primaryFarmerCode,
                    farmerName = farmerLabel,
                    crop = crop,
                    region = hubName,
                    bagCount = (quantityKg / 50.0).toInt().coerceAtLeast(1),
                    netWeightKg = quantityKg,
                    moisturePercent = moisturePercent,
                    foreignMatterPercent = foreignMatterPercent,
                    agentId = currentAgentId,
                    destinationMarket = "EU & Asia",
                    batchCode = batchCode,
                    contributingFarmerCodes = farmerCodes
                )
                _userMessage.value = "Batch $batchCode saved locally for $hubName."
            } catch (e: Exception) {
                _userMessage.value = "Failed to create batch: ${e.localizedMessage}"
            }
        }
    }

    private val _selectedFarmerForLogging = MutableStateFlow<FarmerEntity?>(null)
    val selectedFarmerForLogging: StateFlow<FarmerEntity?> = _selectedFarmerForLogging.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val dashboardMetrics: StateFlow<DashboardMetrics>

    /** Pending records that were saved under a different agent. They are not synced from this account. */
    val otherAccountPendingCount: StateFlow<Int>

    init {
        allFarmers = repository.allFarmers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allPracticeLogs = repository.allPracticeLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allBatches = repository.allBatches.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allSmsLogs = repository.allSmsLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        pendingFarmerSyncCount = combine(allFarmers, _currentAgentId) { farmers, agentId ->
            farmers.count {
                it.syncStatus in PENDING_STATUSES && (agentId.isBlank() || it.agentId == agentId)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        pendingPracticeSyncCount = combine(allPracticeLogs, _currentAgentId) { logs, agentId ->
            logs.count {
                it.syncStatus in PENDING_STATUSES && (agentId.isBlank() || it.agentId == agentId)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        val pendingBatchSyncCount = combine(allBatches, _currentAgentId) { batches, agentId ->
            batches.count {
                it.syncStatus in PENDING_STATUSES && (agentId.isBlank() || it.agentId == agentId)
            }
        }

        totalPendingSyncCount = combine(
            pendingFarmerSyncCount,
            pendingPracticeSyncCount,
            pendingBatchSyncCount
        ) { f, p, b ->
            f + p + b
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        dashboardMetrics = combine(
            allFarmers,
            allPracticeLogs,
            pendingFarmerSyncCount,
            pendingPracticeSyncCount,
            totalPendingSyncCount
        ) { farmers, logs, pendingFarmers, pendingLogs, pendingTotal ->
            DashboardMetrics(
                totalFarmers = farmers.size,
                pendingFarmersSync = pendingFarmers,
                totalPractices = logs.size,
                pendingPracticesSync = pendingLogs,
                totalPending = pendingTotal
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardMetrics()
        )

        otherAccountPendingCount = combine(
            allFarmers,
            allPracticeLogs,
            allBatches,
            _currentAgentId
        ) { farmers, logs, batches, agentId ->
            if (agentId.isBlank()) {
                0
            } else {
                farmers.count { it.syncStatus in PENDING_STATUSES && it.agentId != agentId } +
                    logs.count { it.syncStatus in PENDING_STATUSES && it.agentId != agentId } +
                    batches.count { it.syncStatus in PENDING_STATUSES && it.agentId != agentId }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectFarmerForLogging(farmer: FarmerEntity?) {
        _selectedFarmerForLogging.value = farmer
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setUserMessage(msg: String) {
        _userMessage.value = msg
    }

    /**
     * Agent opens the mobile app and selects "Register New Farmer."
     * Captures: Farmer name, Phone number, GPS coordinates, Farm size with local units (hectares, acres, plots),
     * Crop type, Cooperative affiliation (if any).
     * If offline, stored locally in SQLite with status pending_sync.
     */
    fun registerFarmer(enrollment: FarmerEnrollment, onResult: (Boolean) -> Unit = {}) {
        if (currentAgentId.isBlank()) {
            _userMessage.value = SIGNED_OUT_MESSAGE
            onResult(false)
            return
        }
        viewModelScope.launch {
            // One save at a time, so a quick second tap cannot race the first.
            farmerSaveLock.withLock {
                try {
                    val farmer = repository.registerFarmer(
                        fullName = enrollment.name,
                        phoneNumber = enrollment.phone,
                        state = enrollment.state,
                        lga = enrollment.lga,
                        community = enrollment.community,
                        crop = enrollment.crop,
                        farmSize = enrollment.farmSize,
                        farmSizeUnit = enrollment.farmSizeUnit,
                        latitude = enrollment.latitude,
                        longitude = enrollment.longitude,
                        cooperative = enrollment.cooperative,
                        agentId = currentAgentId,
                        gpsPolygon = enrollment.gpsPolygon,
                        photoPath = enrollment.photoPath,
                        photoConsent = enrollment.photoConsent,
                        clientUuid = enrollment.clientUuid
                    )
                    _selectedFarmerForLogging.value = farmer
                    _userMessage.value = "Farmer ${farmer.fullName} saved on this device. It will upload when you are online."
                    _selectedTab.value = AppTab.REGISTER_FARMER
                    onResult(true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _userMessage.value = "Enrollment failed: ${e.localizedMessage}"
                    onResult(false)
                }
            }
        }
    }

    /**
     * Stage 2: Practice Logging (Throughout the Season)
     * Path A: Agent-Led Logging (Primary)
     * Selects farmer by name or ID.
     * Selects practice type from icon-based menu:
     * 🌱 Planting, 💧 Irrigation, 🧪 Pesticide application, 🌿 Fertilizer application, ✂️ Harvest
     * Captures: Date (today), Product name from approved dropdown (if pesticide/fertilizer), Quantity used, Optional photo of product label.
     * Offline-first: stored locally, synced when online.
     * Backend logs practice with timestamp, GPS, and agent ID.
     */
    fun logPractice(
        farmerCode: String,
        farmerName: String,
        crop: String,
        practiceType: String,
        productName: String = "",
        activeIngredient: String = "",
        quantityUsed: Double = 1.0,
        quantityUnit: String = "Litres",
        calendarDateApplied: Long = System.currentTimeMillis(),
        verificationPhotoUri: String = "",
        liveLatitude: Double = 0.0,
        liveLongitude: Double = 0.0,
        gpsAccuracyMeters: Float = 0.0f,
        phiDays: Int = 0,
        farmerLocalId: Long = 0,
        farmerDisplayId: String = farmerCode,
        nafdacRegNo: String = "",
        gpsCoordinates: String = "",
        syncStatus: String = "pending_sync"
    ) {
        if (currentAgentId.isBlank()) {
            _userMessage.value = SIGNED_OUT_MESSAGE
            return
        }
        viewModelScope.launch {
            try {
                val log = repository.logPractice(
                    farmerCode = farmerCode,
                    farmerName = farmerName,
                    crop = crop,
                    practiceType = practiceType,
                    category = practiceType,
                    productName = productName,
                    activeIngredient = activeIngredient,
                    quantityUsed = quantityUsed,
                    quantityUnit = quantityUnit,
                    calendarDateApplied = calendarDateApplied,
                    verificationPhotoUri = verificationPhotoUri,
                    liveLatitude = liveLatitude,
                    liveLongitude = liveLongitude,
                    gpsAccuracyMeters = gpsAccuracyMeters,
                    source = "Agent Mobile App",
                    phiDays = phiDays,
                    farmerLocalId = farmerLocalId,
                    farmerDisplayId = farmerDisplayId,
                    nafdacRegNo = nafdacRegNo,
                    gpsCoordinates = gpsCoordinates,
                    agentId = currentAgentId,
                    syncStatus = syncStatus
                )
                if (log.riskLevel == "BANNED_MRL_VIOLATION") {
                    _userMessage.value = "ALERT: Input logged as BANNED MRL VIOLATION! Queued offline in SQLite."
                } else {
                    _userMessage.value = "$practiceType saved to offline SQLite (status: pending_sync) for $farmerName."
                }
                _selectedTab.value = AppTab.SYNC_RECORDS
            } catch (e: Exception) {
                _userMessage.value = "Failed to log practice: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Synchronization Engine: Pushes queued records upstream once connectivity returns.
     * Generates unique backend Farmer IDs for unsynced farmers.
     */
    fun syncAllPending() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = repository.syncAllPending()
                _userMessage.value = result.message
            } catch (e: Exception) {
                _userMessage.value = "Sync failed: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
