package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    REGISTER_FARMER("Register New Farmer"),
    LOG_PRACTICE("Practice Logging"),
    SYNC_RECORDS("Sync & Records")
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

    val currentAgentId = "AGENT-NG-042"

    private val repository: ITraceHarvestRepository = repositoryInstance 
        ?: run {
            val db = AppDatabase.getDatabase(application)
            TraceHarvestRepository(db.traceHarvestDao(), db.pendingSyncDao())
        }

    val allFarmers: StateFlow<List<FarmerEntity>>
    val allPracticeLogs: StateFlow<List<PracticeLogEntity>>
    val allBatches: StateFlow<List<HarvestBatchEntity>>
    val allSmsLogs: StateFlow<List<SmsLogEntity>>

    val pendingFarmerSyncCount: StateFlow<Int>
    val pendingPracticeSyncCount: StateFlow<Int>
    val totalPendingSyncCount: StateFlow<Int>

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _selectedTab = MutableStateFlow(AppTab.REGISTER_FARMER)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _selectedFarmerForLogging = MutableStateFlow<FarmerEntity?>(null)
    val selectedFarmerForLogging: StateFlow<FarmerEntity?> = _selectedFarmerForLogging.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val dashboardMetrics: StateFlow<DashboardMetrics>

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

        pendingFarmerSyncCount = repository.pendingFarmerSyncCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        pendingPracticeSyncCount = repository.pendingPracticeSyncCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        totalPendingSyncCount = combine(pendingFarmerSyncCount, pendingPracticeSyncCount) { f, p ->
            f + p
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        dashboardMetrics = combine(allFarmers, allPracticeLogs, totalPendingSyncCount) { farmers, logs, pending ->
            DashboardMetrics(
                totalFarmers = farmers.size,
                pendingFarmersSync = farmers.count { it.syncStatus == "pending_sync" },
                totalPractices = logs.size,
                pendingPracticesSync = logs.count { it.syncStatus == "pending_sync" },
                totalPending = pending
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardMetrics()
        )

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
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
    fun registerFarmer(
        fullName: String,
        phoneNumber: String,
        state: String,
        lga: String,
        community: String,
        crop: String,
        farmSize: Double,
        farmSizeUnit: String,
        latitude: Double,
        longitude: Double,
        cooperative: String,
        isOfflineMode: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val farmer = repository.registerFarmer(
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    state = state,
                    lga = lga,
                    community = community,
                    crop = crop,
                    farmSize = farmSize,
                    farmSizeUnit = farmSizeUnit,
                    latitude = latitude,
                    longitude = longitude,
                    cooperative = cooperative,
                    agentId = currentAgentId,
                    isOfflineMode = isOfflineMode
                )
                _selectedFarmerForLogging.value = farmer
                if (isOfflineMode) {
                    _userMessage.value = "Farmer ${farmer.fullName} stored locally in SQLite (status: pending_sync). Will sync upstream once connectivity returns."
                } else {
                    _userMessage.value = "Farmer ${farmer.fullName} enrolled with Backend Farmer ID: ${farmer.farmerCode}."
                }
                _selectedTab.value = AppTab.LOG_PRACTICE
            } catch (e: Exception) {
                _userMessage.value = "Enrollment failed: ${e.localizedMessage}"
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
