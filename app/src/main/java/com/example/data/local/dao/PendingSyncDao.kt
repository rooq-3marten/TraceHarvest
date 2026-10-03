package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PendingSync
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingSync: PendingSync)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pendingSyncs: List<PendingSync>)

    @Update
    suspend fun update(pendingSync: PendingSync)

    @Query("SELECT * FROM pending_syncs WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingOrFailedSyncs(): List<PendingSync>

    @Query("SELECT * FROM pending_syncs WHERE entityType = :entityType AND status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingSyncsByType(entityType: String): List<PendingSync>

    @Query("SELECT * FROM pending_syncs WHERE id = :id LIMIT 1")
    suspend fun getPendingSyncById(id: String): PendingSync?

    @Query("SELECT COUNT(*) FROM pending_syncs WHERE status IN ('PENDING', 'FAILED')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_syncs WHERE status IN ('PENDING', 'FAILED')")
    suspend fun getPendingCount(): Int

    @Query("UPDATE pending_syncs SET status = 'SYNCING' WHERE id = :id")
    suspend fun markSyncing(id: String)

    @Query("UPDATE pending_syncs SET status = 'SYNCED', syncedAt = :timestamp, lastError = null WHERE id = :id")
    suspend fun markSynced(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pending_syncs SET status = 'FAILED', retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String?)

    @Query("DELETE FROM pending_syncs WHERE status = 'SYNCED' AND syncedAt < :thresholdTimestamp")
    suspend fun purgeOldSynced(thresholdTimestamp: Long)

    @Query("DELETE FROM pending_syncs WHERE id = :id")
    suspend fun deleteById(id: String)
}
