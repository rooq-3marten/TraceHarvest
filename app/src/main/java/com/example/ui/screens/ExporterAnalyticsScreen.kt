package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HarvestBatchEntity
import com.example.ui.components.StatCard
import com.example.ui.theme.BlockchainBlue
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.SesameAmberSecondary
import com.example.ui.theme.ViolationRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DashboardMetrics

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security

@Composable
fun ExporterAnalyticsScreen(
    metrics: DashboardMetrics,
    batches: List<HarvestBatchEntity>,
    onNavigateToSecurity: (() -> Unit)? = null
) {
    val totalWeightMt = batches.sumOf { it.netWeightKg } / 1000.0
    val totalBags = batches.sumOf { it.bagCount }
    val blockedCount = batches.count { it.isFlaggedForRejection || it.mrlStatus == "VIOLATION_BLOCKED" }
    val compliantCount = batches.count { it.mrlStatus == "PASSED_SPS" && !it.isFlaggedForRejection }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("exporter_analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BlockchainBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Exporter & Port Risk Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Apapa / Tin Can Port clearance readiness & MRL trend monitor",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Financial Rejection Avoidance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D3320)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREVENTED LOSS VALUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5D6A7)
                        )
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF81C784)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${metrics.estimatedDollarsSaved.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Estimated financial losses prevented across $blockedCount intercepted non-compliant batches ($42,000 avg cost per rejected 40ft container at EU/Japan borders).",
                        fontSize = 12.sp,
                        color = Color(0xFFE8F5E9),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Exporter API Hardening & WAF Protection Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSecurity?.invoke() }
                    .testTag("exporter_sec_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ComplianceGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Exporter API Hardening & WAF Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "Endpoints protected by MCP JWT + Rate Limiting, Wallarm OpenAPI Firewall & GuardianWAF. Tap to inspect security controls.",
                            fontSize = 11.sp,
                            color = Color(0xFF33691E),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Destination Markets Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Destination Markets & Compliance Standards",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    MarketProgressRow(
                        market = "European Union (EFSA MRLs & EC 396/2005)",
                        share = "45% of volume",
                        progress = 0.45f,
                        color = ComplianceGreen,
                        statusText = "Strict EC No 2020/1085 check active"
                    )

                    MarketProgressRow(
                        market = "Japan (Ministry of Health Food Sanitation Act)",
                        share = "30% of volume",
                        progress = 0.30f,
                        color = Color(0xFF1565C0),
                        statusText = "Positive List System compliance: 100%"
                    )

                    MarketProgressRow(
                        market = "Türkiye (Plant Quarantine Inspection)",
                        share = "18% of volume",
                        progress = 0.18f,
                        color = SesameAmberSecondary,
                        statusText = "Moisture < 10% Aflatoxin check: Passed"
                    )

                    MarketProgressRow(
                        market = "United States & Others (FDA Import Alert)",
                        share = "7% of volume",
                        progress = 0.07f,
                        color = Color(0xFF7B1FA2),
                        statusText = "PICS hermetic packaging standard: Passed"
                    )
                }
            }
        }

        // Regional Rejection Risk Radar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Regional Production Hubs & Risk Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    RegionRiskRow(
                        region = "Kano (Dambatta & Bichi)",
                        crops = "Sesame & Cowpeas",
                        riskLevel = "MODERATE (Sniper storage surveillance active)",
                        riskColor = WarningAmber
                    )

                    RegionRiskRow(
                        region = "Jigawa (Maigatari Export Cluster)",
                        crops = "Sesame (Organic BioNeem)",
                        riskLevel = "LOW RISK (100% Export Pass Rate)",
                        riskColor = ComplianceGreen
                    )

                    RegionRiskRow(
                        region = "Benue (Makurdi Grain Basin)",
                        crops = "Cowpeas & Soya",
                        riskLevel = "LOW RISK (PICS Hermetic Adoption)",
                        riskColor = ComplianceGreen
                    )

                    RegionRiskRow(
                        region = "Kaduna (Kachia Ginger Belt)",
                        crops = "High-Oleoresin Ginger",
                        riskLevel = "LOW RISK (Traceable Cooperatives)",
                        riskColor = ComplianceGreen
                    )
                }
            }
        }

        // Farmer Premium Bonus Engine
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Value Chain Incentive: +18% Farmer Premium",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Farmers with zero pesticide violations and verified GPS provenance receive $1,650/MT from international buyers vs $1,400/MT spot market price (+₦380,000/ton extra income), creating self-sustaining incentive for compliance.",
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 16.sp
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
private fun MarketProgressRow(
    market: String,
    share: String,
    progress: Float,
    color: Color,
    statusText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(market, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(share, fontSize = 11.sp, color = Color.Gray)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = Color(0xFFEEEEEE)
        )
        Text(statusText, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RegionRiskRow(
    region: String,
    crops: String,
    riskLevel: String,
    riskColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAF8), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(region, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(crops, fontSize = 11.sp, color = Color.Gray)
        }
        Text(
            text = riskLevel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = riskColor
        )
    }
}
