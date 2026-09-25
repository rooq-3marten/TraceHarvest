package com.example.worker

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Singleton orchestrator for Android WorkManager background sync.
 * Ensures the mobile app seamlessly synchronizes pending SQLite records
 * whenever the device reconnects to cellular data or Wi-Fi.
 */
object SyncManager {

    const val PERIODIC_WORK_NAME = "traceharvest_periodic_sync"
    const val ONE_TIME_WORK_NAME = "traceharvest_immediate_sync"
    const val SYNC_INTERVAL_MINUTES = 15L

    /**
     * Initializes the background periodic sync worker.
     * Enqueues a periodic CoroutineWorker with [NetworkType.CONNECTED] constraint.
     */
    fun initialize(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<TraceHarvestSyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Enqueues an immediate one-time sync task via WorkManager as soon as
     * connectivity is available.
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TraceHarvestSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }

    /**
     * Observes the periodic sync WorkInfo to display live background sync status in the UI.
     */
    fun observePeriodicWork(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(PERIODIC_WORK_NAME)
            .asFlow()
    }
}
