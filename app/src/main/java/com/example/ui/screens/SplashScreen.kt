package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.MyApplicationTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    // Theme gradients for high fidelity visuals
    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF020617), // Deepest Onyx
                    Color(0xFF0F172A), // Dark Midnight Blue
                    Color(0xFF020617)  // Deepest Onyx
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

    // Logo reveal animations scale and alpha
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .testTag("lootra_splash_screen")
    ) {
        // Glowing decorative ambient background effects
        if (isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0x0F10B981), // 6% Neon Mint
                    radius = 350.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f - 100.dp.toPx())
                )
            }
        }

        // Central Animated Logo Assembly
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lootra_logo_official),
                contentDescription = "Lootra Logo",
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Header Reveal
            Text(
                text = "LOOTRA",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .alpha(alpha.value)
                    .testTag("splash_brand_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cozy Slogan tag
            Text(
                text = "PERFORMANCE & COGNITION WELLNESS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = if (isDark) Color(0xFF10B981) else Color(0xFF0891B2)
                ),
                modifier = Modifier.alpha(alpha.value)
            )
        }

        // Subtly breathing progress bar indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 60.dp)
                .alpha(alpha.value),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    strokeWidth = 3.dp,
                    color = if (isDark) Color(0xFF06B6D4) else Color(0xFF0891B2),
                    modifier = Modifier.size(28.dp).testTag("splash_loader_indicator")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Compiling analytics engine...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}

@Preview(name = "Splash - Dark mode")
@Composable
fun SplashScreenDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        SplashScreen()
    }
}

@Preview(name = "Splash - Light mode")
@Composable
fun SplashScreenLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        SplashScreen()
    }
}
