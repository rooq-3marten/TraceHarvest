package com.example.core.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Intelligent physical location naming service for agricultural field provenance.
 * Resolves raw GPS coordinates into physically named human-readable locations:
 * 1. Online: System [Geocoder] reverse-geocodes to village, community, LGA, and State.
 * 2. Offline: Spatial index calculates nearest landmark agricultural export hub across Nigeria.
 * 3. Contextual fallback: Uses field agent's selected community, LGA, and state.
 */
object GeoLocationNamingService {

    data class AgriculturalLandmark(
        val name: String,
        val lga: String,
        val state: String,
        val latitude: Double,
        val longitude: Double
    )

    // Spatial index of major smallholder agricultural export hubs across Nigeria
    val NIGERIAN_AGRI_HUBS = listOf(
        AgriculturalLandmark("Dambatta Sesame Hub", "Dambatta", "Kano", 12.4382, 8.5147),
        AgriculturalLandmark("Bichi Grain Basin", "Bichi", "Kano", 12.2333, 8.2417),
        AgriculturalLandmark("Kunchi Farm Cluster", "Kunchi", "Kano", 12.5000, 8.2667),
        AgriculturalLandmark("Bagwai Irrigation Scheme", "Bagwai", "Kano", 12.1500, 8.1333),
        AgriculturalLandmark("Dawakin Tofa Export Plots", "Dawakin Tofa", "Kano", 12.1167, 8.3333),
        AgriculturalLandmark("Gwarzo Grain Corridor", "Gwarzo", "Kano", 11.9167, 7.9333),
        AgriculturalLandmark("Kano Central Agriservice Hub", "Kano Municipal", "Kano", 11.9899, 8.5380),
        AgriculturalLandmark("Maigatari Export Commodity Border", "Maigatari", "Jigawa", 12.8622, 9.9078),
        AgriculturalLandmark("Hadejia River Basin Irrigation", "Hadejia", "Jigawa", 12.4500, 10.0333),
        AgriculturalLandmark("Birnin Kudu Farming Zone", "Birnin Kudu", "Jigawa", 11.4500, 9.4833),
        AgriculturalLandmark("Gumel Agricultural Hub", "Gumel", "Jigawa", 12.6333, 9.3833),
        AgriculturalLandmark("Kazaure Crop Plains", "Kazaure", "Jigawa", 12.6500, 8.4167),
        AgriculturalLandmark("Ringim Sesame Belt", "Ringim", "Jigawa", 12.1500, 9.1667),
        AgriculturalLandmark("Zaria Agricultural Research Basin", "Zaria", "Kaduna", 11.0855, 7.7199),
        AgriculturalLandmark("Giwa Grain Corridor", "Giwa", "Kaduna", 11.2833, 7.4167),
        AgriculturalLandmark("Makarfi Vegetable & Grain Zone", "Makarfi", "Kaduna", 11.3833, 7.8833),
        AgriculturalLandmark("Soba Soybean Belt", "Soba", "Kaduna", 10.9833, 8.0500),
        AgriculturalLandmark("Lere Maize Valley", "Lere", "Kaduna", 10.3833, 8.5667),
        AgriculturalLandmark("Makurdi Benue River Basin", "Makurdi", "Benue", 7.7322, 8.5391),
        AgriculturalLandmark("Gboko Soybean & Sesame Corridor", "Gboko", "Benue", 7.3167, 9.0000),
        AgriculturalLandmark("Otukpo Cashew & Grain Hub", "Otukpo", "Benue", 7.1917, 8.1333),
        AgriculturalLandmark("Katsina Export Corridor", "Katsina", "Katsina", 12.9833, 7.6000),
        AgriculturalLandmark("Funtua Cotton & Grain Basin", "Funtua", "Katsina", 11.5333, 7.3167),
        AgriculturalLandmark("Minna Agricultural Corridor", "Minna", "Niger", 9.6177, 6.5569),
        AgriculturalLandmark("Mokwa Grain Belt", "Mokwa", "Niger", 9.2833, 5.0500),
        AgriculturalLandmark("Lafia Cashew & Sesame Plains", "Lafia", "Nasarawa", 8.4933, 8.5153),
        AgriculturalLandmark("Jalingo Grain Basin", "Jalingo", "Taraba", 8.8921, 11.3600),
        AgriculturalLandmark("Wukari Sesame Corridor", "Wukari", "Taraba", 7.8700, 9.7800)
    )

