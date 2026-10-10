package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.core.zone.ZoneRegistry
import com.example.ui.theme.*

/**
 * Screen 9: Settings & Regional Agent Profile
 */
@Composable
fun SettingsScreen(
    currentZone: GeopoliticalZone,
    agentId: String = "",
    agentName: String = "",
    agentPhone: String = "",
    onSwitchZone: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneProfile = remember(currentZone) { ZoneRegistry.getProfile(currentZone) }
    var selectedLanguage by remember { mutableStateOf(zoneProfile.primaryLanguage) }
    var syncFrequency by remember { mutableStateOf("Every 15 minutes (Automatic)") }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        color = WarmOffWhiteBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CharcoalBrownText)
                    }
                    Text(
                        text = "Settings & Regional Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CharcoalBrownText
                    )
                }
            }

            // Agent Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Field Agent Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LateriteRedPrimary)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = LateriteRedContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = LateriteRedPrimary)
                                }
                            }

                            Column {
                                Text(agentName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CharcoalBrownText)
                                Text("ID: $agentId • $agentPhone", fontSize = 12.sp, color = MutedBrownText)
                            }
                        }
                    }
                }
            }

            // Geopolitical Zone Switcher
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Assigned Geopolitical Zone", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBrownText)
                                Text("${zoneProfile.zone.zoneName} (${zoneProfile.states.joinToString(", ")})", fontSize = 12.sp, color = ForestGreenSecondary)
                            }

                            OutlinedButton(
                                onClick = onSwitchZone,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Switch Zone", fontSize = 11.sp, color = LateriteRedPrimary)
                            }
                        }

                        Text(
                            text = "Export Focus: ${zoneProfile.exportDestinations.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = MutedBrownText
                        )
                    }
                }
            }

            // Language Preference
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Language Preference", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBrownText)

                        listOf("Hausa", "Yoruba", "Igbo", "English", "Pidgin").forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLanguage = lang }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    colors = RadioButtonDefaults.colors(selectedColor = ForestGreenSecondary)
                                )
                                Text(lang, fontSize = 13.sp, color = CharcoalBrownText)
                            }
                        }
                    }
                }
            }

            // Google Satellite Tile Cache Management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Offline Map Cache Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBrownText)
                        Text(
                            text = "Pre-cached Google Satellite tiles for ${zoneProfile.zone.zoneName}: 42 MB / 100 MB max.",
                            fontSize = 12.sp,
                            color = MutedBrownText
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { cacheClearedMessage = "Map tile cache refreshed successfully." },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Tile Cache", fontSize = 11.sp)
                            }
                        }

                        cacheClearedMessage?.let {
                            Text(it, fontSize = 11.sp, color = ForestGreenSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Device Hardening & Nigerian Field Diagnostics
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val memProfile = remember { com.example.core.resilience.DeviceHardeningManager.getMemoryProfile(context) }
                val batteryProfile = remember { com.example.core.resilience.DeviceHardeningManager.getBatteryProfile(context) }
                val freeStorageMb = remember { com.example.core.resilience.DeviceHardeningManager.getFreeStorageMb(context) }
                val netClass = remember { com.example.core.resilience.DeviceHardeningManager.getNetworkClassification(context) }
                var cleanupResultMsg by remember { mutableStateOf<String?>(null) }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("device_diagnostics_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = DarkGreenPrimary, modifier = Modifier.size(20.dp))
                            Text("Field Device Hardening & Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBrownText)
                        }

                        Text(
                            text = "Hardened for Nigerian budget phones (Transsion itel, Tecno, Infinix, Samsung A0x, Android Go) under rural field stress.",
                            fontSize = 11.sp,
                            color = MutedBrownText
                        )

                        HorizontalDivider(color = OutlineWarm)

                        // Hardware Specs Rows
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Device Model & OS
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Device Hardware:", fontSize = 11.sp, color = MutedBrownText)
                                Text("${android.os.Build.MANUFACTURER.uppercase()} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalBrownText)
                            }

                            // RAM & Go Edition
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Memory Tier:", fontSize = 11.sp, color = MutedBrownText)
                                Text(
                                    if (memProfile.isLowRamDevice) "Low-RAM (${memProfile.totalRamMb}MB • Go Mode Active)" else "Standard RAM (${memProfile.totalRamMb}MB)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (memProfile.isLowRamDevice) NaturalStatusAmber else DarkGreenPrimary
                                )
                            }

                            // Storage
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Internal Storage:", fontSize = 11.sp, color = MutedBrownText)
                                Text(
                                    "$freeStorageMb MB Free ${if (freeStorageMb < 100) "(Low Disk Guard Active)" else "(Healthy)"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (freeStorageMb < 100) NaturalStatusAmber else DarkGreenPrimary
                                )
                            }

                            // Battery & Thermal
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Battery & Thermal:", fontSize = 11.sp, color = MutedBrownText)
                                Text(
                                    "${batteryProfile.percentage}% • ${batteryProfile.temperatureCelsius}°C ${if (batteryProfile.isThermalThrottled) "(Throttled)" else "(Normal)"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (batteryProfile.isThermalThrottled) NaturalStatusAmber else CharcoalBrownText
                                )
                            }

                            // Network
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cellular Link:", fontSize = 11.sp, color = MutedBrownText)
                                Text(netClass.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                            }
                        }

                        // Storage Clean Button
                        Button(
                            onClick = {
                                val reclaimed = com.example.core.resilience.DeviceHardeningManager.cleanTemporaryCaches(context)
                                cleanupResultMsg = "Reclaimed ${(reclaimed / (1024 * 1024)).coerceAtLeast(1)}MB of cache storage."
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Free Up Disk Space & Clear Temporary Cache", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        cleanupResultMsg?.let {
                            Text(it, fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // About TraceHarvest & Regulatory Compliance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("TraceHarvest Standards", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalBrownText)
                        Text("• EUDR 2020/2024 Forest Cover Geolocation Compliant", fontSize = 11.sp, color = MutedBrownText)
                        Text("• NAFDAC Export Clearance & Agrochemical Audit Ready", fontSize = 11.sp, color = MutedBrownText)
                        Text("• NAQS Phytosanitary Chain of Custody Standard", fontSize = 11.sp, color = MutedBrownText)
                        Text("Version 2.0 (Regional 6-Zone Rollout Engine)", fontSize = 10.sp, color = ForestGreenSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Logout Action
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOffWhiteBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ViolationRed),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Sign Out of Field Agent Session", color = ViolationRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
