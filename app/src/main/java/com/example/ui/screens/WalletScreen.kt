package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionEntity
import com.example.data.model.WalletEntity
import com.example.ui.viewmodel.WalletViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showBonusSuccessDialog by remember { mutableStateOf(false) }
    var animatedCoinsGained by remember { mutableStateOf(0) }

    // Collect success animations from viewmodel
    LaunchedEffect(Unit) {
        viewModel.coinsClaimedAnimationTrigger.collect { amount ->
            animatedCoinsGained = amount
            showBonusSuccessDialog = true
        }
    }

    // Colors aligned with Slate background + Neon Mint accents
    val slateBg = PremiumBlack
    val cardBg = PremiumSurfaceCard
    val accentMint = EmeraldGreen // Neon emerald/mint accent
    val textMuted = TextMuted

    // Animated Coin count-up
    val coinCount = wallet?.totalCoins ?: 0
    val animatedCoins by animateIntAsState(
        targetValue = coinCount,
        animationSpec = tween(durationMillis = 1000),
        label = "coinCountUp"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(slateBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Top Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "LOOTRA WALLET",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshWallet() },
                        modifier = Modifier.testTag("wallet_refresh_button")
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

            // Dynamic Coin cards & layout list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Glassmorphic main wallet balance card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        cardBg,
                                        cardBg.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                            .testTag("glass_wallet_card")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins Logo",
                                tint = accentMint,
                                modifier = Modifier.size(52.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Available Balance",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted,
                                letterSpacing = 2.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Counter with count-up animation
                            Text(
                                text = "$animatedCoins",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.testTag("wallet_coin_count")
                            )

                            Text(
                                text = "Loot Coins",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentMint,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Today's Gain", fontSize = 11.sp, color = textMuted)
                                    Text(
                                        text = "+${wallet?.todayCoins ?: 0}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentMint
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(30.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Lifetime", fontSize = 11.sp, color = textMuted)
                                    Text(
                                        text = "${wallet?.lifetimeCoins ?: 0}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Check If Welcome gift Claim Button of 20 coins should be shown
                val hasBonus = transactions.any { it.type == "BONUS_EARNED" }
                if (!hasBonus) {
                    item {
                        Card(
                            onClick = { viewModel.claimWelcomeBonus() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("welcome_bonus_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accentMint.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, accentMint.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = "Gift Icon",
                                        tint = accentMint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Claim Welcome Reward",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Free welcoming bonus of 20 Coins",
                                            fontSize = 11.sp,
                                            color = textMuted
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentMint)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+20",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = slateBg
                                    )
                                }
                            }
                        }
                    }
                }

                // Title Section: Transaction History
                item {
                    Text(
                        text = "TRANSACTION HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = textMuted,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Empty state or transaction records list
                if (transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = "Empty History",
                                    tint = textMuted.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Transactions Recorded",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = textMuted
                                )
                                Text(
                                    text = "Complete Wellbeing goals or watch TV to earn",
                                    fontSize = 12.sp,
                                    color = textMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(transactions) { tx ->
                        TransactionRow(tx = tx, accentColor = accentMint, textMuted = textMuted)
                    }
                }
            }
        }
    }

    // Success Claim Welcome Gift Dialogue
    if (showBonusSuccessDialog) {
        Dialog(onDismissRequest = { showBonusSuccessDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, accentMint.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .testTag("success_bonus_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Success Coin Graphics/Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(accentMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = accentMint,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Claim Successful!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You've successfully claimed your reward",
                        fontSize = 13.sp,
                        color = textMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "+$animatedCoinsGained Coins",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = accentMint,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showBonusSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AWESOME",
                            fontWeight = FontWeight.Bold,
                            color = slateBg
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    tx: TransactionEntity,
    accentColor: Color,
    textMuted: Color
) {
    val transactionBg = Color(0xFF1E293B)
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(tx.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(transactionBg)
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("tx_row_${tx.transactionId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (tx.amount >= 0) accentColor.copy(alpha = 0.1f)
                        else Color.Red.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.type) {
                        "REWARD_EARNED" -> Icons.Default.Tv
                        "BONUS_EARNED" -> Icons.Default.CardGiftcard
                        "FUTURE_PURCHASE" -> Icons.Default.ShoppingBag
                        else -> Icons.Default.CurrencyExchange
                    },
                    contentDescription = tx.type,
                    tint = if (tx.amount >= 0) accentColor else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tx.description,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (tx.isSynced) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Sync state",
                        tint = if (tx.isSynced) accentColor.copy(alpha = 0.5f) else textMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }
            }
        }

        Text(
            text = if (tx.amount >= 0) "+${tx.amount}" else "${tx.amount}",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (tx.amount >= 0) accentColor else Color.Red
        )
    }
}
