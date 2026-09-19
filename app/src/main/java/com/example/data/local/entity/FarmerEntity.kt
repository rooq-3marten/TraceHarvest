package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmers")
data class FarmerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val farmerCode: String,
    val fullName: String,
    val phoneNumber: String,
    val state: String,
    val lga: String,
    val community: String,
    val crop: String,
    val farmSizeHectares: Double,
    val latitude: Double,
    val longitude: Double,
    val cooperative: String,
    val registrationTimestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)
