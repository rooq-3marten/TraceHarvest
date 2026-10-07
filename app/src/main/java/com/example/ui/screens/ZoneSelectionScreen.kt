package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.zone.GeopoliticalZone
import com.example.core.zone.ZoneProfile
import com.example.core.zone.ZoneRegistry
import com.example.ui.theme.*

/**
 * Screen 1: Zone Selection (First Launch / Setup)
 * Welcomes the field agent and configures crops, languages, aggregation hubs,
 * and export destination defaults for their specific Nigerian geopolitical zone.
 */
@Composable
fun ZoneSelectionScreen(
    currentSelectedZone: GeopoliticalZone,
    onZoneConfirmed: (GeopoliticalZone) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedZone by remember { mutableStateOf(currentSelectedZone) }
    val allZones = remember { ZoneRegistry.getAllZones() }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("zone_selection_screen"),
        color = WarmOffWhiteBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = LateriteRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "TraceHarvest Nigeria",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ForestGreenSecondary
                    )
                }

                Text(
                    text = "Welcome to TraceHarvest",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = CharcoalBrownText
                )

                Text(
                    text = "Select your operational geopolitical zone. This personalizes your crops, languages, NAFDAC chemicals, and export hubs.",
                    fontSize = 14.sp,
                    color = MutedBrownText,
                    lineHeight = 20.sp
                )
            }

            // 6 Geopolitical Zone Cards
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allZones) { profile ->
                    ZoneCardItem(
                        profile = profile,
                        isSelected = profile.zone == selectedZone,
                        onSelect = { selectedZone = profile.zone }
                    )
                }
            }

            // Confirmation Footer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You're setting up for ${selectedZone.zoneName}. You can change this later in Settings.",
                    fontSize = 12.sp,
                    color = MutedBrownText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onZoneConfirmed(selectedZone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_zone_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LateriteRedPrimary)
                ) {
                    Text(
                        text = "Continue to ${selectedZone.zoneName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneCardItem(
    profile: ZoneProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LateriteRedPrimary else OutlineWarm,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SoftCreamSurface else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.zone.zoneName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CharcoalBrownText
                    )
                    Text(
                        text = profile.culturalGreeting,
                        fontSize = 12.sp,
                        color = ForestGreenSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = LateriteRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // States list
            Text(
                text = "States: ${profile.states.joinToString(", ")}",
                fontSize = 12.sp,
                color = MutedBrownText
            )

            // Primary export crops chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profile.primaryCrops.take(4).forEach { crop ->
                    Surface(
                        color = ForestGreenContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = crop,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
