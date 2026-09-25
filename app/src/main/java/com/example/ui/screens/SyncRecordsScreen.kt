package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncRecordsScreen(
    farmers: List<FarmerEntity>,
    practices: List<PracticeLogEntity>,
    pendingFarmerCount: Int,
    pendingPracticeCount: Int,
    totalPendingCount: Int,
    isSyncing: Boolean,
    agentId: String = "AGENT-NG-042",
    onSyncAll: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending Queue ($totalPendingCount)", "All Farmers (${farmers.size})", "Practice Trail (${practices.size})")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("sync_records_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalPendingCount > 0) Color(0xFFFFF8E1) else Color(0xFF0C2417)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (totalPendingCount > 0) Color(0xFFFFD54F) else Color(0xFF1E5234)
                )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (totalPendingCount > 0) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (totalPendingCount > 0) WarningAmber else Color(0xFFA5D6A7),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "OFFLINE-FIRST SYNCHRONIZATION ENGINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (totalPendingCount > 0) Color(0xFF795548) else Color(0xFFA5D6A7)
                            )
                        }

                        Surface(
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "AGENT: $agentId",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPendingCount > 0) Color(0xFF5D4037) else Color(0xFFC8E6C9),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (totalPendingCount > 0) "$totalPendingCount Record(s) Stored Locally" else "All Records Synchronized Upstream",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPendingCount > 0) Color.Black else Color.White
                    )

                    Text(
                        text = if (totalPendingCount > 0) {
                            "Unsynced farmers ($pendingFarmerCount) and practices ($pendingPracticeCount) are securely preserved in SQLite. Push when online to generate backend Farmer IDs."
                        } else {
                            "Central backend has verified all farmer enrollments and seasonal practice audits."
                        },
                        fontSize = 11.sp,
                        color = if (totalPendingCount > 0) Color.DarkGray else Color(0xFFC8E6C9),
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = onSyncAll,
                        enabled = !isSyncing && totalPendingCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("sync_all_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dispatching to Backend Ledger...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (totalPendingCount > 0) "Sync $totalPendingCount Queued Record(s) to Backend" else "Upstream Ledger in Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Tab Navigation for Queue and History
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = HarvestGreenPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) HarvestGreenPrimary else Color.Gray
                            )
                        },
                        modifier = Modifier.testTag("sync_tab_$index")
                    )
                }
            }
        }

        when (selectedTab) {
            // Tab 0: Pending Queue
            0 -> {
                val pendingFarmers = farmers.filter { it.syncStatus == "pending_sync" }
                val pendingPractices = practices.filter { it.syncStatus == "pending_sync" }

                if (pendingFarmers.isEmpty() && pendingPractices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ComplianceGreen, modifier = Modifier.size(36.dp))
                                Text("Queue is Empty", fontWeight = FontWeight.Bold)
                                Text("All local records have been pushed to backend.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    if (pendingFarmers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending Farmers (Waiting for Backend ID Generation):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E4A32)
                            )
                        }
                        items(pendingFarmers, key = { "f_${it.id}" }) { farmer ->
                            FarmerQueueCard(farmer = farmer)
                        }
                    }

                    if (pendingPractices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending Seasonal Practice Logs:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E4A32),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(pendingPractices, key = { "p_${it.id}" }) { log ->
                            PracticeQueueCard(log = log)
                        }
                    }
                }
            }

            // Tab 1: All Farmers
            1 -> {
                items(farmers, key = { "all_f_${it.id}" }) { farmer ->
                    FarmerQueueCard(farmer = farmer)
                }
            }

            // Tab 2: Practice Trail
            2 -> {
                items(practices, key = { "all_p_${it.id}" }) { log ->
                    PracticeQueueCard(log = log)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FarmerQueueCard(farmer: FarmerEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E9E2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(farmer.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Surface(
                        color = if (farmer.syncStatus == "synced") Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = farmer.syncStatus,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (farmer.syncStatus == "synced") ComplianceGreen else WarningAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${farmer.crop} • ${farmer.farmSize} ${farmer.farmSizeUnit} • Phone: ${farmer.phoneNumber}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "GPS: ${farmer.gpsCoordinates} • ${farmer.lga}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Surface(
                color = Color(0xFFF1F8E9),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = farmer.farmerCode.ifBlank { "PENDING" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComplianceGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PracticeQueueCard(log: PracticeLogEntity) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(log.dateApplied))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E9E2))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(log.practiceType.ifBlank { log.category }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Surface(
                        color = if (log.syncStatus == "synced") Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.syncStatus,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (log.syncStatus == "synced") ComplianceGreen else WarningAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(dateStr, fontSize = 11.sp, color = Color.Gray)
            }

            Text(
                text = "${log.farmerName} (${log.farmerDisplayId}) • ${log.crop}",
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            if (log.productName.isNotBlank()) {
                Text(
                    text = "${log.productName} • ${log.dosage}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Agent: ${log.agentId}", fontSize = 9.sp, color = Color.Gray)
                Text("GPS: ${log.gpsCoordinates}", fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}
