package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.zone.GeopoliticalZone
import com.example.core.zone.ZoneRegistry
import com.example.data.local.entity.FarmerEntity
import com.example.ui.components.FarmerAvatarView
import com.example.ui.theme.*

/**
 * Screen 7: Consignment Batch Creation
 * Aggregates verified smallholders into EU-compliant export shipping lots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCreationScreen(
    currentZone: GeopoliticalZone,
    availableFarmers: List<FarmerEntity>,
    onCreateBatch: (
        batchCode: String,
        crop: String,
        quantityKg: Double,
        qualityGrade: String,
        hubName: String,
        contributingFarmerIds: List<String>
    ) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneProfile = remember(currentZone) { ZoneRegistry.getProfile(currentZone) }
    var selectedCrop by remember { mutableStateOf(zoneProfile.primaryCrops.first()) }
    var quantityStr by remember { mutableStateOf("2500") }
    var selectedGrade by remember { mutableStateOf("Grade A Export Ready") }
    var selectedHub by remember { mutableStateOf(zoneProfile.aggregationHubs.first().name) }
    var selectedFarmerIds by remember { mutableStateOf(setOf<String>()) }
    var isCreatedSuccess by remember { mutableStateOf(false) }
    var createdBatchCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gradeOptions = listOf(
        Pair("Grade A Export Ready", "Moisture < 8.0%, zero chemical residue, certified organic/GAP compliant"),
        Pair("Grade B Secondary Processing", "Moisture 8.1–10.0%, compliant for domestic food milling"),
        Pair("Grade C Under-Conditioned", "Moisture > 10.0%, quarantined for mechanical drying")
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("batch_creation_screen"),
        color = WarmOffWhiteBackground
    ) {
        if (isCreatedSuccess) {
            // Success QR Code Modal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ForestGreenSecondary,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = "Export Batch Created!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CharcoalBrownText
                        )

                        Text(
                            text = "Lot: $createdBatchCode",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LateriteRedPrimary
                        )

                        // QR Code Visual Box
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, CharcoalBrownText),
                            modifier = Modifier.size(160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCode2,
                                    contentDescription = "QR Passport",
                                    modifier = Modifier.size(130.dp),
                                    tint = CharcoalBrownText
                                )
                            }
                        }

                        Text(
                            text = "Ready for physical jute bag tagging and port customs clearance.",
                            fontSize = 12.sp,
                            color = MutedBrownText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenSecondary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Done & Return to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Creation Form
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
                        Column {
                            Text(
                                text = "Create Export Consignment Batch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = CharcoalBrownText
                            )
                            Text(
                                text = "${zoneProfile.zone.zoneName} Cluster Aggregation",
                                fontSize = 12.sp,
                                color = ForestGreenSecondary
                            )
                        }
                    }
                }

                // Crop & Quantity Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("1. Consignment Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBrownText)

                            // Crop Selection
                            Text("Target Export Crop:", fontSize = 11.sp, color = MutedBrownText)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                zoneProfile.primaryCrops.take(3).forEach { crop ->
                                    val isSel = crop == selectedCrop
                                    Surface(
                                        onClick = { selectedCrop = crop },
                                        color = if (isSel) LateriteRedPrimary else WarmOffWhiteBackground,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) LateriteRedPrimary else OutlineWarm),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = crop,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else CharcoalBrownText,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Quantity
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("Total Quantity (Kilograms)") },
                                placeholder = { Text("e.g. 5000") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Hub Selection
                            Text("Regional Aggregation Hub:", fontSize = 11.sp, color = MutedBrownText)
                            zoneProfile.aggregationHubs.forEach { hub ->
                                val isHubSel = hub.name == selectedHub
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedHub = hub.name }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isHubSel,
                                        onClick = { selectedHub = hub.name },
                                        colors = RadioButtonDefaults.colors(selectedColor = ForestGreenSecondary)
                                    )
                                    Column {
                                        Text(hub.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CharcoalBrownText)
                                        Text("${hub.state} State • ${hub.warehouseCapacityTons}t Capacity", fontSize = 10.sp, color = MutedBrownText)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quality Grade Selection
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
                            Text("2. Quality Grade Verification", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBrownText)

                            gradeOptions.forEach { (grade, desc) ->
                                val isGradeSel = grade == selectedGrade
                                Surface(
                                    onClick = { selectedGrade = grade },
                                    color = if (isGradeSel) ForestGreenContainer else Color.White,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isGradeSel) ForestGreenSecondary else OutlineWarm),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = isGradeSel,
                                            onClick = { selectedGrade = grade },
                                            colors = RadioButtonDefaults.colors(selectedColor = ForestGreenSecondary)
                                        )
                                        Column {
                                            Text(grade, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalBrownText)
                                            Text(desc, fontSize = 11.sp, color = MutedBrownText, lineHeight = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Contributing Smallholders (Multi-select)
                item {
                    Text(
                        text = "3. Select Contributing Smallholders (${selectedFarmerIds.size} selected)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CharcoalBrownText
                    )
                }

                if (availableFarmers.isEmpty()) {
                    item {
                        Text(
                            text = "No farmers enrolled yet in this region. Enroll farmers first to link their provenance.",
                            fontSize = 12.sp,
                            color = MutedBrownText
                        )
                    }
                } else {
                    items(availableFarmers) { farmer ->
                        val isChecked = selectedFarmerIds.contains(farmer.localId)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFarmerIds = if (isChecked) {
                                        selectedFarmerIds - farmer.localId
                                    } else {
                                        selectedFarmerIds + farmer.localId
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isChecked) WarmOchreContainer else SoftCreamSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selectedFarmerIds = if (it) selectedFarmerIds + farmer.localId else selectedFarmerIds - farmer.localId
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = LateriteRedPrimary)
                                )

                                FarmerAvatarView(
                                    photoPath = farmer.photoPath,
                                    farmerName = farmer.fullName,
                                    zone = currentZone,
                                    size = 40.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(farmer.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalBrownText)
                                    Text("${farmer.crop} • ${farmer.lga}, ${farmer.state}", fontSize = 11.sp, color = MutedBrownText)
                                }
                            }
                        }
                    }
                }

                // Error message
                errorMessage?.let { err ->
                    item {
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                // Submit Button
                item {
                    Button(
                        onClick = {
                            val qty = quantityStr.toDoubleOrNull() ?: 0.0
                            if (qty <= 0.0) {
                                errorMessage = "Please enter a valid consignment quantity"
                                return@Button
                            }
                            val stateCode = zoneProfile.states.first().take(3).uppercase()
                            val code = "NG-${selectedCrop.take(3).uppercase()}-$stateCode-2026-${(1000..9999).random()}"
                            createdBatchCode = code

                            onCreateBatch(
                                code,
                                selectedCrop,
                                qty,
                                selectedGrade,
                                selectedHub,
                                selectedFarmerIds.toList()
                            )
                            isCreatedSuccess = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_create_batch_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LateriteRedPrimary)
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Export Batch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
