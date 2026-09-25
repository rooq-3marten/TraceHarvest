package com.example

import android.app.Application
import com.example.worker.SyncManager

/**
 * Custom Application class for TraceHarvest Android.
 * Initializes Android WorkManager background sync upon app launch,
 * ensuring automatic CoroutineWorker execution whenever cellular or Wi-Fi data connects.
 */
class TraceHarvestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Register periodic background sync with NetworkType.CONNECTED constraint
        SyncManager.initialize(this)
    }
}
