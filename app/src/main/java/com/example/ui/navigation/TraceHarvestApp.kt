package com.example.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.AppGuideDialog
import com.example.ui.screens.EnrollFarmerScreen
import com.example.ui.screens.LogPracticeScreen
import com.example.ui.screens.SyncRecordsScreen
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.Spacing
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TraceHarvestViewModel

/**
 * Streamlined Field Agent Mobile Application
 * Exclusively focused on:
 * 1. Register New Farmer (Offline SQLite -> pending_sync -> Backend Sync -> Unique Farmer ID)
 * 2. Stage 2: Practice Logging (🌱 Planting, 💧 Irrigation, 🧪 Pesticide, 🌿 Fertilizer, ✂️ Harvest)
 * 3. Synchronization Engine (Offline queue and upstream ledger sync)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceHarvestApp(
    viewModel: TraceHarvestViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val farmers by viewModel.allFarmers.collectAsState()
    val practiceLogs by viewModel.allPracticeLogs.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val pendingFarmerCount by viewModel.pendingFarmerSyncCount.collectAsState()
    val pendingPracticeCount by viewModel.pendingPracticeSyncCount.collectAsState()
    val totalPendingCount by viewModel.totalPendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showGuideDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TraceHarvestTopBar(
                    agentId = viewModel.currentAgentId,
                    totalPending = totalPendingCount,
                    isSyncing = isSyncing,
                    onSyncClick = { viewModel.syncAllPending() },
                    onOpenGuideClick = { showGuideDialog = true }
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    TraceHarvestBottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    TraceHarvestNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Crossfade(
                        targetState = selectedTab,
                        animationSpec = tween(250),
                        label = "tab_crossfade"
                    ) { currentTab ->
                        when (currentTab) {
                            AppTab.REGISTER_FARMER -> {
                                EnrollFarmerScreen(
                                    enrolledFarmers = farmers,
                                    agentId = viewModel.currentAgentId,
                                    onNavigateToPractice = { viewModel.selectTab(AppTab.LOG_PRACTICE) },
                                    onNavigateToSync = { viewModel.selectTab(AppTab.SYNC_RECORDS) },
                                    onOpenGuide = { showGuideDialog = true },
                                    onRegisterFarmer = { name, phone, state, lga, comm, crop, size, unit, lat, lng, coop, isOffline ->
                                        viewModel.registerFarmer(name, phone, state, lga, comm, crop, size, unit, lat, lng, coop, isOffline)
                                    }
                                )
                            }
                            AppTab.LOG_PRACTICE -> {
                                LogPracticeScreen(
                                    farmers = farmers,
                                    recentLogs = practiceLogs,
                                    agentId = viewModel.currentAgentId,
                                    pendingSyncCount = pendingPracticeCount,
                                    onSyncAllPending = { viewModel.syncAllPending() },
                                    onLogPractice = { code, name, crop, practiceType, prod, active, qty, unit, date, photoUri, lat, lng, acc, phi, localId, displayId, nafdacNo, coords, syncStatus ->
                                        viewModel.logPractice(
                                            farmerCode = code,
                                            farmerName = name,
                                            crop = crop,
                                            practiceType = practiceType,
                                            productName = prod,
                                            activeIngredient = active,
                                            quantityUsed = qty,
                                            quantityUnit = unit,
                                            calendarDateApplied = date,
                                            verificationPhotoUri = photoUri,
                                            liveLatitude = lat,
                                            liveLongitude = lng,
                                            gpsAccuracyMeters = acc,
                                            phiDays = phi,
                                            farmerLocalId = localId,
                                            farmerDisplayId = displayId,
                                            nafdacRegNo = nafdacNo,
                                            gpsCoordinates = coords,
                                            syncStatus = syncStatus
                                        )
                                    }
                                )
                            }
                            AppTab.SYNC_RECORDS -> {
                                SyncRecordsScreen(
                                    farmers = farmers,
                                    practices = practiceLogs,
                                    pendingFarmerCount = pendingFarmerCount,
                                    pendingPracticeCount = pendingPracticeCount,
                                    totalPendingCount = totalPendingCount,
                                    isSyncing = isSyncing,
                                    agentId = viewModel.currentAgentId,
                                    onSyncAll = { viewModel.syncAllPending() }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showGuideDialog) {
            AppGuideDialog(onDismiss = { showGuideDialog = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TraceHarvestTopBar(
    agentId: String,
    totalPending: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onOpenGuideClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = HarvestGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = "TraceHarvest Field Agent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Agent ID: $agentId",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC8E6C9)
                    )
                }
            }
        },
        actions = {
            // Guide / How-To Button
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clickable { onOpenGuideClick() }
                    .testTag("topbar_guide_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "App Guide",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Guide",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sync Status Chip
            Surface(
                color = if (totalPending > 0) Color(0x33FFD54F) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(end = Spacing.sm)
                    .clickable(enabled = !isSyncing && totalPending > 0) { onSyncClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Syncing...",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (totalPending > 0) WarningAmber else Color(0xFF69F0AE),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (totalPending > 0) "$totalPending Pending" else "Synced",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = HarvestGreenPrimary
        )
    )
}

@Composable
private fun TraceHarvestBottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationDestinations.items.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, fontSize = 11.sp, maxLines = 1, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HarvestGreenPrimary,
                    selectedTextColor = HarvestGreenPrimary,
                    indicatorColor = Color(0xFFE8F5E9)
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
private fun TraceHarvestNavigationRail(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationRail(
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.size(Spacing.md))
        NavigationDestinations.items.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = HarvestGreenPrimary,
                    selectedTextColor = HarvestGreenPrimary,
                    indicatorColor = Color(0xFFE8F5E9)
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
