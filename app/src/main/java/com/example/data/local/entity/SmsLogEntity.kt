package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val farmerPhone: String,
    val farmerName: String,
    val direction: String, // "OUTBOUND" (from Platform) or "INBOUND" (from Farmer via SMS/USSD)
    val messageType: String, // "ENROLLMENT", "PRACTICE_QUERY", "PRACTICE_REPLY", "BATCH_CONFIRM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "DELIVERED" // "DELIVERED", "CONFIRMED", "PENDING"
)
