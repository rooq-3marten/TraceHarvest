package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.HarvestBatchEntity
import com.example.ui.components.QrCodeVisual
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BlockchainBlue
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.ViolationRed
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatchDetailDialog(
    batch: HarvestBatchEntity,
    onDismiss: () -> Unit,
    onToggleFlag: (HarvestBatchEntity) -> Unit,
    onAnchorToBlockchain: (HarvestBatchEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val harvestDateStr = dateFormat.format(Date(batch.harvestDate))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("sps_certificate_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header with official stamp badge and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ComplianceGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SPS EXPORT PROVENANCE PASSPORT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ComplianceGreen
                            )
                            Text(
                                text = "NAFDAC / NEPC / STDF Compliance Record",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_certificate_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Certificate ID & Batch banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (batch.isFlaggedForRejection) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CONSIGNMENT BATCH CODE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF558B2F)
                                )
                                Text(
                                    text = batch.batchCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (batch.isFlaggedForRejection) ViolationRed else ComplianceGreen
                                )
                            }
                            StatusBadge(status = batch.mrlStatus)
                        }

                        if (batch.isFlaggedForRejection && batch.rejectionReason != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFCDD2), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ViolationRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = batch.rejectionReason ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ViolationRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Two columns / grid: QR Code + Key specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QrCodeVisual(
                        payload = batch.qrPayload,
                        size = 130.dp,
                        modifier = Modifier.testTag("batch_qr_code")
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Crop: ${batch.crop}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Destination: ${batch.destinationMarket}",
                            fontSize = 12.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Volume: ${batch.netWeightKg.toInt()} kg (${batch.bagCount} bags)",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Harvested: $harvestDateStr",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Farm Origin & Producer Geotag
                Text(
                    text = "1. FARM & PRODUCER PROVENANCE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailRow(label = "Producer Name", value = batch.farmerName)
                        DetailRow(label = "Farmer Unique ID", value = batch.farmerCode)
                        DetailRow(label = "Production Region", value = batch.region)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ComplianceGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "GPS Geotag Verified (Polygon Boundary Logged)",
                                fontSize = 11.sp,
                                color = ComplianceGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Phytosanitary, MRL & Aflatoxin Safety
                Text(
                    text = "2. SANITARY & PHYTOSANITARY (SPS) METRICS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Grain Moisture Level", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    "${batch.moisturePercent}% (Standard <= 10.0%)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (batch.moisturePercent <= 10.0) ComplianceGreen else ViolationRed
                                )
                            }
                            StatusBadge(status = batch.aflatoxinStatus)
                        }

                        DetailRow(
                            label = "Foreign Matter (Impurities)",
                            value = "${batch.foreignMatterPercent}% (Grade Standard <= 2.0%)"
                        )
                        DetailRow(
                            label = "Pre-Harvest Interval (PHI)",
                            value = "${batch.phiDaysObserved} days observed (Safety verified)"
                        )
                        DetailRow(
                            label = "EU / CODEX MRL Threshold",
                            value = if (batch.mrlStatus == "PASSED_SPS") "Compliant (EC Reg 396/2005 Passed)" else "NON-COMPLIANT / REJECTION RISK"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Blockchain Cryptographic Proof
                Text(
                    text = "3. IMMUTABLE BLOCKCHAIN AUDIT TRAIL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ledger: Polygon Mainnet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BlockchainBlue
                            )
                            if (batch.isBlockchainAnchored) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = BlockchainBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "TAMPER-EVIDENT SECURED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlockchainBlue
                                    )
                                }
                            } else {
                                Text(
                                    "PENDING ANCHOR",
                                    fontSize = 10.sp,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text("Provenance Merkle Root Hash (SHA-256):", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = batch.blockchainHash,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF283593)
                        )

                        Text("Transaction ID:", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = batch.blockchainTxId,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF283593)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Quarantine Flag, Blockchain Anchor, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToggleFlag(batch) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_flag_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (batch.isFlaggedForRejection) ComplianceGreen else ViolationRed
                        )
                    ) {
                        Icon(
                            imageVector = if (batch.isFlaggedForRejection) Icons.Default.CheckCircle else Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (batch.isFlaggedForRejection) "Clear Flag" else "Flag Risk",
                            fontSize = 12.sp
                        )
                    }

                    if (!batch.isBlockchainAnchored) {
                        Button(
                            onClick = { onAnchorToBlockchain(batch) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anchor_blockchain_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlockchainBlue)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anchor", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("done_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ComplianceGreen)
                    ) {
                        Text("Done", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF616161))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
