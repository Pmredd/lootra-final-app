package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LootraTextField
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.PasswordStrength

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    onSignUpSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val deviceId = remember(context) {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    val deviceModel = android.os.Build.MODEL ?: "Android Device"

    // Observe text state flows from standard AuthViewModel
    val username by viewModel.signUpUsername.collectAsState()
    val email by viewModel.signUpEmail.collectAsState()
    val password by viewModel.signUpPassword.collectAsState()
    val confirmPassword by viewModel.signUpConfirmPassword.collectAsState()
    
    val isPasswordVisible by viewModel.isSignUpPasswordVisible.collectAsState()
    val isConfirmVisible by viewModel.isSignUpConfirmPasswordVisible.collectAsState()

    // Validation state flows
    val usernameError by viewModel.signUpUsernameError.collectAsState()
    val emailError by viewModel.signUpEmailError.collectAsState()
    val passwordError by viewModel.signUpPasswordError.collectAsState()
    val confirmError by viewModel.signUpConfirmError.collectAsState()

    val passStrength by viewModel.passwordStrength.collectAsState()
    val isLoading by viewModel.isSignUpLoading.collectAsState()
    val isAccountCreated by viewModel.isAccountCreated.collectAsState()
    val isLegalAccepting by viewModel.isLegalAccepting.collectAsState()
    val legalError by viewModel.legalAgreementError.collectAsState()
    val isSignUpSuccess by viewModel.isSignUpSuccess.collectAsState()

    var showLegalAgreement by remember { mutableStateOf(false) }

    // Screen navigation callback upon compilation
    LaunchedEffect(isSignUpSuccess) {
        if (isSignUpSuccess) {
            onSignUpSuccess()
        }
    }

    if (isAccountCreated || showLegalAgreement) {
        LegalAgreementScreen(
            onAcceptAndContinue = { privacyVer, termsVer, communityVer ->
                viewModel.saveLegalAcceptance(
                    privacyVersion = privacyVer,
                    termsVersion = termsVer,
                    communityVersion = communityVer
                )
            },
            onBack = { showLegalAgreement = false },
            isLoading = isLegalAccepting,
            errorMessage = legalError,
            onClearError = { viewModel.clearLegalError() },
            modifier = modifier
        )
        return
    }

    val bgBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF020617),
                    Color(0xFF0F172A),
                    Color(0xFF020617)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFE2E8F0),
                    Color(0xFFF1F5F9)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0x0A06B6D4), // 4% Cyan glow
                    radius = 350.dp.toPx(),
                    center = Offset(size.width, size.height * 0.15f)
                )
                drawCircle(
                    color = Color(0x0A10B981), // 4% Mint glow
                    radius = 300.dp.toPx(),
                    center = Offset(0f, size.height * 0.85f)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x12FFFFFF) else Color(0xCCFFFFFF)
            ),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0x33FFFFFF), Color(0x08FFFFFF))
                        } else {
                            listOf(Color(0x660F172A), Color(0x0F0F172A))
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("signup_container_card"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Symmetrical decorative layout hook instead of plain text
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = if (isDark) Color(0x1A10B981) else Color(0x1A0F172A),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.testTag("signup_welcome_text")
                )

                Text(
                    text = "Join Lootra to track secure telemetry and boundaries",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Input rows
                LootraTextField(
                    value = username,
                    onValueChange = { viewModel.setSignUpUsername(it) },
                    label = "USERNAME",
                    placeholder = "Pick a unique alias",
                    leadingIcon = Icons.Default.Person,
                    isError = usernameError != null,
                    errorMessage = usernameError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_username_field"
                )

                Spacer(modifier = Modifier.height(16.dp))

                LootraTextField(
                    value = email,
                    onValueChange = { viewModel.setSignUpEmail(it) },
                    label = "EMAIL ADDRESS",
                    placeholder = "Enter your work or personal email",
                    leadingIcon = Icons.Default.Email,
                    isError = emailError != null,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_email_field"
                )

                Spacer(modifier = Modifier.height(16.dp))

                LootraTextField(
                    value = password,
                    onValueChange = { viewModel.setSignUpPassword(it) },
                    label = "CHOOSE PASSWORD",
                    placeholder = "Select a strong phrase",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSignUpPasswordVisibility() }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_password_field"
                )

                // Password strength indicator card
                AnimatedVisibility(
                    visible = password.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDark) Color(0x0AFFFFFF) else Color(0x08000000),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Password Strength",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                            Text(
                                text = passStrength.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(passStrength.colorHex)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress linear gauge
                        LinearProgressIndicator(
                            progress = { passStrength.progress },
                            color = Color(passStrength.colorHex),
                            trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x1F000000),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .testTag("password_strength_bar")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LootraTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.setSignUpConfirmPassword(it) },
                    label = "CONFIRM PASSWORD",
                    placeholder = "Re-enter selection",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSignUpConfirmPasswordVisibility() }) {
                            Icon(
                                imageVector = if (isConfirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirmError != null,
                    errorMessage = confirmError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    testTag = "signup_confirm_password_field"
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action registration button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (viewModel.validateSignUpInputs()) {
                            viewModel.performSignUp(
                                deviceId = deviceId,
                                deviceModel = deviceModel
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                        contentColor = if (isDark) Color(0xFF020617) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = if (isDark) Color(0xFF020617) else Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp).testTag("signup_loading_bar")
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already registered? ",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Sign In instead",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isDark) Color(0xFF10B981) else Color(0xFF0891B2),
                        modifier = Modifier
                            .clickable {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onNavigateToSignIn()
                            }
                            .testTag("btn_goto_signin")
                    )
                }
            }
        }
    }
}

@Preview(name = "Sign Up - Dark Theme")
@Composable
fun SignUpDarkPreview() {
    val mockModel = AuthViewModel()
    MyApplicationTheme(darkTheme = true) {
        SignUpScreen(mockModel, {}, {})
    }
}

@Preview(name = "Sign Up - Light Theme")
@Composable
fun SignUpLightPreview() {
    val mockModel = AuthViewModel()
    MyApplicationTheme(darkTheme = false) {
        SignUpScreen(mockModel, {}, {})
    }
}
