package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Offline-first sync staging entity.
 * Queues local records for background synchronization via WorkManager or immediate push.
 */
@Entity(tableName = "pending_syncs")
data class PendingSync(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // UUID
    val entityType: String,                        // FARMER, PRACTICE_LOG, BATCH
    val entityId: String,                          // local ID or server ID
    val payload: String,                           // JSON serialized record
    val status: String = "PENDING",                // PENDING, SYNCING, FAILED, SYNCED
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)
