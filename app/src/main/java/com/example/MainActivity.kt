package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import com.example.ui.screens.ApiSecurityScreen
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AggregateBatchScreen
import com.example.ui.screens.BatchDetailDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnrollFarmerScreen
import com.example.ui.screens.ExporterAnalyticsScreen
import com.example.ui.screens.LogPracticeScreen
import com.example.ui.screens.UssdGatewayScreen
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenDark
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TraceHarvestViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TraceHarvestViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val selectedTab by viewModel.selectedTab.collectAsState()
                val metrics by viewModel.dashboardMetrics.collectAsState()
                val batches by viewModel.allBatches.collectAsState()
                val farmers by viewModel.allFarmers.collectAsState()
                val practiceLogs by viewModel.allPracticeLogs.collectAsState()
                val smsLogs by viewModel.allSmsLogs.collectAsState()
                val selectedCropFilter by viewModel.selectedCropFilter.collectAsState()
                val selectedBatchDetail by viewModel.selectedBatchDetail.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()
                val gatewayConfig by viewModel.gatewayConfig.collectAsState()
                val securityConfig by viewModel.securityConfig.collectAsState()
                val activeJwt by viewModel.activeJwt.collectAsState()
                val remainingTokens by viewModel.remainingTokens.collectAsState()
                val securityEvents by viewModel.securityEvents.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearUserMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TraceHarvest",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            },
                            actions = {
                                Surface(
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clickable { viewModel.selectTab(AppTab.API_SECURITY) }
                                        .testTag("topbar_security_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFB9F6CA),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "WAF Enforcing",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(end = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF69F0AE), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Offline",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = HarvestGreenPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == AppTab.DASHBOARD,
                                onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Batches") },
                                label = { Text("Batches", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_batches")
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.ENROLL_FARMER,
                                onClick = { viewModel.selectTab(AppTab.ENROLL_FARMER) },
                                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Enroll") },
                                label = { Text("Enroll", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_enroll")
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.LOG_PRACTICE,
                                onClick = { viewModel.selectTab(AppTab.LOG_PRACTICE) },
                                icon = { Icon(Icons.Default.Science, contentDescription = "Inputs") },
                                label = { Text("Inputs", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_inputs")
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.AGGREGATE_BATCH,
                                onClick = { viewModel.selectTab(AppTab.AGGREGATE_BATCH) },
                                icon = { Icon(Icons.Default.Inventory, contentDescription = "Harvest") },
                                label = { Text("Harvest", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_harvest")
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.USSD_SMS_GATEWAY,
                                onClick = { viewModel.selectTab(AppTab.USSD_SMS_GATEWAY) },
                                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "USSD") },
                                label = { Text("USSD/SMS", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_ussd")
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.EXPORTER_ANALYTICS,
                                onClick = { viewModel.selectTab(AppTab.EXPORTER_ANALYTICS) },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Port Risk") },
                                label = { Text("Port Risk", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_analytics")
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.API_SECURITY,
                                onClick = { viewModel.selectTab(AppTab.API_SECURITY) },
                                icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                                label = { Text("Security", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HarvestGreenPrimary,
                                    selectedTextColor = HarvestGreenPrimary,
                                    indicatorColor = Color(0xFFE8F5E9)
                                ),
                                modifier = Modifier.testTag("nav_tab_security")
                            )
                        }
                    },
                    floatingActionButton = {
                        if (selectedTab == AppTab.DASHBOARD) {
                            FloatingActionButton(
                                onClick = { viewModel.selectTab(AppTab.AGGREGATE_BATCH) },
                                containerColor = HarvestGreenPrimary,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("fab_new_batch")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tag Harvest Batch")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            AppTab.DASHBOARD -> {
                                DashboardScreen(
                                    metrics = metrics,
                                    batches = batches,
                                    selectedFilter = selectedCropFilter,
                                    onSelectFilter = { viewModel.setCropFilter(it) },
                                    onSelectBatch = { viewModel.selectBatchDetail(it) },
                                    onToggleFlag = { viewModel.toggleFlagBatch(it) },
                                    onAnchorBatch = { viewModel.anchorBatchToBlockchain(it) },
                                    onNewBatchClick = { viewModel.selectTab(AppTab.AGGREGATE_BATCH) }
                                )
                            }
                            AppTab.ENROLL_FARMER -> {
                                EnrollFarmerScreen(
                                    enrolledFarmers = farmers,
                                    onRegisterFarmer = { name, phone, state, lga, community, crop, ha, lat, lng, coop ->
                                        viewModel.registerFarmer(name, phone, state, lga, community, crop, ha, lat, lng, coop)
                                    }
                                )
                            }
                            AppTab.LOG_PRACTICE -> {
                                LogPracticeScreen(
                                    farmers = farmers,
                                    recentLogs = practiceLogs,
                                    onLogPractice = { code, name, crop, cat, prod, active, dosage, src, phi ->
                                        viewModel.logPractice(code, name, crop, cat, prod, active, dosage, src, phi)
                                    }
                                )
                            }
                            AppTab.AGGREGATE_BATCH -> {
                                AggregateBatchScreen(
                                    farmers = farmers,
                                    onCreateBatch = { code, name, crop, region, bags, weight, moisture, foreign, dest ->
                                        viewModel.createHarvestBatch(code, name, crop, region, bags, weight, moisture, foreign, dest)
                                    }
                                )
                            }
                            AppTab.USSD_SMS_GATEWAY -> {
                                UssdGatewayScreen(
                                    smsLogs = smsLogs,
                                    farmers = farmers,
                                    gatewayConfig = gatewayConfig,
                                    onSelectProvider = { viewModel.updateGatewayProvider(it) },
                                    onExecuteUssd = { hops, phone -> viewModel.executeUssdHop(hops, phone) },
                                    onDispatchSms = { phone, name, text, type -> viewModel.dispatchOutboundSms(phone, name, text, type) },
                                    onProcessInboundSms = { phone, text -> viewModel.processInboundFarmerReply(phone, text) }
                                )
                            }
                            AppTab.EXPORTER_ANALYTICS -> {
                                ExporterAnalyticsScreen(
                                    metrics = metrics,
                                    batches = batches,
                                    onNavigateToSecurity = { viewModel.selectTab(AppTab.API_SECURITY) }
                                )
                            }
                            AppTab.API_SECURITY -> {
                                ApiSecurityScreen(
                                    securityConfig = securityConfig,
                                    activeJwt = activeJwt,
                                    remainingTokens = remainingTokens,
                                    securityEvents = securityEvents,
                                    onGenerateJwt = { role, org, sub -> viewModel.generateJwtToken(role, org, sub) },
                                    onSimulateRequest = { endpoint, method, ip, identity, withJwt, payload ->
                                        viewModel.simulateSecurityRequest(endpoint, method, ip, identity, withJwt, payload)
                                    },
                                    onResetRateLimits = { viewModel.resetSecurityRateLimits() },
                                    onDepleteRateLimits = { viewModel.depleteSecurityRateLimits() },
                                    onCopyClipboard = { text ->
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("TraceHarvest JWT", text))
                                        viewModel.setUserMessage("Authorization header copied to clipboard.")
                                    }
                                )
                            }
                        }

                        // SPS Compliance Certificate & Provenance Passport Modal Dialog
                        selectedBatchDetail?.let { batch ->
                            BatchDetailDialog(
                                batch = batch,
                                onDismiss = { viewModel.selectBatchDetail(null) },
                                onToggleFlag = { viewModel.toggleFlagBatch(it) },
                                onAnchorToBlockchain = { viewModel.anchorBatchToBlockchain(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

