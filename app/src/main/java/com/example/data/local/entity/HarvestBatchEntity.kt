package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "harvest_batches")
data class HarvestBatchEntity(
    @PrimaryKey
    val localId: String = UUID.randomUUID().toString(),
    val serverId: String? = null,
    val batchCode: String,
    val agentId: String = "AGENT-NG-042",
    val totalQuantity: Double = 1.0,
    val qualityGrade: String = "Grade A Export Ready",
    val aggregationGpsLat: Double? = null,
    val aggregationGpsLng: Double? = null,
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),

    // Extra consignment fields
    val id: Long = 0,
    val crop: String = "Sesame",
    val farmerCode: String = "",
    val farmerName: String = "",
    val region: String = "Kano",
    val harvestDate: Long = createdAt,
    val bagCount: Int = 1,
    val netWeightKg: Double = totalQuantity,
    val moisturePercent: Double = 7.5,
    val foreignMatterPercent: Double = 1.0,
    val grade: String = qualityGrade,
    val mrlStatus: String = "PASSED_SPS",
    val aflatoxinStatus: String = "SAFE (<4 ppb)",
    val phiDaysObserved: Int = 14,
    val destinationMarket: String = "EU & United Kingdom",
    val blockchainHash: String = "",
    val blockchainTxId: String = "",
    val isBlockchainAnchored: Boolean = false,
    val isFlaggedForRejection: Boolean = false,
    val rejectionReason: String? = null,
    val qrPayload: String = "",
    val isSynced: Boolean = syncStatus == "SYNCED" || syncStatus == "synced"
)
