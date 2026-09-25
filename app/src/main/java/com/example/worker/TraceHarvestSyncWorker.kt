package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.repository.TraceHarvestRepositoryImpl

/**
 * Background CoroutineWorker managed by Android WorkManager.
 * Automatically triggered whenever the OS detects cellular or Wi-Fi data connectivity.
 * Implements idempotent, batch-oriented upstream sync with the FastAPI backend.
 */
class TraceHarvestSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "TraceHarvestSyncWorker"
        const val KEY_SYNCED_FARMERS = "synced_farmers"
        const val KEY_SYNCED_PRACTICES = "synced_practices"
        const val KEY_SYNC_MESSAGE = "sync_message"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting TraceHarvest background synchronization with FastAPI...")

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = TraceHarvestRepositoryImpl(database.traceHarvestDao())

            val result = repository.syncAllPending()

            Log.i(
                TAG,
                "Background sync completed successfully: ${result.syncedFarmersCount} farmers, ${result.syncedPracticesCount} practices synced."
            )

            val outputData = workDataOf(
                KEY_SYNCED_FARMERS to result.syncedFarmersCount,
                KEY_SYNCED_PRACTICES to result.syncedPracticesCount,
                KEY_SYNC_MESSAGE to result.message
            )

            Result.success(outputData)
        } catch (e: Exception) {
            Log.e(TAG, "Background sync encountered error, scheduling retry", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
