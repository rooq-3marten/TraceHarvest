package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.AgentApprovalStatus
import com.example.core.auth.AgentProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentAuthViewModel

private val TextHeaderDark = Color(0xFF1A2E23)
private val TextSubtleGray = Color(0xFF5A6B60)

/**
 * Authentication Screen Design
 * Deep green & white theme. Stacked on phones, split layout on tablets.
 * Implements Phone + PIN (primary) and Email + Password (supervisors).
 * Warm microcopy throughout.
 * Includes direct action to Self-Registration ("Register as new agent").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentLoginScreen(
    authViewModel: AgentAuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onRouteByStatus: (status: AgentApprovalStatus, profile: AgentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPhoneMode by authViewModel.isPhoneMode.collectAsState()
    val phoneNumber by authViewModel.phoneNumber.collectAsState()
    val pin by authViewModel.pin.collectAsState()
    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val keepMeSignedIn by authViewModel.keepMeSignedIn.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var showPassword by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhiteSurface)
            .testTag("agent_login_screen")
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Split Tablet Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Deep Green Brand Header
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.42f)
                        .background(DarkGreenPrimary)
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BrandHeaderContent()
                }

                // Right Panel: Auth Form
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.58f)
                        .background(PureWhiteSurface)
                        .padding(horizontal = 48.dp, vertical = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    AuthFormContent(
                        isPhoneMode = isPhoneMode,
                        phoneNumber = phoneNumber,
                        pin = pin,
                        email = email,
                        password = password,
                        keepMeSignedIn = keepMeSignedIn,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        showPassword = showPassword,
                        onPhoneNumberChange = { authViewModel.setPhoneNumber(it) },
                        onPinChange = { authViewModel.setPin(it) },
                        onEmailChange = { authViewModel.setEmail(it) },
                        onPasswordChange = { authViewModel.setPassword(it) },
                        onTogglePassword = { showPassword = !showPassword },
                        onToggleMode = { authViewModel.toggleAuthMethod() },
                        onKeepSignedInChange = { authViewModel.setKeepMeSignedIn(it) },
                        onForgotPin = { showForgotPinDialog = true },
                        onRegisterClicked = onNavigateToSignUp,
                        onSubmit = {
                            authViewModel.login { status, profile ->
                                onRouteByStatus(status, profile)
                            }
                        }
                    )
                }
            }
        } else {
            // Stacked Phone Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Deep green header band, 200dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(DarkGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    BrandHeaderContent()
                }

                // Form body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureWhiteSurface)
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    AuthFormContent(
                        isPhoneMode = isPhoneMode,
                        phoneNumber = phoneNumber,
                        pin = pin,
                        email = email,
                        password = password,
                        keepMeSignedIn = keepMeSignedIn,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        showPassword = showPassword,
                        onPhoneNumberChange = { authViewModel.setPhoneNumber(it) },
                        onPinChange = { authViewModel.setPin(it) },
                        onEmailChange = { authViewModel.setEmail(it) },
                        onPasswordChange = { authViewModel.setPassword(it) },
                        onTogglePassword = { showPassword = !showPassword },
                        onToggleMode = { authViewModel.toggleAuthMethod() },
                        onKeepSignedInChange = { authViewModel.setKeepMeSignedIn(it) },
                        onForgotPin = { showForgotPinDialog = true },
                        onRegisterClicked = onNavigateToSignUp,
                        onSubmit = {
                            authViewModel.login { status, profile ->
                                onRouteByStatus(status, profile)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = DarkGreenPrimary) },
            title = { Text("Need to reset your PIN?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "For field security, agent PIN resets are handled by your Area Supervisor or Zonal Extension Office. Contact them with your Agent ID to issue a fresh 4-digit PIN."
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text("Understood", color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun BrandHeaderContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nature,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "TraceHarvest",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Agent App",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.70f)
        )
    }
}

@Composable
private fun AuthFormContent(
    isPhoneMode: Boolean,
    phoneNumber: String,
    pin: String,
    email: String,
    password: String,
    keepMeSignedIn: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    showPassword: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleMode: () -> Unit,
    onKeepSignedInChange: (Boolean) -> Unit,
    onForgotPin: () -> Unit,
    onRegisterClicked: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome text
        Column {
            Text(
                text = "Welcome back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextHeaderDark
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sign in to continue",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSubtleGray
            )
        }

        // Mode Switcher Tab
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGreenBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { if (!isPhoneMode) onToggleMode() },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPhoneMode) PureWhiteSurface else Color.Transparent,
                    shadowElevation = if (isPhoneMode) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Field Agent (PIN)",
                            fontSize = 13.sp,
                            fontWeight = if (isPhoneMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPhoneMode) DarkGreenPrimary else TextSubtleGray
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { if (isPhoneMode) onToggleMode() },
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isPhoneMode) PureWhiteSurface else Color.Transparent,
                    shadowElevation = if (!isPhoneMode) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Supervisor",
                            fontSize = 13.sp,
                            fontWeight = if (!isPhoneMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isPhoneMode) DarkGreenPrimary else TextSubtleGray
                        )
                    }
                }
            }
        }

        if (isPhoneMode) {
            // Method 1: Phone + PIN (primary)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = { Text("Phone number") },
                    placeholder = { Text("+234 803 123 4567") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = DarkGreenPrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkGreenPrimary,
                        unfocusedBorderColor = OutlineWarm
                    )
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = { Text("PIN") },
                    placeholder = { Text("••••") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = DarkGreenPrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkGreenPrimary,
                        unfocusedBorderColor = OutlineWarm
                    )
                )
            }
        } else {
            // Method 2: Email + Password
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Supervisor Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkGreenPrimary,
                        unfocusedBorderColor = OutlineWarm
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onTogglePassword) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkGreenPrimary,
                        unfocusedBorderColor = OutlineWarm
                    )
                )
            }
        }

        // Keep me signed in Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onKeepSignedInChange(!keepMeSignedIn) }
        ) {
            Checkbox(
                checked = keepMeSignedIn,
                onCheckedChange = onKeepSignedInChange,
                colors = CheckboxDefaults.colors(checkedColor = DarkGreenPrimary),
                modifier = Modifier.testTag("login_keep_signed_in_checkbox")
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Keep me signed in",
                fontSize = 14.sp,
                color = TextHeaderDark
            )
        }

        // Inline Error Message
        errorMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Deep Green Sign In Button
        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkGreenPrimary,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Signing you in...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = "Sign in",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Forgot PIN link
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPhoneMode) "Forgot your PIN?" else "Forgot your password?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DarkGreenPrimary,
                modifier = Modifier
                    .clickable(onClick = onForgotPin)
                    .padding(vertical = 4.dp)
                    .testTag("forgot_pin_link")
            )
        }

        HorizontalDivider(
            color = OutlineWarm,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // New Agent Registration Button / Link
        OutlinedButton(
            onClick = onRegisterClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("new_agent_signup_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("New Field Agent? Register here", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
