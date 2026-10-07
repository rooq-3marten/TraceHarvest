package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.FarmVertex
import com.example.ui.components.GoogleSatelliteFarmMap

/**
 * Screen 5: Dedicated Fullscreen Farm Boundary Mapping Screen
 * Integrates Google Satellite & Hybrid imagery for walking farm perimeters.
 */
@Composable
fun FarmBoundaryMappingScreen(
    initialLat: Double,
    initialLng: Double,
    cropType: String,
    farmerName: String,
    onBoundarySaved: (wkt: String, areaHectares: Double, vertices: List<FarmVertex>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    GoogleSatelliteFarmMap(
        initialLat = initialLat,
        initialLng = initialLng,
        cropType = cropType,
        farmerName = farmerName,
        onBoundarySaved = onBoundarySaved,
        onCancel = onCancel,
        modifier = modifier.fillMaxSize()
    )
}
