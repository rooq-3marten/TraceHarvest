package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.data.local.entity.SmsLogEntity
import com.example.data.repository.TraceHarvestRepository
import com.example.data.gateway.GatewayConfig
import com.example.data.gateway.GatewayProviderType
import com.example.data.gateway.UssdEngine
import com.example.data.gateway.UssdRequest
import com.example.data.gateway.UssdResponse
import com.example.data.gateway.SmsEngine
import com.example.data.security.ExporterRole
import com.example.data.security.JwtToken
import com.example.data.security.SecurityEngine
import com.example.data.security.SecurityEngineConfig
import com.example.data.security.SecurityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Batches & SPS"),
    ENROLL_FARMER("Enroll Farmer"),
    LOG_PRACTICE("Log Inputs"),
    AGGREGATE_BATCH("Tag Harvest"),
    USSD_SMS_GATEWAY("USSD / SMS"),
    EXPORTER_ANALYTICS("Port & Risk"),
    API_SECURITY("API Security & WAF")
}

data class DashboardMetrics(
    val totalBatches: Int = 0,
    val exportReadyCount: Int = 0,
    val blockedRiskCount: Int = 0,
    val totalFarmers: Int = 0,
    val totalMetricTons: Double = 0.0,
    val estimatedDollarsSaved: Double = 0.0,
    val complianceRatePercent: Double = 0.0
)

class TraceHarvestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TraceHarvestRepository

    val allFarmers: StateFlow<List<FarmerEntity>>
    val allPracticeLogs: StateFlow<List<PracticeLogEntity>>
    val allBatches: StateFlow<List<HarvestBatchEntity>>
    val allSmsLogs: StateFlow<List<SmsLogEntity>>

    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _selectedBatchDetail = MutableStateFlow<HarvestBatchEntity?>(null)
    val selectedBatchDetail: StateFlow<HarvestBatchEntity?> = _selectedBatchDetail.asStateFlow()

    private val _selectedCropFilter = MutableStateFlow("All")
    val selectedCropFilter: StateFlow<String> = _selectedCropFilter.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val dashboardMetrics: StateFlow<DashboardMetrics>

    // Gateway Integration Engine & Configuration
    private val _gatewayConfig = MutableStateFlow(GatewayConfig())
    val gatewayConfig: StateFlow<GatewayConfig> = _gatewayConfig.asStateFlow()

    private val ussdEngine = com.example.data.gateway.UssdEngine()
    private val smsEngine = com.example.data.gateway.SmsEngine()

    // API Security & Hardening Engine (MCP Security, Wallarm API Firewall, GuardianWAF)
    private val securityEngine = SecurityEngine()
    private val _securityConfig = MutableStateFlow(securityEngine.config)
    val securityConfig: StateFlow<SecurityEngineConfig> = _securityConfig.asStateFlow()

    private val _activeJwt = MutableStateFlow(
        securityEngine.generateJwt(ExporterRole.EU_INSPECTOR, "Port of Rotterdam (NVWA)", "usr_rotterdam_nvwa_01")
    )
    val activeJwt: StateFlow<JwtToken> = _activeJwt.asStateFlow()

    private val _remainingTokens = MutableStateFlow(securityEngine.getRemainingTokens())
    val remainingTokens: StateFlow<Int> = _remainingTokens.asStateFlow()

    private val _securityEvents = MutableStateFlow(securityEngine.eventLogs)
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEvents.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TraceHarvestRepository(db.traceHarvestDao())

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

        dashboardMetrics = combine(allBatches, allFarmers) { batches, farmers ->
            val total = batches.size
            val exportReady = batches.count { it.mrlStatus == "PASSED_SPS" && !it.isFlaggedForRejection }
            val blocked = batches.count { it.isFlaggedForRejection || it.mrlStatus == "VIOLATION_BLOCKED" }
            val weightKg = batches.sumOf { it.netWeightKg }
            val weightMt = weightKg / 1000.0
            // $42,000 avg cost prevented per intercepted non-compliant batch before shipping
            val savedUsd = blocked * 42000.0
            val rate = if (total > 0) (exportReady.toDouble() / total) * 100.0 else 100.0

            DashboardMetrics(
                totalBatches = total,
                exportReadyCount = exportReady,
                blockedRiskCount = blocked,
                totalFarmers = farmers.size,
                totalMetricTons = weightMt,
                estimatedDollarsSaved = savedUsd,
                complianceRatePercent = rate
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardMetrics()
        )

        // Seed initial Nigerian export baseline data if needed
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setCropFilter(crop: String) {
        _selectedCropFilter.value = crop
    }

    fun selectBatchDetail(batch: HarvestBatchEntity?) {
        _selectedBatchDetail.value = batch
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setUserMessage(msg: String) {
        _userMessage.value = msg
    }

    fun registerFarmer(
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
    ) {
        viewModelScope.launch {
            try {
                val farmer = repository.registerFarmer(
                    fullName, phoneNumber, state, lga, community, crop,
                    farmSizeHectares, latitude, longitude, cooperative
                )
                _userMessage.value = "Farmer ${farmer.fullName} enrolled (${farmer.farmerCode})! SMS notification dispatched."
                _selectedTab.value = AppTab.DASHBOARD
            } catch (e: Exception) {
                _userMessage.value = "Enrollment failed: ${e.localizedMessage}"
            }
        }
    }

    fun logPractice(
        farmerCode: String,
        farmerName: String,
        crop: String,
        category: String,
        productName: String,
        activeIngredient: String,
        dosage: String,
        source: String,
        phiDays: Int
    ) {
        viewModelScope.launch {
            try {
                val log = repository.logPractice(
                    farmerCode, farmerName, crop, category, productName,
                    activeIngredient, dosage, source, phiDays
                )
                if (log.riskLevel == "BANNED_MRL_VIOLATION") {
                    _userMessage.value = "ALERT: Input logged as BANNED MRL VIOLATION! Associated batches will be flagged."
                } else {
                    _userMessage.value = "Practice logged for $farmerName. Verification SMS dispatched."
                }
                _selectedTab.value = AppTab.DASHBOARD
            } catch (e: Exception) {
                _userMessage.value = "Failed to log practice: ${e.localizedMessage}"
            }
        }
    }

    fun createHarvestBatch(
        farmerCode: String,
        farmerName: String,
        crop: String,
        region: String,
        bagCount: Int,
        netWeightKg: Double,
        moisturePercent: Double,
        foreignMatterPercent: Double,
        destinationMarket: String
    ) {
        viewModelScope.launch {
            try {
                val batch = repository.createHarvestBatch(
                    farmerCode, farmerName, crop, region, bagCount,
                    netWeightKg, moisturePercent, foreignMatterPercent, destinationMarket
                )
                _userMessage.value = "Batch ${batch.batchCode} aggregated! QR Code & Hash generated."
                _selectedBatchDetail.value = batch
                _selectedTab.value = AppTab.DASHBOARD
            } catch (e: Exception) {
                _userMessage.value = "Failed to create batch: ${e.localizedMessage}"
            }
        }
    }

    fun toggleFlagBatch(batch: HarvestBatchEntity) {
        viewModelScope.launch {
            try {
                repository.toggleFlagBatch(
                    batch.id,
                    batch.isFlaggedForRejection,
                    "Flagged by Exporter Quality Auditor: Pending lab residue re-test"
                )
                _userMessage.value = if (!batch.isFlaggedForRejection) {
                    "Batch ${batch.batchCode} FLAGGED for pre-shipment quarantine."
                } else {
                    "Batch ${batch.batchCode} cleared of flags."
                }
                // Update currently open detail modal if same batch
                if (_selectedBatchDetail.value?.id == batch.id) {
                    _selectedBatchDetail.value = batch.copy(
                        isFlaggedForRejection = !batch.isFlaggedForRejection,
                        rejectionReason = if (!batch.isFlaggedForRejection) "Flagged by Exporter Quality Auditor: Pending lab residue re-test" else null
                    )
                }
            } catch (e: Exception) {
                _userMessage.value = "Action failed: ${e.localizedMessage}"
            }
        }
    }

    fun anchorBatchToBlockchain(batch: HarvestBatchEntity) {
        viewModelScope.launch {
            try {
                repository.anchorBatch(batch.id)
                _userMessage.value = "Batch ${batch.batchCode} anchored to Polygon blockchain ledger with cryptographic proof."
                if (_selectedBatchDetail.value?.id == batch.id) {
                    _selectedBatchDetail.value = batch.copy(isBlockchainAnchored = true)
                }
            } catch (e: Exception) {
                _userMessage.value = "Blockchain anchor failed: ${e.localizedMessage}"
            }
        }
    }

    fun simulateFarmerSms(phone: String, farmerName: String, text: String, messageType: String) {
        viewModelScope.launch {
            try {
                repository.simulateIncomingFarmerSms(phone, farmerName, text, messageType)
                _userMessage.value = "Farmer reply received & processed via SMS Gateway."
            } catch (e: Exception) {
                _userMessage.value = "Simulation failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateGatewayProvider(provider: com.example.data.gateway.GatewayProviderType) {
        _gatewayConfig.value = _gatewayConfig.value.copy(
            provider = provider,
            shortCode = if (provider == com.example.data.gateway.GatewayProviderType.TELKOSH) "7006" else "34461",
            ussdServiceCode = provider.defaultUssdCode
        )
        _userMessage.value = "Active gateway switched to ${provider.displayName}."
    }

    fun updateGatewayConfig(config: com.example.data.gateway.GatewayConfig) {
        _gatewayConfig.value = config
        _userMessage.value = "Gateway configuration updated (${config.provider.displayName})."
    }

    fun executeUssdHop(hopsText: String, phone: String): com.example.data.gateway.UssdResponse {
        val request = com.example.data.gateway.UssdRequest(
            sessionId = "ATUid_" + System.currentTimeMillis().toString().takeLast(8),
            serviceCode = _gatewayConfig.value.ussdServiceCode,
            phoneNumber = phone,
            text = hopsText
        )
        return ussdEngine.processRequest(request, allFarmers.value, allBatches.value)
    }

    fun dispatchOutboundSms(toPhone: String, farmerName: String, text: String, messageType: String) {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                db.traceHarvestDao().insertSmsLog(
                    SmsLogEntity(
                        farmerPhone = toPhone,
                        farmerName = farmerName,
                        direction = "OUTBOUND",
                        messageType = messageType,
                        content = text,
                        timestamp = System.currentTimeMillis(),
                        status = "DELIVERED"
                    )
                )
                _userMessage.value = "SMS dispatched via ${_gatewayConfig.value.provider.displayName} to $toPhone"
            } catch (e: Exception) {
                _userMessage.value = "Failed to send SMS: ${e.localizedMessage}"
            }
        }
    }

    fun processInboundFarmerReply(fromPhone: String, text: String) {
        viewModelScope.launch {
            try {
                val farmer = allFarmers.value.find { it.phoneNumber.replace(" ", "") == fromPhone.replace(" ", "") }
                val farmerName = farmer?.fullName ?: "Farmer"

                val result = smsEngine.processInboundMessage(fromPhone, text, allFarmers.value, allBatches.value)

                val db = AppDatabase.getDatabase(getApplication())
                // Inbound message
                db.traceHarvestDao().insertSmsLog(
                    SmsLogEntity(
                        farmerPhone = fromPhone,
                        farmerName = farmerName,
                        direction = "INBOUND",
                        messageType = "FARMER_KEYWORD_REPLY",
                        content = result.replyText,
                        timestamp = System.currentTimeMillis(),
                        status = "PROCESSED"
                    )
                )

                // Automated reply from Gateway
                db.traceHarvestDao().insertSmsLog(
                    SmsLogEntity(
                        farmerPhone = fromPhone,
                        farmerName = farmerName,
                        direction = "OUTBOUND",
                        messageType = "AUTO_RESPONSE",
                        content = result.automatedResponse,
                        timestamp = System.currentTimeMillis() + 800, // 800ms telco turnaround
                        status = "DELIVERED"
                    )
                )

                _userMessage.value = "Inbound SMS processed: ${result.actionTaken}"
            } catch (e: Exception) {
                _userMessage.value = "Error processing SMS reply: ${e.localizedMessage}"
            }
        }
    }

    fun generateJwtToken(role: ExporterRole, organization: String, subject: String) {
        val token = securityEngine.generateJwt(role, organization, subject)
        _activeJwt.value = token
        _userMessage.value = "New JWT token issued for ${role.roleName} (${organization})"
    }

    fun simulateSecurityRequest(
        endpoint: String,
        method: String,
        clientIp: String,
        clientIdentity: String,
        withJwt: Boolean,
        payload: String
    ) {
        val token = if (withJwt) _activeJwt.value else null
        val event = securityEngine.simulateRequest(
            endpoint = endpoint,
            method = method,
            clientIp = clientIp,
            clientIdentity = clientIdentity,
            jwtToken = token,
            payload = payload
        )
        _securityEvents.value = securityEngine.eventLogs
        _remainingTokens.value = securityEngine.getRemainingTokens()

        val statusName = event.outcome.name.substringAfter('_')
        _userMessage.value = "${event.layer.title}: ${event.httpStatus} $statusName"
    }

    fun resetSecurityRateLimits() {
        securityEngine.resetRateLimitTokens()
        _remainingTokens.value = securityEngine.getRemainingTokens()
        _userMessage.value = "Rate limit tokens restored to ${securityEngine.config.exporterRateLimitRpm} req/min."
    }

    fun depleteSecurityRateLimits() {
        securityEngine.depleteRateLimitTokens()
        _remainingTokens.value = 0
        _userMessage.value = "Rate limit tokens manually depleted to 0. Next request will yield 429 Too Many Requests."
    }
}

