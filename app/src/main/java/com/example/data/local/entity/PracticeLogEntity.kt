package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_logs")
data class PracticeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val farmerCode: String,
    val farmerName: String,
    val crop: String,
    // Icon-based Practice Types: 🌱 Planting, 💧 Irrigation, 🧪 Pesticide application, 🌿 Fertilizer application, ✂️ Harvest
    val practiceType: String = "🧪 Pesticide application",
    val category: String = practiceType,
    val practiceDetails: String = "",
    val productName: String = "", // Selected from dropdown of approved products (if pesticide/fertilizer)
    val activeIngredient: String = "",
    val dosage: String = "",
    val quantityUsed: Double = 1.0,
    val quantityUnit: String = "Litres",
    val dateApplied: Long = System.currentTimeMillis(), // Defaults to today
    val preHarvestIntervalDays: Int = 0,
    val source: String = "Agent Mobile App",
    val riskLevel: String = "COMPLIANT",
    val riskNotes: String = "",
    val isSynced: Boolean = false,
    val farmerLocalId: Long = 0,
    val farmerDisplayId: String = farmerCode,
    val nafdacRegNo: String = "",
    val nafdacApproved: Boolean = true,
    val gpsCoordinates: String = "",
    // Auto GPS from phone hardware
    val liveLatitude: Double = 0.0,
    val liveLongitude: Double = 0.0,
    val gpsAccuracyMeters: Float = 0.0f,
    val gpsFixStatus: String = "GPS_ACQUIRED",
    // Optional photo of product label (via Android Photo Picker)
    val verificationPhotoUri: String = "",
    val photoVerificationType: String = "CONTAINER_LABEL",
    // Offline-first SQLite status & Backend logging attributes
    val syncStatus: String = "pending_sync", // "pending_sync", "synced"
    val agentId: String = "AGENT-NG-042",
    val serverLoggedTimestamp: Long = System.currentTimeMillis(),
    val dosageQuantity: Double = quantityUsed,
    val dosageUnit: String = quantityUnit,
    val calendarDateApplied: Long = dateApplied,
    val clientUuid: String = java.util.UUID.randomUUID().toString(),
    val farmerClientUuid: String = ""
)
