package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.FarmerEntity
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.ViolationRed
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AggregateBatchScreen(
    farmers: List<FarmerEntity>,
    onCreateBatch: (
        farmerCode: String,
        farmerName: String,
        crop: String,
        region: String,
        bagCount: Int,
        netWeightKg: Double,
        moisturePercent: Double,
        foreignMatterPercent: Double,
        destinationMarket: String
    ) -> Unit
) {
    if (farmers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Please enroll farmers first before aggregating harvest batches.")
        }
        return
    }

    var selectedFarmerIndex by remember { mutableStateOf(0) }
    var farmerMenuExpanded by remember { mutableStateOf(false) }

    val currentFarmer = farmers.getOrElse(selectedFarmerIndex) { farmers.first() }

    var bagCountStr by remember { mutableStateOf("30") }
    var netWeightStr by remember { mutableStateOf("1500") }
    var moistureFloat by remember { mutableFloatStateOf(6.8f) }
    var foreignMatterStr by remember { mutableStateOf("1.2") }

    val destinations = listOf(
        "European Union (Rotterdam via Apapa Wharf)",
        "Japan (Yokohama / Kobe via Tin Can)",
        "Türkiye (Mersin via Lagos Apapa)",
        "United Kingdom (London Gateway via Apapa)",
        "China (Qingdao via Tin Can Island)",
        "United States (Port of Newark)",
        "Middle East (Jebel Ali, Dubai)"
    )
    var selectedDestination by remember { mutableStateOf(destinations[0]) }
    var destMenuExpanded by remember { mutableStateOf(false) }

    val isMoistureSafe = moistureFloat <= 9.0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("aggregate_batch_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Authentic Visual Banner of Nigerian Grain Aggregation Warehouse
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_grain_coop_store_1789856097598),
                        contentDescription = "Authentic Nigerian Grain Aggregation & Export Quality Testing Warehouse",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x99000000),
                                        Color(0xE6082416)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "COMMODITY AGGREGATION DEPOT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5D6A7)
                        )
                        Text(
                            text = "Physical Bagging & SPS Quality Intake",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Calibrated digital moisture testing & cryptographic QR lot creation",
                            fontSize = 11.sp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
        }

        // Zero-literacy farmer receipt notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = ComplianceGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Codex Alimentarius & EU TRACES Protocol: Each aggregated lot is assigned a tamper-evident SHA-256 digital passport. The farmer instantly receives a confirmed weight receipt via SMS.",
                        fontSize = 12.sp,
                        color = ComplianceGreen,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Form card
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
                        expanded = farmerMenuExpanded,
                        onExpandedChange = { farmerMenuExpanded = !farmerMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${currentFarmer.fullName} (${currentFarmer.farmerCode})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Delivering Smallholder Farmer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = farmerMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("delivering_farmer_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = farmerMenuExpanded,
                            onDismissRequest = { farmerMenuExpanded = false }
                        ) {
                            farmers.forEachIndexed { index, farmer ->
                                DropdownMenuItem(
                                    text = { Text("${farmer.fullName} (${farmer.crop} • ${farmer.lga}, ${farmer.state})") },
                                    onClick = {
                                        selectedFarmerIndex = index
                                        farmerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Bags & Weight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bagCountStr,
                            onValueChange = {
                                bagCountStr = it
                                val count = it.toIntOrNull() ?: 0
                                netWeightStr = (count * 50).toString()
                            },
                            label = { Text("Standard Bags (50kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bag_count_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = netWeightStr,
                            onValueChange = { netWeightStr = it },
                            label = { Text("Calibrated Weight (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("net_weight_input"),
                            singleLine = true
                        )
                    }

                    // Moisture % Slider with Aflatoxin Safety Guidelines
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Moisture Intake Test (CXS 327-2017)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${"%.1f".format(moistureFloat)}% ${if (isMoistureSafe) "(Export Compliant <9%)" else "(HAZARD >9%)"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMoistureSafe) ComplianceGreen else ViolationRed
                            )
                        }

                        Slider(
                            value = moistureFloat,
                            onValueChange = { moistureFloat = it },
                            valueRange = 4.0f..15.0f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = if (isMoistureSafe) ComplianceGreen else ViolationRed,
                                activeTrackColor = if (isMoistureSafe) ComplianceGreen else ViolationRed
                            ),
                            modifier = Modifier.testTag("moisture_slider")
                        )

                        Text(
                            text = if (isMoistureSafe) {
                                "Moisture within safe SPS limit. Aflatoxin proliferation risk minimal (<4 ppb expected)."
                            } else {
                                "Moisture exceeds export threshold! Must undergo further elevated solar tarp drying prior to container dispatch."
                            },
                            fontSize = 11.sp,
                            color = if (isMoistureSafe) Color(0xFF2E7D32) else ViolationRed
                        )
                    }

                    // Foreign Matter & Target Market
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = foreignMatterStr,
                            onValueChange = { foreignMatterStr = it },
                            label = { Text("Foreign Matter %") },
                            supportingText = { Text("Max 2.0% for Grade A", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("foreign_matter_input"),
                            singleLine = true
                        )

                        ExposedDropdownMenuBox(
                            expanded = destMenuExpanded,
                            onExpandedChange = { destMenuExpanded = !destMenuExpanded },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = selectedDestination,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Export Corridor") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("destination_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = destMenuExpanded,
                                onDismissRequest = { destMenuExpanded = false }
                            ) {
                                destinations.forEach { dest ->
                                    DropdownMenuItem(
                                        text = { Text(dest, fontSize = 12.sp) },
                                        onClick = {
                                            selectedDestination = dest
                                            destMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val bags = bagCountStr.toIntOrNull() ?: 20
                            val weight = netWeightStr.toDoubleOrNull() ?: (bags * 50.0)
                            val foreignMatter = foreignMatterStr.toDoubleOrNull() ?: 1.0
                            onCreateBatch(
                                currentFarmer.farmerCode,
                                currentFarmer.fullName,
                                currentFarmer.crop,
                                "${currentFarmer.state} (${currentFarmer.lga})",
                                bags,
                                weight,
                                moistureFloat.toDouble(),
                                foreignMatter,
                                selectedDestination
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_aggregate_batch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Batch QR & Send Farmer SMS", fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
