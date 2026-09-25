package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.sqrt

@Entity(tableName = "farmers")
data class FarmerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val farmerCode: String = "", // Unique Farmer ID generated upon enrollment / backend sync
    val fullName: String,
    val phoneNumber: String, // Primary identity (no phone app / literacy required for farmer)
    val state: String = "Kano",
    val lga: String = "Dambatta",
    val community: String = "",
    val crop: String, // sesame, cowpea, etc.
    val farmSize: Double = 1.0, // in local units
    val farmSizeUnit: String = "hectares", // "hectares", "acres", "plots"
    val farmSizeHectares: Double = when (farmSizeUnit.lowercase()) {
        "acres" -> farmSize * 0.4047
        "plots" -> farmSize * 0.05
        else -> farmSize
    },
    val latitude: Double, // auto-captured from phone GPS
    val longitude: Double, // auto-captured from phone GPS
    val cooperative: String = "", // Cooperative affiliation (if any)
    val registrationTimestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val farmerDisplayId: String = farmerCode,
    val gpsPolygon: String = "",
    val syncStatus: String = "pending_sync", // "pending_sync", "synced"
    val agentId: String = "AGENT-NG-042",
    val clientUuid: String = java.util.UUID.randomUUID().toString()
) {
    /**
     * Active Local Database Identifier
     */
    val farmerLocalId: Long get() = id

    /**
     * Human-readable formatted center GPS coordinates
     */
    val gpsCoordinates: String get() = "${"%.4f".format(latitude)}°N, ${"%.4f".format(longitude)}°E"

    /**
     * Returns the 4 corner boundary polygon coordinates (geotagged perimeter)
     */
    fun getPolygonCoordinates(): List<Pair<Double, Double>> {
        if (gpsPolygon.isNotBlank()) {
            try {
                val parsed = gpsPolygon.split(";").mapNotNull { entry ->
                    val parts = entry.split(",")
                    if (parts.size >= 2) {
                        Pair(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                    } else null
                }
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {}
        }

        val delta = (sqrt(farmSizeHectares.coerceAtLeast(0.5)) * 0.00075)
        return listOf(
            Pair(latitude + delta, longitude - delta), // NW
            Pair(latitude + delta, longitude + delta), // NE
            Pair(latitude - delta, longitude + delta), // SE
            Pair(latitude - delta, longitude - delta)  // SW
        )
    }

    fun getFormattedPolygonString(): String {
        return getPolygonCoordinates().joinToString(" → ") { (lat, lng) ->
            "[${"%.4f".format(lat)}, ${"%.4f".format(lng)}]"
        }
    }
}
