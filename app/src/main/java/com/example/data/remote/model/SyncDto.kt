package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Farmer DTO matching backend POST /farmers and bulk POST /sync/batch
 */
@JsonClass(generateAdapter = true)
data class FarmerSyncDto(
    @field:Json(name = "client_uuid")
    val clientUuid: String,
    @field:Json(name = "full_name")
    val fullName: String,
    @field:Json(name = "phone_number")
    val phoneNumber: String,
    @field:Json(name = "state")
    val state: String = "Kano",
    @field:Json(name = "lga")
    val lga: String = "Dambatta",
    @field:Json(name = "community")
    val community: String = "",
    @field:Json(name = "crop")
    val crop: String,
    @field:Json(name = "farm_size_hectares")
    val farmSizeHectares: Double = 1.0,
    @field:Json(name = "latitude")
    val latitude: Double,
    @field:Json(name = "longitude")
    val longitude: Double,
    @field:Json(name = "gps_polygon")
    val gpsPolygon: String? = null,
    @field:Json(name = "cooperative_name")
    val cooperativeName: String? = null,
    @field:Json(name = "agent_id")
    val agentId: String = "AGENT-NG-042",
    @field:Json(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Practice Log DTO matching backend POST /practice-logs
 */
@JsonClass(generateAdapter = true)
data class PracticeLogSyncDto(
    @field:Json(name = "client_uuid")
    val clientUuid: String,
    @field:Json(name = "farmer_client_uuid")
    val farmerClientUuid: String,
    @field:Json(name = "farmer_code")
    val farmerCode: String,
    @field:Json(name = "practice_type")
    val practiceType: String,
    @field:Json(name = "product_name")
    val productName: String? = null,
    @field:Json(name = "active_ingredient")
    val activeIngredient: String? = null,
    @field:Json(name = "dosage")
    val dosage: String? = null,
    @field:Json(name = "quantity_used")
    val quantityUsed: Double? = null,
    @field:Json(name = "quantity_unit")
    val quantityUnit: String? = null,
    @field:Json(name = "date_applied_epoch_ms")
    val dateAppliedEpochMs: Long = System.currentTimeMillis(),
    @field:Json(name = "pre_harvest_interval_days")
    val preHarvestIntervalDays: Int = 0,
    @field:Json(name = "nafdac_reg_no")
    val nafdacRegNo: String? = null,
    @field:Json(name = "nafdac_approved")
    val nafdacApproved: Boolean = true,
    @field:Json(name = "gps_coordinates")
    val gpsCoordinates: String = "",
    @field:Json(name = "risk_level")
    val riskLevel: String = "COMPLIANT",
    @field:Json(name = "agent_id")
    val agentId: String = "AGENT-NG-042",
    @field:Json(name = "verification_photo_uri")
    val verificationPhotoUri: String? = null
)

/**
 * Batch DTO matching backend POST /batches
 */
@JsonClass(generateAdapter = true)
data class BatchSyncDto(
    @field:Json(name = "client_uuid")
    val clientUuid: String,
    @field:Json(name = "batch_code")
    val batchCode: String,
    @field:Json(name = "crop")
    val crop: String = "Sesame",
    @field:Json(name = "total_quantity")
    val totalQuantity: Double = 0.0,
    @field:Json(name = "quality_grade")
    val qualityGrade: String = "Grade A Export Ready",
    @field:Json(name = "aggregation_gps_lat")
    val aggregationGpsLat: Double? = null,
    @field:Json(name = "aggregation_gps_lng")
    val aggregationGpsLng: Double? = null,
    @field:Json(name = "agent_id")
    val agentId: String = "AGENT-NG-042",
    @field:Json(name = "farmer_codes")
    val farmerCodes: List<String> = emptyList(),
    @field:Json(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * General pending record wrapper for POST /sync/batch
 */
@JsonClass(generateAdapter = true)
data class PendingRecordDto(
    @field:Json(name = "id")
    val id: String,
    @field:Json(name = "entity_type")
    val entityType: String,
    @field:Json(name = "entity_id")
    val entityId: String,
    @field:Json(name = "payload")
    val payload: String,
    @field:Json(name = "created_at")
    val createdAt: Long
)

/**
 * Batch request sent from Android App to FastAPI endpoint POST /sync/batch or POST /api/v1/sync/upstream
 */
@JsonClass(generateAdapter = true)
data class AgentBatchSyncRequest(
    @field:Json(name = "agent_id")
    val agentId: String = "AGENT-NG-042",
    @field:Json(name = "device_timestamp_ms")
    val deviceTimestampMs: Long = System.currentTimeMillis(),
    @field:Json(name = "farmers")
    val farmers: List<FarmerSyncDto> = emptyList(),
    @field:Json(name = "practices")
    val practices: List<PracticeLogSyncDto> = emptyList(),
    @field:Json(name = "batches")
    val batches: List<BatchSyncDto> = emptyList(),
    @field:Json(name = "records")
    val records: List<PendingRecordDto> = emptyList()
)

/**
 * Single sync item outcome from backend
 */
@JsonClass(generateAdapter = true)
data class SyncRecordResultDto(
    @field:Json(name = "id")
    val id: String,
    @field:Json(name = "status")
    val status: String,
    @field:Json(name = "server_id")
    val serverId: String? = null,
    @field:Json(name = "code")
    val code: String? = null,
    @field:Json(name = "error")
    val error: String? = null
)

/**
 * Response received from FastAPI endpoint POST /sync/batch
 */
@JsonClass(generateAdapter = true)
data class AgentBatchSyncResponse(
    @field:Json(name = "status")
    val status: String = "success",
    @field:Json(name = "synced_farmers_count")
    val syncedFarmersCount: Int = 0,
    @field:Json(name = "synced_practices_count")
    val syncedPracticesCount: Int = 0,
    @field:Json(name = "synced_batches_count")
    val syncedBatchesCount: Int = 0,
    @field:Json(name = "assigned_farmer_ids")
    val assignedFarmerIds: Map<String, String> = emptyMap(),
    @field:Json(name = "results")
    val results: List<SyncRecordResultDto> = emptyList(),
    @field:Json(name = "server_timestamp_ms")
    val serverTimestampMs: Long = System.currentTimeMillis(),
    @field:Json(name = "message")
    val message: String = "Sync completed successfully"
)

/**
 * Response received from GET /sync/status/{agent_id}
 */
@JsonClass(generateAdapter = true)
data class SyncStatusResponse(
    @field:Json(name = "agent_id")
    val agentId: String,
    @field:Json(name = "last_sync_at")
    val lastSyncAt: Long? = null,
    @field:Json(name = "pending_count")
    val pendingCount: Int = 0,
    @field:Json(name = "status")
    val status: String = "ACTIVE"
)

/**
 * Authentication DTOs
 */
@JsonClass(generateAdapter = true)
data class AuthLoginRequest(
    @field:Json(name = "username")
    val username: String,
    @field:Json(name = "password")
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
    @field:Json(name = "access_token")
    val accessToken: String,
    @field:Json(name = "refresh_token")
    val refreshToken: String? = null,
    @field:Json(name = "token_type")
    val tokenType: String = "bearer",
    @field:Json(name = "expires_in")
    val expiresIn: Long = 3600
)

/**
 * Agent Self-Registration Request
 * Note: App never sends a 'status' field. Status is set exclusively by the backend.
 */
@JsonClass(generateAdapter = true)
data class AgentSignUpRequest(
    @field:Json(name = "full_name")
    val fullName: String,
    @field:Json(name = "email")
    val email: String,
    @field:Json(name = "association")
    val association: String,
    @field:Json(name = "location")
    val location: String,
    @field:Json(name = "phone_number")
    val phoneNumber: String,
    @field:Json(name = "password")
    val password: String
)

/**
 * Agent Profile & Approval Status response from Backend
 * Status values: 'pending', 'approved', 'rejected', 'suspended'
 */
@JsonClass(generateAdapter = true)
data class AgentStatusResponse(
    @field:Json(name = "agent_id")
    val agentId: String? = null,
    @field:Json(name = "full_name")
    val fullName: String = "",
    @field:Json(name = "email")
    val email: String = "",
    @field:Json(name = "association")
    val association: String = "",
    @field:Json(name = "location")
    val location: String = "",
    @field:Json(name = "phone_number")
    val phoneNumber: String = "",
    @field:Json(name = "status")
    val status: String = "pending",
    @field:Json(name = "rejection_reason")
    val rejectionReason: String? = null,
    @field:Json(name = "access_token")
    val accessToken: String? = null
)

/**
 * Resubmission Request when status is 'rejected'
 */
@JsonClass(generateAdapter = true)
data class AgentResubmitRequest(
    @field:Json(name = "full_name")
    val fullName: String,
    @field:Json(name = "association")
    val association: String,
    @field:Json(name = "location")
    val location: String,
    @field:Json(name = "phone_number")
    val phoneNumber: String,
    @field:Json(name = "password")
    val password: String? = null
)

