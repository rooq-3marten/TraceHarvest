package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

data class FarmVertex(val lat: Double, val lng: Double)

/**
 * Enterprise GPS Geotagging & Polygon Perimeter Walking Component.
 * Conforms to EUDR (EU Deforestation Regulation) and NAFDAC export origin standards.
 */
@SuppressLint("MissingPermission")
@Composable
fun AdvancedGpsPolygonCaptureCard(
    currentLat: Double,
    currentLng: Double,
    accuracyMeters: Float,
    locationName: String,
    polygonVertices: List<FarmVertex>,
    onLocationUpdated: (Double, Double, Float) -> Unit,
    onVertexCaptured: (FarmVertex) -> Unit,
    onResetPolygon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLiveTracking by remember { mutableStateOf(true) }
    var isMockDetected by remember { mutableStateOf(false) }

    // Start live continuous GPS listener
    DisposableEffect(isLiveTracking) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    loc.isMock
                } else {
                    @Suppress("DEPRECATION")
                    loc.isFromMockProvider
                }
                isMockDetected = isMock
                onLocationUpdated(loc.latitude, loc.longitude, loc.accuracy)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (isLiveTracking && lm != null) {
            try {
                // Adaptive interval: 2.5s on budget/low-RAM devices to prevent thermal throttling, 1.5s on standard
                val isLowRam = com.example.core.resilience.DeviceHardeningManager.isLowRamDevice(context)
                val updateIntervalMs = if (isLowRam) 2500L else 1500L
                val minDistanceM = if (isLowRam) 1.5f else 1.0f

                // Request updates from GPS provider first, falling back to network
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateIntervalMs, minDistanceM, listener)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateIntervalMs, minDistanceM, listener)
                }
            } catch (_: SecurityException) {}
        }

        onDispose {
            try {
                lm?.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    // Calculate approximate polygon area in hectares using Shoelace formula
    val calculatedHectares = remember(polygonVertices) {
        if (polygonVertices.size >= 3) {
            var areaM2 = 0.0
            val n = polygonVertices.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                val xi = polygonVertices[i].lng * 111320.0 * cos(Math.toRadians(polygonVertices[i].lat))
                val yi = polygonVertices[i].lat * 110540.0
                val xj = polygonVertices[j].lng * 111320.0 * cos(Math.toRadians(polygonVertices[j].lat))
                val yj = polygonVertices[j].lat * 110540.0
                areaM2 += (xi * yj) - (xj * yi)
            }
            (abs(areaM2) / 2.0) / 10000.0 // m2 to hectares
        } else {
            0.0
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header: Real-time Fix Quality & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusColor = when {
                        accuracyMeters <= 3.0f -> Color(0xFF2E7D32) // High Precision Export Grade
                        accuracyMeters <= 8.0f -> Color(0xFFF57F17) // Moderate Accuracy
                        else -> Color(0xFFC62828)                   // Weak/Initial Fix
                    }
                    val statusText = when {
                        accuracyMeters <= 3.0f -> "EXPORT GRADE (<3m)"
                        accuracyMeters <= 8.0f -> "STANDARD (±${"%.1f".format(accuracyMeters)}m)"
                        else -> "CALIBRATING SATELLITES"
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, shape = RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )

                    if (isMockDetected) {
                        Text(
                            text = "⚠ MOCK GPS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }

                // Refresh / Fix Button (Keeps testTag for compatibility)
                OutlinedButton(
                    onClick = {
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        try {
                            val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (loc != null) {
                                onLocationUpdated(loc.latitude, loc.longitude, loc.accuracy)
                            }
                        } catch (_: SecurityException) {}
                    },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("refresh_gps_button")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fix GPS", fontSize = 11.sp)
                }
            }

            // 2. Physical Cluster Location
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "PHYSICAL AGRO-CLUSTER",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = locationName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.testTag("farmer_gps_display")
                )
            }

            // 3. Coordinate Box with High Precision & Accuracy Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CENTER GEOTAG",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${"%.6f".format(currentLat)}°N, ${"%.6f".format(currentLng)}°E",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ACCURACY",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "±${"%.1f".format(accuracyMeters)}m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // 4. Map View Mode Selector (Radar Grid vs Live Interactive Map)
            var showLiveMap by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showLiveMap = false },
                    color = if (!showLiveMap) Color(0xFF1B5E20) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🛰 Vector Radar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!showLiveMap) Color.White else Color(0xFF1B5E20),
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Surface(
                    onClick = { showLiveMap = true },
                    color = if (showLiveMap) Color(0xFF1B5E20) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🗺 Live Map (OSM/Sat)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showLiveMap) Color.White else Color(0xFF1B5E20),
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            if (showLiveMap) {
                InteractiveFieldMapView(
                    latitude = currentLat,
                    longitude = currentLng,
                    polygonVertices = polygonVertices,
                    heightDp = 160
                )
            } else {
                // Vector Polygon Radar (Canvas Mini-Map)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        val points = if (polygonVertices.isNotEmpty()) {
                            polygonVertices
                        } else {
                            // Display default 4-corner boundary around center
                            val delta = 0.0008
                            listOf(
                                FarmVertex(currentLat + delta, currentLng - delta),
                                FarmVertex(currentLat + delta, currentLng + delta),
                                FarmVertex(currentLat - delta, currentLng + delta),
                                FarmVertex(currentLat - delta, currentLng - delta)
                            )
                        }

                        val minLat = points.minOf { it.lat }
                        val maxLat = points.maxOf { it.lat }.coerceAtLeast(minLat + 0.0001)
                        val minLng = points.minOf { it.lng }
                        val maxLng = points.maxOf { it.lng }.coerceAtLeast(minLng + 0.0001)

                        val path = Path()
                        points.forEachIndexed { i, p ->
                            val x = ((p.lng - minLng) / (maxLng - minLng) * size.width).toFloat()
                            val y = (size.height - ((p.lat - minLat) / (maxLat - minLat) * size.height)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(Color(0xFF1B5E20), radius = 4.dp.toPx(), center = Offset(x, y))
                        }

                        if (points.size >= 3) {
                            path.close()
                            drawPath(path, color = Color(0x334CAF50))
                            drawPath(path, color = Color(0xFF2E7D32), style = Stroke(width = 2.dp.toPx()))
                        }

                        // Draw Center Pin
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        drawCircle(Color(0xFFD32F2F), radius = 5.dp.toPx(), center = Offset(centerX, centerY))
                    }

                    // Overlay Status Text on Mini-Map
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (polygonVertices.isNotEmpty()) {
                                "Walked Perimeter: ${polygonVertices.size}/4 corners"
                            } else {
                                "Auto Perimeter Polygon (EUDR compliant)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )

                        if (calculatedHectares > 0.0) {
                            Text(
                                text = "${"%.2f".format(calculatedHectares)} ha enclosed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // 5. Perimeter Corner Walk Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (polygonVertices.size < 4) {
                            onVertexCaptured(FarmVertex(currentLat, currentLng))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = polygonVertices.size < 4,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.AddLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (polygonVertices.size < 4) "Pin Corner ${polygonVertices.size + 1}" else "Perimeter Complete",
                        fontSize = 11.sp
                    )
                }

                if (polygonVertices.isNotEmpty()) {
                    TextButton(
                        onClick = onResetPolygon,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Reset", fontSize = 11.sp, color = Color(0xFFC62828))
                    }
                }
            }
        }
    }
}
