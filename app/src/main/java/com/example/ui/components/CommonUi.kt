package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlockchainBlue
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.ViolationRed
import com.example.ui.theme.WarningAmber
import kotlin.math.abs

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        "PASSED_SPS", "Grade A Export Ready", "COMPLIANT", "SAFE (<4 ppb)" -> {
            Quad(
                Color(0xFFE8F5E9),
                ComplianceGreen,
                Icons.Default.CheckCircle,
                if (status == "PASSED_SPS") "SPS Verified" else status
            )
        }
        "PENDING_PHI", "CAUTION", "ELEVATED (4-10 ppb)", "Grade B Local Processing" -> {
            Quad(
                Color(0xFFFFF3E0),
                WarningAmber,
                Icons.Default.Warning,
                if (status == "PENDING_PHI") "Pending PHI" else status
            )
        }
        "VIOLATION_BLOCKED", "Non-Compliant High Risk", "BANNED_MRL_VIOLATION", "UNSAFE (>10 ppb)" -> {
            Quad(
                Color(0xFFFFEBEE),
                ViolationRed,
                Icons.Default.Error,
                if (status == "VIOLATION_BLOCKED") "MRL Blocked" else status
            )
        }
        else -> {
            Quad(
                Color(0xFFEDE7F6),
                Color(0xFF512DA8),
                Icons.Default.Shield,
                status
            )
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .border(1.dp, Color(0xFFE0E5DD), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF61685F)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF757D73)
            )
        }
    }
}

/**
 * Procedural visual QR code renderer that creates realistic 2D matrix
 * QR patterns with corner position detection patterns and data modules.
 */
@Composable
fun QrCodeVisual(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val gridSize = 21 // Version 1 QR matrix is 21x21
    val hash = abs(payload.hashCode())

    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 20.dp)) {
            val cellW = this.size.width / gridSize
            val cellH = this.size.height / gridSize

            // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
            fun drawFinder(startX: Int, startY: Int) {
                // Outer 7x7
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(startX * cellW, startY * cellH),
                    size = Size(7 * cellW, 7 * cellH),
                    cornerRadius = CornerRadius(cellW, cellH)
                )
                // White 5x5
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset((startX + 1) * cellW, (startY + 1) * cellH),
                    size = Size(5 * cellW, 5 * cellH),
                    cornerRadius = CornerRadius(cellW * 0.5f, cellH * 0.5f)
                )
                // Inner 3x3
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset((startX + 2) * cellW, (startY + 2) * cellH),
                    size = Size(3 * cellW, 3 * cellH),
                    cornerRadius = CornerRadius(cellW * 0.3f, cellH * 0.3f)
                )
            }

            drawFinder(0, 0)
            drawFinder(gridSize - 7, 0)
            drawFinder(0, gridSize - 7)

            // Timing patterns
            for (i in 8 until gridSize - 8) {
                if (i % 2 == 0) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(i * cellW, 6 * cellH),
                        size = Size(cellW, cellH)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(6 * cellW, i * cellH),
                        size = Size(cellW, cellH)
                    )
                }
            }

            // Pseudo-random data modules derived consistently from payload hash
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    // Skip finder pattern zones
                    val inTopLeft = r < 8 && c < 8
                    val inTopRight = r < 8 && c >= gridSize - 8
                    val inBottomLeft = r >= gridSize - 8 && c < 8
                    if (inTopLeft || inTopRight || inBottomLeft) continue

                    // Deterministic cell active state
                    val cellSeed = (r * 31 + c * 17 + hash)
                    if (cellSeed % 3 == 0 || (cellSeed % 5 == 0 && (r + c) % 2 == 0)) {
                        drawRect(
                            color = Color(0xFF1B5E20),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.92f, cellH * 0.92f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockchainAnchorChip(
    isAnchored: Boolean,
    txId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isAnchored) Color(0xFFE3F2FD) else Color(0xFFECEFF1),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = if (isAnchored) BlockchainBlue else Color.Gray,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isAnchored) "Polygon: ${txId.take(10)}..." else "Unanchored",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isAnchored) BlockchainBlue else Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
