package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.ViolationRed
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChemicalPreset(
    val name: String,
    val active: String,
    val defaultDosage: String,
    val phiDays: Int,
    val isBanned: Boolean,
    val warningNote: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPracticeScreen(
    farmers: List<FarmerEntity>,
    recentLogs: List<PracticeLogEntity>,
    onLogPractice: (
        farmerCode: String,
        farmerName: String,
        crop: String,
        category: String,
        productName: String,
        activeIngredient: String,
        dosage: String,
        source: String,
        phiDays: Int
    ) -> Unit
) {
    if (farmers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Please enroll at least one farmer first before logging practices.")
        }
        return
    }

    var selectedFarmerIndex by remember { mutableStateOf(0) }
    var farmerDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Pesticide Application", "Storage & Aflatoxin", "Fertilizer / Soil", "Harvest Prep")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    val currentFarmer = farmers.getOrElse(selectedFarmerIndex) { farmers.first() }

    // Chemical Presets reflecting Nigerian agricultural realities and export standards
    val chemicalPresets = listOf(
        ChemicalPreset(
            name = "Lambda-Super 5EC",
            active = "Lambda-cyhalothrin",
            defaultDosage = "400 ml / hectare",
            phiDays = 14,
            isBanned = false,
            warningNote = "EU Compliant. Observed Pre-Harvest Interval must be >= 14 days."
        ),
        ChemicalPreset(
            name = "BioNeem Extract",
            active = "Azadirachtin (Bio-botanical)",
            defaultDosage = "1.5 L / hectare",
            phiDays = 3,
            isBanned = false,
            warningNote = "Zero synthetic residue. Qualifies for organic export premium."
        ),
        ChemicalPreset(
            name = "PICS Hermetic Bags",
            active = "Triple-layer barrier (Chemical Free)",
            defaultDosage = "50-100 bags",
            phiDays = 0,
            isBanned = false,
            warningNote = "Aflatoxin & Weevil prevention standard. Zero chemical residue."
        ),
        ChemicalPreset(
            name = "Sniper 1000EC (DDVP)",
            active = "Dichlorvos (DDVP)",
            defaultDosage = "500 ml / tank",
            phiDays = 90,
            isBanned = true,
            warningNote = "CRITICAL VIOLATION: Dichlorvos is strictly BANNED in the EU. Guaranteed export rejection!"
        ),
        ChemicalPreset(
            name = "Chlorpyrifos-Ethyl",
            active = "Chlorpyrifos",
            defaultDosage = "1.0 L / hectare",
            phiDays = 60,
            isBanned = true,
            warningNote = "CRITICAL VIOLATION: EU MRL set at limit of quantification (0.01 mg/kg). Immediate seizure risk."
        )
    )

    var selectedPreset by remember { mutableStateOf<ChemicalPreset?>(chemicalPresets[0]) }
    var productName by remember { mutableStateOf(chemicalPresets[0].name) }
    var activeIngredient by remember { mutableStateOf(chemicalPresets[0].active) }
    var dosage by remember { mutableStateOf(chemicalPresets[0].defaultDosage) }
    var phiDaysStr by remember { mutableStateOf(chemicalPresets[0].phiDays.toString()) }

    val sources = listOf("Agent Mobile App", "USSD (*384*748#)", "SMS Reply (34461)")
    var selectedSource by remember { mutableStateOf(sources[0]) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("log_practice_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HarvestGreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Input & MRL Practice Logging",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track pesticides, fertilizers & aflatoxin control protocols",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Farmer Selector
                    ExposedDropdownMenuBox(
                        expanded = farmerDropdownExpanded,
                        onExpandedChange = { farmerDropdownExpanded = !farmerDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${currentFarmer.fullName} (${currentFarmer.farmerCode}) - ${currentFarmer.crop}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Farmer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = farmerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("select_farmer_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = farmerDropdownExpanded,
                            onDismissRequest = { farmerDropdownExpanded = false }
                        ) {
                            farmers.forEachIndexed { index, farmer ->
                                DropdownMenuItem(
                                    text = { Text("${farmer.fullName} (${farmer.crop} • ${farmer.lga})") },
                                    onClick = {
                                        selectedFarmerIndex = index
                                        farmerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Chips
                    Text("Practice Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Fast Presets
                    Text("Quick Product Selector (MRL Standards)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chemicalPresets) { preset ->
                            Surface(
                                color = if (selectedPreset == preset) {
                                    if (preset.isBanned) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                } else Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedPreset == preset) {
                                        if (preset.isBanned) ViolationRed else ComplianceGreen
                                    } else Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedPreset = preset
                                        productName = preset.name
                                        activeIngredient = preset.active
                                        dosage = preset.defaultDosage
                                        phiDaysStr = preset.phiDays.toString()
                                    }
                                    .testTag("preset_${preset.name.take(6)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (preset.isBanned) Icons.Default.Error else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (preset.isBanned) ViolationRed else ComplianceGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = preset.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedPreset == preset) FontWeight.Bold else FontWeight.Normal,
                                        color = if (preset.isBanned) ViolationRed else Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // Product and active ingredient inputs
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Product / Input Commercial Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = activeIngredient,
                        onValueChange = { activeIngredient = it },
                        label = { Text("Active Ingredient / Formulation") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_ingredient_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dosage / Quantity") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("dosage_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phiDaysStr,
                            onValueChange = { phiDaysStr = it },
                            label = { Text("PHI (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("phi_days_input"),
                            singleLine = true
                        )
                    }

                    // MRL Real-time Warning or Approval Box
                    val isBannedSelected = selectedPreset?.isBanned == true ||
                            productName.contains("Sniper", ignoreCase = true) ||
                            productName.contains("Dichlorvos", ignoreCase = true) ||
                            activeIngredient.contains("Dichlorvos", ignoreCase = true) ||
                            activeIngredient.contains("Chlorpyrifos", ignoreCase = true)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBannedSelected) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isBannedSelected) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isBannedSelected) ViolationRed else ComplianceGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isBannedSelected) {
                                    "WARNING: Banned substance! Export containers using this will be intercepted before port loading to avoid millions in foreign rejection penalties."
                                } else {
                                    "COMPLIANT: Approved under Codex & EU MRL Regulations. Maintain Pre-Harvest Interval before aggregation."
                                },
                                fontSize = 11.sp,
                                color = if (isBannedSelected) ViolationRed else ComplianceGreen,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Source Selector
                    Text("Data Capture Channel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sources) { src ->
                            FilterChip(
                                selected = selectedSource == src,
                                onClick = { selectedSource = src },
                                label = { Text(src, fontSize = 11.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val phi = phiDaysStr.toIntOrNull() ?: 14
                            onLogPractice(
                                currentFarmer.farmerCode,
                                currentFarmer.fullName,
                                currentFarmer.crop,
                                selectedCategory,
                                productName,
                                activeIngredient,
                                dosage,
                                selectedSource,
                                phi
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_log_practice_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBannedSelected) ViolationRed else HarvestGreenPrimary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Practice & Trigger SMS Verification", fontSize = 14.sp)
                    }
                }
            }
        }

        // Section: Recent Field Practice Logs
        item {
            Text(
                text = "Recent Input & Practice Audit Trail (${recentLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(recentLogs.take(8), key = { it.id }) { log ->
            val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(log.dateApplied))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E9E2))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = log.farmerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${log.crop} • via ${log.source} • $dateStr",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        StatusBadge(status = log.riskLevel)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${log.productName} (${log.activeIngredient}) - ${log.dosage}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (log.riskNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.riskNotes,
                            fontSize = 11.sp,
                            color = if (log.riskLevel == "BANNED_MRL_VIOLATION") ViolationRed else Color(0xFF555F54)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
