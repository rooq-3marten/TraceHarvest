package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.HarvestBatchEntity
import com.example.ui.components.BlockchainAnchorChip
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BlockchainBlue
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenContainer
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.SesameAmberSecondary
import com.example.ui.theme.ViolationRed
import com.example.ui.viewmodel.DashboardMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    batches: List<HarvestBatchEntity>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onSelectBatch: (HarvestBatchEntity) -> Unit,
    onToggleFlag: (HarvestBatchEntity) -> Unit,
    onAnchorBatch: (HarvestBatchEntity) -> Unit,
    onNewBatchClick: () -> Unit
) {
    val filteredBatches = when (selectedFilter) {
        "All" -> batches
        "Sesame" -> batches.filter { it.crop.contains("Sesame", ignoreCase = true) }
        "Cowpea" -> batches.filter { it.crop.contains("Cowpea", ignoreCase = true) }
        "Ginger" -> batches.filter { it.crop.contains("Ginger", ignoreCase = true) }
        "Flagged Only" -> batches.filter { it.isFlaggedForRejection || it.mrlStatus == "VIOLATION_BLOCKED" }
        "Export Ready" -> batches.filter { it.mrlStatus == "PASSED_SPS" && !it.isFlaggedForRejection }
        else -> batches
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Banner with Authentic Nigerian Farmer Photograph
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nigerian_farmer_hero_1789856067408),
                    contentDescription = "Nigerian Sesame Farmer in Dambatta Kano",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x990D3320),
                                    Color(0xF0082416)
                                )
                            )
                        )
                )

                // Hero content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NAQS / CODEX STAN 218-1999",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = Color(0x66FFFFFF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "KANO & JIGAWA EXPORT BELT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TraceHarvest Provenance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time field compliance • Direct farmer SMS integration • Port of Lagos transit",
                        fontSize = 12.sp,
                        color = Color(0xFFC8E6C9)
                    )
                }
            }
        }

        // Metrics 2x2 Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Export-Ready",
                        value = "${metrics.exportReadyCount} Batches",
                        subtitle = "${"%.1f".format(metrics.totalMetricTons)} MT Certified",
                        icon = Icons.Default.CheckCircle,
                        accentColor = ComplianceGreen,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_export_ready"
                    )
                    StatCard(
                        title = "Pre-Shipment Saved",
                        value = "$${metrics.estimatedDollarsSaved.toInt()}",
                        subtitle = "${metrics.blockedRiskCount} High-Risk Intercepted",
                        icon = Icons.Default.Shield,
                        accentColor = ViolationRed,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_rejections_prevented"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Registered Farmers",
                        value = "${metrics.totalFarmers}",
                        subtitle = "Geotagged & SMS active",
                        icon = Icons.Default.People,
                        accentColor = HarvestGreenPrimary,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_farmers"
                    )
                    StatCard(
                        title = "SPS Pass Rate",
                        value = "${"%.1f".format(metrics.complianceRatePercent)}%",
                        subtitle = "Zero MRL Violations Target",
                        icon = Icons.Default.LocalShipping,
                        accentColor = SesameAmberSecondary,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_compliance_rate"
                    )
                }
            }
        }

        // Confirmed Nigerian Export Port & Terminal Transit Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7F3)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E2D5))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = HarvestGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Export Transit Corridor Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = HarvestGreenPrimary
                            )
                        }
                        Surface(
                            color = ComplianceGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACTIVE DISPATCH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComplianceGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Kano Dry Port → Apapa Wharf",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2D24)
                            )
                            Text(
                                text = "Sesame: 4,250 MT in bonded transit",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "NAQS Lab Phyto: Cleared",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = ComplianceGreen
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Makurdi Depot → Tin Can Port",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2D24)
                            )
                            Text(
                                text = "Cowpea: 1,200 MT hermetic PICS",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "EU TRACES NT: Pre-Logged",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }
        }

        // Crop Filter Chips
        item {
            val filters = listOf("All", "Sesame", "Cowpea", "Ginger", "Export Ready", "Flagged Only")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HarvestGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consignment Batches (${filteredBatches.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap batch for SPS Certificate",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Batch Items
        if (filteredBatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No batches match filter '$selectedFilter'.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredBatches, key = { it.id }) { batch ->
                BatchCardItem(
                    batch = batch,
                    onSelect = { onSelectBatch(batch) },
                    onToggleFlag = { onToggleFlag(batch) },
                    onAnchor = { onAnchorBatch(batch) }
                )
            }
        }
    }
}

@Composable
fun BatchCardItem(
    batch: HarvestBatchEntity,
    onSelect: () -> Unit,
    onToggleFlag: () -> Unit,
    onAnchor: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val harvestDateStr = dateFormat.format(Date(batch.harvestDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onSelect() }
            .testTag("batch_card_${batch.batchCode}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (batch.isFlaggedForRejection) Color(0xFFFFCDD2) else Color(0xFFE2E6DF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: Code + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = batch.batchCode,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (batch.isFlaggedForRejection) ViolationRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${batch.crop} • ${batch.region}",
                        fontSize = 12.sp,
                        color = Color(0xFF61685F)
                    )
                }
                StatusBadge(status = batch.mrlStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F8F5), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Weight", fontSize = 10.sp, color = Color.Gray)
                    Text("${batch.netWeightKg.toInt()} kg", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Bags", fontSize = 10.sp, color = Color.Gray)
                    Text("${batch.bagCount} bags", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Moisture", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "${batch.moisturePercent}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (batch.moisturePercent <= 10.0) ComplianceGreen else ViolationRed
                    )
                }
                Column {
                    Text("Target Market", fontSize = 10.sp, color = Color.Gray)
                    Text(batch.destinationMarket.take(12), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Farmer & Blockchain tag
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Farmer: ${batch.farmerName}",
                    fontSize = 11.sp,
                    color = Color(0xFF424242)
                )
                BlockchainAnchorChip(
                    isAnchored = batch.isBlockchainAnchored,
                    txId = batch.blockchainTxId
                )
            }

            // Action row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .testTag("view_passport_button_${batch.batchCode}"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HarvestGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SPS Passport", fontSize = 11.sp, color = HarvestGreenPrimary)
                }

                if (!batch.isBlockchainAnchored) {
                    Button(
                        onClick = onAnchor,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("anchor_button_${batch.batchCode}"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlockchainBlue),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Anchor", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = onToggleFlag,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("flag_button_${batch.batchCode}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (batch.isFlaggedForRejection) ComplianceGreen else ViolationRed
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (batch.isFlaggedForRejection) "Clear" else "Flag Risk",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
