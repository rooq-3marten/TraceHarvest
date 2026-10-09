package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentAuthViewModel

private val TextHeaderDark = Color(0xFF1A2E23)
private val TextSubtleGray = Color(0xFF5A6B60)

/**
 * Section 1: Agent Self-Registration Screen
 * Fields:
 * - Full name
 * - Email
 * - Farmers association / cooperative
 * - Operating location (LGA / State)
 * - Nigerian Phone number (+234 format)
 * - Password & Confirm password
 *
 * Client-side validation with inline error messaging.
 * Never sends a 'status' value to server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSignUpScreen(
    authViewModel: AgentAuthViewModel,
    onNavigateBackToSignIn: () -> Unit,
    onSignUpSuccessPending: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullName by authViewModel.signUpFullName.collectAsState()
    val email by authViewModel.signUpEmail.collectAsState()
    val association by authViewModel.signUpAssociation.collectAsState()
    val location by authViewModel.signUpLocation.collectAsState()
    val phone by authViewModel.signUpPhone.collectAsState()
    val password by authViewModel.signUpPassword.collectAsState()
    val confirmPassword by authViewModel.signUpConfirmPassword.collectAsState()

    val errors by authViewModel.signUpErrors.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val serverError by authViewModel.errorMessage.collectAsState()

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Agent Registration",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBackToSignIn,
                        modifier = Modifier.testTag("signup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to sign in",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreenPrimary
                )
            )
        },
        containerColor = PureWhiteSurface,
        modifier = modifier.testTag("agent_signup_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Intro
            Card(
                colors = CardDefaults.cardColors(containerColor = SoftGreenBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Agriculture,
                        contentDescription = null,
                        tint = DarkGreenPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Join TraceHarvest Field Team",
                            fontWeight = FontWeight.Bold,
                            color = TextHeaderDark,
                            fontSize = 16.sp
                        )
                        Text(
                            "Submit your details for supervisor verification. Once approved, your device will be authorized for offline farmer registration.",
                            color = TextSubtleGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Global Error Banner (e.g. duplicate email/phone from backend)
            if (serverError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = serverError!!,
                            color = AlertRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Field 1: Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { authViewModel.setSignUpFullName(it) },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. Aminu Bello Dambatta") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ForestGreenAccent) },
                isError = errors.containsKey("fullName"),
                supportingText = errors["fullName"]?.let { { Text(it, color = AlertRed) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_fullname_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Field 2: Email Address
            OutlinedTextField(
                value = email,
                onValueChange = { authViewModel.setSignUpEmail(it) },
                label = { Text("Email Address *") },
                placeholder = { Text("agent.name@traceharvest.ng") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenAccent) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errors.containsKey("email"),
                supportingText = errors["email"]?.let { { Text(it, color = AlertRed) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Field 3: Farmers Association / Cooperative
            OutlinedTextField(
                value = association,
                onValueChange = { authViewModel.setSignUpAssociation(it) },
                label = { Text("Farmers Association / Cooperative *") },
                placeholder = { Text("e.g. Kano Rice & Grains Cooperative") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = ForestGreenAccent) },
                isError = errors.containsKey("association"),
                supportingText = errors["association"]?.let { { Text(it, color = AlertRed) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_association_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Field 4: Operating Location (LGA, State)
            OutlinedTextField(
                value = location,
                onValueChange = { authViewModel.setSignUpLocation(it) },
                label = { Text("Operating Location (LGA / State) *") },
                placeholder = { Text("e.g. Dambatta LGA, Kano State") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = ForestGreenAccent) },
                isError = errors.containsKey("location"),
                supportingText = errors["location"]?.let { { Text(it, color = AlertRed) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_location_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Field 5: Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { authViewModel.setSignUpPhone(it) },
                label = { Text("Phone Number *") },
                placeholder = { Text("+234 803 123 4567") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenAccent) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = errors.containsKey("phone"),
                supportingText = {
                    Text(
                        errors["phone"] ?: "Standard Nigerian format (+234 or 080...)",
                        color = if (errors.containsKey("phone")) AlertRed else TextSubtleGray
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Field 6: Password
            OutlinedTextField(
                value = password,
                onValueChange = { authViewModel.setSignUpPassword(it) },
                label = { Text("Password *") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenAccent) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = errors.containsKey("password"),
                supportingText = errors["password"]?.let { { Text(it, color = AlertRed) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Field 7: Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { authViewModel.setSignUpConfirmPassword(it) },
                label = { Text("Confirm Password *") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = ForestGreenAccent) },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = errors.containsKey("confirmPassword"),
                supportingText = errors["confirmPassword"]?.let { { Text(it, color = AlertRed) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    authViewModel.submitSignUp(onSuccessPending = onSignUpSuccessPending)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkGreenPrimary,
                    contentColor = PureWhiteSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("signup_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PureWhiteSurface,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Submitting Application...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Up as Field Agent", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Return to Sign In link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateBackToSignIn() }
            ) {
                Text("Already registered? ", color = TextSubtleGray, fontSize = 14.sp)
                Text(
                    "Sign in here",
                    color = DarkGreenPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
