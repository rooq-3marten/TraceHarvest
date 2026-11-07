package com.example.ui.screens

import androidx.compose.runtime.saveable.Saver
import com.example.ui.components.FarmVertex

/** Everything the enrolment form hands over when the agent taps Save. */
data class FarmerEnrollment(
    val name: String,
    val phone: String,
    val state: String,
    val lga: String,
    /** Blank when the agent did not enter one. Nothing is filled in on their behalf. */
    val community: String,
    val crop: String,
    val farmSize: Double,
    val farmSizeUnit: String,
    val latitude: Double,
    val longitude: Double,
    val cooperative: String,
    /** The boundary the agent walked, as "lat,lng;lat,lng". Blank when no boundary was captured. */
    val gpsPolygon: String,
    /** Path of the photo file kept inside the app, or null when there is none. */
    val photoPath: String?,
    val photoConsent: Boolean,
    /** Made once per form. Saving the same form twice stores one farmer, not two. */
    val clientUuid: String
)

/** Boundary text for saving. A boundary needs at least three points, otherwise nothing is stored. */
fun List<FarmVertex>.toPolygonText(): String =
    if (size < 3) "" else joinToString(";") { "${it.lat},${it.lng}" }

/** Keeps captured boundary points when the screen is rebuilt (rotation, or Android freeing memory). */
val FarmVertexListSaver: Saver<List<FarmVertex>, String> = Saver(
    save = { vertices -> vertices.joinToString(";") { "${it.lat},${it.lng}" } },
    restore = { text ->
        text.split(";").mapNotNull { pair ->
            val parts = pair.split(",")
            val lat = parts.getOrNull(0)?.toDoubleOrNull()
            val lng = parts.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lng != null) FarmVertex(lat, lng) else null
        }
    }
)
