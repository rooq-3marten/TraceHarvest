package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Enterprise Google Satellite Imagery Farm Boundary Mapping & WKT Exporter.
 * Adheres to EUDR (EU Deforestation Regulation) geospatial polygon requirements.
 */
@SuppressLint("SetJavaScriptEnabled", "MissingPermission")
@Composable
fun GoogleSatelliteFarmMap(
    initialLat: Double,
    initialLng: Double,
    cropType: String,
    farmerName: String,
    existingPoints: List<FarmVertex> = emptyList(),
    onBoundarySaved: (wkt: String, areaHectares: Double, vertices: List<FarmVertex>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentLat by remember { mutableDoubleStateOf(initialLat) }
    var currentLng by remember { mutableDoubleStateOf(initialLng) }
    var gpsAccuracy by remember { mutableFloatStateOf(4.2f) }
    var vertices by remember { mutableStateOf(existingPoints.toMutableList()) }
    var isSatelliteLayer by remember { mutableStateOf(true) }
    var isWalkTrackingActive by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var accuracyWarningMessage by remember { mutableStateOf<String?>(null) }

    // Start live high-accuracy location listener
    DisposableEffect(isWalkTrackingActive) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                currentLat = loc.latitude
                currentLng = loc.longitude
                gpsAccuracy = loc.accuracy

                accuracyWarningMessage = when {
                    loc.accuracy > 30f -> "GPS signal weak (±${"%.0f".format(loc.accuracy)}m) — move to open sky away from tall trees."
                    loc.accuracy > 10f -> "Calibrating satellites (±${"%.1f".format(loc.accuracy)}m) — wait for <10m precision."
                    else -> null
                }
            }
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }

        if (isWalkTrackingActive && lm != null) {
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, listener)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0.5f, listener)
                }
            } catch (_: SecurityException) {}
        }

        onDispose {
            try { lm?.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    // Dynamic color coding by crop type
    val cropHexColor = when (cropType.lowercase()) {
        "sesame" -> "#2D5A27"     // Forest green
        "cowpea" -> "#D4A03C"     // Ochre yellow
        "groundnut" -> "#8B3A2B"  // Laterite red
        "cocoa" -> "#4E342E"      // Cocoa umber
        "cashew" -> "#E65100"     // Cashew orange
        "ginger" -> "#FFB300"     // Ginger amber
        "rice" -> "#7CB342"       // Paddy green
        "oil palm" -> "#1B5E20"   // Dark palm emerald
        else -> "#2D5A27"
    }

    // Calculate real-time area in hectares using Shoelace algorithm
    val calculatedHectares = remember(vertices.size) {
        if (vertices.size >= 3) {
            var areaM2 = 0.0
            val n = vertices.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                val xi = vertices[i].lng * 111320.0 * cos(Math.toRadians(vertices[i].lat))
                val yi = vertices[i].lat * 110540.0
                val xj = vertices[j].lng * 111320.0 * cos(Math.toRadians(vertices[j].lat))
                val yj = vertices[j].lat * 110540.0
                areaM2 += (xi * yj) - (xj * yi)
            }
            (abs(areaM2) / 2.0) / 10000.0
        } else 0.0
    }

    // Google Satellite Imagery HTML Engine
    val mapHtml = remember(currentLat, currentLng, vertices.size, isSatelliteLayer) {
        val polygonCoords = if (vertices.size >= 2) {
            val coords = vertices.joinToString(",") { "[${it.lat}, ${it.lng}]" }
            "L.polygon([$coords], {color: '$cropHexColor', fillColor: '$cropHexColor', fillOpacity: 0.38, weight: 3}).addTo(map);"
        } else ""

        val markersJs = vertices.mapIndexed { idx, p ->
            "L.marker([${p.lat}, ${p.lng}], {icon: cornerIcon}).addTo(map).bindPopup('Corner #${idx + 1}');"
        }.joinToString("\n")

        // Official Google Satellite & Hybrid Tiles (lyrs=s: satellite, lyrs=y: hybrid with labels)
        val tileUrl = if (isSatelliteLayer) {
            "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"
        } else {
            "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
        }

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #263238; }
                .leaflet-control-attribution { font-size: 8px !important; }
                .agent-pulse {
                    background: #8B3A2B;
                    border: 3px solid #FFF;
                    border-radius: 50%;
                    width: 16px;
                    height: 16px;
                    box-shadow: 0 0 10px rgba(0,0,0,0.8);
                }
                .corner-marker {
                    background: #2D5A27;
                    border: 2px solid #FFF;
                    border-radius: 50%;
                    width: 12px;
                    height: 12px;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                try {
                    var map = L.map('map', {zoomControl: false}).setView([$currentLat, $currentLng], 17);
                    L.tileLayer('$tileUrl', {
                        maxZoom: 20,
                        attribution: 'Google Satellite Imagery • TraceHarvest'
                    }).addTo(map);

                    var agentIcon = L.divIcon({className: 'agent-pulse', iconSize: [16, 16], iconAnchor: [8, 8]});
                    var cornerIcon = L.divIcon({className: 'corner-marker', iconSize: [12, 12], iconAnchor: [6, 6]});

                    // Live agent position marker
                    L.marker([$currentLat, $currentLng], {icon: agentIcon}).addTo(map);

                    // Draw captured corners and polygon
                    $markersJs
                    $polygonCoords

                    // Auto-fit bounds if 3+ points
                    ${if (vertices.size >= 3) {
                        val coords = vertices.joinToString(",") { "[${it.lat}, ${it.lng}]" }
                        "var b = L.latLngBounds([$coords]); map.fitBounds(b, {padding: [30, 30]});"
                    } else ""}
                } catch(e) {
                    console.error("Map load error: " + e);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Fullscreen Map AndroidView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://traceharvest.local", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://traceharvest.local", mapHtml, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation Bar Overlay
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            color = SoftCreamSurface.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CharcoalBrownText)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Farm Perimeter Walk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CharcoalBrownText
                    )
                    Text(
                        text = "$farmerName • $cropType",
                        fontSize = 11.sp,
                        color = MutedBrownText
                    )
                }

                IconButton(
                    onClick = { isSatelliteLayer = !isSatelliteLayer },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Toggle Layer",
                        tint = LateriteRedPrimary
                    )
                }
            }
        }

        // GPS Accuracy Status Banner
        accuracyWarningMessage?.let { warning ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 80.dp),
                shape = RoundedCornerShape(10.dp),
                color = WarmOchreAccent.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CharcoalBrownText, modifier = Modifier.size(16.dp))
                    Text(text = warning, fontSize = 11.sp, color = CharcoalBrownText, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Bottom Controls Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(20.dp),
            color = WarmOffWhiteBackground.copy(alpha = 0.96f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Area & Points Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CALCULATED AREA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedBrownText
                        )
                        Text(
                            text = if (calculatedHectares > 0.0) "${"%.3f".format(calculatedHectares)} Hectares" else "Walk perimeter to measure",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenSecondary
                        )
                    }

                    Surface(
                        color = ForestGreenContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${vertices.size} corners logged",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Helper microcopy
                Text(
                    text = "Walk to each field corner and tap 'Add Corner Point'. Minimum 3 points required.",
                    fontSize = 12.sp,
                    color = MutedBrownText,
                    lineHeight = 16.sp
                )

                // Actions: Add Point, Undo, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Undo Point
                    OutlinedButton(
                        onClick = {
                            if (vertices.isNotEmpty()) {
                                vertices = vertices.dropLast(1).toMutableList()
                            }
                        },
                        enabled = vertices.isNotEmpty(),
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo", fontSize = 12.sp)
                    }

                    // Add Current GPS Point
                    Button(
                        onClick = {
                            if (gpsAccuracy > 35f) {
                                // Prevent registering severely inaccurate point
                            } else {
                                vertices = (vertices + FarmVertex(currentLat, currentLng)).toMutableList()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LateriteRedPrimary)
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Point #${vertices.size + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save Boundary (WKT)
                    Button(
                        onClick = {
                            if (vertices.size >= 3) {
                                val closed = vertices + vertices.first()
                                val wkt = "POLYGON((" + closed.joinToString(", ") { "${it.lng} ${it.lat}" } + "))"
                                onBoundarySaved(wkt, calculatedHectares, vertices)
                            }
                        },
                        enabled = vertices.size >= 3,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenSecondary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
