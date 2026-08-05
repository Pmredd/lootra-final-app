package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LootraTextField
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    val email by viewModel.forgotEmail.collectAsState()
    val emailError by viewModel.forgotEmailError.collectAsState()
    val isLoading by viewModel.isForgotLoading.collectAsState()
    val isSent by viewModel.isResetLinkSent.collectAsState()

    // Reset success indicators upon leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetForgotSuccess()
        }
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
                    center = Offset(size.width / 2f, size.height * 0.9f)
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
                .testTag("forgot_container_card"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AnimatedContent(
                targetState = isSent,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ForgotFormState"
            ) { sent ->
                if (!sent) {
                    // 1. INPUT CODE FLOW STATE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header back linkage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isDark) Color(0x10FFFFFF) else Color(0x0A000000),
                                        shape = CircleShape
                                    )
                                    .testTag("forgot_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = if (isDark) Color(0x1A06B6D4) else Color(0x1A0891B2),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF06B6D4) else Color(0xFF0891B2),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.testTag("forgot_title_text")
                        )

                        Text(
                            text = "Enter your verified email profile, and we'll dispatch an encryption reset ticket.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                        )

                        LootraTextField(
                            value = email,
                            onValueChange = { viewModel.setForgotEmail(it) },
                            label = "REGISTERED EMAIL",
                            placeholder = "Insert your accounts email address",
                            leadingIcon = Icons.Default.Email,
                            isError = emailError != null,
                            errorMessage = emailError,
                            testTag = "forgot_email_field"
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.performForgotPasswordReset()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("forgot_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF06B6D4) else Color(0xFF0F172A),
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
                                    modifier = Modifier.size(20.dp).testTag("forgot_loading_bar")
                                )
                            } else {
                                Text(
                                    text = "Send Reset Link",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // 2. MAJESTIC SUCCESS DISPATCH CODE STATE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = if (isDark) Color(0x3310B981) else Color(0x1B10B981),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF10B981) else Color(0xFF10B981),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = if (isDark) Color(0xFF020617) else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Ecosystem Signal Sent!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.testTag("forgot_success_title")
                        )

                        Text(
                            text = "We have dispatched a secure password override ticket to:\n\n$email\n\nPlease check your email main inbox as well as the promotional or junk filters.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.resetForgotSuccess()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("forgot_dismiss_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                                contentColor = if (isDark) Color(0xFF020617) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Return to Login",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Forgot - Standard view")
@Composable
fun ForgotPasswordPreviewDark() {
    val model = AuthViewModel()
    MyApplicationTheme(darkTheme = true) {
        ForgotPasswordScreen(model, {})
    }
}

@Preview(name = "Forgot - Light view")
@Composable
fun ForgotPasswordPreviewLight() {
    val model = AuthViewModel()
    MyApplicationTheme(darkTheme = false) {
        ForgotPasswordScreen(model, {})
    }
}