    /**
     * Resolves geographical coordinates to a human-readable physical location name.
     */
    suspend fun resolveLocationName(
        context: Context,
        latitude: Double,
        longitude: Double,
        fallbackState: String = "",
        fallbackLga: String = "",
        fallbackCommunity: String = ""
    ): String = withContext(Dispatchers.IO) {
        // 1. Try Geocoder if available
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var result: List<Address>? = null
                    val lock = Object()
                    var done = false
                    geocoder.getFromLocation(latitude, longitude, 1) { list ->
                        synchronized(lock) {
                            result = list
                            done = true
                            lock.notifyAll()
                        }
                    }
                    synchronized(lock) {
                        if (!done) {
                            lock.wait(1200)
                        }
                    }
                    result
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val parts = mutableListOf<String>()

                    val locality = addr.subLocality ?: addr.thoroughfare ?: addr.featureName
                    val subAdmin = addr.subAdminArea ?: addr.locality
                    val admin = addr.adminArea

                    if (!locality.isNullOrBlank() && locality != subAdmin && !locality.matches(Regex("^-?\\d+(\\.\\d+)?$"))) {
                        parts.add(locality)
                    }
                    if (!subAdmin.isNullOrBlank() && subAdmin != admin) {
                        parts.add(subAdmin)
                    }
                    if (!admin.isNullOrBlank()) {
                        parts.add(admin)
                    }

                    if (parts.isNotEmpty()) {
                        return@withContext parts.joinToString(", ")
                    }
                }
            } catch (_: Throwable) {
                // Network unavailable or Geocoder failed, seamlessly proceed to offline hub resolver
            }
        }

        // 2. Offline Spatial Index: Match against nearest known agricultural landmark
        val nearest = findNearestHub(latitude, longitude)
        if (nearest != null && nearest.second <= 45.0) {
            val hub = nearest.first
            val dist = nearest.second.roundToInt()
            return@withContext if (dist <= 3) {
                "${hub.name}, ${hub.state} State"
            } else {
                "${hub.name} Area (${dist}km), ${hub.state} State"
            }
        }

        // 3. Contextual Community / LGA / State Fallback
        val contextualParts = listOf(
            fallbackCommunity.trim(),
            if (fallbackLga.isNotBlank()) "${fallbackLga.trim()} LGA" else "",
            if (fallbackState.isNotBlank()) "${fallbackState.trim()} State" else ""
        ).filter { it.isNotBlank() }

        if (contextualParts.isNotEmpty()) {
            return@withContext contextualParts.joinToString(", ")
        }

        // 4. Default formatted coordinates
        return@withContext "${"%.4f".format(latitude)}°N, ${"%.4f".format(longitude)}°E"
    }

    /**
     * Finds the closest Nigerian agricultural landmark hub using the Haversine formula.
     * Returns Pair of [AgriculturalLandmark] and distance in kilometers.
     */
    fun findNearestHub(lat: Double, lng: Double): Pair<AgriculturalLandmark, Double>? {
        if (lat == 0.0 && lng == 0.0) return null

        var closest: AgriculturalLandmark? = null
        var minDistance = Double.MAX_VALUE

        for (hub in NIGERIAN_AGRI_HUBS) {
            val dist = haversineDistanceKm(lat, lng, hub.latitude, hub.longitude)
            if (dist < minDistance) {
                minDistance = dist
                closest = hub
            }
        }

        return if (closest != null) Pair(closest, minDistance) else null
    }

    /**
     * Calculates great-circle distance between two GPS coordinates in kilometers.
     */
    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
