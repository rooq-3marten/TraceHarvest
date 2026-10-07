package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "practice_logs")
data class PracticeLogEntity(
    @PrimaryKey
    val localId: String = UUID.randomUUID().toString(),
    val serverId: String? = null,
    val farmerId: String? = null,
    val practiceType: String = "🧪 Pesticide application",
    val productName: String? = null,
    val quantity: Double = 1.0,
    val logDate: Long = System.currentTimeMillis(),
    val source: String = "agent", // "agent", "ussd"
    val agentId: String = "AGENT-NG-042",
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),

    // Extra GAP audit and NAFDAC fields
    val id: Long = 0,
    val farmerCode: String = "",
    val farmerName: String = "",
    val crop: String = "",
    val category: String = practiceType,
    val practiceDetails: String = "",
    val activeIngredient: String = "",
    val dosage: String = "",
    val quantityUsed: Double = quantity,
    val quantityUnit: String = "Litres",
    val dateApplied: Long = logDate,
    val preHarvestIntervalDays: Int = 0,
    val riskLevel: String = "COMPLIANT",
    val riskNotes: String = "",
    val isSynced: Boolean = syncStatus == "SYNCED" || syncStatus == "synced",
    val farmerLocalId: Long = 0,
    val farmerDisplayId: String = farmerCode,
    val nafdacRegNo: String = "",
    val nafdacApproved: Boolean = true,
    val gpsCoordinates: String = "",
    val liveLatitude: Double = 0.0,
    val liveLongitude: Double = 0.0,
    val gpsAccuracyMeters: Float = 0.0f,
    val gpsFixStatus: String = "GPS_ACQUIRED",
    val verificationPhotoUri: String = "",
    val photoVerificationType: String = "CONTAINER_LABEL",
    val serverLoggedTimestamp: Long = System.currentTimeMillis(),
    val dosageQuantity: Double = quantity,
    val dosageUnit: String = quantityUnit,
    val calendarDateApplied: Long = dateApplied,
    val clientUuid: String = localId,
    val farmerClientUuid: String = farmerId ?: ""
)
