package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val deviceId = remember(context) {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    val deviceModel = android.os.Build.MODEL ?: "Android Device"
    val isSessionActiveOnAnotherDevice by viewModel.isSessionActiveOnAnotherDevice.collectAsState()

    if (isSessionActiveOnAnotherDevice) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingForceLogout() },
            title = { Text("Account Already Active", fontWeight = FontWeight.Bold) },
            text = { Text("This account is currently active on another device: ${deviceModel}.\n\nWould you like to force logout and continue logging in here?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.performForceLogoutAndLogin(deviceId, deviceModel) }
                ) {
                    Text("Force Logout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelPendingForceLogout() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // State bindings
    val email by viewModel.signInEmail.collectAsState()
    val password by viewModel.signInPassword.collectAsState()
    val isPasswordVisible by viewModel.isSignInPasswordVisible.collectAsState()
    val emailError by viewModel.signInEmailError.collectAsState()
    val passwordError by viewModel.signInPasswordError.collectAsState()
    val isLoading by viewModel.isSignInLoading.collectAsState()
    val isLoginSuccess by viewModel.isLoginSuccess.collectAsState()

    // Trigger success callback on login action completion
    LaunchedEffect(isLoginSuccess) {
        if (isLoginSuccess) {
            onLoginSuccess()
        }
    }

    // Glowing premium background color setup
    val bgBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF020617), // Slate Black
                    Color(0xFF0F172A), // Deep Midnight
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
        // Glowing decorative elements in background for premium look
        if (isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0x0A06B6D4), // 4% Electric Cyan glow
                    radius = 300.dp.toPx(),
                    center = Offset(0f, size.height * 0.1f)
                )
                drawCircle(
                    color = Color(0x0A10B981), // 4% Neon Mint glow
                    radius = 350.dp.toPx(),
                    center = Offset(size.width, size.height * 0.9f)
                )
            }
        }

        // Layout constraint to keep it modern and elegant on tablets (Adaptive widths)
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
                .testTag("signin_container_card"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val signInAsAdmin by viewModel.signInAsAdmin.collectAsState()

                // Top Custom Animated Illustration Area
                TopIllustrationArea(isDark = isDark)

                Spacer(modifier = Modifier.height(16.dp))

                // Welcome texts
                Text(
                    text = if (signInAsAdmin) "Secure Terminal Access" else "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.testTag("signin_welcome_text")
                )

                Text(
                    text = if (signInAsAdmin) "Enter administrator credentials to manage Lootra systems" else "Enter your credentials to monitor your digital cycles",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Input Core Fields
                LootraTextField(
                    value = email,
                    onValueChange = { viewModel.setSignInEmail(it) },
                    label = "EMAIL",
                    placeholder = "Enter your work or personal email",
                    leadingIcon = Icons.Default.Email,
                    isError = emailError != null,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signin_email_field"
                )

                Spacer(modifier = Modifier.height(16.dp))

                LootraTextField(
                    value = password,
                    onValueChange = { viewModel.setSignInPassword(it) },
                    label = "PASSWORD",
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSignInPasswordVisibility() }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.performSignIn(deviceId, deviceModel)
                        }
                    ),
                    testTag = "signin_password_field"
                )

                // Forgot Password and Admin Sign in row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp),
                    horizontalArrangement = if (signInAsAdmin) Arrangement.Start else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.setSignInAsAdmin(!signInAsAdmin) }
                            .testTag("admin_toggle_row")
                    ) {
                        Switch(
                            checked = signInAsAdmin,
                            onCheckedChange = { viewModel.setSignInAsAdmin(it) },
                            modifier = Modifier.testTag("admin_signin_switch").scale(0.81f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                                checkedTrackColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.3f),
                                uncheckedThumbColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                uncheckedTrackColor = if (isDark) Color(0x3364748B) else Color(0x3394A3B8)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Mode",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        )
                    }

                    if (!signInAsAdmin) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF06B6D4) else Color(0xFF0891B2)
                            ),
                            modifier = Modifier
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onNavigateToForgotPassword()
                                }
                                .testTag("btn_forgot_password")
                        )
                    }
                }

                // Sign In Button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.performSignIn(deviceId, deviceModel)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signin_submit_button"),
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
                            modifier = Modifier.size(20.dp).testTag("signin_loading_bar")
                        )
                    } else {
                        val buttonText = if (signInAsAdmin) "Secure Admin Sign In" else "Sign In to Lootra"
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                if (!signInAsAdmin) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Account section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New to our ecosystem? ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isDark) Color(0xFF475569) else Color(0xFF64748B)
                        )
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isDark) Color(0xFF10B981) else Color(0xFF0891B2),
                            modifier = Modifier
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onNavigateToSignUp()
                                }
                                .testTag("btn_goto_signup")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopIllustrationArea(isDark: Boolean) {
    // Beautiful dynamic canvas rotation animation to draw users into the interface
    val infiniteTransition = rememberInfiniteTransition(label = "halo_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 3.dp.toPx()

            // Outer Orbit with Cyan accent
            drawCircle(
                color = if (isDark) Color(0x3306B6D4) else Color(0x330891B2),
                radius = 50.dp.toPx(),
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Dynamic rotating dash arc representing Lootra telemetry tracking
            drawArc(
                color = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                startAngle = angle,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - 40.dp.toPx(), center.y - 40.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 80.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )

            // Inner solid Core circle
            drawCircle(
                color = if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF0F172A).copy(alpha = 0.08f),
                radius = 30.dp.toPx(),
                center = center
            )
            
            // Little neon central point
            drawCircle(
                color = if (isDark) Color(0xFF10B981) else Color(0xFF0891B2),
                radius = 6.dp.toPx(),
                center = center
            )
        }
    }
}

@Preview(name = "Sign In - Dark Theme")
@Composable
fun SignInDarkPreview() {
    val mockModel = AuthViewModel()
    MyApplicationTheme(darkTheme = true) {
        SignInScreen(mockModel, {}, {}, {})
    }
}

@Preview(name = "Sign In - Light Theme")
@Composable
fun SignInLightPreview() {
    val mockModel = AuthViewModel()
    MyApplicationTheme(darkTheme = false) {
        SignInScreen(mockModel, {}, {}, {})
    }
}
