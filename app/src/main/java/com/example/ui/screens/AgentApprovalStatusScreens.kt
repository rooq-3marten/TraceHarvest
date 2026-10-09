package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.AgentApprovalStatus
import com.example.core.auth.AgentProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentAuthViewModel

private val TextHeaderDark = Color(0xFF1A2E23)
private val TextSubtleGray = Color(0xFF5A6B60)

/**
 * Screen 1: Pending Approval Screen
 * Shown to new or awaiting agents. Provides a manual Refresh button,
 * status details, supervisor support info, and an option to sign out.
 */
@Composable
fun AgentPendingApprovalScreen(
    authViewModel: AgentAuthViewModel,
    profile: AgentProfile,
    onApproved: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading by authViewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhiteSurface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("agent_pending_approval_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pending Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(GoldenAmber.copy(alpha = 0.15f), shape = CircleShape)
                .border(2.dp, GoldenAmber, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = "Pending approval",
                tint = GoldenAmber,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Account Pending Approval",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextHeaderDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Welcome to TraceHarvest, ${profile.name}! Your application has been submitted to the admin dashboard and is awaiting supervisor verification.",
            fontSize = 14.sp,
            color = TextSubtleGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Application Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftGreenBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Submitted Application Details",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                DetailRow("Cooperative:", profile.association)
                DetailRow("Operating LGA:", profile.location)
                DetailRow("Phone:", profile.phone)
                DetailRow("Email:", profile.email)
                DetailRow("Status:", "PENDING VERIFICATION", isStatus = true)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Refresh Button
        Button(
            onClick = {
                authViewModel.refreshAgentStatus { status ->
                    if (status == AgentApprovalStatus.APPROVED) {
                        onApproved()
                    }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkGreenPrimary,
                contentColor = PureWhiteSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("pending_refresh_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking status with dashboard...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Approval Status", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sandbox Simulator Helper: lets developers/testers simulate admin approval immediately
        OutlinedButton(
            onClick = {
                authViewModel.simulateAdminApproval { onApproved() }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
            modifier = Modifier.fillMaxWidth().testTag("simulate_admin_approve_button")
        ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Dashboard Approval (Sandbox)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("pending_signout_button")
        ) {
            Text("Sign out / Use another account", color = TextSubtleGray)
        }
    }
}

/**
 * Screen 2: Rejected Screen
 * Shows rejection reason with an option to update details and resubmit.
 */
@Composable
fun AgentRejectedScreen(
    authViewModel: AgentAuthViewModel,
    profile: AgentProfile,
    onResubmitted: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    val resubmitName by authViewModel.resubmitFullName.collectAsState()
    val resubmitAssoc by authViewModel.resubmitAssociation.collectAsState()
    val resubmitLoc by authViewModel.resubmitLocation.collectAsState()
    val resubmitPhone by authViewModel.resubmitPhone.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.populateResubmitFromCached()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhiteSurface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("agent_rejected_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Rejected Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(AlertRed.copy(alpha = 0.12f), shape = CircleShape)
                .border(2.dp, AlertRed, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Application Rejected",
                tint = AlertRed,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Registration Not Approved",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextHeaderDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Reason Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Reason Provided by Supervisor:",
                    fontWeight = FontWeight.Bold,
                    color = AlertRed,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    profile.rejectionReason ?: "Information provided could not be verified against the state cooperative registry. Please review your association name and operating LGA.",
                    color = TextHeaderDark,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isEditing) {
            Text(
                "You can correct your information and resubmit your application for re-review.",
                fontSize = 13.sp,
                color = TextSubtleGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { isEditing = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkGreenPrimary,
                    contentColor = PureWhiteSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("rejected_update_details_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Details & Resubmit")
            }
        } else {
            // Editable Resubmission Form
            if (errorMessage != null) {
                Text(errorMessage!!, color = AlertRed, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = resubmitName,
                onValueChange = { authViewModel.setResubmitFullName(it) },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("resubmit_fullname_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = resubmitAssoc,
                onValueChange = { authViewModel.setResubmitAssociation(it) },
                label = { Text("Cooperative / Association") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("resubmit_association_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = resubmitLoc,
                onValueChange = { authViewModel.setResubmitLocation(it) },
                label = { Text("Operating Location (LGA, State)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("resubmit_location_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = resubmitPhone,
                onValueChange = { authViewModel.setResubmitPhone(it) },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("resubmit_phone_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    authViewModel.resubmitDetails(onSuccess = onResubmitted)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkGreenPrimary,
                    contentColor = PureWhiteSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("resubmit_send_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Resubmit for Approval")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { isEditing = false }) {
                Text("Cancel", color = TextSubtleGray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("rejected_signout_button")
        ) {
            Text("Sign out", color = TextSubtleGray)
        }
    }
}

/**
 * Screen 3: Account Suspended Screen
 * Shown when an agent account is suspended by admin. Provides supervisor contact info.
 */
@Composable
fun AgentSuspendedScreen(
    profile: AgentProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhiteSurface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("agent_suspended_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Suspended Icon
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(AlertRed.copy(alpha = 0.15f), shape = CircleShape)
                .border(2.dp, AlertRed, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Account Suspended",
                tint = AlertRed,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Account Suspended",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextHeaderDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Your field agent account (${profile.agentId}) has been suspended by project administration.",
            fontSize = 14.sp,
            color = TextSubtleGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SoftGreenBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Supervisor Contact & Inquiries",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "To resolve this suspension or request an operational review, please contact your zonal supervisor or the TraceHarvest administrative office:",
                    fontSize = 13.sp,
                    color = TextHeaderDark,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Zonal Office:", "North-West Operations, Kano")
                DetailRow("Support Line:", "+234 800-TRACE-NG")
                DetailRow("Email:", "support@traceharvest.ng")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("suspended_signout_button")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isStatus: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSubtleGray)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isStatus) GoldenAmber else TextHeaderDark
        )
    }
}
