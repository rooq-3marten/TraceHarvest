package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.Spacing
import com.example.ui.theme.WarningAmber

/**
 * Interactive Intro Banner that introduces what the application is for
 * and how to use it in 3 clear, practical steps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppIntroBanner(
    modifier: Modifier = Modifier,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToPractice: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onOpenFullGuide: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_intro_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F3820), Color(0xFF1B5E20))
                        )
                    )
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFA5D6A7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "What is TraceHarvest?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Agricultural Field Agent Provenance Companion",
                            color = Color(0xFFC8E6C9),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Tap to minimize" else "Quick Guide",
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Animated content body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Human Farmer Photo & Overview Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_farmer_portrait),
                            contentDescription = "Nigerian smallholder farmer in crop field",
                            modifier = Modifier
                                .weight(0.38f)
                                .height(105.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier.weight(0.62f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Field-to-Export Compliance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = HarvestGreenPrimary
                            )
                            Text(
                                text = "Enables agents to register smallholders and record seasonal farm inputs with zero internet. Captures GPS, calculates chemical safe dates, and guarantees export market readiness.",
                                fontSize = 11.sp,
                                color = Color(0xFF37474F),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Zero Literacy & Key Principles Badge Row
                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCEDC8))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = HarvestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Zero-Literacy Principle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HarvestGreenPrimary
                                )
                                Text(
                                    text = "Farmers do NOT need smartphones, apps, or literacy. Their mobile number is their verifiable trade identity.",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF2E7D32),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "HOW TO USE THIS APP IN 3 SIMPLE STEPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF455A64),
                        letterSpacing = 0.5.sp
                    )

                    // Step 1
                    IntroStepRow(
                        stepNumber = "1",
                        title = "Register New Farmer",
                        description = "Capture farmer name, primary phone number, GPS coordinates, farm size (hectares, acres, or plots), and crop. Saved locally even when completely offline.",
                        icon = Icons.Default.PersonAdd,
                        actionLabel = "Enroll",
                        onAction = onNavigateToRegister
                    )

                    // Step 2
                    IntroStepRow(
                        stepNumber = "2",
                        title = "Log Seasonal Practices",
                        description = "Select the farmer and pick an icon: 🌱 Planting, 💧 Irrigation, 🧪 Pesticides, 🌿 Fertilizers, or ✂️ Harvest. Validates NAFDAC approval & safe harvest dates.",
                        icon = Icons.Default.Science,
                        actionLabel = "Log",
                        onAction = onNavigateToPractice
                    )

                    // Step 3
                    IntroStepRow(
                        stepNumber = "3",
                        title = "Sync & Verify Records",
                        description = "When cellular signal is available, push pending records upstream to generate permanent official IDs and ledger provenance.",
                        icon = Icons.Default.CloudSync,
                        actionLabel = "Queue",
                        onAction = onNavigateToSync
                    )

                    // Footer actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onOpenFullGuide,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("open_detailed_guide_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Agent Guide & FAQs", fontSize = 11.sp)
                        }

                        TextButton(
                            onClick = { isExpanded = false }
                        ) {
                            Text("Hide Guide", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroStepRow(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(HarvestGreenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = HarvestGreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF1B5E20)
                    )
                }
                Text(
                    text = description,
                    fontSize = 10.5.sp,
                    color = Color(0xFF546E7A),
                    lineHeight = 14.sp
                )
            }

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.clickable { onAction() }
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HarvestGreenPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Full-screen detailed modal guide for agents explaining purpose,
 * compliance rules, and best practices.
 */
@Composable
fun AppGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("app_guide_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F3820), Color(0xFF1B5E20))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Agent Handbook & Guide",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Complete instructions for TraceHarvest field operations, compliance rules, and offline synchronization.",
                                fontSize = 12.sp,
                                color = Color(0xFFC8E6C9),
                                lineHeight = 16.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Body content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_farmer_hero),
                        contentDescription = "Nigerian smallholder farmers inspecting crops in field",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Section 1: What is TraceHarvest for?
                    GuideSection(
                        icon = Icons.Default.Shield,
                        title = "What is TraceHarvest For?",
                        content = "TraceHarvest solves agricultural provenance for Nigerian export cash crops (Sesame, Cowpea, Ginger, Hibiscus, Soybeans, Cashew). It ensures smallholder crops meet international standards (EU Deforestation Regulation EUDR and Maximum Residue Limits MRLs) before aggregation."
                    )

                    // Section 2: Zero-Literacy Principle
                    GuideSection(
                        icon = Icons.Default.PhoneAndroid,
                        title = "The Zero-Literacy Principle",
                        content = "Field agents do all digital work on behalf of farmers. Smallholders require NO app download, NO internet data bundles, and NO literacy. Their standard mobile phone number serves as their provenance identity, and they receive SMS transaction confirmations."
                    )

                    // Section 3: Farmer Registration Workflow
                    GuideSection(
                        icon = Icons.Default.PersonAdd,
                        title = "Step 1: Enrolling a Farmer",
                        content = "• Name & Phone: Enter the farmer's full name and active phone number.\n" +
                                "• GPS Auto-Capture: The app automatically acquires phone GPS latitude and longitude with meter accuracy.\n" +
                                "• Farm Size: Enter in local units (hectares, acres, or traditional 'plots'). The app automatically calculates standard metric hectares.\n" +
                                "• Cooperative: Select the farmer's local cooperative union or mark as Independent.\n" +
                                "• Offline-First: If you are in a remote rural area with no cellular network, the enrollment is saved directly into local SQLite storage with status 'pending_sync'."
                    )

                    // Section 4: Stage 2 Practice Logging
                    GuideSection(
                        icon = Icons.Default.Science,
                        title = "Step 2: Seasonal Practice Logging",
                        content = "Select the farmer by name or code and choose an icon:\n" +
                                "• 🌱 Planting: Sowing certified seed varieties.\n" +
                                "• 💧 Irrigation: Furrow or basin watering cycles.\n" +
                                "• 🧪 Pesticide application: Filtered against official NAFDAC catalog (e.g. Karate 5 EC). Automatically tracks Pre-Harvest Intervals (PHI) and blocks banned chemicals like Sniper (Dichlorvos).\n" +
                                "• 🌿 Fertilizer application: Record Indorama NPK 15:15:15 or Granular Urea.\n" +
                                "• ✂️ Harvest: Safe harvesting after the chemical Pre-Harvest Interval has elapsed.\n" +
                                "• Photo Proof: Take or select an image of the chemical bottle label or field barcode."
                    )

                    // Section 5: Offline Sync Engine
                    GuideSection(
                        icon = Icons.Default.CloudSync,
                        title = "Step 3: Background Synchronization",
                        content = "All records remain 100% safe in local device SQLite storage. Whenever your device reconnects to mobile data or Wi-Fi, tap 'Sync Now' in the top bar or Sync tab. The backend generates permanent official IDs (e.g., TH-KAN-2026-XXXX) and binds the records to the export ledger."
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Got it, Start Working", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6EE9C))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HarvestGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF1B5E20)
                )
            }
            Text(
                text = content,
                fontSize = 11.5.sp,
                color = Color(0xFF37474F),
                lineHeight = 16.sp
            )
        }
    }
}
