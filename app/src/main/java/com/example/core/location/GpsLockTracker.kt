package com.example.core.location

/**
 * One position reading. Kept free of Android types so the lock rules can be unit tested.
 */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val isMock: Boolean = false,
    val isGpsProvider: Boolean = true,
    val timeMs: Long = 0L
)

enum class GpsLockStatus { SEARCHING, WEAK_FIX, LOCKED }

/**
 * Decides when a farm location is trustworthy enough to save.
 *
 * Rules:
 *  - Mock locations are ignored and flagged.
 *  - Readings with no real accuracy (0, negative, NaN) or impossible coordinates are ignored.
 *    A reading with accuracy 0 must never be shown as a perfect fix.
 *  - A network fix that arrives soon after a GPS fix is ignored, so a rough network position
 *    cannot pull the location away from a good GPS one.
 *  - The location locks automatically once readings are within [lockAccuracyMeters]: at once
 *    if a reading is within [instantLockMeters], otherwise after [readingsToLock] good readings.
 *    The lock keeps the best (smallest accuracy) reading seen.
 *  - If the signal stays weak, the agent may accept the best reading, but only if it is within
 *    [usableAccuracyMeters].
 */
class GpsLockTracker(
    private val lockAccuracyMeters: Float = 15f,
    private val instantLockMeters: Float = 5f,
    private val readingsToLock: Int = 3,
    val usableAccuracyMeters: Float = 50f,
    private val networkIgnoreAfterGpsMs: Long = 15_000L
) {
    var best: GpsFix? = null
        private set
    var locked: GpsFix? = null
        private set
    /** Latest accepted reading. Used for walking the farm perimeter. */
    var latest: GpsFix? = null
        private set
    var mockSeen: Boolean = false
        private set

    private var goodReadings = 0
    private var lastGpsMs: Long? = null

    val status: GpsLockStatus
        get() {
            if (locked != null) return GpsLockStatus.LOCKED
            val b = best
            return if (b != null && b.accuracyMeters <= usableAccuracyMeters) GpsLockStatus.WEAK_FIX else GpsLockStatus.SEARCHING
        }

    fun reset() {
        best = null
        locked = null
        latest = null
        mockSeen = false
        goodReadings = 0
        lastGpsMs = null
    }

    /** Re-creates a lock that was already saved, for example after the screen was rotated. */
    fun restoreLocked(fix: GpsFix) {
        locked = fix
        best = fix
    }

    /** Returns true if the reading was accepted. */
    fun onFix(fix: GpsFix): Boolean {
        if (fix.isMock) {
            mockSeen = true
            return false
        }
        if (!isValid(fix)) return false

        if (fix.isGpsProvider) {
            lastGpsMs = fix.timeMs
        } else {
            val g = lastGpsMs
            if (g != null && fix.timeMs - g in 0..networkIgnoreAfterGpsMs) return false
        }

        latest = fix
        if (locked != null) return true

        val b = best
        if (b == null || fix.accuracyMeters <= b.accuracyMeters) best = fix

        if (fix.accuracyMeters <= lockAccuracyMeters) {
            goodReadings++
            if (fix.accuracyMeters <= instantLockMeters || goodReadings >= readingsToLock) {
                locked = best
            }
        }
        return true
    }

    /** Accept the best reading so far although it is not tight enough to lock by itself. */
    fun acceptBest(): Boolean {
        val b = best ?: return false
        if (b.accuracyMeters > usableAccuracyMeters) return false
        locked = b
        return true
    }

    private fun isValid(f: GpsFix): Boolean {
        if (f.accuracyMeters.isNaN() || f.accuracyMeters <= 0f) return false
        if (f.latitude.isNaN() || f.longitude.isNaN()) return false
        if (f.latitude < -90.0 || f.latitude > 90.0 || f.longitude < -180.0 || f.longitude > 180.0) return false
        if (f.latitude == 0.0 && f.longitude == 0.0) return false
        return true
    }
}
