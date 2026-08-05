package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.example.data.model.ReelEntity
import com.example.ui.viewmodel.ReelsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsTVScreen(
    viewModel: ReelsViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reels by viewModel.filteredReels.collectAsState()
    val colleges by viewModel.colleges.collectAsState()
    val selectedCollegeId by viewModel.selectedCollegeId.collectAsState()
    val selectedCollegeName by viewModel.selectedCollegeName.collectAsState()
    val userLikedReels by viewModel.userLikedReels.collectAsState()
    val userWatchedReels by viewModel.userWatchedReels.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(reels) {
        android.util.Log.d("ReelsTVScreen", "Reels list updated in UI. Adapter/Item count: ${reels.size}")
    }

    var isLocationConfirmed by remember { mutableStateOf(false) }
    var tempSelectedCollegeId by remember(selectedCollegeId) { mutableStateOf(selectedCollegeId) }
    var tempSelectedCollegeName by remember(selectedCollegeName) { mutableStateOf(selectedCollegeName) }

    var activePlayingReel by remember { mutableStateOf<ReelEntity?>(null) }

    // Visual Constants
    val slateBg = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val accentMint = Color(0xFF10B981)
    val accentCoral = Color(0xFFF43F5E)
    val textMuted = Color(0xFF94A3B8)

    LaunchedEffect(Unit) {
        viewModel.loadLocations()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isLocationConfirmed) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "SELECT LOCATION",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackToHome) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = slateBg
                    )
                )
            },
            containerColor = slateBg,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(accentMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentMint,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Choose Your Campus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select your location to load exclusive promotion reels and earn coins on campus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (colleges.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentMint)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(colleges) { college ->
                                val isSelected = college.collegeId == tempSelectedCollegeId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tempSelectedCollegeId = college.collegeId
                                            tempSelectedCollegeName = college.name
                                        }
                                        .testTag("location_select_item_${college.collegeId}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) cardBg else cardBg.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) accentMint else Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationCity,
                                                contentDescription = null,
                                                tint = if (isSelected) accentMint else textMuted,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = college.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                tempSelectedCollegeId = college.collegeId
                                                tempSelectedCollegeName = college.name
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = accentMint,
                                                unselectedColor = textMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (tempSelectedCollegeId.isNotEmpty() && tempSelectedCollegeName.isNotEmpty()) {
                            viewModel.selectLocation(tempSelectedCollegeId, tempSelectedCollegeName)
                            isLocationConfirmed = true
                        } else {
                            Toast.makeText(context, "Please select a campus to continue", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("location_select_continue_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentMint,
                        disabledContainerColor = accentMint.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = tempSelectedCollegeId.isNotEmpty()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "CONTINUE",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = slateBg
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = slateBg
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "REELS TV",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentMint.copy(alpha = 0.12f))
                                    .clickable { isLocationConfirmed = false }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Active Location",
                                    tint = accentMint,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = selectedCollegeName.ifEmpty { "Select Campus" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentMint
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackToHome) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshReels() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = accentMint
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = slateBg
                    )
                )
            },
            containerColor = slateBg,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (reels.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎥", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Reels Found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active promotion reels are currently assigned for $selectedCollegeName. Tap the top bar to switch location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { isLocationConfirmed = false },
                            colors = ButtonDefaults.buttonColors(containerColor = accentMint),
                            modifier = Modifier.testTag("reels_select_location_btn")
                        ) {
                            Text("Switch Location", color = slateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reels) { reel ->
                            val isLiked = userLikedReels.any { it.reelId == reel.reelId }
                            val isWatched = userWatchedReels.any { it.reelId == reel.reelId && it.rewardGiven }

                            ReelItemCard(
                                reel = reel,
                                isLiked = isLiked,
                                isWatched = isWatched,
                                onWatch = { activePlayingReel = reel },
                                accentMint = accentMint,
                                cardBg = cardBg,
                                textMuted = textMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Reel Player Dialog Overlay
    activePlayingReel?.let { reel ->
        ReelPlayerDialog(
            reel = reel,
            isInitiallyLiked = userLikedReels.any { it.reelId == reel.reelId },
            isInitiallyWatched = userWatchedReels.any { it.reelId == reel.reelId && it.rewardGiven },
            onToggleLike = { viewModel.toggleLike(reel.reelId) },
            onClaimReward = { callback ->
                viewModel.claimReelReward(reel.reelId) { success, msg ->
                    callback(success, msg)
                }
            },
            onVideoCompleted = {
                viewModel.recordReelView(reel.reelId)
            },
            onDismiss = { activePlayingReel = null },
            accentMint = accentMint,
            accentCoral = accentCoral,
            slateBg = slateBg,
            textMuted = textMuted
        )
    }
}
}

@Composable
fun ReelItemCard(
    reel: ReelEntity,
    isLiked: Boolean,
    isWatched: Boolean,
    onWatch: () -> Unit,
    accentMint: Color,
    cardBg: Color,
    textMuted: Color
) {
    val accentCoral = Color(0xFFF43F5E)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (isWatched) accentMint.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            )
            .testTag("reel_card_${reel.reelId}"),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column {
            // Hero Thumbnail Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = reel.thumbnailUrl,
                    contentDescription = reel.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Bottom Gradient Overlay for typography readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Location Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentMint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reel.location,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Reward Coin Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentMint.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${reel.rewardCoins} Coins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                // Duration Indicator overlay bottom-right
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${reel.duration}s",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Body Metadata Section
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = reel.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reel.description,
                    fontSize = 12.sp,
                    color = textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.White.copy(alpha = 0.05f))

                Spacer(modifier = Modifier.height(12.dp))

                val campaignCoins = if (reel.campaignCoins > 0) reel.campaignCoins else 1000
                val distributed = reel.distributedCoins
                val remaining = maxOf(0, campaignCoins - distributed)
                val progress = if (campaignCoins > 0) distributed.toFloat() / campaignCoins.toFloat() else 0f
                val isCompleted = remaining <= 0

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Campaign Progress",
                                tint = if (isCompleted) accentCoral else accentMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Campaign Progress",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) accentCoral else accentMint
                            )
                        }

                        // Likes count
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Likes",
                                tint = textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${reel.likes}",
                                fontSize = 12.sp,
                                color = textMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isCompleted) {
                        Text(
                            text = "Campaign Completed • No More Rewards Available",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCoral
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$distributed / $campaignCoins Coins Distributed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "$remaining Coins Remaining",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.coerceIn(0f, 1f),
                        label = "campaign_progress_bar"
                    )
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isCompleted) accentCoral else accentMint,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isWatched) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentMint.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Claimed",
                                    tint = accentMint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Claimed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentMint
                                )
                            }
                        } else {
                            Button(
                                onClick = onWatch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) Color.Gray.copy(alpha = 0.3f) else accentMint
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp).testTag("watch_reel_btn_${reel.reelId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCompleted) Color.White else Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCompleted) "Watch" else "Watch & Earn",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) Color.White else Color.Black
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
fun ReelPlayerDialog(
    reel: ReelEntity,
    isInitiallyLiked: Boolean,
    isInitiallyWatched: Boolean,
    onToggleLike: () -> Unit,
    onClaimReward: (onComplete: (Boolean, String) -> Unit) -> Unit,
    onVideoCompleted: () -> Unit,
    onDismiss: () -> Unit,
    accentMint: Color,
    accentCoral: Color,
    slateBg: Color,
    textMuted: Color
) {
    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(isInitiallyLiked) }
    var likesCount by remember { mutableStateOf(reel.likes) }
    var isWatched by remember { mutableStateOf(isInitiallyWatched) }

    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    var videoDurationMs by remember { mutableStateOf(0L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var showRewardClaimButton by remember { mutableStateOf(false) }

    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var showConfettiDialog by remember { mutableStateOf(false) }

    // Intercept system back gestures to dismiss the overlay
    BackHandler(onBack = onDismiss)

    // Derive progress values based on real media metrics
    val secondsWatched = (currentPositionMs / 1000).toInt()
    val videoDurationSeconds = (videoDurationMs / 1000).toInt()
    val watchProgress = if (videoDurationMs > 0) currentPositionMs.toFloat() / videoDurationMs.toFloat() else 0f

    var hasRecordedView by remember { mutableStateOf(false) }

    // Trigger reward claim eligibility and unique view recording when the video has been watched completely
    LaunchedEffect(currentPositionMs, videoDurationMs) {
        if (videoDurationMs > 0 && currentPositionMs >= videoDurationMs - 400L) {
            if (!isWatched) {
                showRewardClaimButton = true
            }
            if (!hasRecordedView) {
                hasRecordedView = true
                android.util.Log.d("ReelPlayerDialog", "[DEBUG] Video playback completed (currentPositionMs=$currentPositionMs >= videoDurationMs=$videoDurationMs - 400L). Triggering view registration.")
                onVideoCompleted()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Real Video Player using ExoPlayer
        VideoPlayer(
            videoUrl = reel.videoUrl,
            isPlaying = isPlaying,
            isMuted = isMuted,
            onProgressUpdated = { currentPos, duration ->
                currentPositionMs = currentPos
                videoDurationMs = duration
            },
            modifier = Modifier.fillMaxSize()
        )

            // Top Header overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Reel",
                        tint = Color.White
                    )
                }

                // Header Location badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentMint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reel.location,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Right-side Interactive Action Overlay
            Column(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 120.dp)
                    .align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Play/Pause Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Reel",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPlaying) "Pause" else "Play",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Mute/Unmute Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute/Unmute Reel",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isMuted) "Unmute" else "Mute",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Like Button Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                            onToggleLike()
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, if (isLiked) accentCoral else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Reel",
                            tint = if (isLiked) accentCoral else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$likesCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Description info button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showDescriptionSheet = true },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Full Description",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Info",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Contact button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showContactSheet = true },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, accentMint.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Contact Publisher",
                            tint = accentMint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contact",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Reward Amount Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(accentMint.copy(alpha = 0.15f))
                            .border(1.5.dp, accentMint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins Reward",
                            tint = accentMint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${reel.rewardCoins} COINS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = accentMint
                    )
                }
            }

            // Bottom metadata info Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Info Section
                Text(
                    text = "@${reel.publisherName}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reel.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Watch Timer Progress Bar or Claim Actions
                if (isWatched) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentMint.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Reward claimed",
                            tint = accentMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reward already claimed for this Reel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentMint
                        )
                    }
                } else if (reel.remainingCoins <= 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentCoral.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Campaign Completed",
                            tint = accentCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Campaign Completed • No More Rewards Available",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCoral
                        )
                    }
                } else if (showRewardClaimButton) {
                    Button(
                        onClick = {
                            onClaimReward { success, msg ->
                                if (success) {
                                    isWatched = true
                                    showRewardClaimButton = false
                                    showConfettiDialog = true
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("claim_reel_reward_btn")
                    ) {
                        Text(
                            text = "CLAIM +${reel.rewardCoins} LOOTRA COINS",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = slateBg
                        )
                    }
                } else {
                    // Countdown Progress indicator
                    val secondsLeft = maxOf(0, videoDurationSeconds - secondsWatched)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WATCHING TO UNLOCK COINS...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${secondsLeft}s remaining",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentMint
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = watchProgress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentMint,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Description Sheet Dialog Drawer representation
            if (showDescriptionSheet) {
                Dialog(onDismissRequest = { showDescriptionSheet = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = slateBg),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reel Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = { showDescriptionSheet = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                item {
                                    Text(
                                        text = reel.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = reel.description,
                                        fontSize = 13.sp,
                                        color = textMuted,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "About Promotion",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = accentMint,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "This content is sponsored by ${reel.publisherName} exclusively for Lootra students on the ${reel.location} campus. Complete the watch requirement fully to claim rewards and contact the publisher directly for admissions, offers, and courses.",
                                        fontSize = 12.sp,
                                        color = textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Contact Sheet Drawer dialog
            if (showContactSheet) {
                Dialog(onDismissRequest = { showContactSheet = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = slateBg),
                        border = BorderStroke(1.dp, accentMint.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Contact Publisher",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = { showContactSheet = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = reel.publisherName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            if (reel.contactPerson.isNotEmpty()) {
                                Text(
                                    text = "Contact Person: ${reel.contactPerson}",
                                    fontSize = 12.sp,
                                    color = textMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (reel.phone.isNotEmpty()) {
                                    ContactRowItem(
                                        icon = Icons.Default.Phone,
                                        text = "Call: ${reel.phone}",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${reel.phone}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot dial: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        accentMint = accentMint
                                    )
                                }

                                if (reel.whatsapp.isNotEmpty()) {
                                    ContactRowItem(
                                        icon = Icons.Default.Message,
                                        text = "WhatsApp: ${reel.whatsapp}",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${reel.whatsapp}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        accentMint = accentMint
                                    )
                                }

                                if (reel.website.isNotEmpty()) {
                                    ContactRowItem(
                                        icon = Icons.Default.Language,
                                        text = "Website: ${reel.website}",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse(reel.website)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        accentMint = accentMint
                                    )
                                }

                                if (reel.mapsLink.isNotEmpty()) {
                                    ContactRowItem(
                                        icon = Icons.Default.Map,
                                        text = "Locate on Google Maps",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse(reel.mapsLink)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open Maps: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        accentMint = accentMint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confetti Reward Transfer completed success dialog
            if (showConfettiDialog) {
                Dialog(onDismissRequest = { showConfettiDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = slateBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, accentMint.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
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
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Success",
                                    tint = accentMint,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Lootra Coins Transferred!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Ad/Reel completion successfully validated. Reward credited directly to your global Lootra balance.",
                                fontSize = 13.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "+${reel.rewardCoins} COINS",
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = accentMint,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showConfettiDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = accentMint),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "CONTINUE",
                                    fontWeight = FontWeight.Bold,
                                    color = slateBg
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun ContactRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    accentMint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentMint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    onProgressUpdated: (currentPositionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // Initialize ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE // Loop single video
            playWhenReady = isPlaying
        }
    }

    // Register ExoPlayer Listener for buffering and state callbacks
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE)
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration
                    if (duration > 0) {
                        onProgressUpdated(exoPlayer.currentPosition, duration)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message
                android.util.Log.e("VideoPlayer", "ExoPlayer Playback Error: ${error.message}", error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Handle updates to videoUrl
    LaunchedEffect(videoUrl) {
        try {
            playbackError = null
            isBuffering = true
            android.util.Log.d("VideoPlayer", "Loading videoUrl: $videoUrl")
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } catch (e: Exception) {
            playbackError = e.message
            android.util.Log.e("VideoPlayer", "Error preparing videoUrl: ${e.message}", e)
        }
    }

    // Handle play/pause state
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Handle mute/unmute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Track current position of the video while playing
    LaunchedEffect(isPlaying, videoUrl) {
        if (isPlaying) {
            while (true) {
                val duration = exoPlayer.duration
                if (duration > 0) {
                    onProgressUpdated(exoPlayer.currentPosition, duration)
                }
                delay(250L)
            }
        }
    }

    // Manage player lifecycle to pause/resume and release resources
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            android.util.Log.d("VideoPlayer", "ExoPlayer resources released")
        }
    }

    // Render ExoPlayer via AndroidView with Buffering/Error Overlays
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Custom UI overlay handles play/pause, volume
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Full screen zoom style
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(color = Color.White)
        }

        if (playbackError != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Playback Error",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Playback Error:\n$playbackError",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
