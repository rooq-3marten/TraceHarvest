package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Pydantic v2 compatible DTO for syncing Farmer entity to FastAPI + PostgreSQL.
 * [clientUuid] serves as the idempotent key in PostgreSQL (ON CONFLICT DO NOTHING / UPDATE).
 */
@JsonClass(generateAdapter = true)
data class FarmerSyncDto(
    @Json(name = "client_uuid")
    val clientUuid: String,
    @Json(name = "full_name")
    val fullName: String,
    @Json(name = "phone_number")
    val phoneNumber: String,
    @Json(name = "state")
    val state: String,
    @Json(name = "lga")
    val lga: String,
    @Json(name = "community")
    val community: String,
    @Json(name = "crop")
    val crop: String,
    @Json(name = "farm_size_hectares")
    val farmSizeHectares: Double,
    @Json(name = "latitude")
    val latitude: Double,
    @Json(name = "longitude")
    val longitude: Double,
    @Json(name = "gps_polygon")
    val gpsPolygon: String? = null,
    @Json(name = "cooperative_name")
    val cooperativeName: String? = null,
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

/**
 * Pydantic v2 compatible DTO for syncing Practice Log entity to FastAPI + PostgreSQL.
 * [clientUuid] guarantees idempotency over unstable rural 2G/3G networks.
 */
@JsonClass(generateAdapter = true)
data class PracticeLogSyncDto(
    @Json(name = "client_uuid")
    val clientUuid: String,
    @Json(name = "farmer_client_uuid")
    val farmerClientUuid: String,
    @Json(name = "farmer_code")
    val farmerCode: String,
    @Json(name = "practice_type")
    val practiceType: String,
    @Json(name = "product_name")
    val productName: String? = null,
    @Json(name = "active_ingredient")
    val activeIngredient: String? = null,
    @Json(name = "dosage")
    val dosage: String? = null,
    @Json(name = "quantity_used")
    val quantityUsed: Double? = null,
    @Json(name = "quantity_unit")
    val quantityUnit: String? = null,
    @Json(name = "date_applied_epoch_ms")
    val dateAppliedEpochMs: Long,
    @Json(name = "pre_harvest_interval_days")
    val preHarvestIntervalDays: Int = 0,
    @Json(name = "nafdac_reg_no")
    val nafdacRegNo: String? = null,
    @Json(name = "nafdac_approved")
    val nafdacApproved: Boolean = true,
    @Json(name = "gps_coordinates")
    val gpsCoordinates: String,
    @Json(name = "risk_level")
    val riskLevel: String = "COMPLIANT",
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "verification_photo_uri")
    val verificationPhotoUri: String? = null
)

/**
 * Batch request sent from Android App to FastAPI endpoint POST /api/v1/sync/upstream
 */
@JsonClass(generateAdapter = true)
data class AgentBatchSyncRequest(
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "device_timestamp_ms")
    val deviceTimestampMs: Long,
    @Json(name = "farmers")
    val farmers: List<FarmerSyncDto>,
    @Json(name = "practices")
    val practices: List<PracticeLogSyncDto>
)

/**
 * Response received from FastAPI endpoint POST /api/v1/sync/upstream
 */
@JsonClass(generateAdapter = true)
data class AgentBatchSyncResponse(
    @Json(name = "status")
    val status: String = "success",
    @Json(name = "synced_farmers_count")
    val syncedFarmersCount: Int = 0,
    @Json(name = "synced_practices_count")
    val syncedPracticesCount: Int = 0,
    @Json(name = "assigned_farmer_ids")
    val assignedFarmerIds: Map<String, String> = emptyMap(), // Maps clientUuid -> Official Server ID (e.g. TH-KAN-2026-1048)
    @Json(name = "server_timestamp_ms")
    val serverTimestampMs: Long = System.currentTimeMillis(),
    @Json(name = "message")
    val message: String = "Sync completed successfully"
)
