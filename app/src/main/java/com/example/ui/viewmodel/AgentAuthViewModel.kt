package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.auth.AgentApprovalStatus
import com.example.core.auth.AgentProfile
import com.example.core.auth.SessionManager
import com.example.data.remote.NetworkClient
import com.example.data.remote.model.AgentResubmitRequest
import com.example.data.remote.model.AgentSignUpRequest
import com.example.data.remote.model.AuthLoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Enhanced Authentication & Approval ViewModel.
 * Governs Agent Self-Registration, Approval-Aware Sign-In, Live Status Verification,
 * and Profile Syncing with Offline Caching.
 */
class AgentAuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)

    // Sign-in state
    private val _isPhoneMode = MutableStateFlow(false)
    val isPhoneMode: StateFlow<Boolean> = _isPhoneMode.asStateFlow()

    private val _phoneNumber = MutableStateFlow("+2348031234567")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _pin = MutableStateFlow("1234")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _email = MutableStateFlow("supervisor.kano@traceharvest.ng")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _keepMeSignedIn = MutableStateFlow(true)
    val keepMeSignedIn: StateFlow<Boolean> = _keepMeSignedIn.asStateFlow()

    // Sign-up Form State
    private val _signUpFullName = MutableStateFlow("")
    val signUpFullName: StateFlow<String> = _signUpFullName.asStateFlow()

    private val _signUpEmail = MutableStateFlow("")
    val signUpEmail: StateFlow<String> = _signUpEmail.asStateFlow()

    private val _signUpAssociation = MutableStateFlow("")
    val signUpAssociation: StateFlow<String> = _signUpAssociation.asStateFlow()

    private val _signUpLocation = MutableStateFlow("Dambatta, Kano State")
    val signUpLocation: StateFlow<String> = _signUpLocation.asStateFlow()

    private val _signUpPhone = MutableStateFlow("+234")
    val signUpPhone: StateFlow<String> = _signUpPhone.asStateFlow()

    private val _signUpPassword = MutableStateFlow("")
    val signUpPassword: StateFlow<String> = _signUpPassword.asStateFlow()

    private val _signUpConfirmPassword = MutableStateFlow("")
    val signUpConfirmPassword: StateFlow<String> = _signUpConfirmPassword.asStateFlow()

    // Inline errors for Sign-up
    val signUpErrors = MutableStateFlow<Map<String, String>>(emptyMap())

    // Resubmission form state (for rejected agents)
    private val _resubmitFullName = MutableStateFlow("")
    val resubmitFullName: StateFlow<String> = _resubmitFullName.asStateFlow()

    private val _resubmitAssociation = MutableStateFlow("")
    val resubmitAssociation: StateFlow<String> = _resubmitAssociation.asStateFlow()

    private val _resubmitLocation = MutableStateFlow("")
    val resubmitLocation: StateFlow<String> = _resubmitLocation.asStateFlow()

    private val _resubmitPhone = MutableStateFlow("")
    val resubmitPhone: StateFlow<String> = _resubmitPhone.asStateFlow()

    // Global loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Current cached profile & status
    val currentProfile: StateFlow<AgentProfile> = sessionManager.agentProfileFlow as? StateFlow<AgentProfile>
        ?: MutableStateFlow(AgentProfile()).also { flow ->
            viewModelScope.launch {
                sessionManager.agentProfileFlow.collect { flow.value = it }
            }
        }

    // Setters for Sign-In
    fun setPhoneNumber(num: String) {
        _phoneNumber.value = num
        _errorMessage.value = null
    }

    fun setPin(p: String) {
        if (p.length <= 6 && (p.isEmpty() || p.all { it.isDigit() })) {
            _pin.value = p
            _errorMessage.value = null
        }
    }

    fun setEmail(e: String) {
        _email.value = e
        _errorMessage.value = null
    }

    fun setPassword(p: String) {
        _password.value = p
        _errorMessage.value = null
    }

    fun toggleAuthMethod() {
        _isPhoneMode.value = !_isPhoneMode.value
        _errorMessage.value = null
    }

    fun setKeepMeSignedIn(keep: Boolean) {
        _keepMeSignedIn.value = keep
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Setters for Sign-Up
    fun setSignUpFullName(v: String) {
        _signUpFullName.value = v
        clearFieldError("fullName")
    }

    fun setSignUpEmail(v: String) {
        _signUpEmail.value = v
        clearFieldError("email")
    }

    fun setSignUpAssociation(v: String) {
        _signUpAssociation.value = v
        clearFieldError("association")
    }

    fun setSignUpLocation(v: String) {
        _signUpLocation.value = v
        clearFieldError("location")
    }

    fun setSignUpPhone(v: String) {
        _signUpPhone.value = v
        clearFieldError("phone")
    }

    fun setSignUpPassword(v: String) {
        _signUpPassword.value = v
        clearFieldError("password")
    }

    fun setSignUpConfirmPassword(v: String) {
        _signUpConfirmPassword.value = v
        clearFieldError("confirmPassword")
    }

    private fun clearFieldError(field: String) {
        val current = signUpErrors.value.toMutableMap()
        current.remove(field)
        signUpErrors.value = current
    }

    // Setters for Resubmission
    fun setResubmitFullName(v: String) { _resubmitFullName.value = v }
    fun setResubmitAssociation(v: String) { _resubmitAssociation.value = v }
    fun setResubmitLocation(v: String) { _resubmitLocation.value = v }
    fun setResubmitPhone(v: String) { _resubmitPhone.value = v }

    fun populateResubmitFromCached() {
        viewModelScope.launch {
            val cached = sessionManager.getCachedProfile()
            _resubmitFullName.value = cached.name
            _resubmitAssociation.value = cached.association
            _resubmitLocation.value = cached.location
            _resubmitPhone.value = cached.phone
        }
    }

    /**
     * Client-side validation for Sign-Up
     */
    fun validateSignUpForm(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (_signUpFullName.value.trim().isBlank()) {
            errors["fullName"] = "Full name is required"
        }

        val em = _signUpEmail.value.trim()
        if (em.isBlank()) {
            errors["email"] = "Email address is required"
        } else if (!em.contains("@") || !em.contains(".")) {
            errors["email"] = "Please enter a valid email address (e.g. agent@example.com)"
        }

        if (_signUpAssociation.value.trim().isBlank()) {
            errors["association"] = "Farmers association or cooperative is required"
        }

        if (_signUpLocation.value.trim().isBlank()) {
            errors["location"] = "Operating LGA/State location is required"
        }

        val ph = _signUpPhone.value.trim()
        if (ph.isBlank()) {
            errors["phone"] = "Phone number is required"
        } else if (!isValidNigerianPhone(ph)) {
            errors["phone"] = "Must be a valid Nigerian number (+234... or 080...)"
        }

        val pwd = _signUpPassword.value
        if (pwd.length < 6) {
            errors["password"] = "Password must be at least 6 characters"
        }

        if (pwd != _signUpConfirmPassword.value) {
            errors["confirmPassword"] = "Passwords do not match"
        }

        signUpErrors.value = errors
        return errors.isEmpty()
    }


    /** Reads {"message": ...}, {"detail": {"message": ...}} or {"detail": "..."} from an error body. */
    private fun serverMessage(response: retrofit2.Response<*>?): String? {
        val raw: String? = try { response?.errorBody()?.string() } catch (e: Exception) { null }
        if (raw.isNullOrBlank()) return null
        return try {
            val json = org.json.JSONObject(raw)
            val detail = json.opt("detail")
            when {
                json.optString("message").isNotBlank() -> json.optString("message")
                detail is org.json.JSONObject && detail.optString("message").isNotBlank() -> detail.optString("message")
                detail is String && detail.isNotBlank() -> detail
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Submit Self-Registration
     * Rule: Never sends a status value. The backend assigns 'pending'.
     */
    fun submitSignUp(onSuccessPending: () -> Unit) {
        if (!validateSignUpForm()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val req = AgentSignUpRequest(
                fullName = _signUpFullName.value.trim(),
                email = _signUpEmail.value.trim(),
                association = _signUpAssociation.value.trim(),
                location = _signUpLocation.value.trim(),
                phoneNumber = _signUpPhone.value.trim(),
                password = _signUpPassword.value
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    try {
                        NetworkClient.apiService.registerAgent(req)
                    } catch (e: Exception) {
                        null
                    }
                }

                val body = response?.body()
                val token = body?.accessToken
                val newAgentId = body?.agentId

                if (response != null && response.isSuccessful && body != null &&
                    !token.isNullOrBlank() && !newAgentId.isNullOrBlank()
                ) {
                    sessionManager.saveSession(
                        token = token,
                        refreshToken = null,
                        agentId = newAgentId,
                        name = body.fullName.ifBlank { req.fullName },
                        phone = body.phoneNumber.ifBlank { req.phoneNumber },
                        email = body.email.ifBlank { req.email },
                        association = body.association.ifBlank { req.association },
                        location = body.location.ifBlank { req.location },
                        status = AgentApprovalStatus.fromRaw(body.status),
                        rejectionReason = null,
                        keepSignedIn = true
                    )
                    NetworkClient.setAuthToken(token)
                    _isLoading.value = false
                    onSuccessPending()
                } else if (response == null) {
                    _isLoading.value = false
                    _errorMessage.value =
                        "Could not reach the server. Check your connection and try again. Your application has NOT been submitted."
                } else {
                    _isLoading.value = false
                    _errorMessage.value = serverMessage(response) ?: when (response.code()) {
                        409 -> "An account with this email or phone number already exists. Please sign in."
                        429 -> "Too many attempts. Please wait 15 minutes and try again."
                        else -> "Registration failed (error ${response.code()}). Please try again."
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "We couldn't connect to the server. Please check your connection."
            }
        }
    }

    /**
     * Resubmit profile after rejection
     */
    fun resubmitDetails(onSuccess: () -> Unit) {
        val name = _resubmitFullName.value.trim()
        val assoc = _resubmitAssociation.value.trim()
        val loc = _resubmitLocation.value.trim()
        val ph = _resubmitPhone.value.trim()

        if (name.isBlank() || assoc.isBlank() || loc.isBlank() || ph.isBlank()) {
            _errorMessage.value = "Please complete all required fields before resubmitting"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val req = AgentResubmitRequest(
                fullName = name,
                association = assoc,
                location = loc,
                phoneNumber = ph
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    try {
                        NetworkClient.apiService.resubmitAgentDetails(req)
                    } catch (e: Exception) {
                        null
                    }
                }

                val result = response?.body()
                if (response != null && response.isSuccessful && result != null) {
                    // Only now (server confirmed) update the local session
                    sessionManager.updateAgentStatus(
                        status = AgentApprovalStatus.fromRaw(result.status),
                        rejectionReason = result.rejectionReason,
                        updatedName = name,
                        updatedAssociation = assoc,
                        updatedLocation = loc,
                        updatedPhone = ph
                    )
                    _isLoading.value = false
                    onSuccess()
                } else if (response == null) {
                    _isLoading.value = false
                    _errorMessage.value = "Could not reach the server. Your details have NOT been resubmitted."
                } else {
                    _isLoading.value = false
                    _errorMessage.value = serverMessage(response) ?: "Resubmission failed (error ${response.code()}). Please try again."
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Network error. Please try again."
            }
        }
    }

    /**
     * Sign in and route by live Agent Approval Status:
     * - APPROVED -> home screen
     * - PENDING -> Pending screen
     * - REJECTED -> Rejected screen
     * - SUSPENDED -> Suspended screen
     */
    fun login(onRouteStatus: (AgentApprovalStatus, AgentProfile) -> Unit) {
        if (_isPhoneMode.value) {
            _errorMessage.value = "Phone and PIN sign-in is not available yet. Please use your email and password."
            return
        } else {
            val em = _email.value.trim()
            val pw = _password.value.trim()

            if (em.isBlank() || !em.contains("@")) {
                _errorMessage.value = "Please enter a valid email address"
                return
            }
            if (pw.length < 4) {
                _errorMessage.value = "Password must be at least 4 characters"
                return
            }
            executeLogin(identifier = em, secret = pw, isPhone = false, onRouteStatus = onRouteStatus)
        }
    }

    private fun executeLogin(
        identifier: String,
        secret: String,
        isPhone: Boolean,
        onRouteStatus: (AgentApprovalStatus, AgentProfile) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = withContext(Dispatchers.IO) {
                    try {
                        NetworkClient.apiService.login(AuthLoginRequest(username = identifier, password = secret))
                    } catch (e: Exception) {
                        null
                    }
                }

                if (response == null) {
                    _isLoading.value = false
                    _errorMessage.value = "Could not reach the server. Check your connection and try again."
                    return@launch
                }
                val tokenBody = response.body()
                if (!response.isSuccessful || tokenBody == null) {
                    _isLoading.value = false
                    _errorMessage.value = serverMessage(response) ?: "That email or password doesn't match our records"
                    return@launch
                }

                val token = tokenBody.accessToken
                NetworkClient.setAuthToken(token)

                // The profile and approval status always come from the server
                val statusResult = fetchAgentStatusFromServer()
                val serverAgentId = statusResult?.agentId
                if (statusResult == null || serverAgentId.isNullOrBlank()) {
                    NetworkClient.setAuthToken(null)
                    _isLoading.value = false
                    _errorMessage.value = "Signed in, but your profile could not be loaded. Please try again."
                    return@launch
                }

                val finalProfile = AgentProfile(
                    agentId = serverAgentId,
                    name = statusResult.fullName,
                    phone = statusResult.phoneNumber,
                    email = statusResult.email.ifBlank { identifier },
                    association = statusResult.association,
                    location = statusResult.location,
                    status = AgentApprovalStatus.fromRaw(statusResult.status),
                    rejectionReason = statusResult.rejectionReason,
                    role = "FIELD_AGENT"
                )

                sessionManager.saveSession(
                    token = token,
                    refreshToken = tokenBody.refreshToken,
                    agentId = finalProfile.agentId,
                    name = finalProfile.name,
                    phone = finalProfile.phone,
                    email = finalProfile.email,
                    association = finalProfile.association,
                    location = finalProfile.location,
                    status = finalProfile.status,
                    rejectionReason = finalProfile.rejectionReason,
                    keepSignedIn = _keepMeSignedIn.value
                )

                _isLoading.value = false
                onRouteStatus(finalProfile.status, finalProfile)

            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "We couldn't reach the server. Check your connection."
            }
        }
    }

    /**
     * Polls or refreshes status on demand (e.g. from the Pending screen's Refresh button,
     * or on app launch/foreground).
     */
    fun refreshAgentStatus(onResult: (AgentApprovalStatus) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val statusResult = fetchAgentStatusFromServer()
            _isLoading.value = false

            if (statusResult != null) {
                val newStatus = AgentApprovalStatus.fromRaw(statusResult.status)
                sessionManager.updateAgentStatus(
                    status = newStatus,
                    rejectionReason = statusResult.rejectionReason,
                    updatedName = statusResult.fullName.ifBlank { null },
                    updatedAssociation = statusResult.association.ifBlank { null },
                    updatedLocation = statusResult.location.ifBlank { null },
                    updatedPhone = statusResult.phoneNumber.ifBlank { null }
                )
                onResult(newStatus)
            } else {
                // If offline, return current cached status
                val cached = sessionManager.getCachedProfile()
                onResult(cached.status)
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            NetworkClient.setAuthToken(null)
            onComplete()
        }
    }

    private suspend fun fetchAgentStatusFromServer(): com.example.data.remote.model.AgentStatusResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = NetworkClient.apiService.getAgentStatus()
                if (response.isSuccessful) response.body() else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("TraceHarvest_Salt_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidNigerianPhone(phone: String): Boolean {
        val clean = phone.replace(" ", "").replace("-", "")
        return (clean.startsWith("+234") && clean.length in 13..14) ||
               (clean.startsWith("234") && clean.length in 12..13) ||
               (clean.startsWith("0") && clean.length in 10..11)
    }
}
