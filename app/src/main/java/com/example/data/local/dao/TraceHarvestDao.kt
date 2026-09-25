package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.data.local.entity.SmsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TraceHarvestDao {

    // --- Farmers ---
    @Query("SELECT * FROM farmers ORDER BY registrationTimestamp DESC")
    fun getAllFarmers(): Flow<List<FarmerEntity>>

    @Query("SELECT * FROM farmers WHERE farmerCode = :code LIMIT 1")
    suspend fun getFarmerByCode(code: String): FarmerEntity?

    @Query("SELECT COUNT(*) FROM farmers")
    fun getFarmerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: FarmerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmers(farmers: List<FarmerEntity>)

    @Query("DELETE FROM farmers WHERE id = :id")
    suspend fun deleteFarmerById(id: Long)

    @Query("SELECT * FROM farmers WHERE syncStatus = 'pending_sync' ORDER BY registrationTimestamp ASC")
    suspend fun getPendingSyncFarmers(): List<FarmerEntity>

    @Query("SELECT COUNT(*) FROM farmers WHERE syncStatus = 'pending_sync'")
    fun getPendingSyncFarmersCount(): Flow<Int>

    @Query("UPDATE farmers SET syncStatus = 'synced', isSynced = 1, farmerCode = :serverCode, farmerDisplayId = :serverCode WHERE id = :id")
    suspend fun markFarmerSynced(id: Long, serverCode: String)

    @Query("UPDATE farmers SET syncStatus = 'synced', isSynced = 1 WHERE id IN (:ids)")
    suspend fun markFarmersSynced(ids: List<Long>)

    // --- Practice Logs ---
    @Query("SELECT * FROM practice_logs ORDER BY dateApplied DESC")
    fun getAllPracticeLogs(): Flow<List<PracticeLogEntity>>

    @Query("SELECT * FROM practice_logs WHERE farmerCode = :farmerCode ORDER BY dateApplied DESC")
    fun getPracticeLogsForFarmer(farmerCode: String): Flow<List<PracticeLogEntity>>

    @Query("SELECT COUNT(*) FROM practice_logs WHERE riskLevel = 'BANNED_MRL_VIOLATION'")
    fun getBannedPesticideViolationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeLog(log: PracticeLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeLogs(logs: List<PracticeLogEntity>)

    @Query("SELECT * FROM practice_logs WHERE syncStatus = 'pending_sync' ORDER BY dateApplied ASC")
    suspend fun getPendingSyncPracticeLogs(): List<PracticeLogEntity>

    @Query("SELECT COUNT(*) FROM practice_logs WHERE syncStatus = 'pending_sync'")
    fun getPendingSyncPracticeLogsCount(): Flow<Int>

    @Query("UPDATE practice_logs SET syncStatus = 'synced', isSynced = 1 WHERE id = :id")
    suspend fun markPracticeLogSynced(id: Long)

    @Query("UPDATE practice_logs SET syncStatus = 'synced', isSynced = 1 WHERE id IN (:ids)")
    suspend fun markPracticeLogsSynced(ids: List<Long>)

    // --- Harvest Batches ---
    @Query("SELECT * FROM harvest_batches ORDER BY harvestDate DESC")
    fun getAllBatches(): Flow<List<HarvestBatchEntity>>

    @Query("SELECT * FROM harvest_batches WHERE batchCode = :code LIMIT 1")
    suspend fun getBatchByCode(code: String): HarvestBatchEntity?

    @Query("SELECT * FROM harvest_batches WHERE isFlaggedForRejection = 1")
    fun getFlaggedBatches(): Flow<List<HarvestBatchEntity>>

    @Query("SELECT COUNT(*) FROM harvest_batches")
    fun getTotalBatchesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM harvest_batches WHERE mrlStatus = 'PASSED_SPS'")
    fun getExportReadyBatchesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM harvest_batches WHERE isFlaggedForRejection = 1 OR mrlStatus = 'VIOLATION_BLOCKED'")
    fun getBlockedBatchesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: HarvestBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<HarvestBatchEntity>)

    @Update
    suspend fun updateBatch(batch: HarvestBatchEntity)

    @Query("UPDATE harvest_batches SET isBlockchainAnchored = 1, blockchainTxId = :txId WHERE id = :batchId")
    suspend fun anchorBatchToBlockchain(batchId: Long, txId: String)

    @Query("UPDATE harvest_batches SET isFlaggedForRejection = :isFlagged, rejectionReason = :reason WHERE id = :batchId")
    suspend fun setBatchFlaggedStatus(batchId: Long, isFlagged: Boolean, reason: String?)

    // --- SMS Logs ---
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogs(): Flow<List<SmsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLogs(logs: List<SmsLogEntity>)
}
