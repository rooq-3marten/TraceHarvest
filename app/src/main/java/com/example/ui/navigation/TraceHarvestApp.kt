package com.example.ui.navigation

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.auth.SessionManager
import com.example.core.zone.GeopoliticalZone
import com.example.core.zone.ZoneRegistry
import com.example.data.remote.NetworkClient
import com.example.ui.components.AppGuideDialog
import com.example.ui.components.FarmVertex
import com.example.ui.screens.*
import com.example.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.auth.AgentApprovalStatus
import com.example.core.auth.AgentProfile
import com.example.ui.viewmodel.AgentAuthViewModel
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TraceHarvestViewModel
import kotlinx.coroutines.launch

enum class AppNavState {
    SPLASH,
    LOGIN,
    SIGN_UP,
    PENDING_APPROVAL,
    REJECTED,
    SUSPENDED,
    MAIN
}

data class FarmMapTarget(
    val initialLat: Double,
    val initialLng: Double,
    val crop: String,
    val farmerName: String
)

/**
 * Human-Centered Field Agent Application Architecture with Authentication Gate:
 * - Splash Screen (1.5 seconds) with DataStore JWT token validation & approval check
 * - Self-registration screen for new agents
 * - Approval status routing (pending, approved, rejected, suspended)
 * - Lifecycle foreground status check
 * - Lands on Home with "Register a farmer" as the prominent primary action
 * - BackHandler on all sub-screens to prevent accidental exits
 * - Offline-first synchronization with the Central Admin Dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceHarvestApp(
    viewModel: TraceHarvestViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager.getInstance(context) }
    val authViewModel: AgentAuthViewModel = viewModel()

    var appNavState by rememberSaveable { mutableStateOf(AppNavState.SPLASH) }
    var currentAgentProfile by remember { mutableStateOf(AgentProfile()) }

    // Every farmer, practice log and sync is attributed to the signed-in agent
    LaunchedEffect(currentAgentProfile.agentId) {
        viewModel.currentAgentId = currentAgentProfile.agentId
    }

    // Collect profile updates from SessionManager
    LaunchedEffect(Unit) {
        sessionManager.agentProfileFlow.collect { profile ->
            currentAgentProfile = profile
        }
    }

    // Lifecycle Guard: Re-check approval status whenever app returns to foreground (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    if (sessionManager.hasValidSession()) {
                        authViewModel.refreshAgentStatus { status ->
                            when (status) {
                                AgentApprovalStatus.APPROVED -> {
                                    if (appNavState in listOf(AppNavState.PENDING_APPROVAL, AppNavState.REJECTED, AppNavState.SUSPENDED)) {
                                        appNavState = AppNavState.MAIN
                                    }
                                }
                                AgentApprovalStatus.PENDING -> {
                                    appNavState = AppNavState.PENDING_APPROVAL
                                }
                                AgentApprovalStatus.REJECTED -> {
                                    appNavState = AppNavState.REJECTED
                                }
                                AgentApprovalStatus.SUSPENDED -> {
                                    appNavState = AppNavState.SUSPENDED
                                }
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (appNavState) {
        AppNavState.SPLASH -> {
            SplashScreen(
                onNavigateToHome = {
                    appNavState = AppNavState.MAIN
                    viewModel.selectTab(AppTab.HOME_DASHBOARD)
                },
                onNavigateToLogin = {
                    appNavState = AppNavState.LOGIN
                },
                onNavigateToStatus = { status, profile ->
                    currentAgentProfile = profile
                    appNavState = when (status) {
                        AgentApprovalStatus.APPROVED -> AppNavState.MAIN
                        AgentApprovalStatus.PENDING -> AppNavState.PENDING_APPROVAL
                        AgentApprovalStatus.REJECTED -> AppNavState.REJECTED
                        AgentApprovalStatus.SUSPENDED -> AppNavState.SUSPENDED
                    }
                },
                modifier = modifier
            )
            return
        }

        AppNavState.LOGIN -> {
            AgentLoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = {
                    appNavState = AppNavState.SIGN_UP
                },
                onRouteByStatus = { status, profile ->
                    currentAgentProfile = profile
                    when (status) {
                        AgentApprovalStatus.APPROVED -> {
                            appNavState = AppNavState.MAIN
                            viewModel.selectTab(AppTab.HOME_DASHBOARD)
                        }
                        AgentApprovalStatus.PENDING -> {
                            appNavState = AppNavState.PENDING_APPROVAL
                        }
                        AgentApprovalStatus.REJECTED -> {
                            appNavState = AppNavState.REJECTED
                        }
                        AgentApprovalStatus.SUSPENDED -> {
                            appNavState = AppNavState.SUSPENDED
                        }
                    }
                },
                modifier = modifier
            )
            return
        }

        AppNavState.SIGN_UP -> {
            BackHandler {
                appNavState = AppNavState.LOGIN
            }
            AgentSignUpScreen(
                authViewModel = authViewModel,
                onNavigateBackToSignIn = {
                    appNavState = AppNavState.LOGIN
                },
                onSignUpSuccessPending = {
                    appNavState = AppNavState.PENDING_APPROVAL
                },
                modifier = modifier
            )
            return
        }

        AppNavState.PENDING_APPROVAL -> {
            BackHandler {
                appNavState = AppNavState.LOGIN
            }
            AgentPendingApprovalScreen(
                authViewModel = authViewModel,
                profile = currentAgentProfile,
                onApproved = {
                    appNavState = AppNavState.MAIN
                    viewModel.selectTab(AppTab.HOME_DASHBOARD)
                },
                onSignOut = {
                    authViewModel.signOut {
                        appNavState = AppNavState.LOGIN
                    }
                },
                modifier = modifier
            )
            return
        }

        AppNavState.REJECTED -> {
            BackHandler {
                appNavState = AppNavState.LOGIN
            }
            AgentRejectedScreen(
                authViewModel = authViewModel,
                profile = currentAgentProfile,
                onResubmitted = {
                    appNavState = AppNavState.PENDING_APPROVAL
                },
                onSignOut = {
                    authViewModel.signOut {
                        appNavState = AppNavState.LOGIN
                    }
                },
                modifier = modifier
            )
            return
        }

        AppNavState.SUSPENDED -> {
            BackHandler {
                // Suspended agents cannot back into the app
            }
            AgentSuspendedScreen(
                profile = currentAgentProfile,
                onSignOut = {
                    authViewModel.signOut {
                        appNavState = AppNavState.LOGIN
                    }
                },
                modifier = modifier
            )
            return
        }

        AppNavState.MAIN -> {
            // Main App Flow below
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentZone by viewModel.currentZone.collectAsStateWithLifecycle()
    val farmers by viewModel.allFarmers.collectAsStateWithLifecycle()
    val practiceLogs by viewModel.allPracticeLogs.collectAsStateWithLifecycle()
    val batches by viewModel.allBatches.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val pendingFarmerCount by viewModel.pendingFarmerSyncCount.collectAsStateWithLifecycle()
    val pendingPracticeCount by viewModel.pendingPracticeSyncCount.collectAsStateWithLifecycle()
    val totalPendingCount by viewModel.totalPendingSyncCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var showGuideDialog by remember { mutableStateOf(false) }
    var activeMapTarget by remember { mutableStateOf<FarmMapTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept back key: when on secondary tab, back key returns to HOME_DASHBOARD
    BackHandler(enabled = selectedTab != AppTab.HOME_DASHBOARD) {
        viewModel.selectTab(AppTab.HOME_DASHBOARD)
    }

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
            onBoundarySaved = { _, _, _ ->
                activeMapTarget = null
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
                    agentId = currentAgentProfile.agentId,
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
                                    agentName = currentAgentProfile.name,
                                    agentAssociation = currentAgentProfile.association,
                                    agentLocation = currentAgentProfile.location.ifBlank { "Dambatta, Kano State" },
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
                                    agentId = currentAgentProfile.agentId,
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
                                    agentId = currentAgentProfile.agentId,
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
                                    agentId = currentAgentProfile.agentId,
                                    onSyncAll = { viewModel.syncAllPending() }
                                )
                            }
                            AppTab.SETTINGS -> {
                                SettingsScreen(
                                    currentZone = currentZone,
                                    agentId = currentAgentProfile.agentId,
                                    agentName = currentAgentProfile.name,
                                    agentPhone = currentAgentProfile.phone,
                                    onSwitchZone = { viewModel.selectTab(AppTab.ZONE_SELECTION) },
                                    onLogout = {
                                        coroutineScope.launch {
                                            authViewModel.signOut {
                                                appNavState = AppNavState.LOGIN
                                            }
                                        }
                                    },
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
                    .testTag("topbar_sync_badge")
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
