package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.viewmodel.WellbeingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsagePermissionScreen(
    viewModel: WellbeingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isUsageGranted by viewModel.isUsageGranted.collectAsState()
    val isNotificationsGranted by viewModel.isNotificationsGranted.collectAsState()
    val isBatteryIgnoring by viewModel.isBatteryIgnoring.collectAsState()

    // Query permission statuses whenever returning from system backgrounds
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launchers
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.checkPermission()
    }

    // Visual calculations
    var completedCount = 0
    if (isUsageGranted) completedCount++
    if (isNotificationsGranted) completedCount++
    if (isBatteryIgnoring) completedCount++
    val totalSteps = 3
    val completionFraction = completedCount.toFloat() / totalSteps.toFloat()

    // Slate Background pairing
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate Dark
            Color(0xFF020617)  // Deep Midnight
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Shield Header Logo Animation Area
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color(0x1A10B981), // 10% Neon Mint
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = "Shield Logo",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Activate Lootra Wellbeing",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Welcome to our productivity ecosystem. To track coin generation and enforce screen time balances securely, adjust the settings below.",
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = Color(0xFF94A3B8)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Onboarding Progress Header Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Onboarding Completion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$completedCount / $totalSteps Verified",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { completionFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF10B981),
                    trackColor = Color(0x22FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step 1: Usage Access Settings Card
        PermissionOnboardingCard(
            title = "1. Device Usage Access",
            description = "Allows background calculation of screen times to index correct productivity balance scores.",
            isGranted = isUsageGranted,
            isMandatory = true,
            actionText = "Grant Usage Access",
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    } catch (ex: Exception) {
                        // fallback ignore
                    }
                }
            },
            testTag = "step_usage_access"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Runtime Notification Permissions Card
        PermissionOnboardingCard(
            title = "2. Real-time Notification Alerts",
            description = "Enables push reminders for screen limits, goal achievement alerts, and background synced scores.",
            isGranted = isNotificationsGranted,
            isMandatory = true,
            actionText = "Enable Notifications",
            onAction = {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // fallback ignore
                    }
                }
            },
            testTag = "step_notifications"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Battery Optimizations Card
        PermissionOnboardingCard(
            title = "3. Background Battery Exclusion",
            description = "Prevents Android from suspending Lootra synchronizations and WorkManager jobs during idle modes.",
            isGranted = isBatteryIgnoring,
            isMandatory = false,
            actionText = "Exclude Battery Limits",
            onAction = {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
            },
            testTag = "step_battery_opt"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom status hint information
        Text(
            text = "Lootra operates offline-first. All gathered benchmarks represent encrypted scores stored on this local device.",
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PermissionOnboardingCard(
    title: String,
    description: String,
    isGranted: Boolean,
    isMandatory: Boolean,
    actionText: String,
    onAction: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0x1F10B981) else Color(0x06FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isGranted) Color(0x6610B981) else Color(0x1AFFFFFF)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                if (isMandatory) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Required", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0x1AFF5555),
                            labelColor = Color(0xFFFF5555)
                        ),
                        border = null
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Optional", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0x1A64748B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    color = if (isGranted) Color(0xA3FFFFFF) else Color(0xFF94A3B8)
                )
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
