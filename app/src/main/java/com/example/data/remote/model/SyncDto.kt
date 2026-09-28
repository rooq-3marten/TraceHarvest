package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Pydantic v2 compatible DTO for syncing Farmer entity to FastAPI + PostgreSQL.
 * [clientUuid] serves as the idempotent key in PostgreSQL (ON CONFLICT DO NOTHING / UPDATE).
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
    val state: String,
    @field:Json(name = "lga")
    val lga: String,
    @field:Json(name = "community")
    val community: String,
    @field:Json(name = "crop")
    val crop: String,
    @field:Json(name = "farm_size_hectares")
    val farmSizeHectares: Double,
    @field:Json(name = "latitude")
    val latitude: Double,
    @field:Json(name = "longitude")
    val longitude: Double,
    @field:Json(name = "gps_polygon")
    val gpsPolygon: String? = null,
    @field:Json(name = "cooperative_name")
    val cooperativeName: String? = null,
    @field:Json(name = "agent_id")
    val agentId: String,
    @field:Json(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

/**
 * Pydantic v2 compatible DTO for syncing Practice Log entity to FastAPI + PostgreSQL.
 * [clientUuid] guarantees idempotency over unstable rural 2G/3G networks.
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
    val dateAppliedEpochMs: Long,
    @field:Json(name = "pre_harvest_interval_days")
    val preHarvestIntervalDays: Int = 0,
    @field:Json(name = "nafdac_reg_no")
    val nafdacRegNo: String? = null,
    @field:Json(name = "nafdac_approved")
    val nafdacApproved: Boolean = true,
    @field:Json(name = "gps_coordinates")
    val gpsCoordinates: String,
    @field:Json(name = "risk_level")
    val riskLevel: String = "COMPLIANT",
    @field:Json(name = "agent_id")
    val agentId: String,
    @field:Json(name = "verification_photo_uri")
    val verificationPhotoUri: String? = null
)

/**
 * Batch request sent from Android App to FastAPI endpoint POST /api/v1/sync/upstream
 */
@JsonClass(generateAdapter = true)
data class AgentBatchSyncRequest(
    @field:Json(name = "agent_id")
    val agentId: String,
    @field:Json(name = "device_timestamp_ms")
    val deviceTimestampMs: Long,
    @field:Json(name = "farmers")
    val farmers: List<FarmerSyncDto>,
    @field:Json(name = "practices")
    val practices: List<PracticeLogSyncDto>
)

/**
 * Response received from FastAPI endpoint POST /api/v1/sync/upstream
 */
@JsonClass(generateAdapter = true)
data class AgentBatchSyncResponse(
    @field:Json(name = "status")
    val status: String = "success",
    @field:Json(name = "synced_farmers_count")
    val syncedFarmersCount: Int = 0,
    @field:Json(name = "synced_practices_count")
    val syncedPracticesCount: Int = 0,
    @field:Json(name = "assigned_farmer_ids")
    val assignedFarmerIds: Map<String, String> = emptyMap(), // Maps clientUuid -> Official Server ID (e.g. TH-KAN-2026-1048)
    @field:Json(name = "server_timestamp_ms")
    val serverTimestampMs: Long = System.currentTimeMillis(),
    @field:Json(name = "message")
    val message: String = "Sync completed successfully"
)
