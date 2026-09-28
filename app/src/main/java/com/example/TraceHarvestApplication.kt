package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.worker.SyncManager

/**
 * Custom Application class for TraceHarvest Android.
 * Implements [Configuration.Provider] for on-demand WorkManager initialization
 * and starts background sync upon app launch.
 */
class TraceHarvestApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Register periodic background sync with NetworkType.CONNECTED constraint
        SyncManager.initialize(this)
    }
}
