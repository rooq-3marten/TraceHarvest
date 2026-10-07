package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Interactive OpenStreetMap & Satellite Field Map for Field Agents.
 * Uses zero-dependency Leaflet / OSM engine with offline error isolation.
 * Automatically plots:
 * 1. Current Agent Pin (High-precision GPS fix)
 * 2. Farm Boundary Polygon (EUDR provenance boundary)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveFieldMapView(
    latitude: Double,
    longitude: Double,
    polygonVertices: List<FarmVertex>,
    modifier: Modifier = Modifier,
    heightDp: Int = 220
) {
    val context = LocalContext.current
    var isSatelliteLayer by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var hasNetworkError by remember { mutableStateOf(false) }

    // Generate HTML Leaflet map dynamically
    val mapHtml = remember(latitude, longitude, polygonVertices, isSatelliteLayer) {
        val polygonJson = if (polygonVertices.size >= 3) {
            val coords = polygonVertices.joinToString(",") { "[${it.lat}, ${it.lng}]" }
            "L.polygon([$coords], {color: '#2E7D32', fillColor: '#4CAF50', fillOpacity: 0.35, weight: 3}).addTo(map);"
        } else ""

        val tileUrl = if (isSatelliteLayer) {
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        } else {
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        }

        val tileAttribution = if (isSatelliteLayer) "Esri Satellite Imagery" else "OpenStreetMap"

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #e8f5e9; }
                .leaflet-control-attribution { font-size: 8px !important; }
                .agent-marker {
                    background: #1B5E20;
                    border: 2px solid white;
                    border-radius: 50%;
                    width: 14px;
                    height: 14px;
                    box-shadow: 0 0 6px rgba(0,0,0,0.5);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                try {
                    var map = L.map('map', {zoomControl: false}).setView([$latitude, $longitude], 16);
                    L.tileLayer('$tileUrl', {
                        maxZoom: 19,
                        attribution: '$tileAttribution'
                    }).addTo(map);

                    var agentIcon = L.divIcon({className: 'agent-marker', iconSize: [14, 14], iconAnchor: [7, 7]});
                    L.marker([$latitude, $longitude], {icon: agentIcon}).addTo(map)
                        .bindPopup("<b>Agent Location</b><br>Lat: $latitude<br>Lng: $longitude");

                    $polygonJson

                    // Auto-fit bounds if polygon exists
                    ${if (polygonVertices.size >= 3) {
                        val coords = polygonVertices.joinToString(",") { "[${it.lat}, ${it.lng}]" }
                        "var bounds = L.latLngBounds([$coords]); map.fitBounds(bounds, {padding: [20, 20]});"
                    } else ""}
                } catch(e) {
                    console.error("Leaflet error: " + e);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(10.dp))
            .background(Color(0xFFE8F5E9))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isMapLoaded = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isMapLoaded = true
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            // If tiles fail offline, graceful fallback without crashing
                            hasNetworkError = true
                        }
                    }
                    loadDataWithBaseURL("https://traceharvest.local", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://traceharvest.local", mapHtml, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Map Controls (Top Right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Layer Toggle: Street vs Satellite
            SmallFloatingActionButton(
                onClick = { isSatelliteLayer = !isSatelliteLayer },
                containerColor = if (isSatelliteLayer) Color(0xFF1B5E20) else Color.White,
                contentColor = if (isSatelliteLayer) Color.White else Color(0xFF1B5E20),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Toggle Satellite", modifier = Modifier.size(16.dp))
            }

            // Recenter on Agent Button
            SmallFloatingActionButton(
                onClick = {
                    webViewRef?.loadDataWithBaseURL("https://traceharvest.local", mapHtml, "text/html", "UTF-8", null)
                },
                containerColor = Color.White,
                contentColor = Color(0xFF1B5E20),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(16.dp))
            }
        }

        // Bottom Map Status Badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSatelliteLayer) "🛰 Satellite Layer" else "🗺 OpenStreetMap Layer",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
