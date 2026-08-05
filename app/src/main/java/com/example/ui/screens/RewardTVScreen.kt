package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RewardEntity
import com.example.ui.viewmodel.RewardTVViewModel
import com.example.ui.viewmodel.ReelsViewModel
import com.example.ui.screens.ReelsTVScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardTVScreen(
    viewModel: RewardTVViewModel,
    reelsViewModel: ReelsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemScreenTimeMin by viewModel.todayScreenTimeMinutes.collectAsState()
    val rewards by viewModel.rewards.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val adPlayingRewardId by viewModel.adPlayingRewardId.collectAsState()
    val config by viewModel.config.collectAsState()

    val isAdLoaded by viewModel.isAdLoaded.collectAsState()
    val isLoadingAd by viewModel.isLoadingAd.collectAsState()
    val loadAdError by viewModel.loadAdError.collectAsState()

    var activeAdRewardId by remember { mutableStateOf("") }

    var showClaimSuccess by remember { mutableStateOf(false) }
    var claimedCoinsState by remember { mutableStateOf(0) }

    var activeSubScreen by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        viewModel.preloadRewardedAd(context)
    }

    // Collect claim notifications
    LaunchedEffect(Unit) {
        viewModel.rewardClaimedSuccessAmount.collect { amount ->
            claimedCoinsState = amount
            showClaimSuccess = true
        }
    }

    // Color definitions
    val slateBg = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val accentMint = Color(0xFF10B981)
    val accentCoral = Color(0xFFF43F5E)
    val textMuted = Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(slateBg)
    ) {
        if (activeSubScreen == "home") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lootra Premium TV",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select your entertainment channel to earn coins daily",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textMuted,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Card 1: Wellbeing TV
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { activeSubScreen = "wellbeing" }
                        .testTag("wellbeing_tv_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentMint.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧠", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wellbeing TV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Achieve daily screen targets, track goals, and unlock rewards.",
                                fontSize = 12.sp,
                                color = textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = accentMint
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Card 2: Reels TV
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { activeSubScreen = "reels" }
                        .testTag("reels_tv_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, accentMint.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentCoral.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎥", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Reels TV",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentMint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slateBg
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Watch campus short-form promotion reels and earn instant coins.",
                                fontSize = 12.sp,
                                color = textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = accentMint
                        )
                    }
                }
            }
        } else if (activeSubScreen == "reels") {
            ReelsTVScreen(
                viewModel = reelsViewModel,
                onBackToHome = { activeSubScreen = "home" }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dedicated Header Top Bar
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "WELLBEING TV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { activeSubScreen = "home" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshRewards() },
                            modifier = Modifier.testTag("reward_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = accentMint
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner warning if rewards are disabled by Super Admin
                if (!config.rewardEnabled) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, accentCoral.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = accentCoral.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Warning",
                                    tint = accentCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Rewards are temporarily paused by the Super Admin. Earning and claiming is currently disabled.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Large Netflix-Style TV Card Design in Center
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF111827),
                                        Color(0xFF1F2937)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                            .testTag("reward_tv_netflix_banner")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Large stylized screen representation with AdMob Status Integration
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(1.5.dp, when {
                                        loadAdError != null -> accentCoral.copy(alpha = 0.5f)
                                        isAdLoaded -> accentMint.copy(alpha = 0.5f)
                                        else -> textMuted.copy(alpha = 0.3f)
                                    }, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    when {
                                        loadAdError != null -> {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Error icon",
                                                tint = accentCoral,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "AD ERROR: $loadAdError",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentCoral,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { viewModel.preloadRewardedAd(context) },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentCoral.copy(alpha = 0.2f)),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp).testTag("retry_load_ad_button")
                                            ) {
                                                Text("Retry Loading Ad", fontSize = 10.sp, color = accentCoral, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        isLoadingAd -> {
                                            CircularProgressIndicator(
                                                color = accentMint,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "LOADING PRE-BUFFERED ADS...",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textMuted,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        isAdLoaded -> {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = "Ready icon",
                                                tint = accentMint,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(accentMint)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "REWARDS FEED IS LIVE",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = accentMint,
                                                    letterSpacing = 1.5.sp
                                                )
                                            }
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = "Connected icon",
                                                tint = textMuted,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "ADS OFFLINE - PRESS RETRY",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textMuted,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { viewModel.preloadRewardedAd(context) },
                                                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Initialize Ad", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Interactive Live screen time reading today
                            val totalHours = systemScreenTimeMin / 60
                            val remainingMinutes = systemScreenTimeMin % 60
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Today's Screen Time",
                                        fontSize = 12.sp,
                                        color = textMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${totalHours}h ${remainingMinutes}m",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentMint.copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentMint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress indicators towards maximum level of 9h
                            val maxLimit = 540f // 9h is 540 minutes
                            val progressFactor = (systemScreenTimeMin.toFloat() / maxLimit).coerceIn(0f, 1f)
                            
                            LinearProgressIndicator(
                                progress = { progressFactor },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentMint,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0m", fontSize = 10.sp, color = textMuted)
                                Text("1h", fontSize = 10.sp, color = textMuted)
                                Text("3h", fontSize = 10.sp, color = textMuted)
                                Text("5h", fontSize = 10.sp, color = textMuted)
                                Text("7h", fontSize = 10.sp, color = textMuted)
                                Text("9h (Max)", fontSize = 10.sp, color = textMuted)
                            }
                        }
                    }
                }

                // Header title list: Available Rewards To Claim
                item {
                    Text(
                        text = "AVAILABLE SCREEN-TIME TARGETS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = textMuted,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Render Levels list
                if (rewards.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Preparing rewarding levels...",
                                    fontSize = 14.sp,
                                    color = textMuted
                                )
                            }
                        }
                    }
                } else {
                    items(rewards) { item ->
                        RewardLevelCard(
                            reward = item,
                            currentScreenTimeMin = systemScreenTimeMin,
                            adsRequired = config.adsRequired,
                            onWatchAd = {
                                viewModel.watchRewardedAd(context, item.rewardId)
                            },
                            onClaim = {
                                viewModel.claimReward(item.rewardId, item.coins)
                            },
                            accentMint = accentMint,
                            cardBg = cardBg,
                            textMuted = textMuted
                        )
                    }
                }
            }
        }

        // Dedicated loader overlay representing AdMob buffer state loading UX
        if (isLoadingAd || adPlayingRewardId != null) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(slateBg.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accentMint,
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "ESTABLISHING AD STREAM...",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = accentMint,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Retrieving AdMob Rewarded Video content securely",
                            fontSize = 12.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Coins burst animation dialog on claim success
        if (showClaimSuccess) {
            Dialog(onDismissRequest = { showClaimSuccess = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, accentMint.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .testTag("claim_coins_success_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(accentMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coin Claim",
                                tint = accentMint,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Coins Transferred!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your screen-time reward has been credited safely to your Lootra wallet.",
                            fontSize = 13.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "+$claimedCoinsState COINS",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = accentMint,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showClaimSuccess = false },
                            colors = ButtonDefaults.buttonColors(containerColor = accentMint),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CHECK WALLET BALANCE",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun RewardLevelCard(
    reward: RewardEntity,
    currentScreenTimeMin: Long,
    adsRequired: Int,
    onWatchAd: () -> Unit,
    onClaim: () -> Unit,
    accentMint: Color,
    cardBg: Color,
    textMuted: Color
) {
    val progressPercent = if (reward.thresholdMinutes > 0) {
        (currentScreenTimeMin.toFloat() / reward.thresholdMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (reward.isClaimed) accentMint.copy(alpha = 0.2f)
                else if (reward.isUnlocked) accentMint.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .testTag("reward_level_card_${reward.thresholdMinutes}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reward.isClaimed) cardBg.copy(alpha = 0.4f) else cardBg
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (reward.isClaimed) accentMint.copy(alpha = 0.1f)
                                else if (reward.isUnlocked) accentMint.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (reward.isClaimed) Icons.Default.Check
                            else if (reward.isUnlocked) Icons.Default.LockOpen
                            else Icons.Outlined.Lock,
                            contentDescription = "Lock State",
                            tint = if (reward.isUnlocked) accentMint else textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val hours = reward.thresholdMinutes / 60
                        val mins = reward.thresholdMinutes % 60
                        val thresholdStr = if (hours > 0) "${hours}h ${if (mins > 0) "${mins}m" else ""}" else "${mins}m"
                        
                        Text(
                            text = "Screen Target: $thresholdStr",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (reward.isClaimed) Color.White.copy(alpha = 0.6f) else Color.White
                        )
                        Text(
                            text = "Reward: ${reward.coins} Coins",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (reward.isClaimed) accentMint.copy(alpha = 0.6f) else accentMint
                        )
                    }
                }

                // Right Status / Button
                if (reward.isClaimed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Claimed",
                            tint = accentMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Claimed",
                            fontSize = 12.sp,
                            color = accentMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (reward.isUnlocked) {
                    if (reward.adsWatchedCount >= adsRequired) {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = accentMint),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("claim_btn_${reward.thresholdMinutes}")
                        ) {
                            Text(
                                "CLAIM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    } else {
                        Button(
                            onClick = onWatchAd,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentMint.copy(alpha = 0.15f),
                                contentColor = accentMint
                            ),
                            border = BorderStroke(1.dp, accentMint.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("watch_btn_${reward.thresholdMinutes}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "WATCH & CLAIM (${reward.adsWatchedCount}/$adsRequired)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val neededMin = reward.thresholdMinutes - currentScreenTimeMin
                    Text(
                        text = "Need ${neededMin}m more",
                        fontSize = 12.sp,
                        color = textMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Progress Bar for unlocked status representation
            if (!reward.isClaimed && !reward.isUnlocked) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = textMuted.copy(alpha = 0.6f),
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
            }
        }
    }
}
