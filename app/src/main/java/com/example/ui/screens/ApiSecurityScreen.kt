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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.ExporterRole
import com.example.data.security.JwtToken
import com.example.data.security.SecurityActionOutcome
import com.example.data.security.SecurityEngineConfig
import com.example.data.security.SecurityEvent
import com.example.data.security.SecurityLayer
import com.example.ui.theme.BlockchainBlue
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenDark
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.SesameAmberSecondary
import com.example.ui.theme.ViolationRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApiSecurityScreen(
    securityConfig: SecurityEngineConfig,
    activeJwt: JwtToken,
    remainingTokens: Int,
    securityEvents: List<SecurityEvent>,
    onGenerateJwt: (ExporterRole, String, String) -> Unit,
    onSimulateRequest: (endpoint: String, method: String, clientIp: String, clientIdentity: String, withJwt: Boolean, payload: String) -> Unit,
    onResetRateLimits: () -> Unit,
    onDepleteRateLimits: () -> Unit,
    onCopyClipboard: (String) -> Unit
) {
    val tabs = listOf("Threat Simulator", "JWT & Exporter Credentials", "Security Audit Logs", "Architecture Guide")
    var selectedTab by remember { mutableStateOf(tabs[0]) }

    val blockedCount = securityEvents.count { it.outcome != SecurityActionOutcome.ALLOWED_200 }
    val passedCount = securityEvents.count { it.outcome == SecurityActionOutcome.ALLOWED_200 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("api_security_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
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
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "API Security & Hardening",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MCP Security (FastAPI JWT) • Wallarm API Firewall • GuardianWAF",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Real-time KPI summary row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = ComplianceGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WAF Status", fontSize = 10.sp, color = Color.Gray)
                        }
                        Text("ACTIVE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComplianceGreen)
                        Text("Enforcing", fontSize = 9.sp, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = SesameAmberSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate Limit", fontSize = 10.sp, color = Color.Gray)
                        }
                        Text("$remainingTokens / ${securityConfig.exporterRateLimitRpm}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        LinearProgressIndicator(
                            progress = { remainingTokens.toFloat() / securityConfig.exporterRateLimitRpm.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 4.dp),
                            color = if (remainingTokens > 50) ComplianceGreen else ViolationRed,
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = ViolationRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Threats", fontSize = 10.sp, color = Color.Gray)
                        }
                        Text("$blockedCount Blocked", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ViolationRed)
                        Text("$passedCount Passed", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Subtabs
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HarvestGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("sec_tab_${tab.take(4)}")
                    )
                }
            }
        }

        when (selectedTab) {
            "Threat Simulator" -> {
                item {
                    Text(
                        text = "Interactive Attack & Validation Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trigger real-world attack vectors and verification probes to observe how each layer defends TraceHarvest exporter endpoints.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Scenario 1: Legitimate Exporter Query
                item {
                    SimulationActionCard(
                        title = "1. Authorized Exporter Query (Rotterdam Port)",
                        description = "Simulates EU border inspector querying batch NG-SES-2026-0042 using a valid JWT Bearer token with 'read:provenance' scope.",
                        expectedOutcome = "200 OK • Cleared by MCP Security • Consumes 1 Token",
                        buttonText = "Execute Valid Query",
                        buttonColor = ComplianceGreen,
                        testTag = "sim_valid_query",
                        onClick = {
                            onSimulateRequest(
                                "/api/v1/export/provenance/NG-SES-2026-0042",
                                "GET",
                                "145.22.89.14",
                                "EU Port Inspector (${activeJwt.organization})",
                                true,
                                ""
                            )
                        }
                    )
                }

                // Scenario 2: Unauthenticated Query
                item {
                    SimulationActionCard(
                        title = "2. Unauthenticated Exporter Endpoint Query",
                        description = "Simulates an external consumer attempting to access the restricted consignment passport without sending the Authorization header.",
                        expectedOutcome = "401 Unauthorized • Blocked by MCP Security Framework",
                        buttonText = "Test Missing Auth Header",
                        buttonColor = ViolationRed,
                        testTag = "sim_unauth_query",
                        onClick = {
                            onSimulateRequest(
                                "/api/v1/export/provenance/NG-SES-2026-0042",
                                "GET",
                                "197.210.55.82",
                                "Anonymous Client",
                                false,
                                ""
                            )
                        }
                    )
                }

                // Scenario 3: High-Frequency Rate Limit Attack
                item {
                    SimulationActionCard(
                        title = "3. Exporter API Rate Limit Exhaustion",
                        description = "Simulates an automated scraping bot or unthrottled exporter script exhausting the ${securityConfig.exporterRateLimitRpm} req/min window.",
                        expectedOutcome = "429 Too Many Requests • Blocked by MCP Rate Limiter",
                        buttonText = "Simulate 429 Rate Limit Burst",
                        buttonColor = Color(0xFFE65100),
                        testTag = "sim_rate_limit_burst",
                        onClick = {
                            onDepleteRateLimits()
                            onSimulateRequest(
                                "/api/v1/batches/verify",
                                "GET",
                                "194.38.20.198",
                                "Automated Scraper Bot",
                                true,
                                ""
                            )
                        }
                    )
                }

                // Scenario 4: OpenAPI Schema Tampering (Wallarm)
                item {
                    SimulationActionCard(
                        title = "4. Payload Schema Tampering (Wallarm API Firewall)",
                        description = "Simulates a compromised client attempting to inject a negative moisture level (-4.5%) and force an uncertified MRL override.",
                        expectedOutcome = "400 Bad Request • Blocked by Wallarm API Firewall (OpenAPI spec)",
                        buttonText = "Simulate Schema Tampering",
                        buttonColor = Color(0xFF6A1B9A),
                        testTag = "sim_schema_tamper",
                        onClick = {
                            onSimulateRequest(
                                "/api/v1/inspections/mrl-signoff",
                                "POST",
                                "102.89.34.120",
                                "Compromised Terminal",
                                true,
                                "{\"batchCode\":\"NG-SES-2026-0042\",\"moisture_percent\":-4.5,\"mrl_override\":true}"
                            )
                        }
                    )
                }

                // Scenario 5: SQL Injection (GuardianWAF)
                item {
                    SimulationActionCard(
                        title = "5. SQL Injection Attack (GuardianWAF)",
                        description = "Simulates an attacker attempting SQL injection in batch code parameters: /provenance/batch?id=' UNION SELECT * FROM users--",
                        expectedOutcome = "403 Forbidden • Intercepted by GuardianWAF In-Memory Rules",
                        buttonText = "Simulate SQL Injection",
                        buttonColor = ViolationRed,
                        testTag = "sim_sqli_attack",
                        onClick = {
                            onSimulateRequest(
                                "/api/v1/export/provenance/batch?id=' UNION SELECT * FROM users--",
                                "GET",
                                "185.220.101.5",
                                "Malicious Exploit Scanner",
                                true,
                                ""
                            )
                        }
                    )
                }

                // Scenario 6: XSS Injection (GuardianWAF)
                item {
                    SimulationActionCard(
                        title = "6. Cross-Site Scripting Probe (GuardianWAF)",
                        description = "Simulates an agent attempting to inject malicious script tags into farmer audit remarks: <script>stealApiKeys()</script>",
                        expectedOutcome = "403 Forbidden • Filtered by GuardianWAF Signature Engine",
                        buttonText = "Simulate XSS Attack",
                        buttonColor = ViolationRed,
                        testTag = "sim_xss_attack",
                        onClick = {
                            onSimulateRequest(
                                "/api/v1/farmers/remarks",
                                "POST",
                                "197.210.12.9",
                                "Untrusted Agent Portal",
                                true,
                                "<script>alert('XSS_ATTEMPT')</script>"
                            )
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onResetRateLimits,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("reset_rate_limits_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Rate Limit Counter to ${securityConfig.exporterRateLimitRpm} req/min", fontSize = 12.sp)
                    }
                }
            }

            "JWT & Exporter Credentials" -> {
                item {
                    Text(
                        text = "JWT Authentication & RBAC Provisioning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "FastAPI uses MCP Security Framework to validate cryptographically signed JSON Web Tokens issued to international buyers, NAFDAC labs, and export terminals.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Role selection chips
                item {
                    Text("Select Exporter Role to Issue Token:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExporterRole.entries) { role ->
                            val isSelected = activeJwt.role == role
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val org = when (role) {
                                        ExporterRole.EU_INSPECTOR -> "Port of Rotterdam (NVWA)"
                                        ExporterRole.CERTIFIED_EXPORTER -> "Valency Agro Nigeria Ltd"
                                        ExporterRole.NAFDAC_LAB_OFFICER -> "NAFDAC Oshodi Central Lab"
                                        ExporterRole.PUBLIC_BUYER -> "Global Consumer Verification"
                                    }
                                    val sub = "usr_${role.name.lowercase().take(8)}_01"
                                    onGenerateJwt(role, org, sub)
                                },
                                label = { Text(role.roleName.take(24) + "...", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HarvestGreenDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Active JWT Display Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E231E)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                                    Text("Active JWT Token (HS256)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VALID",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Token Claims
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0C120C), RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Subject: ${activeJwt.subject}", fontSize = 11.sp, color = Color(0xFFA5D6A7), fontFamily = FontFamily.Monospace)
                                Text("Organization: ${activeJwt.organization}", fontSize = 11.sp, color = Color.White)
                                Text("Role: ${activeJwt.role.roleName}", fontSize = 11.sp, color = SesameAmberSecondary, fontWeight = FontWeight.SemiBold)
                                Text("Rate Limit: ${activeJwt.role.defaultRateLimitRpm} req/minute", fontSize = 10.sp, color = Color.LightGray)
                                Text("Scopes: [${activeJwt.scopes.joinToString()}]", fontSize = 10.sp, color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace)
                            }

                            // Raw Token string
                            Text("Raw Bearer Header:", fontSize = 10.sp, color = Color.Gray)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF050805), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Authorization: Bearer ${activeJwt.rawToken}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF76FF03),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }

                            Button(
                                onClick = { onCopyClipboard("Authorization: Bearer ${activeJwt.rawToken}") },
                                colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("copy_jwt_header_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Authorization Header", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            "Security Audit Logs" -> {
                item {
                    Text(
                        text = "Real-Time WAF & API Firewall Security Logs (${securityEvents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detailed audit trail of inbound exporter API traffic showing request decisions across MCP Security, Wallarm, and GuardianWAF.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                items(securityEvents, key = { it.id }) { event ->
                    val isAllowed = event.outcome == SecurityActionOutcome.ALLOWED_200
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val timeStr = dateFormat.format(Date(event.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("sec_event_${event.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAllowed) Color(0xFFF9FAF8) else Color(0xFFFFF7F7)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAllowed) Color(0xFFE0E5DD) else Color(0xFFFFCDD2)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
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
                                        if (isAllowed) Icons.Default.CheckCircle else Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isAllowed) ComplianceGreen else ViolationRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${event.method} ${event.endpoint.take(32)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Surface(
                                    color = if (isAllowed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${event.httpStatus} ${event.outcome.name.substringAfter('_')}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllowed) ComplianceGreen else ViolationRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Client: ${event.clientIdentity} (${event.clientIp}) • Time: $timeStr",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFEEEEEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = event.layer.title,
                                        fontSize = 9.sp,
                                        color = Color(0xFF424242),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = event.reason,
                                    fontSize = 11.sp,
                                    color = if (isAllowed) Color(0xFF2E7D32) else ViolationRed,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            "Architecture Guide" -> {
                item {
                    Text(
                        text = "TraceHarvest API Security Layer Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    SecurityToolOverviewCard(
                        toolName = "1. MCP Security Framework (FastAPI Library)",
                        role = "Core Authentication & Rate Limiting",
                        recommendationStage = "Recommended for Immediate Pilot",
                        keyBenefits = listOf(
                            "FastAPI-native Python middleware with minimal overhead",
                            "HMAC-SHA256 & RS256 JWT Bearer token validation",
                            "Role-Based Access Control (RBAC) with custom scopes",
                            "Sliding-window Redis or in-memory token bucket rate limiting"
                        )
                    )
                }

                item {
                    SecurityToolOverviewCard(
                        toolName = "2. API Firewall (Wallarm)",
                        role = "OpenAPI Specification Enforcement",
                        recommendationStage = "Stricter Schema Validation Layer",
                        keyBenefits = listOf(
                            "Open-source reverse proxy deployed in front of FastAPI",
                            "Validates every exporter request and response against openapi.json",
                            "Blocks malformed pesticide residue updates & tampered numbers",
                            "Prevents data leaks by sanitizing unauthorized response fields"
                        )
                    )
                }

                item {
                    SecurityToolOverviewCard(
                        toolName = "3. GuardianWAF",
                        role = "Lightweight Edge Filtering",
                        recommendationStage = "Lightweight WAF Option",
                        keyBenefits = listOf(
                            "Go-based WAF with in-memory or file-based event storage",
                            "Functional options API for direct library mode or proxy",
                            "Filters SQL injection, XSS, and command injection attacks",
                            "Low memory footprint suitable for edge container deployments"
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SimulationActionCard(
    title: String,
    description: String,
    expectedOutcome: String,
    buttonText: String,
    buttonColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = Color(0xFF616161), lineHeight = 15.sp)
            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Expected: $expectedOutcome",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            ) {
                Text(buttonText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SecurityToolOverviewCard(
    toolName: String,
    role: String,
    recommendationStage: String,
    keyBenefits: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(toolName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = recommendationStage,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ComplianceGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text("Role: $role", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            keyBenefits.forEach { benefit ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("•", fontSize = 12.sp, color = ComplianceGreen, fontWeight = FontWeight.Bold)
                    Text(benefit, fontSize = 11.sp, color = Color(0xFF424242), lineHeight = 15.sp)
                }
            }
        }
    }
}
