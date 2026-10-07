package com.example.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import com.example.ui.components.AppGuideDialog
import com.example.ui.components.FarmVertex
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TraceHarvestViewModel

data class FarmMapTarget(
    val initialLat: Double,
    val initialLng: Double,
    val crop: String,
    val farmerName: String
)

/**
 * Human-Centered Field Agent Application Architecture.
 * Supports:
 * - 6 Geopolitical Zones configuration & switching
 * - Google Satellite Imagery farm boundary mapping & WKT export
 * - Ethical farmer avatar system (zero AI stock faces)
 * - Offline-first synchronization with the Central Admin Dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceHarvestApp(
    viewModel: TraceHarvestViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentZone by viewModel.currentZone.collectAsState()
    val farmers by viewModel.allFarmers.collectAsState()
    val practiceLogs by viewModel.allPracticeLogs.collectAsState()
    val batches by viewModel.allBatches.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val pendingFarmerCount by viewModel.pendingFarmerSyncCount.collectAsState()
    val pendingPracticeCount by viewModel.pendingPracticeSyncCount.collectAsState()
    val totalPendingCount by viewModel.totalPendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showGuideDialog by remember { mutableStateOf(false) }
    var activeMapTarget by remember { mutableStateOf<FarmMapTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val zoneProfile = remember(currentZone) { ZoneRegistry.getProfile(currentZone) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Fullscreen Google Satellite Perimeter Mapping Screen overlay
    activeMapTarget?.let { target ->
        FarmBoundaryMappingScreen(
            initialLat = target.initialLat,
            initialLng = target.initialLng,
            cropType = target.crop,
            farmerName = target.farmerName,
            onBoundarySaved = { wkt, areaHa, vertices ->
                activeMapTarget = null
                // Return to enrollment screen with saved polygon
            },
            onCancel = { activeMapTarget = null }
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp
        val showBottomNav = selectedTab != AppTab.ZONE_SELECTION

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TraceHarvestTopBar(
                    agentId = viewModel.currentAgentId,
                    currentZone = currentZone,
                    totalPending = totalPendingCount,
                    isSyncing = isSyncing,
                    onSyncClick = { viewModel.syncAllPending() },
                    onOpenGuideClick = { showGuideDialog = true },
                    onOpenSettings = { viewModel.selectTab(AppTab.SETTINGS) }
                )
            },
            bottomBar = {
                if (!isExpandedScreen && showBottomNav) {
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
                if (isExpandedScreen && showBottomNav) {
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
                            AppTab.ZONE_SELECTION -> {
                                ZoneSelectionScreen(
                                    currentSelectedZone = currentZone,
                                    onZoneConfirmed = { newZone ->
                                        viewModel.setZone(newZone)
                                    }
                                )
                            }
                            AppTab.HOME_DASHBOARD -> {
                                HomeDashboardScreen(
                                    agentName = "Aminu Bello",
                                    currentZone = currentZone,
                                    totalFarmersCount = farmers.size,
                                    totalPracticesCount = practiceLogs.size,
                                    totalBatchesCount = batches.size,
                                    pendingSyncCount = totalPendingCount,
                                    onNavigateToRegisterFarmer = { viewModel.selectTab(AppTab.REGISTER_FARMER) },
                                    onNavigateToLogPractice = { viewModel.selectTab(AppTab.LOG_PRACTICE) },
                                    onNavigateToCreateBatch = { viewModel.selectTab(AppTab.BATCH_CREATION) },
                                    onNavigateToSync = { viewModel.selectTab(AppTab.SYNC_RECORDS) },
                                    onOpenSettings = { viewModel.selectTab(AppTab.SETTINGS) }
                                )
                            }
                            AppTab.REGISTER_FARMER -> {
                                EnrollFarmerScreen(
                                    enrolledFarmers = farmers,
                                    agentId = viewModel.currentAgentId,
                                    zone = currentZone,
                                    onNavigateToPractice = { viewModel.selectTab(AppTab.LOG_PRACTICE) },
                                    onNavigateToSync = { viewModel.selectTab(AppTab.SYNC_RECORDS) },
                                    onOpenGuide = { showGuideDialog = true },
                                    onOpenFullSatelliteMap = { lat, lng, crop, name ->
                                        activeMapTarget = FarmMapTarget(lat, lng, crop, name)
                                    },
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
                            AppTab.BATCH_CREATION -> {
                                BatchCreationScreen(
                                    currentZone = currentZone,
                                    availableFarmers = farmers,
                                    onCreateBatch = { code, crop, qty, grade, hub, farmerIds ->
                                        viewModel.createConsignmentBatch(code, crop, qty, grade, hub, farmerIds)
                                    },
                                    onNavigateBack = { viewModel.selectTab(AppTab.HOME_DASHBOARD) }
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
                            AppTab.SETTINGS -> {
                                SettingsScreen(
                                    currentZone = currentZone,
                                    agentName = "Aminu Bello Dambatta",
                                    agentPhone = "+2348031234567",
                                    onSwitchZone = { viewModel.selectTab(AppTab.ZONE_SELECTION) },
                                    onLogout = { viewModel.selectTab(AppTab.ZONE_SELECTION) },
                                    onNavigateBack = { viewModel.selectTab(AppTab.HOME_DASHBOARD) }
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
    currentZone: GeopoliticalZone,
    totalPending: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onOpenGuideClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(DarkGreenDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Nature,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TraceHarvest",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${currentZone.code} Zone • Nigeria",
                        fontSize = 10.sp,
                        color = Color(0xFFD4EDDA)
                    )
                }
            }
        },
        actions = {
            // Guide Button
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
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

            // Sync Status Badge
            Surface(
                color = if (totalPending > 0) Color(0x33FFD54F) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(end = 6.dp)
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
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (totalPending > 0) NaturalStatusAmber else Color(0xFFD4EDDA),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (totalPending > 0) "$totalPending" else "Synced",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkGreenPrimary
        )
    )
}

@Composable
private fun TraceHarvestBottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = PureWhiteSurface,
        tonalElevation = 6.dp,
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
                label = {
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkGreenPrimary,
                    selectedTextColor = DarkGreenPrimary,
                    indicatorColor = DarkGreenContainer,
                    unselectedIconColor = MutedDarkText,
                    unselectedTextColor = MutedDarkText
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
        containerColor = PureWhiteSurface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.size(16.dp))
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
                label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = DarkGreenPrimary,
                    selectedTextColor = DarkGreenPrimary,
                    indicatorColor = DarkGreenContainer
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
