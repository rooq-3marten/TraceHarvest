package com.example.core.resilience

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Hardening & Resilience Engine for Android devices across Nigeria:
 * - Budget phones (itel, Tecno, Infinix, Samsung A0x, Android Go Edition)
 * - Low-RAM environments (1GB–3GB RAM)
 * - Low storage (<500MB free)
 * - Spotty rural connectivity (2G/3G/EDGE/GPRS)
 * - Thermal & battery stress in field operations (Northern savanna heat)
 */
object DeviceHardeningManager {

    private const val TAG = "DeviceHardening"

    /**
     * Determines if the current device is a Low-RAM / Android Go device.
     */
    fun isLowRamDevice(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.isLowRamDevice ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads current memory usage info.
     */
    fun getMemoryProfile(context: Context): MemoryProfile {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)

            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val isLowRam = am?.isLowRamDevice == true || totalMb <= 2048

            MemoryProfile(
                totalRamMb = totalMb,
                availableRamMb = availMb,
                isLowMemory = memInfo.lowMemory,
                isLowRamDevice = isLowRam
            )
        } catch (_: Exception) {
            MemoryProfile(2048, 512, false, false)
        }
    }

    /**
     * Checks internal disk space in Megabytes.
     */
    fun getFreeStorageMb(context: Context): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes / (1024 * 1024)
        } catch (_: Exception) {
            100L
        }
    }

    /**
     * Returns true if storage is in danger of full disk errors (<50MB).
     */
    fun isStorageCriticallyLow(context: Context): Boolean {
        return getFreeStorageMb(context) < 50L
    }

    /**
     * Cleans temporary files, downsampled image caches, and stale logs.
     * Returns number of bytes reclaimed.
     */
    fun cleanTemporaryCaches(context: Context): Long {
        var reclaimed = 0L
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".jpg") || file.name.endsWith(".webp"))) {
                    val size = file.length()
                    if (file.delete()) {
                        reclaimed += size
                    }
                }
            }

            val appPrivateDir = File(context.filesDir, "photos_temp")
            if (appPrivateDir.exists()) {
                appPrivateDir.listFiles()?.forEach { f ->
                    val size = f.length()
                    if (f.delete()) reclaimed += size
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed during cache clean: ${e.message}")
        }
        return reclaimed
    }

    /**
     * Inspects battery level and charging state to prevent battery exhaustion in rural fields.
     */
    fun getBatteryProfile(context: Context): BatteryProfile {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            // Temperature is in tenths of a degree Celsius (e.g. 385 = 38.5°C)
            val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = rawTemp / 10.0

            BatteryProfile(
                percentage = pct,
                isCharging = isCharging,
                temperatureCelsius = tempC,
                isThermalThrottled = tempC > 42.0
            )
        } catch (_: Exception) {
            BatteryProfile(80, false, 32.0, false)
        }
    }

    /**
     * Inspects real network bandwidth category (2G / 3G / 4G / Wi-Fi / Offline).
     */
    fun getNetworkClassification(context: Context): NetworkClassification {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkClassification.OFFLINE

            val network = cm.activeNetwork ?: return NetworkClassification.OFFLINE
            val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkClassification.OFFLINE

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkClassification.WIFI_HIGH_SPEED
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val downstreamKbps = capabilities.linkDownstreamBandwidthKbps
                    when {
                        downstreamKbps > 5000 -> NetworkClassification.CELLULAR_4G
                        downstreamKbps > 1000 -> NetworkClassification.CELLULAR_3G
                        else -> NetworkClassification.CELLULAR_2G_EDGE
                    }
                }
                else -> NetworkClassification.CELLULAR_2G_EDGE
            }
        } catch (_: Exception) {
            NetworkClassification.CELLULAR_2G_EDGE
        }
    }
}

data class MemoryProfile(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val isLowMemory: Boolean,
    val isLowRamDevice: Boolean
)

data class BatteryProfile(
    val percentage: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Double,
    val isThermalThrottled: Boolean
)

enum class NetworkClassification(val label: String, val isSlowConnection: Boolean) {
    WIFI_HIGH_SPEED("Wi-Fi (Broadband)", false),
    CELLULAR_4G("4G LTE (Good)", false),
    CELLULAR_3G("3G (Moderate)", true),
    CELLULAR_2G_EDGE("2G / EDGE (Low Bandwidth)", true),
    OFFLINE("Offline (Store & Forward Queue Active)", true)
}
