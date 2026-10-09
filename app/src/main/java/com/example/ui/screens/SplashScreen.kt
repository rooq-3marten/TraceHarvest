package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.AgentApprovalStatus
import com.example.core.auth.AgentProfile
import com.example.core.auth.SessionManager
import com.example.data.remote.NetworkClient
import com.example.ui.theme.DarkGreenDark
import com.example.ui.theme.DarkGreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Section 1A: Splash Screen (1.5 seconds)
 * Checks session in DataStore & verifies live Agent Approval Status:
 * - approved -> Home
 * - pending -> Pending Approval screen
 * - rejected -> Rejected screen
 * - suspended -> Suspended screen
 * - no session -> Login
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToStatus: (AgentApprovalStatus, AgentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        val sessionManager = SessionManager.getInstance(context)
        val hasSession = sessionManager.hasValidSession()

        // 1.5 seconds brand presentation
        delay(1500)

        if (hasSession) {
            val cachedProfile = sessionManager.getCachedProfile()
            val token = sessionManager.getAuthToken()
            if (token != null) {
                NetworkClient.setAuthToken(token)
            }

            // Attempt online status check if network is reachable
            val liveStatusResponse = withContext(Dispatchers.IO) {
                try {
                    val resp = NetworkClient.apiService.getAgentStatus()
                    if (resp.isSuccessful) resp.body() else null
                } catch (e: Exception) {
                    null
                }
            }

            val finalStatus = if (liveStatusResponse != null) {
                val status = AgentApprovalStatus.fromRaw(liveStatusResponse.status)
                sessionManager.updateAgentStatus(
                    status = status,
                    rejectionReason = liveStatusResponse.rejectionReason,
                    updatedName = liveStatusResponse.fullName.ifBlank { null },
                    updatedAssociation = liveStatusResponse.association.ifBlank { null },
                    updatedLocation = liveStatusResponse.location.ifBlank { null },
                    updatedPhone = liveStatusResponse.phoneNumber.ifBlank { null }
                )
                status
            } else {
                cachedProfile.status
            }

            val updatedProfile = sessionManager.getCachedProfile()
            when (finalStatus) {
                AgentApprovalStatus.APPROVED -> onNavigateToHome()
                AgentApprovalStatus.PENDING,
                AgentApprovalStatus.REJECTED,
                AgentApprovalStatus.SUSPENDED -> onNavigateToStatus(finalStatus, updatedProfile)
            }
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGreenPrimary)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim.value)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nature,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "TraceHarvest",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Central Agricultural Registry",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(36.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Field Agent Security • FMC & NCX Verified",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.60f)
            )
        }
    }
}
