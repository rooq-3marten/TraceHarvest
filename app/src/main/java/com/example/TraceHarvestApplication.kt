package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.core.auth.SessionManager
import com.example.data.remote.NetworkClient
import com.example.worker.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class for TraceHarvest Android.
 * Implements [Configuration.Provider] for on-demand WorkManager initialization,
 * registers background sync, and performs startup session checks.
 */
class TraceHarvestApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 1. Install global CrashGuard against unhandled crashes and structural failures
        com.example.core.resilience.CrashGuard.install(this)

        // Enables silent session renewal for every API call
        NetworkClient.initialize(this)

        // 2. Register periodic background sync with NetworkType.CONNECTED constraint
        SyncManager.initialize(this)

        // 3. Startup Session Check: verify DataStore auth token & inject into NetworkClient
        applicationScope.launch {
            try {
                val sessionManager = SessionManager.getInstance(this@TraceHarvestApplication)
                // Restore the stored access token even if past its initial window so a 401 triggers silent refresh.
                val cached = sessionManager.getCachedProfile()
                val token = if (cached.agentId.isNotBlank()) sessionManager.currentAccessToken() else null
                if (!token.isNullOrBlank()) {
                    NetworkClient.setAuthToken(token)
                    Log.i("TraceHarvestApp", "Active session restored for agent")

                    // Check if token needs refresh (if older than 7 days)
                    if (sessionManager.shouldRefreshToken()) {
                        Log.i("TraceHarvestApp", "Token is older than 7 days; scheduled for foreground refresh")
                    }
                } else {
                    Log.i("TraceHarvestApp", "No active session found. App will require authentication.")
                }
            } catch (e: Exception) {
                Log.w("TraceHarvestApp", "Session check skipped during app launch: ${e.message}")
            }
        }
    }
}
