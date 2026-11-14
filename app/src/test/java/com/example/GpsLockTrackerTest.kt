package com.example

import com.example.core.location.GpsFix
import com.example.core.location.GpsLockStatus
import com.example.core.location.GpsLockTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsLockTrackerTest {

    private fun fix(acc: Float, lat: Double = 12.43, lng: Double = 8.51, t: Long = 0L, gps: Boolean = true, mock: Boolean = false) =
        GpsFix(lat, lng, acc, isMock = mock, isGpsProvider = gps, timeMs = t)

    @Test
    fun startsSearchingWithNoFix() {
        val t = GpsLockTracker()
        assertEquals(GpsLockStatus.SEARCHING, t.status)
        assertNull(t.locked)
    }

    @Test
    fun zeroAccuracyIsNeverTreatedAsAFix() {
        val t = GpsLockTracker()
        assertFalse(t.onFix(fix(0f)))
        assertFalse(t.onFix(fix(-1f)))
        assertFalse(t.onFix(fix(Float.NaN)))
        assertNull(t.best)
        assertEquals(GpsLockStatus.SEARCHING, t.status)
    }

    @Test
    fun impossibleCoordinatesAreIgnored() {
        val t = GpsLockTracker()
        assertFalse(t.onFix(fix(5f, lat = 0.0, lng = 0.0)))
        assertFalse(t.onFix(fix(5f, lat = 95.0)))
        assertFalse(t.onFix(fix(5f, lng = 190.0)))
        assertNull(t.best)
    }

    @Test
    fun mockLocationIsIgnoredAndFlagged() {
        val t = GpsLockTracker()
        assertFalse(t.onFix(fix(3f, mock = true)))
        assertTrue(t.mockSeen)
        assertNull(t.locked)
    }

    @Test
    fun veryAccurateReadingLocksImmediately() {
        val t = GpsLockTracker()
        assertTrue(t.onFix(fix(4f)))
        assertEquals(GpsLockStatus.LOCKED, t.status)
        assertEquals(4f, t.locked!!.accuracyMeters, 0.001f)
    }

    @Test
    fun moderateReadingsNeedThreeInARowAndKeepTheBest() {
        val t = GpsLockTracker()
        t.onFix(fix(14f, lat = 12.4001))
        t.onFix(fix(9f, lat = 12.4002))
        assertNull(t.locked)
        t.onFix(fix(12f, lat = 12.4003))
        assertEquals(GpsLockStatus.LOCKED, t.status)
        assertEquals(9f, t.locked!!.accuracyMeters, 0.001f)
        assertEquals(12.4002, t.locked!!.latitude, 1e-9)
    }

    @Test
    fun weakReadingsDoNotLockButCanBeAcceptedWithinLimit() {
        val t = GpsLockTracker()
        t.onFix(fix(40f))
        assertEquals(GpsLockStatus.WEAK_FIX, t.status)
        assertNull(t.locked)
        assertTrue(t.acceptBest())
        assertEquals(GpsLockStatus.LOCKED, t.status)
    }

    @Test
    fun readingsWorseThanLimitCannotBeAccepted() {
        val t = GpsLockTracker()
        t.onFix(fix(120f))
        assertEquals(GpsLockStatus.SEARCHING, t.status)
        assertFalse(t.acceptBest())
        assertNull(t.locked)
    }

    @Test
    fun roughNetworkFixDoesNotOverrideRecentGps() {
        val t = GpsLockTracker()
        t.onFix(fix(30f, lat = 12.40, t = 1_000L, gps = true))
        assertFalse(t.onFix(fix(800f, lat = 12.90, t = 4_000L, gps = false)))
        assertEquals(12.40, t.best!!.latitude, 1e-9)
    }

    @Test
    fun networkFixIsUsedWhenThereIsNoRecentGps() {
        val t = GpsLockTracker()
        assertTrue(t.onFix(fix(35f, t = 100_000L, gps = false)))
        assertNotNull(t.best)
    }

    @Test
    fun lockStaysFixedButLatestKeepsMovingForPerimeterWalk() {
        val t = GpsLockTracker()
        t.onFix(fix(3f, lat = 12.4000))
        val lockedLat = t.locked!!.latitude
        t.onFix(fix(6f, lat = 12.4010, t = 2_000L))
        assertEquals(lockedLat, t.locked!!.latitude, 1e-9)
        assertEquals(12.4010, t.latest!!.latitude, 1e-9)
    }

    @Test
    fun resetClearsEverything() {
        val t = GpsLockTracker()
        t.onFix(fix(3f))
        t.onFix(fix(3f, mock = true))
        t.reset()
        assertNull(t.locked)
        assertNull(t.best)
        assertNull(t.latest)
        assertFalse(t.mockSeen)
        assertEquals(GpsLockStatus.SEARCHING, t.status)
    }

    @Test
    fun restoredLockIsReportedAsLocked() {
        val t = GpsLockTracker()
        t.restoreLocked(fix(6f))
        assertEquals(GpsLockStatus.LOCKED, t.status)
    }
}
