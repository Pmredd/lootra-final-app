package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.WellbeingApplication
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.FirestoreRepositoryImpl
import com.example.data.repository.StorageRepository
import com.example.data.repository.StorageRepositoryImpl
import com.example.data.repository.WalletRepository
import com.example.data.repository.RewardRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

// Represents strength of the password
enum class PasswordStrength(val label: String, val progress: Float, val colorHex: Long) {
    EMPTY("", 0f, 0xFF94A3B8),
    WEAK("Weak", 0.33f, 0xFFEF4444),
    MODERATE("Moderate", 0.66f, 0xFFF59E0B),
    STRONG("Strong", 1f, 0xFF10B981)
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
    private val storageRepository: StorageRepository = StorageRepositoryImpl(),
    private val walletRepository: WalletRepository? = null,
    private val rewardRepository: RewardRepository? = null,
    private val wellbeingRepository: com.example.data.repository.WellbeingRepository? = null
) : ViewModel() {

    // --- Splash State ---
    private val _isSplashFinished = MutableStateFlow(false)
    val isSplashFinished: StateFlow<Boolean> = _isSplashFinished.asStateFlow()

    // --- Sign In State ---
    private val _signInEmail = MutableStateFlow("")
    val signInEmail = _signInEmail.asStateFlow()

    private val _signInPassword = MutableStateFlow("")
    val signInPassword = _signInPassword.asStateFlow()

    private val _isSignInPasswordVisible = MutableStateFlow(false)
    val isSignInPasswordVisible = _isSignInPasswordVisible.asStateFlow()

    private val _signInEmailError = MutableStateFlow<String?>(null)
    val signInEmailError = _signInEmailError.asStateFlow()

    private val _signInPasswordError = MutableStateFlow<String?>(null)
    val signInPasswordError = _signInPasswordError.asStateFlow()

    private val _isSignInLoading = MutableStateFlow(false)
    val isSignInLoading = _isSignInLoading.asStateFlow()

    private val _isLoginSuccess = MutableStateFlow(false)
    val isLoginSuccess = _isLoginSuccess.asStateFlow()

    // --- Sign Up State ---
    private val _signUpUsername = MutableStateFlow("")
    val signUpUsername = _signUpUsername.asStateFlow()

    private val _signUpEmail = MutableStateFlow("")
    val signUpEmail = _signUpEmail.asStateFlow()

    private val _signUpPassword = MutableStateFlow("")
    val signUpPassword = _signUpPassword.asStateFlow()

    private val _signUpConfirmPassword = MutableStateFlow("")
    val signUpConfirmPassword = _signUpConfirmPassword.asStateFlow()

    private val _isSignUpPasswordVisible = MutableStateFlow(false)
    val isSignUpPasswordVisible = _isSignUpPasswordVisible.asStateFlow()

    private val _isSignUpConfirmPasswordVisible = MutableStateFlow(false)
    val isSignUpConfirmPasswordVisible = _isSignUpConfirmPasswordVisible.asStateFlow()

    private val _signUpUsernameError = MutableStateFlow<String?>(null)
    val signUpUsernameError = _signUpUsernameError.asStateFlow()

    private val _signUpEmailError = MutableStateFlow<String?>(null)
    val signUpEmailError = _signUpEmailError.asStateFlow()

    private val _signUpPasswordError = MutableStateFlow<String?>(null)
    val signUpPasswordError = _signUpPasswordError.asStateFlow()

    private val _signUpConfirmError = MutableStateFlow<String?>(null)
    val signUpConfirmError = _signUpConfirmError.asStateFlow()

    private val _isSignUpLoading = MutableStateFlow(false)
    val isSignUpLoading = _isSignUpLoading.asStateFlow()

    private val _isAccountCreated = MutableStateFlow(false)
    val isAccountCreated = _isAccountCreated.asStateFlow()

    private val _isLegalAccepting = MutableStateFlow(false)
    val isLegalAccepting = _isLegalAccepting.asStateFlow()

    private val _legalAgreementError = MutableStateFlow<String?>(null)
    val legalAgreementError = _legalAgreementError.asStateFlow()

    private val _isSignUpSuccess = MutableStateFlow(false)
    val isSignUpSuccess = _isSignUpSuccess.asStateFlow()

    // --- Forgot Password State ---
    private val _forgotEmail = MutableStateFlow("")
    val forgotEmail = _forgotEmail.asStateFlow()

    private val _forgotEmailError = MutableStateFlow<String?>(null)
    val forgotEmailError = _forgotEmailError.asStateFlow()

    private val _isForgotLoading = MutableStateFlow(false)
    val isForgotLoading = _isForgotLoading.asStateFlow()

    private val _isResetLinkSent = MutableStateFlow(false)
    val isResetLinkSent = _isResetLinkSent.asStateFlow()

    // --- User Profile Session State ---
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _isDeleteAccountLoading = MutableStateFlow(false)
    val isDeleteAccountLoading = _isDeleteAccountLoading.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError = _deleteAccountError.asStateFlow()

    private val legalRepository = com.example.data.repository.LegalRepository()

    private val _needsPolicyUpdate = MutableStateFlow(false)
    val needsPolicyUpdate = _needsPolicyUpdate.asStateFlow()

    fun checkPolicyUpdate(profile: UserProfile) {
        viewModelScope.launch {
            try {
                val privacyPage = legalRepository.getLegalPageOnce("privacy_policy")
                val termsPage = legalRepository.getLegalPageOnce("terms_conditions")
                val communityPage = legalRepository.getLegalPageOnce("community_guidelines")

                val privacyVerMatch = profile.privacyVersion == privacyPage.version
                val termsVerMatch = profile.termsVersion == termsPage.version
                val communityVerMatch = profile.communityVersion == communityPage.version

                if (!profile.legalAccepted || !privacyVerMatch || !termsVerMatch || !communityVerMatch) {
                    _needsPolicyUpdate.value = true
                } else {
                    _needsPolicyUpdate.value = false
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed policy update check: ${e.message}")
                _needsPolicyUpdate.value = false
            }
        }
    }

    fun acceptUpdatedPolicies() {
        val user = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            try {
                val privacyPage = legalRepository.getLegalPageOnce("privacy_policy")
                val termsPage = legalRepository.getLegalPageOnce("terms_conditions")
                val communityPage = legalRepository.getLegalPageOnce("community_guidelines")

                val res = legalRepository.updateUserLegalAcceptance(
                    uid = user.uid,
                    privacyVersion = privacyPage.version,
                    termsVersion = termsPage.version,
                    communityVersion = communityPage.version
                )
                if (res.isSuccess) {
                    _needsPolicyUpdate.value = false
                    _userProfile.value = _userProfile.value?.copy(
                        legalAccepted = true,
                        acceptedAt = System.currentTimeMillis(),
                        privacyVersion = privacyPage.version,
                        termsVersion = termsPage.version,
                        communityVersion = communityPage.version
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed accepting updated policies: ${e.message}")
            }
        }
    }

    private val _isSessionActiveOnAnotherDevice = MutableStateFlow(false)
    val isSessionActiveOnAnotherDevice = _isSessionActiveOnAnotherDevice.asStateFlow()

    private val _pendingLoginEmail = MutableStateFlow("")
    private val _pendingLoginPassword = MutableStateFlow("")


    fun cancelPendingForceLogout() {
        _isSessionActiveOnAnotherDevice.value = false
        _pendingLoginEmail.value = ""
        _pendingLoginPassword.value = ""
        authRepository.signOut()
    }

    private val _isAdmin = MutableStateFlow<Boolean>(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _adminRole = MutableStateFlow<String?>(null)
    val adminRole = _adminRole.asStateFlow()

    private val _adminCollegeId = MutableStateFlow<String>("")
    val adminCollegeId = _adminCollegeId.asStateFlow()

    private val _signInAsAdmin = MutableStateFlow<Boolean>(false)
    val signInAsAdmin = _signInAsAdmin.asStateFlow()

    fun setSignInAsAdmin(value: Boolean) {
        _signInAsAdmin.value = value
    }

    private val _isProfileSaving = MutableStateFlow(false)
    val isProfileSaving = _isProfileSaving.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError = _profileError.asStateFlow()

    // --- Password Strength ---
    private val _passwordStrength = MutableStateFlow(PasswordStrength.EMPTY)
    val passwordStrength = _passwordStrength.asStateFlow()

    init {
        // Run startup delay block for Splash screen and restore session first
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    Log.d("AuthViewModel", "[SESSION RESTORE] Found active Firebase Auth session for UID: ${user.uid}")
                    // 1. Fetch user profile
                    val result = firestoreRepository.getUserProfile(user.uid)
                    if (result.isSuccess) {
                        val p = result.getOrNull()
                        if (p != null) {
                            _userProfile.value = p
                            checkPolicyUpdate(p)
                            Log.d("AuthViewModel", "[SESSION RESTORE] Successfully loaded profile for UID: ${user.uid}")
                        } else {
                            Log.d("AuthViewModel", "[SESSION RESTORE] Profile not found. Creating default for UID: ${user.uid}")
                            val defaultProf = UserProfile(
                                uid = user.uid,
                                username = user.displayName ?: "Lootra User",
                                email = user.email ?: "",
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            firestoreRepository.createUserProfile(defaultProf)
                            _userProfile.value = defaultProf
                            checkPolicyUpdate(defaultProf)
                        }
                    } else {
                        Log.e("AuthViewModel", "[SESSION RESTORE] Failed to load user profile: ${result.exceptionOrNull()?.message}")
                    }

                    // 2. Sync wallet/rewards summary
                    try {
                        walletRepository?.getOrCreateLocalWallet(user.uid)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "[SESSION RESTORE] Wallet sync error: ${e.message}")
                    }
                    try {
                        rewardRepository?.getOrCreateLocalRewardSummary(user.uid)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "[SESSION RESTORE] Reward sync error: ${e.message}")
                    }

                    // 3. Determine role from Firestore
                    Log.d("AuthViewModel", "[SESSION RESTORE] Checking admins collection for UID: ${user.uid}")
                    val adminDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("admins")
                        .document(user.uid)
                        .get()
                        .await()

                    if (adminDoc.exists()) {
                        val activeState = adminDoc.getBoolean("isActive") ?: false
                        val role = adminDoc.getString("role") ?: "sub_admin"
                        _isAdmin.value = activeState
                        _adminRole.value = role
                        _adminCollegeId.value = adminDoc.getString("collegeId") ?: ""
                        Log.d(
                            "AuthViewModel",
                            "[SESSION RESTORE] Admin document exists. ActiveState=$activeState, Role=$role, CollegeId=${_adminCollegeId.value}"
                        )
                    } else {
                        _isAdmin.value = false
                        _adminRole.value = null
                        _adminCollegeId.value = ""
                        Log.d("AuthViewModel", "[SESSION RESTORE] Admin document does not exist. Treating as Normal User.")
                    }
                } else {
                    Log.d("AuthViewModel", "[SESSION RESTORE] No active Firebase Auth session.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "[SESSION RESTORE] Uncaught session restore exception: ${e.message}", e)
                _isAdmin.value = false
                _adminRole.value = null
                _adminCollegeId.value = ""
            } finally {
                // Ensure elegant visual reveal of Splash screen (at least 1000ms)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingDelay = 1000L - elapsedTime
                if (remainingDelay > 0) {
                    delay(remainingDelay)
                }
                _isSplashFinished.value = true
                Log.d("AuthViewModel", "[SESSION RESTORE] Splash finished. Triggering navigation.")
            }
        }
    }

    // --- State setters ---

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    fun loadCurrentUserProfile() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            viewModelScope.launch {
                val result = firestoreRepository.getUserProfile(user.uid)
                if (result.isSuccess) {
                    val p = result.getOrNull()
                    if (p != null) {
                        _userProfile.value = p
                        Log.d("AuthViewModel", "Successfully loaded existing profile from Firestore for ${user.uid}")
                    } else {
                        Log.d("AuthViewModel", "No profile found in Firestore for ${user.uid}. Creating default.")
                        val defaultProf = UserProfile(
                            uid = user.uid,
                            username = user.displayName ?: "Lootra User",
                            email = user.email ?: "",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val createResult = firestoreRepository.createUserProfile(defaultProf)
                        if (createResult.isSuccess) {
                            Log.d("AuthViewModel", "Successfully created default profile in Firestore")
                        } else {
                            Log.e("AuthViewModel", "Failed to create default profile in Firestore", createResult.exceptionOrNull())
                        }
                        _userProfile.value = defaultProf
                    }
                    // Ensure local and remote database records for wallet and rewards summary are created/synced for the authenticated user
                    try {
                        walletRepository?.getOrCreateLocalWallet(user.uid)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to auto-create/sync wallet on profile load: ${e.message}")
                    }
                    try {
                        rewardRepository?.getOrCreateLocalRewardSummary(user.uid)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to auto-create/sync reward summary on profile load: ${e.message}")
                    }
                    // Validate if user has admin privileges
                    try {
                        val adminDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("admins")
                            .document(user.uid)
                            .get()
                            .await()
                        if (adminDoc.exists()) {
                            val activeState = adminDoc.getBoolean("isActive") ?: false
                            _isAdmin.value = activeState
                            val role = adminDoc.getString("role") ?: "sub_admin"
                            _adminRole.value = role
                            _adminCollegeId.value = adminDoc.getString("collegeId") ?: ""
                        } else {
                            _isAdmin.value = false
                            _adminRole.value = null
                            _adminCollegeId.value = ""
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to load admin verification status: ${e.message}")
                        _isAdmin.value = false
                        _adminRole.value = null
                        _adminCollegeId.value = ""
                    }
                } else {
                    Log.e("AuthViewModel", "Failed to retrieve user profile from Firestore", result.exceptionOrNull())
                }
            }
        }
    }

    fun setSignInEmail(value: String) {
        _signInEmail.value = value
        if (_signInEmailError.value != null && isValidEmail(value)) {
            _signInEmailError.value = null
        }
    }

    fun setSignInPassword(value: String) {
        _signInPassword.value = value
        if (_signInPasswordError.value != null && value.length >= 6) {
            _signInPasswordError.value = null
        }
    }

    fun toggleSignInPasswordVisibility() {
        _isSignInPasswordVisible.value = !_isSignInPasswordVisible.value
    }

    fun setSignUpUsername(value: String) {
        _signUpUsername.value = value
        if (_signUpUsernameError.value != null && value.trim().length >= 3) {
            _signUpUsernameError.value = null
        }
    }

    fun setSignUpEmail(value: String) {
        _signUpEmail.value = value
        if (_signUpEmailError.value != null && isValidEmail(value)) {
            _signUpEmailError.value = null
        }
    }

    fun setSignUpPassword(value: String) {
        _signUpPassword.value = value
        calculatePasswordStrength(value)
        if (_signUpPasswordError.value != null && value.length >= 6) {
            _signUpPasswordError.value = null
        }
        if (_signUpConfirmPassword.value.isNotEmpty() && _signUpConfirmPassword.value == value) {
            _signUpConfirmError.value = null
        }
    }

    fun setSignUpConfirmPassword(value: String) {
        _signUpConfirmPassword.value = value
        if (_signUpConfirmError.value != null && value == _signUpPassword.value) {
            _signUpConfirmError.value = null
        }
    }

    fun toggleSignUpPasswordVisibility() {
        _isSignUpPasswordVisible.value = !_isSignUpPasswordVisible.value
    }

    fun toggleSignUpConfirmPasswordVisibility() {
        _isSignUpConfirmPasswordVisible.value = !_isSignUpConfirmPasswordVisible.value
    }

    fun setForgotEmail(value: String) {
        _forgotEmail.value = value
        if (_forgotEmailError.value != null && isValidEmail(value)) {
            _forgotEmailError.value = null
        }
    }

    // --- Authenticated Actions ---

    fun performSignIn(deviceId: String, deviceModel: String) {
        var hasError = false
        val email = _signInEmail.value
        val password = _signInPassword.value

        if (!isValidEmail(email)) {
            _signInEmailError.value = "Please enter a valid email address"
            hasError = true
        } else {
            _signInEmailError.value = null
        }

        if (password.length < 6) {
            _signInPasswordError.value = "Password must be at least 6 characters"
            hasError = true
        } else {
            _signInPasswordError.value = null
        }

        if (hasError) return

         viewModelScope.launch {
            _isSignInLoading.value = true
            val result = authRepository.signIn(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    // Check if the user has an administrative role
                    var isUserAdmin = false
                    try {
                        val adminDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("admins")
                            .document(user.uid)
                            .get()
                            .await()
                        if (adminDoc.exists()) {
                            isUserAdmin = adminDoc.getBoolean("isActive") ?: false
                        } else {
                            val adminQueryByEmail = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("admins")
                                .whereEqualTo("email", user.email ?: "")
                                .get()
                                .await()
                            if (!adminQueryByEmail.isEmpty) {
                                isUserAdmin = adminQueryByEmail.documents.firstOrNull()?.getBoolean("isActive") ?: false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Administrative check during sign-in failed: ${e.message}")
                    }

                    // Enforce device binding for non-admins
                    if (!isUserAdmin && deviceId.isNotEmpty() && deviceId != "unknown") {
                        try {
                            val bindingDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("device_bindings")
                                .document(deviceId)
                                .get()
                                .await()
                            if (bindingDoc.exists()) {
                                val boundUserId = bindingDoc.getString("userId") ?: ""
                                if (boundUserId.isNotEmpty() && boundUserId != user.uid) {
                                    // Log blocked attempt
                                    val attemptData = mapOf(
                                        "deviceId" to deviceId,
                                        "timestamp" to System.currentTimeMillis(),
                                        "reason" to "login_blocked",
                                        "attemptedUserId" to user.uid,
                                        "attemptedEmail" to (user.email ?: email),
                                        "explanation" to "This device is already bound to another account (userId: $boundUserId)"
                                    )
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("blocked_attempts")
                                        .add(attemptData)

                                    authRepository.signOut()
                                    _signInPasswordError.value = "This device is already linked to another account."
                                    _isSignInLoading.value = false
                                    return@launch
                                }
                            } else {
                                // Bind this device to this user permanently on their first login
                                val bindingData = mapOf(
                                    "deviceId" to deviceId,
                                    "userId" to user.uid,
                                    "email" to (user.email ?: email),
                                    "createdAt" to System.currentTimeMillis(),
                                    "updatedAt" to System.currentTimeMillis()
                                )
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("device_bindings")
                                    .document(deviceId)
                                    .set(bindingData)
                                    .await()
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Device binding enforcement failed: ${e.message}")
                        }
                    }

                    // Check if already active elsewhere
                    _isSessionActiveOnAnotherDevice.value = false
                    val checkResult = firestoreRepository.getUserProfile(user.uid)
                    if (checkResult.isSuccess) {
                        val profile = checkResult.getOrNull()
                        if (profile != null && profile.sessionActive && profile.activeDeviceId.isNotEmpty() && profile.activeDeviceId != deviceId) {
                            _pendingLoginEmail.value = email
                            _pendingLoginPassword.value = password
                            _isSessionActiveOnAnotherDevice.value = true
                            authRepository.signOut()
                            _isSignInLoading.value = false
                            return@launch
                        }
                    }

                    // Proceed normally
                    try {
                        val userProfileResult = firestoreRepository.getUserProfile(user.uid)
                        val updatedProfile = if (userProfileResult.isSuccess && userProfileResult.getOrNull() != null) {
                            userProfileResult.getOrNull()!!.copy(
                                activeDeviceId = deviceId,
                                deviceModel = deviceModel,
                                lastLoginTime = System.currentTimeMillis(),
                                sessionActive = true,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            UserProfile(
                                uid = user.uid,
                                username = user.displayName ?: "Lootra User",
                                email = user.email ?: "",
                                activeDeviceId = deviceId,
                                deviceModel = deviceModel,
                                lastLoginTime = System.currentTimeMillis(),
                                sessionActive = true,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        firestoreRepository.createUserProfile(updatedProfile)
                        _userProfile.value = updatedProfile


                        val adminDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("admins")
                            .document(user.uid)
                            .get()
                            .await()
                        if (adminDoc.exists()) {
                            val isActive = adminDoc.getBoolean("isActive") ?: false
                            _isAdmin.value = isActive
                            val role = adminDoc.getString("role") ?: "sub_admin"
                            _adminRole.value = role
                            _adminCollegeId.value = adminDoc.getString("collegeId") ?: ""
                            
                            if (isActive) {
                                _isLoginSuccess.value = true
                            } else {
                                performSignOut()
                                _signInPasswordError.value = "Access Denied: This account has been disabled."
                                _isLoginSuccess.value = false
                            }
                        } else {
                            _isAdmin.value = false
                            _adminRole.value = null
                            _adminCollegeId.value = ""
                            _isLoginSuccess.value = true
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Sign-in verification or profile session update failed: ${e.message}")
                        _isAdmin.value = false
                        _adminRole.value = null
                        _adminCollegeId.value = ""
                        _isLoginSuccess.value = true
                    }
                } else {
                    _isLoginSuccess.value = false
                }
            } else {
                _signInPasswordError.value = result.exceptionOrNull()?.message ?: "Sign In failed"
            }
            _isSignInLoading.value = false
        }
    }

    fun performForceLogoutAndLogin(deviceId: String, deviceModel: String) {
        viewModelScope.launch {
            _isSignInLoading.value = true
            val email = _pendingLoginEmail.value
            val password = _pendingLoginPassword.value

            val result = authRepository.signIn(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    // Check if the user has an administrative role
                    var isUserAdmin = false
                    try {
                        val adminDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("admins")
                            .document(user.uid)
                            .get()
                            .await()
                        if (adminDoc.exists()) {
                            isUserAdmin = adminDoc.getBoolean("isActive") ?: false
                        } else {
                            val adminQueryByEmail = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("admins")
                                .whereEqualTo("email", user.email ?: "")
                                .get()
                                .await()
                            if (!adminQueryByEmail.isEmpty) {
                                isUserAdmin = adminQueryByEmail.documents.firstOrNull()?.getBoolean("isActive") ?: false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Administrative check during force-login failed: ${e.message}")
                    }

                    // Enforce device binding for non-admins
                    if (!isUserAdmin && deviceId.isNotEmpty() && deviceId != "unknown") {
                        try {
                            val bindingDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("device_bindings")
                                .document(deviceId)
                                .get()
                                .await()
                            if (bindingDoc.exists()) {
                                val boundUserId = bindingDoc.getString("userId") ?: ""
                                if (boundUserId.isNotEmpty() && boundUserId != user.uid) {
                                    // Log blocked attempt
                                    val attemptData = mapOf(
                                        "deviceId" to deviceId,
                                        "timestamp" to System.currentTimeMillis(),
                                        "reason" to "force_login_blocked",
                                        "attemptedUserId" to user.uid,
                                        "attemptedEmail" to (user.email ?: email),
                                        "explanation" to "This device is already bound to another account (userId: $boundUserId)"
                                    )
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("blocked_attempts")
                                        .add(attemptData)

                                    authRepository.signOut()
                                    _signInPasswordError.value = "This device is already linked to another account."
                                    _isSignInLoading.value = false
                                    return@launch
                                }
                            } else {
                                // Bind this device to this user permanently on their first login
                                val bindingData = mapOf(
                                    "deviceId" to deviceId,
                                    "userId" to user.uid,
                                    "email" to (user.email ?: email),
                                    "createdAt" to System.currentTimeMillis(),
                                    "updatedAt" to System.currentTimeMillis()
                                )
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("device_bindings")
                                    .document(deviceId)
                                    .set(bindingData)
                                    .await()
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Device binding enforcement on force-login failed: ${e.message}")
                        }
                    }

                    try {
                        val currentProfileResult = firestoreRepository.getUserProfile(user.uid)
                        val updatedProfile = if (currentProfileResult.isSuccess && currentProfileResult.getOrNull() != null) {
                            currentProfileResult.getOrNull()!!.copy(
                                activeDeviceId = deviceId,
                                deviceModel = deviceModel,
                                lastLoginTime = System.currentTimeMillis(),
                                sessionActive = true,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            UserProfile(
                                uid = user.uid,
                                username = user.displayName ?: "Lootra User",
                                email = user.email ?: "",
                                activeDeviceId = deviceId,
                                deviceModel = deviceModel,
                                lastLoginTime = System.currentTimeMillis(),
                                sessionActive = true,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        
                        firestoreRepository.createUserProfile(updatedProfile)
                        _userProfile.value = updatedProfile
                        _isSessionActiveOnAnotherDevice.value = false
                        _isLoginSuccess.value = true
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Force logout and login profile update failed: ${e.message}")
                    }
                }
            } else {
                _signInPasswordError.value = result.exceptionOrNull()?.message ?: "Force login failed"
            }
            _isSignInLoading.value = false
        }
    }

    fun validateSignUpInputs(): Boolean {
        var hasError = false
        val username = _signUpUsername.value
        val email = _signUpEmail.value
        val password = _signUpPassword.value
        val confirm = _signUpConfirmPassword.value

        if (username.trim().length < 3) {
            _signUpUsernameError.value = "Username must be at least 3 characters"
            hasError = true
        } else {
            _signUpUsernameError.value = null
        }

        if (!isValidEmail(email)) {
            _signUpEmailError.value = "Please enter a valid email address"
            hasError = true
        } else {
            _signUpEmailError.value = null
        }

        if (password.length < 6) {
            _signUpPasswordError.value = "Password must be at least 6 characters"
            hasError = true
        } else {
            _signUpPasswordError.value = null
        }

        if (password != confirm) {
            _signUpConfirmError.value = "Passwords do not match"
            hasError = true
        } else {
            _signUpConfirmError.value = null
        }

        return !hasError
    }

    fun performSignUp(
        deviceId: String,
        deviceModel: String,
        privacyVersion: String = "1.0.0",
        termsVersion: String = "1.0.0",
        communityVersion: String = "1.0.0"
    ) {
        if (!validateSignUpInputs()) return

        val username = _signUpUsername.value
        val email = _signUpEmail.value
        val password = _signUpPassword.value

        viewModelScope.launch {
            _isSignUpLoading.value = true
            _signUpConfirmError.value = null
            _signUpEmailError.value = null

            // Check if registering email is pre-registered/defined as an admin
            var isAdminRegistering = false
            try {
                val adminQuery = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("admins")
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                if (!adminQuery.isEmpty) {
                    isAdminRegistering = true
                    Log.d("AuthViewModel", "Registering email matches an administrative role. Bypassing device constraint.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Admin verification search failed: ${e.message}")
            }

            if (!isAdminRegistering && deviceId.isNotEmpty() && deviceId != "unknown") {
                // If the registrant is not an admin, enforce the permanent device binding rule
                try {
                    val bindingDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("device_bindings")
                        .document(deviceId)
                        .get()
                        .await()
                    if (bindingDoc.exists()) {
                        val boundUserId = bindingDoc.getString("userId") ?: ""
                        if (boundUserId.isNotEmpty()) {
                            _signUpConfirmError.value = "1 device= 1 wallet only. This device is already linked to another account."
                            _isSignUpLoading.value = false
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Device binding check on signup failed: ${e.message}")
                }
            }

            val result = authRepository.signUp(email, password)
            if (result.isSuccess) {
                val firebaseUser = result.getOrNull()
                if (firebaseUser != null) {
                    val defaultProf = UserProfile(
                        uid = firebaseUser.uid,
                        username = username,
                        email = email,
                        activeDeviceId = deviceId,
                        deviceModel = deviceModel,
                        lastLoginTime = System.currentTimeMillis(),
                        sessionActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        legalAccepted = false,
                        privacyVersion = privacyVersion,
                        termsVersion = termsVersion,
                        communityVersion = communityVersion
                    )
                    val createResult = firestoreRepository.createUserProfile(defaultProf)
                    if (createResult.isSuccess) {
                        Log.d("AuthViewModel", "Successfully created Firestore user profile on signUp")
                    } else {
                        Log.e("AuthViewModel", "Failed to create Firestore user profile on signUp", createResult.exceptionOrNull())
                    }

                    // Create permanent device binding mapping
                    if (deviceId.isNotEmpty() && deviceId != "unknown") {
                        try {
                            val bindingData = mapOf(
                                "deviceId" to deviceId,
                                "userId" to firebaseUser.uid,
                                "email" to email,
                                "createdAt" to System.currentTimeMillis(),
                                "updatedAt" to System.currentTimeMillis()
                            )
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("device_bindings")
                                .document(deviceId)
                                .set(bindingData)
                                .await()
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Storing device binding mapping failed: ${e.message}")
                        }
                    }

                    _userProfile.value = defaultProf
                    try {
                        walletRepository?.getOrCreateLocalWallet(firebaseUser.uid)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to auto-create wallet on signup: ${e.message}")
                    }
                    try {
                        rewardRepository?.getOrCreateLocalRewardSummary(firebaseUser.uid)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to auto-create reward summary on signup: ${e.message}")
                    }
                    _isAccountCreated.value = true
                }
            } else {
                val errMessage = result.exceptionOrNull()?.message ?: "Sign Up failed"
                if (errMessage.contains("already registered", ignoreCase = true) || errMessage.contains("collision", ignoreCase = true)) {
                    _signUpEmailError.value = "This email is already registered. Please sign in."
                } else {
                    _signUpConfirmError.value = errMessage
                }
            }
            _isSignUpLoading.value = false
        }
    }

    fun saveLegalAcceptance(
        privacyVersion: String = "1.0.0",
        termsVersion: String = "1.0.0",
        communityVersion: String = "1.0.0"
    ) {
        val user = authRepository.getCurrentUser()
        val uid = user?.uid ?: _userProfile.value?.uid
        if (uid == null) {
            _legalAgreementError.value = "Unable to complete setup. Please try again."
            return
        }

        viewModelScope.launch {
            _isLegalAccepting.value = true
            _legalAgreementError.value = null

            try {
                val updatedProf = _userProfile.value?.copy(
                    legalAccepted = true,
                    acceptedAt = System.currentTimeMillis(),
                    privacyVersion = privacyVersion,
                    termsVersion = termsVersion,
                    communityVersion = communityVersion,
                    updatedAt = System.currentTimeMillis()
                ) ?: UserProfile(
                    uid = uid,
                    username = "Lootra User",
                    email = user?.email ?: "",
                    legalAccepted = true,
                    acceptedAt = System.currentTimeMillis(),
                    privacyVersion = privacyVersion,
                    termsVersion = termsVersion,
                    communityVersion = communityVersion,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val res = legalRepository.updateUserLegalAcceptance(
                    uid = uid,
                    privacyVersion = privacyVersion,
                    termsVersion = termsVersion,
                    communityVersion = communityVersion
                )

                if (res.isSuccess) {
                    firestoreRepository.createUserProfile(updatedProf)
                    _userProfile.value = updatedProf
                    _needsPolicyUpdate.value = false
                    _isSignUpSuccess.value = true
                    _isLegalAccepting.value = false
                } else {
                    val createRes = firestoreRepository.createUserProfile(updatedProf)
                    if (createRes.isSuccess) {
                        _userProfile.value = updatedProf
                        _needsPolicyUpdate.value = false
                        _isSignUpSuccess.value = true
                        _isLegalAccepting.value = false
                    } else {
                        Log.e("AuthViewModel", "Failed to save legal agreement in Firestore")
                        _legalAgreementError.value = "Unable to complete setup. Please try again."
                        _isLegalAccepting.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception during saveLegalAcceptance: ${e.message}", e)
                _legalAgreementError.value = "Unable to complete setup. Please try again."
                _isLegalAccepting.value = false
            }
        }
    }

    fun clearLegalError() {
        _legalAgreementError.value = null
    }

    fun performForgotPasswordReset() {
        val email = _forgotEmail.value
        if (!isValidEmail(email)) {
            _forgotEmailError.value = "Please enter a valid email address"
            return
        }
        _forgotEmailError.value = null

        viewModelScope.launch {
            _isForgotLoading.value = true
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                _isResetLinkSent.value = true
            } else {
                _forgotEmailError.value = result.exceptionOrNull()?.message ?: "Reset request failed"
            }
            _isForgotLoading.value = false
        }
    }

    fun updateUserProfile(username: String, localImageUri: Uri?, onFinished: (Boolean) -> Unit) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        _profileError.value = null

        viewModelScope.launch {
            _isProfileSaving.value = true
            var finalImageUrl = _userProfile.value?.profileImage ?: ""

            if (localImageUri != null) {
                val uploadResult = storageRepository.uploadProfileImage(uid, localImageUri)
                if (uploadResult.isSuccess) {
                    finalImageUrl = uploadResult.getOrNull() ?: ""
                } else {
                    // Fallback to Uri string locally if offline/no configuration
                    finalImageUrl = localImageUri.toString()
                }
            }

            val result = firestoreRepository.updateUserProfile(uid, username, finalImageUrl)
            if (result.isSuccess) {
                _userProfile.value = _userProfile.value?.copy(
                    username = username,
                    profileImage = finalImageUrl,
                    updatedAt = System.currentTimeMillis()
                )
                onFinished(true)
            } else {
                _profileError.value = result.exceptionOrNull()?.message ?: "Save profile failed"
                onFinished(false)
            }
            _isProfileSaving.value = false
        }
    }

    fun performSignOut() {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    // Update user sessionActive to false in remote Firestore profile prior to signing out
                    val currentProfile = _userProfile.value
                    if (currentProfile != null) {
                        val updatedProfile = currentProfile.copy(
                            sessionActive = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        firestoreRepository.createUserProfile(updatedProfile)
                    }

                    // 1. Clear daily wellbeing cache and statistics
                    wellbeingRepository?.clearUsageStatsAndReportsOnLogout(uid)

                    // 2. Clear daily ad rewards session state
                    rewardRepository?.clearRewardSessionOnLogout(uid)
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error clearing local session caches on signout: ${e.message}")
                }
            }
        }
        authRepository.signOut()
        _userProfile.value = null
        _isAdmin.value = false
        forceResetAuthStates()
    }

    fun performAccountDeletion(context: android.content.Context, onFinished: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isDeleteAccountLoading.value = true
            _deleteAccountError.value = null
            try {
                val user = authRepository.getCurrentUser()
                val uid = user?.uid ?: _userProfile.value?.uid

                if (uid.isNullOrEmpty() || user == null) {
                    val msg = "No authenticated user session found."
                    _deleteAccountError.value = msg
                    _isDeleteAccountLoading.value = false
                    onFinished(false, msg)
                    return@launch
                }

                // 1. Delete all user-owned Firestore documents
                try {
                    firestoreRepository.deleteUserData(uid)
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Firestore deleteUserData error: ${e.message}")
                }

                // 2. Clear Room Database tables
                try {
                    com.example.data.database.WellbeingDatabase.getDatabase(context).clearAllTables()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error clearing Room DB on delete: ${e.message}")
                }

                // 3. Clear DataStore & Preferences
                try {
                    wellbeingRepository?.clearUsageStatsAndReportsOnLogout(uid)
                    rewardRepository?.clearRewardSessionOnLogout(uid)
                    val app = context.applicationContext as? WellbeingApplication
                    app?.container?.preferences?.clearAll()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error clearing DataStore on delete: ${e.message}")
                }

                // 4. Clear Cached Files
                try {
                    context.cacheDir.deleteRecursively()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error clearing cache directory: ${e.message}")
                }

                // 5. Delete Firebase Authentication account
                val authDeleteResult = authRepository.deleteAccount()
                if (authDeleteResult.isFailure) {
                    val ex = authDeleteResult.exceptionOrNull()
                    val msg = if (ex is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        "This operation is sensitive and requires recent authentication. Please log in again before deleting your account."
                    } else {
                        ex?.message ?: "Failed to delete authentication account."
                    }
                    _deleteAccountError.value = msg
                    _isDeleteAccountLoading.value = false
                    onFinished(false, msg)
                    return@launch
                }

                // 6. Reset session & auth states
                _userProfile.value = null
                _isAdmin.value = false
                forceResetAuthStates()

                _isDeleteAccountLoading.value = false
                onFinished(true, null)
            } catch (e: Exception) {
                val msg = e.message ?: "An unexpected error occurred during account deletion."
                _deleteAccountError.value = msg
                _isDeleteAccountLoading.value = false
                onFinished(false, msg)
            }
        }
    }

    fun resetForgotSuccess() {
        _isResetLinkSent.value = false
        _forgotEmail.value = ""
    }

    fun forceResetAuthStates() {
        _isLoginSuccess.value = false
        _isAccountCreated.value = false
        _isLegalAccepting.value = false
        _legalAgreementError.value = null
        _isSignUpSuccess.value = false
        _isResetLinkSent.value = false
        _signInPassword.value = ""
        _signUpPassword.value = ""
        _signUpConfirmPassword.value = ""
        _signInAsAdmin.value = false
        _passwordStrength.value = PasswordStrength.EMPTY
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        return Pattern.compile(emailPattern).matcher(email).matches()
    }

    private fun calculatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            _passwordStrength.value = PasswordStrength.EMPTY
            return
        }
        if (password.length < 6) {
            _passwordStrength.value = PasswordStrength.WEAK
            return
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        if (password.length >= 8 && hasLetter && hasDigit && hasSpecial) {
            _passwordStrength.value = PasswordStrength.STRONG
        } else {
            _passwordStrength.value = PasswordStrength.MODERATE
        }
    }

    // Factory pattern using standard creation extras injection
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WellbeingApplication
                val container = application.container
                return AuthViewModel(
                    authRepository = container.authRepository,
                    firestoreRepository = container.firestoreRepository,
                    storageRepository = container.storageRepository,
                    walletRepository = container.walletRepository,
                    rewardRepository = container.rewardRepository,
                    wellbeingRepository = container.repository
                ) as T
            }
        }
    }
}
