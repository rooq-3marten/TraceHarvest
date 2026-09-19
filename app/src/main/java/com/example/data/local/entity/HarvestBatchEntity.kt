package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "harvest_batches")
data class HarvestBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchCode: String,
    val crop: String,
    val farmerCode: String,
    val farmerName: String,
    val region: String,
    val harvestDate: Long = System.currentTimeMillis(),
    val bagCount: Int,
    val netWeightKg: Double,
    val moisturePercent: Double, // Target < 10% to prevent aflatoxin
    val foreignMatterPercent: Double, // Target < 2%
    val grade: String, // "Grade A Export Ready", "Grade B Local Processing", "Non-Compliant High Risk"
    val mrlStatus: String, // "PASSED_SPS", "PENDING_PHI", "VIOLATION_BLOCKED"
    val aflatoxinStatus: String, // "SAFE (<4 ppb)", "ELEVATED (4-10 ppb)", "UNSAFE (>10 ppb)"
    val phiDaysObserved: Int,
    val destinationMarket: String = "EU & United Kingdom", // "EU & United Kingdom", "Türkiye", "Japan", "United States"
    val blockchainHash: String,
    val blockchainTxId: String,
    val isBlockchainAnchored: Boolean = false,
    val isFlaggedForRejection: Boolean = false,
    val rejectionReason: String? = null,
    val qrPayload: String,
    val isSynced: Boolean = true
)
