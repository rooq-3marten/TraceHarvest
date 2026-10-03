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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.remote.NetworkClient
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

    var showServerSettings by remember { mutableStateOf(false) }
    var serverInputUrl by remember { mutableStateOf(NetworkClient.getServerUrl()) }
    var serverStatusMessage by remember { mutableStateOf<String?>(null) }

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
                                text = "OFFLINE QUEUE & SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (totalPendingCount > 0) Color(0xFF795548) else Color(0xFFA5D6A7)
                            )
                        }
                    }

                    Text(
                        text = if (totalPendingCount > 0) "$totalPendingCount Record(s) Stored Locally" else "All Records Synchronized",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPendingCount > 0) Color.Black else Color.White
                    )

                    Text(
                        text = if (totalPendingCount > 0) {
                            "$totalPendingCount local record(s) queued on device ($pendingFarmerCount farmers, $pendingPracticeCount practices). Tap below to sync when online."
                        } else {
                            "Central registry has verified all farmer enrollments and seasonal practice audits."
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
                            Text("Syncing records...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (totalPendingCount > 0) "Sync Now ($totalPendingCount Queued)" else "All Records Up-to-Date",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Admin Website Connection Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Admin Website Connection",
                                tint = HarvestGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "ADMIN WEBSITE CONNECTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "Target: ${NetworkClient.getServerUrl()}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showServerSettings = !showServerSettings },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (showServerSettings) Icons.Default.Link else Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showServerSettings) "Close" else "Configure", fontSize = 11.sp)
                        }
                    }

                    if (showServerSettings) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Enter your deployed Admin Website / API Base URL. Sync records will be transmitted directly to this endpoint via POST /api/v1/sync/upstream.",
                            fontSize = 10.sp,
                            color = Color(0xFF424242),
                            lineHeight = 14.sp
                        )

                        OutlinedTextField(
                            value = serverInputUrl,
                            onValueChange = {
                                serverInputUrl = it
                                serverStatusMessage = null
                            },
                            label = { Text("Admin API Base URL", fontSize = 11.sp) },
                            placeholder = { Text("https://my-admin-website.com/", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    NetworkClient.setServerUrl(serverInputUrl)
                                    serverStatusMessage = "Target URL updated: ${NetworkClient.getServerUrl()}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Endpoint", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    serverInputUrl = NetworkClient.DEFAULT_SERVER_URL
                                    NetworkClient.setServerUrl(NetworkClient.DEFAULT_SERVER_URL)
                                    serverStatusMessage = "Reset to default endpoint"
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Reset Default", fontSize = 11.sp)
                            }
                        }

                        serverStatusMessage?.let { msg ->
                            Text(
                                text = "✓ $msg",
                                fontSize = 10.sp,
                                color = ComplianceGreen,
                                fontWeight = FontWeight.SemiBold
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

            if (!log.productName.isNullOrBlank()) {
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
