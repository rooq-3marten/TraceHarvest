package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.zone.GeopoliticalZone
import com.example.core.zone.ZoneRegistry
import com.example.ui.theme.*

/**
 * Screen 2: Agent Login
 * Uses warm, human-centered language ("Welcome back") and zone indicators.
 */
@Composable
fun AgentLoginScreen(
    currentZone: GeopoliticalZone,
    onLoginSuccess: (agentEmail: String) -> Unit,
    onSwitchZone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("aminu.bello@traceharvest.ng") }
    var password by remember { mutableStateOf("••••••••") }
    var rememberMe by remember { mutableStateOf(true) }
    val zoneProfile = remember(currentZone) { ZoneRegistry.getProfile(currentZone) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("agent_login_screen"),
        color = WarmOffWhiteBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Nature, contentDescription = null, tint = ForestGreenSecondary)
                            Text(
                                text = "TraceHarvest Field Agent",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ForestGreenSecondary
                            )
                        }

                        Text(
                            text = "Welcome back",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = CharcoalBrownText
                        )

                        Text(
                            text = "Sign in to record farmer enrollments, spray logs, and consignment batches.",
                            fontSize = 13.sp,
                            color = MutedBrownText
                        )
                    }

                    // Zone indicator chip
                    Surface(
                        onClick = onSwitchZone,
                        color = WarmOchreContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OPERATIONAL REGION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmOchreDark
                                )
                                Text(
                                    text = "${zoneProfile.zone.zoneName} (${zoneProfile.states.first()})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalBrownText
                                )
                            }
                            Text(
                                text = "Change",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LateriteRedPrimary
                            )
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Agent Email") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MutedBrownText) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MutedBrownText) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Remember Me & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = ForestGreenSecondary)
                            )
                            Text(text = "Remember me", fontSize = 12.sp, color = CharcoalBrownText)
                        }

                        TextButton(onClick = { /* Reset */ }) {
                            Text("Need help?", fontSize = 12.sp, color = LateriteRedPrimary)
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = { onLoginSuccess(email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("agent_login_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LateriteRedPrimary)
                    ) {
                        Text(
                            text = "Continue to Dashboard",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
