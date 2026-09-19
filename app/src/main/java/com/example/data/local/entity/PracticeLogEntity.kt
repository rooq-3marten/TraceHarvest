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
    val category: String, // "Pesticide Application", "Fertilizer / Soil", "Drying & Aflatoxin Control", "Harvest Preparation"
    val practiceDetails: String,
    val productName: String,
    val activeIngredient: String,
    val dosage: String,
    val dateApplied: Long = System.currentTimeMillis(),
    val preHarvestIntervalDays: Int = 14,
    val source: String = "Agent Mobile App", // "Agent Mobile App", "USSD (*384*748#)", "SMS Reply"
    val riskLevel: String = "COMPLIANT", // "COMPLIANT", "CAUTION", "BANNED_MRL_VIOLATION"
    val riskNotes: String = "",
    val isSynced: Boolean = true
)
