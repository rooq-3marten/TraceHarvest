package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.zone.GeopoliticalZone
import com.example.core.zone.ZoneRegistry
import com.example.ui.theme.*
import java.util.Calendar

/**
 * Screen 3: Home Dashboard
 * Human-centered, warm agricultural overview with time-aware greeting,
 * today's operational summaries, large icon-first quick actions, and zone advisory.
 */
@Composable
fun HomeDashboardScreen(
    agentName: String = "Aminu Bello",
    currentZone: GeopoliticalZone,
    totalFarmersCount: Int,
    totalPracticesCount: Int,
    totalBatchesCount: Int,
    pendingSyncCount: Int,
    onNavigateToRegisterFarmer: () -> Unit,
    onNavigateToLogPractice: () -> Unit,
    onNavigateToCreateBatch: () -> Unit,
    onNavigateToSync: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneProfile = remember(currentZone) { ZoneRegistry.getProfile(currentZone) }

    // Time-aware greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmOffWhiteBackground)
            .padding(horizontal = 20.dp)
            .testTag("home_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Top Greeting & Region Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$greeting,",
                        fontSize = 14.sp,
                        color = MutedBrownText
                    )
                    Text(
                        text = agentName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalBrownText
                    )
                }

                // Zone Badge
                Surface(
                    onClick = onOpenSettings,
                    color = SoftCreamSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ForestGreenSecondary, CircleShape)
                        )
                        Text(
                            text = "${zoneProfile.zone.code} • ${zoneProfile.states.first()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalBrownText
                        )
                    }
                }
            }
        }

        // Persistent Sync Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSync),
                shape = RoundedCornerShape(16.dp),
                color = if (pendingSyncCount > 0) WarmOchreContainer else ForestGreenContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (pendingSyncCount > 0) WarmOchreAccent.copy(alpha = 0.5f) else ForestGreenLight.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (pendingSyncCount > 0) Icons.Default.CloudUpload else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (pendingSyncCount > 0) WarmOchreDark else ForestGreenSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (pendingSyncCount > 0) "$pendingSyncCount records waiting to sync" else "Everything's sent!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalBrownText
                            )
                            Text(
                                text = if (pendingSyncCount > 0) "Saved locally. Tap to upload to central registry." else "Central export registry up to date.",
                                fontSize = 11.sp,
                                color = MutedBrownText
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open sync",
                        tint = MutedBrownText
                    )
                }
            }
        }

        // Today's Operational Summary
        item {
            Text(
                text = "Your Regional Progress",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CharcoalBrownText
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Enrolled",
                    count = totalFarmersCount,
                    unit = "Farmers",
                    containerColor = SoftCreamSurface,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Audited",
                    count = totalPracticesCount,
                    unit = "Spray Logs",
                    containerColor = SoftCreamSurface,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Batched",
                    count = totalBatchesCount,
                    unit = "Consignments",
                    containerColor = SoftCreamSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions (Large, Icon-First, Accessible)
        item {
            Text(
                text = "Field Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CharcoalBrownText
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCard(
                    title = "Register a farmer",
                    subtitle = "Capture GPS coordinates, boundary walk, and crop profile",
                    icon = Icons.Default.PersonAdd,
                    iconTint = LateriteRedPrimary,
                    containerColor = SoftCreamSurface,
                    onClick = onNavigateToRegisterFarmer,
                    testTag = "action_register_farmer"
                )

                ActionCard(
                    title = "Log a farming practice",
                    subtitle = "Audit NAFDAC chemical applications, fertilizer, and harvests",
                    icon = Icons.Default.Agriculture,
                    iconTint = ForestGreenSecondary,
                    containerColor = SoftCreamSurface,
                    onClick = onNavigateToLogPractice,
                    testTag = "action_log_practice"
                )

                ActionCard(
                    title = "Create an export batch",
                    subtitle = "Aggregate verified smallholders into traceable shipping lots",
                    icon = Icons.Default.Inventory2,
                    iconTint = WarmOchreDark,
                    containerColor = SoftCreamSurface,
                    onClick = onNavigateToCreateBatch,
                    testTag = "action_create_batch"
                )
            }
        }

        // Regional Seasonal Advice Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ForestGreenSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${zoneProfile.zone.zoneName} Seasonal Calendar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CharcoalBrownText
                        )
                    }

                    Text(
                        text = "Planting: ${zoneProfile.seasonalCalendar.plantingMonths} • Harvest: ${zoneProfile.seasonalCalendar.harvestMonths}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenSecondary
                    )

                    Text(
                        text = zoneProfile.seasonalCalendar.currentSeasonAdvice,
                        fontSize = 12.sp,
                        color = MutedBrownText,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    unit: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 11.sp, color = MutedBrownText, fontWeight = FontWeight.Medium)
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalBrownText
            )
            Text(unit, fontSize = 10.sp, color = ForestGreenSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalBrownText
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MutedBrownText,
                    lineHeight = 16.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MutedBrownText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
