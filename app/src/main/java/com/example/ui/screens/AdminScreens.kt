package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.PromotionPlanViewModel
import com.example.ui.viewmodel.PromotionUiState
import com.example.data.model.PromotionPlan
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Cosmic Neon Palette
val SciFiBg = Color(0xFF0F0E17)
val SciFiSurface = Color(0xFF1F1E29)
val SciFiPrimary = Color(0xFF00FFCC) // Aqua blue neon
val SciFiSecondary = Color(0xFFE53935) // Neon Red/Orange
val SciFiAccent = Color(0xFFD500F9) // Neon Purple / Pink
val SciFiGreen = Color(0xFF00E676) // Neon Green
val SciFiOrange = Color(0xFFF9A825) // Gold Yellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    currentUserUid: String,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isVerifyingAdmin by viewModel.isVerifyingAdmin.collectAsState()

    // Trigger Admin Access verification on loading
    LaunchedEffect(currentUserUid) {
        if (currentUserUid.isNotEmpty()) {
            viewModel.checkAdminPermission(currentUserUid) { hasAccess ->
                if (!hasAccess) {
                    Toast.makeText(context, "Access Denied: Non-Admin account detected.", Toast.LENGTH_LONG).show()
                    onNavigateBack() // Redirect to regular dashboard
                }
            }
        } else {
            onNavigateBack()
        }
    }

    if (isVerifyingAdmin || isAdmin == null) {
        // Sci-Fi Tech Decoding Authorization Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SciFiBg)
                .testTag("admin_verifying_access"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                OrbitSpinner()
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "L O O T R A   S E C U R E   I D E N T I T Y",
                    color = SciFiPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DECODING AUTHORITY TOKENS FROM SECURE FIRESTORE...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else if (isAdmin == true) {
        // Authorized Space Sci-Fi Admin Dashboard
        AdminMainLayout(
            viewModel = viewModel,
            currentUserUid = currentUserUid,
            onNavigateBack = onNavigateBack,
            onSignOut = onSignOut,
            modifier = modifier
        )
    }
}

@Composable
fun OrbitSpinner() {
    val transition = rememberInfiniteTransition(label = "orbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Canvas(modifier = Modifier.size(72.dp)) {
        drawCircle(
            color = SciFiPrimary.copy(alpha = 0.15f),
            radius = size.minDimension / 2,
            style = Stroke(width = 4.dp.toPx())
        )
        drawArc(
            color = SciFiPrimary,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx())
        )
        drawArc(
            color = SciFiAccent,
            startAngle = angle + 185f,
            sweepAngle = 60f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainLayout(
    viewModel: AdminViewModel,
    currentUserUid: String,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Terminal", "Inventory", "Orders", "Users", "Campuses", "Sub Admins", "Reels", "Wellbeing TV", "Promotions", "Emergency", "Legal Content")
    var showSuperAdminAnalytics by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSuperAdminAnalytics) {
        OrderAnalyticsScreen(
            assignedCollegeId = null,
            isSuperAdmin = true,
            onDismiss = { showSuperAdminAnalytics = false }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = SciFiBg,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(SciFiBg)
                            .padding(16.dp)
                    ) {
                        // Drawer Header
                        Text(
                            text = "LOOTRA SYSTEM CONTROL",
                            color = SciFiPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Text(
                            text = "AUTHORIZATION LEVEL: SUPER ADMIN",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        HorizontalDivider(color = SciFiPrimary.copy(alpha = 0.15f), thickness = 1.dp)
                        
                        // Drawer Items (Scrollable)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                val icon = when (index) {
                                    0 -> Icons.Default.Analytics
                                    1 -> Icons.Default.Inventory2
                                    2 -> Icons.Default.ShoppingCart
                                    3 -> Icons.Default.People
                                    4 -> Icons.Default.LocationOn
                                    5 -> Icons.Default.SupervisorAccount
                                    6 -> Icons.Default.PlayArrow
                                    7 -> Icons.Default.Tv
                                    8 -> Icons.Default.Star
                                    9 -> Icons.Default.Warning
                                    else -> Icons.Default.Extension
                                }
                                
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = title.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        selectedTab = index
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = if (isSelected) Color.Black else SciFiPrimary
                                        )
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = SciFiPrimary,
                                        unselectedContainerColor = Color.Transparent,
                                        selectedIconColor = Color.Black,
                                        unselectedIconColor = SciFiPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("drawer_tab_${title.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                        
                        // Fixed Bottom Section
                        HorizontalDivider(color = SciFiPrimary.copy(alpha = 0.15f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        var showLogoutDialog by remember { mutableStateOf(false) }
                        
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "LOGOUT SESSION",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SciFiSecondary
                                )
                            },
                            selected = false,
                            onClick = {
                                showLogoutDialog = true
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = SciFiSecondary
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = SciFiSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("drawer_logout_btn")
                        )
                        
                        if (showLogoutDialog) {
                            AlertDialog(
                                onDismissRequest = { showLogoutDialog = false },
                                title = {
                                    Text(
                                        text = "Logout",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Are you sure you want to sign out of the Super Admin Panel?",
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showLogoutDialog = false
                                            onSignOut()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SciFiSecondary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("confirm_logout_btn")
                                    ) {
                                        Text(
                                            text = "Logout",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showLogoutDialog = false },
                                        modifier = Modifier.testTag("cancel_logout_btn")
                                    ) {
                                        Text(
                                            text = "Cancel",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = SciFiPrimary
                                        )
                                    }
                                },
                                containerColor = SciFiSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SciFiPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LOOTRA SYSTEM CONTROL",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    fontSize = 18.sp
                                )
                            }
                        },
                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag("admin_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Exit to User Shell",
                                        tint = SciFiPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("admin_menu_drawer_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open Navigation Menu",
                                        tint = SciFiPrimary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SciFiSurface,
                            titleContentColor = Color.White
                        ),
                        actions = {
                            Text(
                                text = "SECURE ADMIN",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SciFiGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, SciFiGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .padding(end = 12.dp)
                            )
                        }
                    )
                },
            bottomBar = {
                val isDark = isSystemInDarkTheme()
                val barBg = if (isDark) SciFiSurface else MaterialTheme.colorScheme.surface
                val activeIndicatorColor = if (isDark) SciFiPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                val activeLabelColor = if (isDark) SciFiPrimary else MaterialTheme.colorScheme.primary
                val inactiveLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                Surface(
                    color = barBg,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val screenWidth = maxWidth
                        val itemWidth = 100.dp
                        val density = LocalDensity.current
                        val centerOffsetPx = with(density) {
                            val screenWidthPx = screenWidth.roundToPx()
                            val itemWidthPx = itemWidth.roundToPx()
                            - (screenWidthPx / 2 - itemWidthPx / 2)
                        }

                        val lazyListState = rememberLazyListState()

                        LaunchedEffect(selectedTab) {
                            lazyListState.animateScrollToItem(selectedTab, centerOffsetPx)
                        }

                        LazyRow(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentPadding = PaddingValues(horizontal = (screenWidth - itemWidth) / 2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(tabs) { index, title ->
                                val isSelected = selectedTab == index
                                val icon = when (index) {
                                    0 -> Icons.Default.Analytics
                                    1 -> Icons.Default.Inventory2
                                    2 -> Icons.Default.ShoppingCart
                                    3 -> Icons.Default.People
                                    4 -> Icons.Default.LocationOn
                                    5 -> Icons.Default.SupervisorAccount
                                    6 -> Icons.Default.PlayArrow
                                    7 -> Icons.Default.Tv
                                    8 -> Icons.Default.Star
                                    9 -> Icons.Default.Warning
                                    else -> Icons.Default.Extension
                                }

                                Box(
                                    modifier = Modifier
                                        .width(itemWidth)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedTab = index }
                                        .testTag("admin_tab_${title.lowercase().replace(" ", "_")}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(56.dp)
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isSelected) activeIndicatorColor else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = title,
                                                tint = if (isSelected) activeLabelColor else inactiveLabelColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = title,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) activeLabelColor else inactiveLabelColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SciFiBg)
                    .testTag("admin_dashboard_screen")
            ) {
                tabs.forEachIndexed { index, title ->
                    val isVisible = selectedTab == index
                    val hasBeenSelected = remember(index) { mutableStateOf(false) }
                    if (isVisible) {
                        hasBeenSelected.value = true
                    }
                    if (hasBeenSelected.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isVisible) 1f else 0f
                                    translationX = if (isVisible) 0f else 100000f
                                }
                        ) {
                            when (index) {
                                0 -> TerminalTabScreen(viewModel, onLaunchOrderAnalytics = { showSuperAdminAnalytics = true })
                                1 -> InventoryTabScreen(viewModel)
                                2 -> OrdersTabScreen(viewModel)
                                3 -> UsersTabScreen(viewModel)
                                4 -> CampusesTabScreen(viewModel)
                                5 -> SubAdminsTabScreen(viewModel)
                                6 -> ReelsTabScreen(viewModel)
                                7 -> WellbeingTvSettingsTabScreen(viewModel)
                                8 -> PromotionPlansTabScreen()
                                9 -> EmergencyControlTabScreen(currentUserUid = currentUserUid)
                                10 -> LegalContentManagementTabScreen()
                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Future Module: $title",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
// TAB 1: TERMINAL / ANALYTICS SCREEN
// ==========================================
@Composable
fun TerminalTabScreen(
    viewModel: AdminViewModel,
    onLaunchOrderAnalytics: () -> Unit
) {
    val analyticsState by viewModel.analyticsState.collectAsState()
    val scrollState = rememberScrollState()

    // Refresh analytics once when entering
    LaunchedEffect(Unit) {
        viewModel.refreshAnalytics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Launch Order Analytics trigger
        Button(
            onClick = onLaunchOrderAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("launch_order_analytics_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, SciFiPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = SciFiPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LAUNCH ORDER ANALYTICS TERMINAL",
                    color = SciFiPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
        // Sub Header Pulse
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CORE SYSTEM ANALYTICS",
                fontWeight = FontWeight.Bold,
                color = SciFiPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            IconButton(onClick = { viewModel.refreshAnalytics() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Core State",
                    tint = SciFiPrimary
                )
            }
        }

        when (analyticsState) {
            is AdminUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OrbitSpinner()
                }
            }
            is AdminUiState.Error -> {
                SciFiErrorBox(
                    error = (analyticsState as AdminUiState.Error).message,
                    onRetry = { viewModel.refreshAnalytics() }
                )
            }
            is AdminUiState.Success -> {
                val data = (analyticsState as AdminUiState.Success).data
                
                // Analytics Tiles Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = "TOTAL REGISTERED USERS",
                            value = data.totalUsers.toString(),
                            icon = Icons.Default.People,
                            glowColor = SciFiPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsCard(
                            title = "TOTAL SYSTEM PRODUCTS",
                            value = data.totalProducts.toString(),
                            icon = Icons.Default.Inventory,
                            glowColor = SciFiAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = "TOTAL PLACED ORDERS",
                            value = data.totalOrders.toString(),
                            icon = Icons.Default.ShoppingCart,
                            glowColor = SciFiOrange,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsCard(
                            title = "ACTIVE USERS (DAU)",
                            value = data.dailyActiveUsers.toString(),
                            icon = Icons.Default.WifiTethering,
                            glowColor = SciFiGreen,
                            isPulse = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = "PENDING SYSTEM ORDERS",
                            value = data.pendingOrders.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            glowColor = SciFiPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsCard(
                            title = "DELIVERED ORDERS",
                            value = data.deliveredOrders.toString(),
                            icon = Icons.Default.TaskAlt,
                            glowColor = SciFiGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Coins Ledger Chart
                CoinsLedgerVisualizer(
                    distributed = data.totalCoinsEarned,
                    redeemed = data.totalCoinsSpent
                )

                // Product Rankings
                ProductPopularityRankingsCard(rankings = data.mostPurchasedProducts)
            }
        }
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    glowColor: Color,
    isPulse: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by if (isPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Card(
        modifier = modifier
            .border(0.5.dp, glowColor.copy(alpha = alphaAnim), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glowColor.copy(alpha = alphaAnim),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CoinsLedgerVisualizer(distributed: Int, redeemed: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SciFiPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "COINS BALANCE MATRIX (EARNED VS SPENT)",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SciFiPrimary
            )

            val total = distributed + redeemed
            val earnedRatio = if (total > 0) distributed.toFloat() / total else 1f
            val spentRatio = if (total > 0) redeemed.toFloat() / total else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Earned
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coins Earned",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$distributed Coins",
                        fontSize = 18.sp,
                        color = SciFiGreen,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Spent
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coins Spent",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$redeemed Coins",
                        fontSize = 18.sp,
                        color = SciFiAccent,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Custom Styled Matrix Bar Chart
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    if (distributed > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(if (earnedRatio > 0.01f) earnedRatio else 0.01f)
                                .background(SciFiGreen)
                        )
                    }
                    if (redeemed > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(if (spentRatio > 0.01f) spentRatio else 0.01f)
                                .background(SciFiAccent)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SciFiGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Earned (${"%.1f".format(earnedRatio * 100)}%)",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SciFiAccent))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Spent (${"%.1f".format(spentRatio * 100)}%)",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPopularityRankingsCard(rankings: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SciFiAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "PRODUCT POPULARITY RANKING",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SciFiAccent
            )

            if (rankings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO PURCHASE SIGNAL DETECTED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            } else {
                val maxPurchase = rankings.maxOf { it.second }.toFloat()

                rankings.forEachIndexed { index, pair ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${index + 1}",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = when (index) {
                                        0 -> SciFiPrimary
                                        1 -> SciFiAccent
                                        else -> SciFiOrange
                                    },
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = pair.first,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${pair.second} orders",
                                fontSize = 11.sp,
                                color = SciFiAccent,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Beautiful Filled Proportion Line
                        val fillRatio = if (maxPurchase > 0) pair.second.toFloat() / maxPurchase else 1f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fillRatio)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                SciFiAccent.copy(alpha = 0.5f),
                                                SciFiAccent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SciFiErrorBox(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SciFiSecondary, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = SciFiSecondary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "CRITICAL: SYSTEM EXCEPTION",
                color = SciFiSecondary,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = error,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SciFiSecondary)
            ) {
                Text("RETRY INTERACTION LINK", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

// ==========================================
// TAB 2: INVENTORY / PRODUCT MANAGEMENT
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryTabScreen(viewModel: AdminViewModel) {
    val productsState by viewModel.productsState.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val context = LocalContext.current

    val allExistingProducts = (productsState as? AdminUiState.Success<List<ProductEntity>>)?.data ?: emptyList()

    if (showAddDialog) {
        ProductDialog(
            title = "ADD NEW SYSTEM INVENTORY PRODUCT",
            existingProducts = allExistingProducts,
            onDismiss = { showAddDialog = false },
            onConfirm = { prod ->
                viewModel.addProduct(prod) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showAddDialog = false
                }
            }
        )
    }

    if (productToEdit != null) {
        ProductDialog(
            title = "EDIT SYSTEM INVENTORY PRODUCT",
            existingProduct = productToEdit,
            existingProducts = allExistingProducts,
            onDismiss = { productToEdit = null },
            onConfirm = { updated ->
                viewModel.editProduct(updated) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) productToEdit = null
                }
            }
        )
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = {
                Text(
                    text = "CONFIRM INVENTORY PURGE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SciFiSecondary,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to purge product '${productToDelete?.name}' from the Firestore system completely?",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prod = productToDelete ?: return@Button
                        viewModel.deleteProduct(prod.productId) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) productToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiSecondary),
                    modifier = Modifier.testTag("dialog_delete_product_confirm")
                ) {
                    Text("PURGE PRODUCT")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.5f))
                }
            },
            containerColor = SciFiSurface
        )
    }

    // Main layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Addition Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setProductSearchQuery(it) },
                placeholder = { Text("Search products / category...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_search_products_input"),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SciFiPrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SciFiPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = SciFiSurface,
                    unfocusedContainerColor = SciFiBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary, contentColor = SciFiBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("btn_add_product")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Product Catalog List
        when (productsState) {
            is AdminUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OrbitSpinner()
                }
            }
            is AdminUiState.Error -> {
                SciFiErrorBox(
                    error = (productsState as AdminUiState.Error).message,
                    onRetry = { viewModel.refreshAnalytics() }
                )
            }
            is AdminUiState.Success -> {
                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO PRODUCTS SIGNAL FOUND",
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProducts, key = { it.productId }) { product ->
                            AdminProductCard(
                                product = product,
                                onEdit = { productToEdit = product },
                                onDelete = { productToDelete = product },
                                onToggleActive = { viewModel.toggleProductActive(product.productId, product.isActive) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = if (product.isActive) SciFiPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Image(
                painter = rememberAsyncImagePainter(model = product.imageUrl),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Active badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (product.isActive) SciFiGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.10f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (product.isActive) "ACTIVE" else "DISABLED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.isActive) SciFiGreen else Color.White.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = product.description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pricingText = when (product.pricingType) {
                        "COINS_ONLY" -> "🪙 ${product.coinsRequired} Coins"
                        "MONEY_ONLY" -> "₹${product.moneyPrice}"
                        "HYBRID" -> "🪙 ${product.coinsRequired} + ₹${product.moneyPrice}"
                        else -> "🪙 ${product.coinPrice} Coins"
                    }
                    Text(
                        text = pricingText,
                        color = SciFiPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Stock: ${product.stock}",
                        color = if (product.stock > 0) SciFiOrange else SciFiSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = product.category,
                        color = SciFiAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Enable/Disable Switch
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier.size(28.dp).testTag("product_toggle_active_${product.productId}")
                ) {
                    Icon(
                        imageVector = if (product.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle state",
                        tint = if (product.isActive) SciFiGreen else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp).testTag("product_edit_btn_${product.productId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Product",
                        tint = SciFiPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("product_delete_btn_${product.productId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Purge Product",
                        tint = SciFiSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    title: String,
    existingProduct: ProductEntity? = null,
    existingProducts: List<ProductEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var desc by remember { mutableStateOf(existingProduct?.description ?: "") }
    var imageUrl by remember { mutableStateOf(existingProduct?.imageUrl ?: "") }
    var publicId by remember { mutableStateOf(existingProduct?.publicId ?: "") }
    var imageWidth by remember { mutableIntStateOf(existingProduct?.imageWidth ?: 0) }
    var imageHeight by remember { mutableIntStateOf(existingProduct?.imageHeight ?: 0) }
    var uploadedAt by remember { mutableLongStateOf(existingProduct?.uploadedAt ?: 0L) }

    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }
    var uploadSuccessMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageRepo = remember { com.example.data.repository.StorageRepositoryImpl() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingImage = true
            uploadProgress = 0f
            uploadErrorMessage = null
            uploadSuccessMessage = null

            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val preparedResult = com.example.util.ImageUtils.prepareAndCompressImage(context, uri)
                if (preparedResult.isFailure) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isUploadingImage = false
                        uploadErrorMessage = preparedResult.exceptionOrNull()?.message ?: "Failed to process image format"
                    }
                    return@launch
                }

                val compressedUri = preparedResult.getOrThrow()
                val uploadResult = storageRepo.uploadProductImage(compressedUri) { progress ->
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        uploadProgress = progress
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isUploadingImage = false
                    if (uploadResult.isSuccess) {
                        val resp = uploadResult.getOrThrow()
                        imageUrl = resp.secureUrl
                        publicId = resp.publicId
                        imageWidth = resp.width
                        imageHeight = resp.height
                        uploadedAt = resp.uploadedAt
                        uploadSuccessMessage = "✓ Image uploaded successfully to Cloudinary"
                    } else {
                        uploadErrorMessage = uploadResult.exceptionOrNull()?.message ?: "Cloudinary image upload failed. Please try again."
                    }
                }
            }
        }
    }
    
    // Support three pricing modes
    var pricingType by remember { mutableStateOf(existingProduct?.pricingType ?: "COINS_ONLY") }
    var coinsRequired by remember { mutableStateOf(existingProduct?.coinsRequired?.toString() ?: existingProduct?.coinPrice?.toString() ?: "10") }
    var moneyPrice by remember { mutableStateOf(existingProduct?.moneyPrice?.toString() ?: "0") }
    
    var stock by remember { mutableStateOf(existingProduct?.stock?.toString() ?: "100") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "Electronics") }

    var collegeId by remember { mutableStateOf(existingProduct?.collegeId ?: "") }
    var collegeName by remember { mutableStateOf(existingProduct?.collegeName ?: "") }
    var pickupLocation by remember { mutableStateOf(existingProduct?.pickupLocation ?: "") }

    var colleges by remember { mutableStateOf<List<com.example.data.model.College>>(emptyList()) }
    var locations by remember { mutableStateOf<List<com.example.data.model.PickupLocation>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("colleges").get().addOnSuccessListener { snapshot ->
                colleges = snapshot.documents.map { com.example.data.model.College.fromMap(it.data) }
            }
            firestore.collection("pickup_locations").get().addOnSuccessListener { snapshot ->
                locations = snapshot.documents.map { com.example.data.model.PickupLocation.fromMap(it.data) }
            }
        } catch (e: Exception) {
            Log.e("AdminScreens", "Error loading locations: ${e.message}")
        }
    }

    val isNameDuplicate = remember(name) {
        val trimmed = name.trim()
        trimmed.isNotEmpty() && existingProducts.any {
            it.productId != existingProduct?.productId && it.name.trim().equals(trimmed, ignoreCase = true)
        }
    }

    val parsedCoins = coinsRequired.toIntOrNull() ?: 0
    val parsedMoney = moneyPrice.toIntOrNull() ?: 0

    val isPricingValid = when (pricingType) {
        "COINS_ONLY" -> parsedCoins > 0
        "MONEY_ONLY" -> parsedMoney > 0
        "HYBRID" -> parsedCoins > 0 && parsedMoney > 0
        else -> false
    }

    val isFormValid = name.isNotBlank() && desc.isNotBlank() && imageUrl.isNotBlank() && !isUploadingImage && stock.toIntOrNull() != null && category.isNotBlank() && !isNameDuplicate && isPricingValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SciFiPrimary,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_name")
                )
                if (isNameDuplicate) {
                    Text(
                        text = "A product with this name already exists",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp).testTag("product_dialog_name_error")
                    )
                }

                // Description
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_desc")
                )

                // Product Image Section (Upload, Progress, Preview, Replace/Remove)
                Text(
                    text = "PRODUCT IMAGE",
                    color = SciFiPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (imageUrl.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUrl),
                                contentDescription = "Uploaded Product Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay Success Badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Uploaded",
                                        tint = SciFiGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Cloudinary Verified",
                                        color = SciFiGreen,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Action Buttons (Replace / Remove)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SciFiPrimary),
                                    border = BorderStroke(1.dp, SciFiPrimary),
                                    modifier = Modifier.testTag("product_image_replace_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Replace Image", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = {
                                        imageUrl = ""
                                        publicId = ""
                                        imageWidth = 0
                                        imageHeight = 0
                                        uploadedAt = 0L
                                        uploadSuccessMessage = null
                                        uploadErrorMessage = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                    modifier = Modifier.testTag("product_image_remove_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else if (isUploadingImage) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SciFiBg),
                        border = BorderStroke(1.dp, SciFiPrimary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { uploadProgress },
                                color = SciFiPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = SciFiPrimary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "Uploading to Cloudinary... ${(uploadProgress * 100).toInt()}%",
                                color = SciFiPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .testTag("product_image_upload_btn"),
                        colors = CardDefaults.cardColors(containerColor = SciFiBg),
                        border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Upload Image",
                                tint = SciFiPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "🖼 Upload Product Image",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Supports JPG, JPEG, PNG, WEBP (Max 10 MB)",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (uploadErrorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = Color.Red)
                            Text(
                                text = uploadErrorMessage ?: "",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                if (uploadSuccessMessage != null && imageUrl.isNotBlank()) {
                    Text(
                        text = uploadSuccessMessage ?: "",
                        color = SciFiGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Pricing Type Selection
                Text("Pricing Type", color = SciFiPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "COINS_ONLY" to "Coins Only",
                        "MONEY_ONLY" to "Money Only",
                        "HYBRID" to "Coins + Money"
                    )
                    types.forEach { (typeVal, typeLabel) ->
                        val isSel = pricingType == typeVal
                        FilterChip(
                            selected = isSel,
                            onClick = { pricingType = typeVal },
                            label = { Text(typeLabel, color = if (isSel) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SciFiPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("pricing_type_chip_$typeVal")
                        )
                    }
                }

                // Coins Required (Visible for Coins Only and Coins + Money)
                if (pricingType == "COINS_ONLY" || pricingType == "HYBRID") {
                    OutlinedTextField(
                        value = coinsRequired,
                        onValueChange = { coinsRequired = it },
                        label = { Text("Coins Required") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("product_dialog_coins_required")
                    )
                }

                // Price (₹) (Visible for Money Only and Coins + Money)
                if (pricingType == "MONEY_ONLY" || pricingType == "HYBRID") {
                    OutlinedTextField(
                        value = moneyPrice,
                        onValueChange = { moneyPrice = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("product_dialog_money_price")
                    )
                }

                // Stock Quantity
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Quantity Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_stock")
                )

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Class Group Category") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_category")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // College Campus Selection
                Text("Assigned College Campus:", color = SciFiPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                if (colleges.isEmpty()) {
                    Text("No colleges active. Please add colleges in the management panel.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colleges.forEach { col ->
                            val isSel = col.collegeId == collegeId
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    collegeId = if (isSel) "" else col.collegeId
                                    collegeName = if (isSel) "" else col.name
                                },
                                label = { Text(col.name, color = if (isSel) Color.Black else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SciFiPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Pickup Location Selection
                Text("Assigned Pickup Location:", color = SciFiPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                if (locations.isEmpty()) {
                    Text("No pickup locations active. Please add locations.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locations.forEach { loc ->
                            val isSel = loc.name == pickupLocation
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    pickupLocation = if (isSel) "" else loc.name
                                },
                                label = { Text(loc.name, color = if (isSel) Color.Black else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SciFiPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        val finalCoins = if (pricingType == "MONEY_ONLY") 0 else parsedCoins
                        val finalMoney = if (pricingType == "COINS_ONLY") 0 else parsedMoney
                        val finalCoinPrice = if (pricingType == "MONEY_ONLY") 0 else finalCoins

                        val product = ProductEntity(
                            productId = existingProduct?.productId ?: UUID.randomUUID().toString(),
                            name = name,
                            description = desc,
                            imageUrl = imageUrl,
                            publicId = publicId,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            uploadedAt = if (uploadedAt == 0L) System.currentTimeMillis() else uploadedAt,
                            coinPrice = finalCoinPrice,
                            stock = stock.toInt(),
                            category = category,
                            isActive = existingProduct?.isActive ?: true,
                            createdAt = existingProduct?.createdAt ?: System.currentTimeMillis(),
                            collegeId = collegeId,
                            collegeName = collegeName,
                            pickupLocation = pickupLocation,
                            pricingType = pricingType,
                            coinsRequired = finalCoins,
                            moneyPrice = finalMoney
                        )
                        onConfirm(product)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary, contentColor = SciFiBg),
                modifier = Modifier.testTag("product_dialog_confirm_btn")
            ) {
                Text("APPLY CONTROL")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", color = Color.White.copy(alpha = 0.5f))
            }
        },
        containerColor = SciFiSurface
    )
}

// ==========================================
// TAB 3: ORDER MANAGEMENT SCREEN
// ==========================================
@Composable
fun OrdersTabScreen(viewModel: AdminViewModel) {
    val ordersState by viewModel.ordersState.collectAsState()
    val searchQuery by viewModel.orderSearchQuery.collectAsState()
    val filterStatus by viewModel.orderFilterStatus.collectAsState()
    val filteredOrders by viewModel.filteredOrders.collectAsState()

    var selectedOrderForDetail by remember { mutableStateOf<OrderEntity?>(null) }
    var selectedOrderUserEmail by remember { mutableStateOf("loading...") }
    var selectedOrderUsername by remember { mutableStateOf("loading...") }
    val context = LocalContext.current

    val statuses = listOf("All", "Pending", "Confirmed", "Packed", "Shipped", "Out For Delivery", "Delivered", "Cancelled")

    if (selectedOrderForDetail != null) {
        val order = selectedOrderForDetail!!
        
        // Dynamic User Metadata Fetch when dialog opens
        LaunchedEffect(order.uid) {
            val db = FirebaseFirestore.getInstance()
            try {
                val doc = db.collection("users").document(order.uid).get().await()
                if (doc.exists()) {
                    selectedOrderUserEmail = doc.getString("email") ?: "anonymous@lootra.com"
                    selectedOrderUsername = doc.getString("username") ?: "Lootra Resident"
                } else {
                    selectedOrderUserEmail = "not_found@lootra.com"
                    selectedOrderUsername = "Unknown Resident"
                }
            } catch (e: Exception) {
                selectedOrderUserEmail = "error_fetching"
                selectedOrderUsername = "Fetch Error"
            }
        }

        AlertDialog(
            onDismissRequest = { selectedOrderForDetail = null },
            title = {
                Text(
                    text = "ORDER CONTROL MANIFEST",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SciFiPrimary,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ID
                    DetailItem(label = "Order Ident Key", value = order.orderId)
                    DetailItem(
                        label = "Registry Date",
                        value = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(order.createdAt))
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Status
                    Text(
                        text = "ORDER DISPATCH STATE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.orderStatus,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (order.orderStatus) {
                                "Delivered", "Delivered Successfully" -> SciFiGreen
                                "Cancelled" -> SciFiSecondary
                                else -> SciFiOrange
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }

                    // User Details
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        text = "RESIDENT RECIPIENT METRICS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    DetailItem(label = "Resident Name", value = selectedOrderUsername)
                    DetailItem(label = "Resident Mail", value = selectedOrderUserEmail)
                    DetailItem(label = "Resident UID", value = order.uid)

                    // Product details
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        text = "SECURED CONTENT DATA",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    DetailItem(label = "Payload Title", value = order.productName)
                    DetailItem(label = "Payload Price", value = "${order.coinPrice} Coins")
                    DetailItem(label = "Payload ID", value = order.productId)

                    // Delivery details
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        text = "SHIPMENT SECTOR COORDINATES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    val mockAddress = remember(order.uid) {
                        val region = when (order.uid.take(2).uppercase()) {
                            "AB" -> "Neo-Tokyo, Core-Ring 4"
                            "CD" -> "Elysium Mars Colony, Sector 9"
                            "EF" -> "Luna City South, Bay 12"
                            else -> "Nova Metropolis, Grid Sector 8-A"
                        }
                        "Dome $region, Platform 4B, Lot ${order.createdAt.toString().takeLast(3)}"
                    }
                    Text(
                        text = mockAddress,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Status Change Trigger Control
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        text = "COMMAND DISPATCH TRANSMIT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SciFiPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Simple Box buttons for each status transition state
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        mainAxisSpacing = 6.dp,
                        crossAxisSpacing = 6.dp
                    ) {
                        listOf("Pending", "Confirmed", "Packed", "Shipped", "Out For Delivery", "Delivered", "Cancelled").forEach { dispatchState ->
                            val isSelected = order.orderStatus.equals(dispatchState, ignoreCase = true) || 
                                             (dispatchState == "Delivered" && order.orderStatus == "Delivered Successfully")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) SciFiPrimary else Color.White.copy(alpha = 0.05f))
                                    .border(0.5.dp, if (isSelected) SciFiPrimary else Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.updateOrderStatus(order.orderId, dispatchState) { success ->
                                            if (success) {
                                                Toast.makeText(context, "State changed to $dispatchState", Toast.LENGTH_SHORT).show()
                                                selectedOrderForDetail = order.copy(orderStatus = dispatchState)
                                            } else {
                                                Toast.makeText(context, "Failed to update state", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("status_toggle_${dispatchState.lowercase()}")
                            ) {
                                Text(
                                    text = dispatchState.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) SciFiBg else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedOrderForDetail = null }) {
                    Text("CLOSE CONTROL")
                }
            },
            containerColor = SciFiSurface
        )
    }

    // Main layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setOrderSearchQuery(it) },
            placeholder = { Text("Search by Order ID / User ID / Content...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_search_orders_input"),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SciFiPrimary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SciFiPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = SciFiSurface,
                unfocusedContainerColor = SciFiBg,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Horizontal filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { statusText ->
                val isSelected = filterStatus == statusText
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SciFiPrimary else SciFiSurface)
                        .clickable { viewModel.setOrderFilterStatus(statusText) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = statusText.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) SciFiBg else Color.White.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // List of placed system orders
        when (ordersState) {
            is AdminUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OrbitSpinner()
                }
            }
            is AdminUiState.Error -> {
                SciFiErrorBox(
                    error = (ordersState as AdminUiState.Error).message,
                    onRetry = { viewModel.refreshAnalytics() }
                )
            }
            is AdminUiState.Success -> {
                if (filteredOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO INCOMING ORDER SIGNAL SIGNALED",
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredOrders, key = { it.orderId }) { order ->
                            AdminOrderCard(
                                order = order,
                                onClick = { selectedOrderForDetail = order }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: OrderEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ID: ${order.orderId.substringBefore("-")}-SEC",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SciFiPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.productName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${order.coinPrice} Coins",
                        color = SciFiGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(order.createdAt)),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // State Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (order.orderStatus) {
                            "Delivered", "Delivered Successfully" -> SciFiGreen.copy(alpha = 0.15f)
                            "Cancelled" -> SciFiSecondary.copy(alpha = 0.15f)
                            else -> SciFiOrange.copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        0.5.dp,
                        when (order.orderStatus) {
                            "Delivered", "Delivered Successfully" -> SciFiGreen
                            "Cancelled" -> SciFiSecondary
                            else -> SciFiOrange
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = order.orderStatus.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = when (order.orderStatus) {
                        "Delivered", "Delivered Successfully" -> SciFiGreen
                        "Cancelled" -> SciFiSecondary
                        else -> SciFiOrange
                    }
                )
            }
        }
    }
}

// ==========================================
// TAB 4: USERS / REGISTRY PANEL
// ==========================================
@Composable
fun UsersTabScreen(viewModel: AdminViewModel) {
    val usersState by viewModel.usersState.collectAsState()
    val searchQuery by viewModel.userSearchQuery.collectAsState()
    val filteredUsers by viewModel.filteredUsers.collectAsState()

    var selectedUserForMetrics by remember { mutableStateOf<UserProfile?>(null) }

    val wallet by viewModel.selectedUserWallet.collectAsState()
    val ordersCount by viewModel.selectedUserOrdersCount.collectAsState()
    val rewardsMap by viewModel.selectedUserRewards.collectAsState()
    val isLoadingUserMetrics by viewModel.isLoadingUserMetrics.collectAsState()

    if (selectedUserForMetrics != null) {
        val user = selectedUserForMetrics!!

        LaunchedEffect(user.uid) {
            viewModel.loadUserMetrics(user.uid)
        }

        AlertDialog(
            onDismissRequest = { selectedUserForMetrics = null },
            title = {
                Text(
                    text = "RESIDENT PROFILE DATABASE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SciFiPrimary,
                    fontSize = 15.sp
                )
            },
            text = {
                if (isLoadingUserMetrics) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OrbitSpinner()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(model = user.profileImage),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, SciFiPrimary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = user.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(text = user.email, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Wallet Balance
                        Text(
                            text = "SECURE WALLET STATE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SciFiPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        DetailItem(label = "Liquid Balance", value = "${wallet?.totalCoins ?: 0} Lootra Coins")
                        DetailItem(label = "Accumulated Coins", value = "${wallet?.lifetimeCoins ?: 0} Lootra Coins")
                        DetailItem(label = "Earned Today Balance", value = "${wallet?.todayCoins ?: 0} Lootra Coins")

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Reward Stats
                        Text(
                            text = "REWARD METRICS LOG",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SciFiAccent,
                            fontWeight = FontWeight.Bold
                        )
                        val totalTimeSec = (rewardsMap["screenTimeMinutes"] as? Number)?.toInt() ?: 0
                        val adViews = (rewardsMap["adsWatched"] as? Number)?.toInt() ?: 0
                        val claimedRew = (rewardsMap["claimedRewards"] as? Number)?.toInt() ?: 0

                        DetailItem(label = "Tracking Minutes Today", value = "$totalTimeSec minutes")
                        DetailItem(label = "TV Views", value = "$adViews advertisements")
                        DetailItem(label = "Claimed Prize Targets", value = "$claimedRew target items")

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Order count
                        Text(
                            text = "COMMERCE METRICS LOG",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SciFiOrange,
                            fontWeight = FontWeight.Bold
                        )
                        DetailItem(label = "Placed Store Orders", value = "$ordersCount orders")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedUserForMetrics = null }) {
                    Text("OK")
                }
            },
            containerColor = SciFiSurface
        )
    }

    // Main layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setUserSearchQuery(it) },
            placeholder = { Text("Search by username / email / UID...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_search_users_input"),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SciFiPrimary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SciFiPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = SciFiSurface,
                unfocusedContainerColor = SciFiBg,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // List of users
        when (usersState) {
            is AdminUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OrbitSpinner()
                }
            }
            is AdminUiState.Error -> {
                SciFiErrorBox(
                    error = (usersState as AdminUiState.Error).message,
                    onRetry = { viewModel.refreshAnalytics() }
                )
            }
            is AdminUiState.Success -> {
                if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO REGISTERED USER SIGNAL SIGNALED",
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            AdminUserCard(
                                user = user,
                                onClick = { selectedUserForMetrics = user }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = user.profileImage),
                contentDescription = user.username,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, SciFiPrimary, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = user.email,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SciFiPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

// Helpers
@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

/**
 * A simple FlowRow implementation because Accompanist FlowLayout might not be available or imported.
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val xSpacing = mainAxisSpacing.roundToPx()
        val ySpacing = crossAxisSpacing.roundToPx()

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowHeights = mutableListOf<Int>()
        val rowWidths = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        for (measurable in measurables) {
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))

            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowHeights.add(currentRowHeight)
                rowWidths.add(currentRowWidth)

                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width + xSpacing
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentRowHeight)
            rowWidths.add(currentRowWidth)
        }

        val totalHeight = rowHeights.sum() + (rows.size - 1) * ySpacing
        val maxWidth = rowWidths.maxOrNull() ?: 0

        layout(maxOf(constraints.minWidth, maxWidth), maxOf(constraints.minHeight, totalHeight)) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                val rowHeight = rowHeights[rowIndex]
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + xSpacing
                }
                y += rowHeight + ySpacing
            }
        }
    }
}

@Composable
fun CampusesTabScreen(viewModel: AdminViewModel) {
    var colleges by remember { mutableStateOf<List<com.example.data.model.College>>(emptyList()) }
    var locations by remember { mutableStateOf<List<com.example.data.model.PickupLocation>>(emptyList()) }

    var newCollegeName by remember { mutableStateOf("") }
    var newLocationName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    fun loadData() {
        firestore.collection("colleges").get().addOnSuccessListener { snapshot ->
            colleges = snapshot.documents.map { com.example.data.model.College.fromMap(it.data) }
        }
        firestore.collection("pickup_locations").get().addOnSuccessListener { snapshot ->
            locations = snapshot.documents.map { com.example.data.model.PickupLocation.fromMap(it.data) }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Add College
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REGISTER NEW COLLEGE CAMPUS",
                        color = SciFiPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCollegeName,
                        onValueChange = { newCollegeName = it },
                        label = { Text("College Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SciFiPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_colleges_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (newCollegeName.isNotBlank()) {
                                val collegeId = UUID.randomUUID().toString()
                                val college = com.example.data.model.College(collegeId, newCollegeName.trim())
                                firestore.collection("colleges").document(collegeId).set(college.toMap())
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "${college.name} successfully registered!", Toast.LENGTH_SHORT).show()
                                        newCollegeName = ""
                                        loadData()
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary, contentColor = SciFiBg),
                        modifier = Modifier.fillMaxWidth().testTag("admin_add_college_btn")
                    ) {
                        Text("REGISTER COLLEGE")
                    }
                }
            }
        }

        // Section: Active Colleges List
        item {
            Text(
                text = "ACTIVE REGISTERED COLLEGES",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        if (colleges.isEmpty()) {
            item {
                Text("No active colleges found.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            items(colleges) { college ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(college.name, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                firestore.collection("colleges").document(college.collegeId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "College removed", Toast.LENGTH_SHORT).show()
                                        loadData()
                                    }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete college", tint = SciFiSecondary)
                        }
                    }
                }
            }
        }

        // Section: Add Pickup Point
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REGISTER NEW PICKUP POINT",
                        color = SciFiPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newLocationName,
                        onValueChange = { newLocationName = it },
                        label = { Text("Pickup Location Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SciFiPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_location_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (newLocationName.isNotBlank()) {
                                val locId = UUID.randomUUID().toString()
                                val location = com.example.data.model.PickupLocation(locId, newLocationName.trim())
                                firestore.collection("pickup_locations").document(locId).set(location.toMap())
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "${location.name} successfully registered!", Toast.LENGTH_SHORT).show()
                                        newLocationName = ""
                                        loadData()
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary, contentColor = SciFiBg),
                        modifier = Modifier.fillMaxWidth().testTag("admin_add_location_btn")
                    ) {
                        Text("REGISTER PICKUP POINT")
                    }
                }
            }
        }

        // Section: Active Locations List
        item {
            Text(
                text = "ACTIVE REGISTERED PICKUP POINTS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        if (locations.isEmpty()) {
            item {
                Text("No active pickup locations found.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            items(locations) { loc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(loc.name, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                firestore.collection("pickup_locations").document(loc.locationId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Pickup point removed", Toast.LENGTH_SHORT).show()
                                        loadData()
                                    }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete pickup", tint = SciFiSecondary)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 6: SUB-ADMINS MANAGEMENT (SUPER ADMIN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAdminsTabScreen(viewModel: AdminViewModel) {
    val subAdminsState by viewModel.subAdminsState.collectAsState()
    val filteredSubAdmins by viewModel.filteredSubAdmins.collectAsState()
    val searchQuery by viewModel.subAdminSearchQuery.collectAsState()
    
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<com.example.data.repository.SubAdminUser?>(null) }
    
    var colleges by remember { mutableStateOf<List<com.example.data.model.College>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    
    LaunchedEffect(Unit) {
        viewModel.loadSubAdmins()
        firestore.collection("colleges").get().addOnSuccessListener { snapshot ->
            colleges = snapshot.documents.map { com.example.data.model.College.fromMap(it.data) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TELEMETRY SUMMARY HEADER
        Card(
            colors = CardDefaults.cardColors(containerColor = SciFiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL ASSIGNED SUB-ADMINS",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (val state = subAdminsState) {
                            is AdminUiState.Success -> "${state.data.size}"
                            is AdminUiState.Loading -> "LOADING..."
                            else -> "0"
                        },
                        color = SciFiPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_sub_admin_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New sub-admin",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CREATE SUB ADMIN",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // FILTER SEARCH
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSubAdminSearchQuery(it) },
            placeholder = { Text("FILTER BY NAME, EMAIL OR CAMPUS...", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SciFiPrimary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sub_admin_search"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SciFiPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedContainerColor = SciFiSurface,
                unfocusedContainerColor = SciFiSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // LIST LAYOUT
        when (subAdminsState) {
            is AdminUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SciFiPrimary)
                }
            }
            is AdminUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (subAdminsState as AdminUiState.Error).message,
                        color = SciFiSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            is AdminUiState.Success -> {
                if (filteredSubAdmins.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO CONFIGURED SUB-ADMIN CHANNELS",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSubAdmins) { subAdmin ->
                            SubAdminRowCard(
                                subAdmin = subAdmin,
                                onEdit = { showEditDialog = subAdmin },
                                onDelete = {
                                    viewModel.deleteSubAdmin(subAdmin.uid) { res ->
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Sub Admin profile dissolved", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Deletion failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onToggleActive = { active ->
                                    val updated = subAdmin.copy(isActive = active)
                                    viewModel.updateSubAdmin(updated) { res ->
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Status set to ${if (active) "Active" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // CREATE POPUP
    if (showCreateDialog) {
        CreateSubAdminDialog(
            colleges = colleges,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, email, password, college ->
                viewModel.createSubAdmin(
                    email = email,
                    password = password,
                    name = name,
                    collegeId = college.collegeId,
                    collegeName = college.name,
                    isActive = true
                ) { res ->
                    if (res.isSuccess) {
                        showCreateDialog = false
                        Toast.makeText(context, "Sub Admin account successfully initialized!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Auth Node failure: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // configure popup
    if (showEditDialog != null) {
        EditSubAdminDialog(
            subAdmin = showEditDialog!!,
            colleges = colleges,
            onDismiss = { showEditDialog = null },
            onConfirm = { updatedSubAdmin ->
                viewModel.updateSubAdmin(updatedSubAdmin) { res ->
                    if (res.isSuccess) {
                        showEditDialog = null
                        Toast.makeText(context, "Gateway parameters successfully hot-swapped", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Update failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun SubAdminRowCard(
    subAdmin: com.example.data.repository.SubAdminUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp, 
                if (subAdmin.isActive) SciFiPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), 
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subAdmin.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subAdmin.email,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Config", tint = SciFiOrange)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SciFiSecondary)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(SciFiPrimary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, SciFiPrimary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subAdmin.collegeName.uppercase().ifEmpty { "UNASSIGNED CAMPUS" },
                        color = SciFiPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (subAdmin.isActive) "ACTIVE GATE" else "DISABLED",
                        color = if (subAdmin.isActive) SciFiGreen else Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = subAdmin.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SciFiGreen,
                            checkedTrackColor = SciFiGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.3f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.scale(0.72f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (com.example.data.model.College) -> Unit
) {
    var collegesList by remember { mutableStateOf<List<com.example.data.model.College>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("colleges").get()
            .addOnSuccessListener { snapshot ->
                collegesList = snapshot.documents.map { com.example.data.model.College.fromMap(it.data) }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                errorMsg = exception.message ?: "Failed to load campuses"
                isLoading = false
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "SELECT CAMPUS CONFIGURATION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SciFiPrimary,
                fontSize = 15.sp
            )
        },
        containerColor = SciFiSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Campuses...", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SciFiPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("college_search_input")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = SciFiPrimary)
                    } else if (errorMsg != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = SciFiSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = errorMsg ?: "Connection Error",
                                color = SciFiSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMsg = null
                                    firestore.collection("colleges").get()
                                        .addOnSuccessListener { snapshot ->
                                            collegesList = snapshot.documents.map { com.example.data.model.College.fromMap(it.data) }
                                            isLoading = false
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMsg = exception.message ?: "Failed to load campuses"
                                            isLoading = false
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("RETRY", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    } else {
                        val filtered = collegesList.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (filtered.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "No Campuses Found",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered) { col ->
                                    Card(
                                        onClick = { onSelect(col) },
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .testTag("college_select_item_${col.collegeId}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = col.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "ID: ${col.collegeId.take(8).uppercase()}",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            color = if (col.available) SciFiGreen else SciFiSecondary,
                                                            shape = CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = if (col.available) "ONLINE" else "OFFLINE",
                                                    color = if (col.available) SciFiGreen else SciFiSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DISCARD", color = SciFiSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSubAdminDialog(
    colleges: List<com.example.data.model.College>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, com.example.data.model.College) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedCollege by remember { mutableStateOf<com.example.data.model.College?>(null) }
    var showCollegeSelector by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "PROVISION SYSTEM SUB-ADMIN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SciFiPrimary,
                fontSize = 15.sp
            )
        },
        containerColor = SciFiSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Sub-Admin Full Name", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_sub_admin_name")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Secure Email Gateway", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_sub_admin_email")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Encrypted Password Access", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_sub_admin_password")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCollegeSelector = true }
                ) {
                    OutlinedTextField(
                        value = selectedCollege?.name ?: "TAP TO CHOOSE CAMPUS...",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Assigned College Campus", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledBorderColor = Color.White.copy(alpha = 0.15f),
                            disabledLabelColor = Color.White.copy(alpha = 0.5f),
                            disabledTrailingIconColor = SciFiPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("college_dropdown_trigger"),
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = SciFiPrimary)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCollege == null) {
                        Toast.makeText(com.google.firebase.FirebaseApp.getInstance().applicationContext, "Gateway Blocked: Campus is not selected!", Toast.LENGTH_SHORT).show()
                    } else if (name.isNotEmpty() && email.isNotEmpty() && password.length >= 6) {
                        onConfirm(name, email, password, selectedCollege!!)
                    } else {
                        Toast.makeText(com.google.firebase.FirebaseApp.getInstance().applicationContext, "Please satisfy all credentials (Password min 6 chars)", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_sub_admin_confirm")
            ) {
                Text("DEPLOY ACCOUNT", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DISCARD", color = SciFiSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    )

    if (showCollegeSelector) {
        CollegeSelectionDialog(
            onDismiss = { showCollegeSelector = false },
            onSelect = { col ->
                selectedCollege = col
                showCollegeSelector = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubAdminDialog(
    subAdmin: com.example.data.repository.SubAdminUser,
    colleges: List<com.example.data.model.College>,
    onDismiss: () -> Unit,
    onConfirm: (com.example.data.repository.SubAdminUser) -> Unit
) {
    var name by remember { mutableStateOf(subAdmin.name) }
    var selectedCollege by remember { mutableStateOf<com.example.data.model.College?>(colleges.find { it.collegeId == subAdmin.collegeId } ?: if (subAdmin.collegeId.isNotEmpty()) com.example.data.model.College(subAdmin.collegeId, subAdmin.collegeName) else null) }
    var showCollegeSelector by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(subAdmin.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "HOT-SWAP CONFIGURE PARAMS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SciFiPrimary,
                fontSize = 15.sp
            )
        },
        containerColor = SciFiSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_sub_admin_name_field")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCollegeSelector = true }
                ) {
                    OutlinedTextField(
                        value = selectedCollege?.name ?: "REASSIGN CAMPUS SEGMENT...",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Assigned College Campus", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledBorderColor = Color.White.copy(alpha = 0.15f),
                            disabledLabelColor = Color.White.copy(alpha = 0.5f),
                            disabledTrailingIconColor = SciFiPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("college_dropdown_trigger"),
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = SciFiPrimary)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HOT-LINE ROUTING GATEWAY", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SciFiGreen,
                            checkedTrackColor = SciFiGreen.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCollege == null) {
                        Toast.makeText(com.google.firebase.FirebaseApp.getInstance().applicationContext, "Gateway Blocked: Campus is not selected!", Toast.LENGTH_SHORT).show()
                    } else if (name.isNotEmpty()) {
                        onConfirm(
                            subAdmin.copy(
                                name = name,
                                collegeId = selectedCollege!!.collegeId,
                                collegeName = selectedCollege!!.name,
                                isActive = isActive
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("edit_sub_admin_confirm")
            ) {
                Text("APPLY PARAMS", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DISCARD", color = SciFiSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    )

    if (showCollegeSelector) {
        CollegeSelectionDialog(
            onDismiss = { showCollegeSelector = false },
            onSelect = { col ->
                selectedCollege = col
                showCollegeSelector = false
            }
        )
    }
}

// ==========================================
// SUB-ADMIN MAIN REALTIME DRIVEN TERMINAL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAdminDashboardScreen(
    viewModel: AdminViewModel,
    assignedCollegeId: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val maintenanceEnabled by com.example.data.manager.AppControlManager.maintenanceEnabled.collectAsState()
    val maintenanceTitle by com.example.data.manager.AppControlManager.maintenanceTitle.collectAsState()
    val maintenanceReason by com.example.data.manager.AppControlManager.reason.collectAsState()

    var ordersList by remember { mutableStateOf<List<com.example.data.model.OrderEntity>>(emptyList()) }
    var selectedOrderForDetails by remember { mutableStateOf<com.example.data.model.OrderEntity?>(null) }
    var collegeName by remember { mutableStateOf("CAMPUS OUTPOST") }
    var showAnalytics by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val firestore = FirebaseFirestore.getInstance()
    
    fun refreshData() {
        if (assignedCollegeId.isNotEmpty()) {
            firestore.collection("orders")
                .whereEqualTo("selectedCollegeId", assignedCollegeId)
                .get()
                .addOnSuccessListener { snapshot ->
                    ordersList = snapshot.documents.map { com.example.data.model.OrderEntity.fromMap(it.data) }
                        .sortedByDescending { it.createdAt }
                }
                .addOnFailureListener {
                    firestore.collection("orders")
                        .whereEqualTo("collegeId", assignedCollegeId)
                        .get()
                        .addOnSuccessListener { snapshot2 ->
                            ordersList = snapshot2.documents.map { com.example.data.model.OrderEntity.fromMap(it.data) }
                                .sortedByDescending { it.createdAt }
                        }
                }
        }
    }

    LaunchedEffect(assignedCollegeId) {
        if (assignedCollegeId.isNotEmpty()) {
            refreshData()
            
            // Listen real-time
            val registration = firestore.collection("orders")
                .whereEqualTo("selectedCollegeId", assignedCollegeId)
                .addSnapshotListener { snapshot, err ->
                    if (err != null) return@addSnapshotListener
                    if (snapshot != null) {
                        ordersList = snapshot.documents.map { com.example.data.model.OrderEntity.fromMap(it.data) }
                            .sortedByDescending { it.createdAt }
                    }
                }
            
            firestore.collection("colleges").document(assignedCollegeId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    collegeName = doc.getString("name") ?: "CAMPUS OUTPOST"
                }
            }
        }
    }

    if (showAnalytics) {
        OrderAnalyticsScreen(
            assignedCollegeId = assignedCollegeId,
            isSuperAdmin = false,
            onDismiss = { showAnalytics = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "LOOTRA OUTPOST TERMINAL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                fontSize = 15.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SciFiPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = collegeName.uppercase(),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SciFiPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SciFiSurface,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(
                            onClick = { showAnalytics = true },
                            modifier = Modifier.testTag("sub_admin_analytics_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Analytics, contentDescription = "Order Analytics", tint = SciFiPrimary)
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sub_admin_signout")
                        ) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log Out", tint = SciFiSecondary)
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SciFiBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (maintenanceEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SciFiSecondary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SciFiSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .testTag("subadmin_maintenance_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = SciFiSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "EMERGENCY SYSTEM MAINTENANCE ACTIVE",
                                color = SciFiSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Title: $maintenanceTitle",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (maintenanceReason.isNotEmpty()) {
                                Text(
                                    text = "Reason: $maintenanceReason",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, SciFiPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val pendingCount = ordersList.count { it.orderStatus.lowercase() == "pending" }
                    val activeCount = ordersList.count { it.orderStatus.lowercase() in listOf("pending", "accepted", "ready for pickup") }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PENDING", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$pendingCount", color = SciFiOrange, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ACTIVE JOBS", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$activeCount", color = SciFiPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL CARGO", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("${ordersList.size}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Text(
                "AUTHENTICATED ORDER PIPELINE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )

            // Real-time animated search bar
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val borderThickness by animateDpAsState(
                targetValue = if (isFocused) 1.5.dp else 1.dp,
                animationSpec = tween(durationMillis = 250)
            )
            val borderColor by animateColorAsState(
                targetValue = if (isFocused) SciFiPrimary else Color.White.copy(alpha = 0.15f),
                animationSpec = tween(durationMillis = 250)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by Order ID",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_search_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = if (isFocused) SciFiPrimary else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = SciFiSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = SciFiSurface,
                    unfocusedContainerColor = SciFiSurface.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                interactionSource = interactionSource
            )

            // Future-ready filter architecture
            val filterCriteria = remember(searchQuery) {
                object {
                    val query = searchQuery
                    fun matches(order: com.example.data.model.OrderEntity): Boolean {
                        if (query.isBlank()) return true
                        val lowerQuery = query.lowercase().trim()
                        // Currently filters by Order ID (Full/Partial match, case insensitive)
                        // This can easily be extended to support filters like Customer Name, Phone, Status etc.
                        return order.orderId.lowercase().contains(lowerQuery)
                    }
                }
            }

            val filteredOrders = remember(filterCriteria, ordersList) {
                ordersList.filter { filterCriteria.matches(it) }
            }

            if (ordersList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NO REGISTERED SHIPMENTS FOR THIS STATION",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (filteredOrders.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .border(1.dp, SciFiSecondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .testTag("search_no_results_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(SciFiSecondary.copy(alpha = 0.12f), CircleShape)
                                .border(1.dp, SciFiSecondary.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "No Results",
                                tint = SciFiSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "NO MATCHING ORDER FOUND",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "No cargo matches: \"$searchQuery\"\nPlease check the ID or search query and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = { searchQuery = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SciFiSecondary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .testTag("btn_clear_search")
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "CLEAR SEARCH",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders) { order ->
                        SubAdminOrderRowCard(
                            order = order,
                            onClick = { selectedOrderForDetails = order }
                        )
                    }
                }
            }
        }
    }
    }

    if (selectedOrderForDetails != null) {
        val order = selectedOrderForDetails!!
        SubAdminOrderStatusUpdateDialog(
            order = order,
            onDismiss = { selectedOrderForDetails = null },
            onUpdateStatus = { newStatus ->
                val updateMap = mapOf(
                    "orderStatus" to newStatus,
                    "assignedSubAdmin" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "")
                )
                firestore.collection("orders").document(order.orderId).update(updateMap)
                    .addOnSuccessListener {
                        selectedOrderForDetails = null
                        refreshData()
                        Toast.makeText(context, "Cargo routing pipeline updated to: $newStatus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Hotline transmission error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        )
    }
}

@Composable
fun SubAdminOrderRowCard(
    order: com.example.data.model.OrderEntity,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ORDER #${order.orderId.takeLast(6).uppercase()}",
                        color = SciFiPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(order.createdAt)),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.productName.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SciFiOrange, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.pickupLocation.uppercase().ifEmpty { "MAIN DROPPOINT" },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = when (order.orderStatus.lowercase()) {
                            "pending" -> SciFiOrange.copy(alpha = 0.1f)
                            "accepted" -> SciFiPrimary.copy(alpha = 0.1f)
                            "ready for pickup" -> SciFiGreen.copy(alpha = 0.12f)
                            "delivered" -> SciFiGreen.copy(alpha = 0.15f)
                            else -> SciFiSecondary.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = when (order.orderStatus.lowercase()) {
                            "pending" -> SciFiOrange
                            "accepted" -> SciFiPrimary
                            "ready for pickup" -> SciFiGreen
                            "delivered" -> SciFiGreen
                            else -> SciFiSecondary
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = order.orderStatus.uppercase(),
                    color = when (order.orderStatus.lowercase()) {
                        "pending" -> SciFiOrange
                        "accepted" -> SciFiPrimary
                        "ready for pickup" -> SciFiGreen
                        "delivered" -> SciFiGreen
                        else -> SciFiSecondary
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SubAdminOrderStatusUpdateDialog(
    order: com.example.data.model.OrderEntity,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "DISPATCH CONTROL GATEWAY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SciFiPrimary,
                    fontSize = 14.sp
                )
                Text(
                    "ID: #${order.orderId.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        },
        containerColor = SciFiSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ITEM: ${order.productName.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "STATION: ${order.pickupLocation.uppercase().ifEmpty { "MAIN HUB" }}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "HOT-SWAP STATUS STATE:",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                val statuses = listOf("Pending", "Accepted", "Ready for Pickup", "Delivered", "Cancelled")
                
                statuses.forEach { statusText ->
                    val isCurrent = order.orderStatus == statusText
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) SciFiPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateStatus(statusText) }
                            .border(
                                0.5.dp, 
                                if (isCurrent) SciFiPrimary else Color.White.copy(alpha = 0.05f), 
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusText.uppercase(),
                                color = if (isCurrent) SciFiPrimary else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SciFiPrimary)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE CHANNEL", color = SciFiSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    )
}

// ==========================================
// ORDER ANALYTICS SYSTEM TERMINAL
// ==========================================
@Composable
fun FilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag("filter_dropdown_${label.lowercase()}"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SciFiPrimary,
                containerColor = SciFiSurface
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            border = BorderStroke(0.5.dp, SciFiPrimary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: $selectedValue",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SciFiPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SciFiPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SciFiSurface)
                .border(0.5.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class OptionCollege(val id: String, val name: String)

@Composable
fun CollegeFilterDropdown(
    selectedValue: String,
    options: List<OptionCollege>,
    onSelect: (OptionCollege) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag("filter_dropdown_campus"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SciFiPrimary,
                containerColor = SciFiSurface
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            border = BorderStroke(0.5.dp, SciFiPrimary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAMPUS: ${selectedValue.uppercase()}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SciFiPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SciFiPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SciFiSurface)
                .border(0.5.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt.name.uppercase(),
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DailyOrdersChart(dailyCounts: IntArray, selectedMonthName: String) {
    val maxCount = dailyCounts.maxOrNull() ?: 1
    val displayMax = if (maxCount == 0) 5 else maxCount
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DAILY ORDERS TIMELINE - $selectedMonthName",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SciFiPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyCounts.forEachIndexed { index, count ->
                        val barHeightFactor = count.toFloat() / displayMax.toFloat()
                        val animatedFactor = remember(count) { Animatable(0f) }
                        LaunchedEffect(count) {
                            animatedFactor.animateTo(
                                targetValue = barHeightFactor,
                                animationSpec = tween(durationMillis = 800, delayMillis = index * 10)
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (count > 0) {
                                Text(
                                    text = "$count",
                                    color = SciFiPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(fraction = animatedFactor.value.coerceIn(0.01f, 1f))
                                    .fillMaxWidth(fraction = 0.7f)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(SciFiPrimary, SciFiPrimary.copy(alpha = 0.15f))
                                        ),
                                        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                    )
                                    .border(0.5.dp, SciFiPrimary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Day 1", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("Day ${dailyCounts.size / 2}", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("Day ${dailyCounts.size}", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun OrderStatusAllocationPie(pending: Int, processing: Int, delivered: Int, cancelled: Int) {
    val total = pending + processing + delivered + cancelled
    val safeTotal = if (total == 0) 1 else total
    
    val pendingPct = pending.toFloat() / safeTotal
    val processingPct = processing.toFloat() / safeTotal
    val deliveredPct = delivered.toFloat() / safeTotal
    val cancelledPct = cancelled.toFloat() / safeTotal
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SciFiAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CARGO STATUS CLASSIFICATION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SciFiAccent,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        var startAngle = -90f
                        
                        val delSweep = deliveredPct * 360f
                        if (delSweep > 0) {
                            drawArc(
                                color = SciFiGreen,
                                startAngle = startAngle,
                                sweepAngle = delSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += delSweep
                        }
                        
                        val procSweep = processingPct * 360f
                        if (procSweep > 0) {
                            drawArc(
                                color = SciFiPrimary,
                                startAngle = startAngle,
                                sweepAngle = procSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += procSweep
                        }
                        
                        val pendSweep = pendingPct * 360f
                        if (pendSweep > 0) {
                            drawArc(
                                color = SciFiOrange,
                                startAngle = startAngle,
                                sweepAngle = pendSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += pendSweep
                        }
                        
                        val cancSweep = cancelledPct * 360f
                        if (cancSweep > 0) {
                            drawArc(
                                color = SciFiSecondary,
                                startAngle = startAngle,
                                sweepAngle = cancSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (total > 0) "${(deliveredPct * 100).toInt()}%" else "0%",
                            color = SciFiGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DELIVERED",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusRowItem("PENDING", pending, pendingPct, SciFiOrange)
                    StatusRowItem("PROCESSING", processing, processingPct, SciFiPrimary)
                    StatusRowItem("DELIVERED", delivered, deliveredPct, SciFiGreen)
                    StatusRowItem("CANCELLED", cancelled, cancelledPct, SciFiSecondary)
                }
            }
        }
    }
}

@Composable
fun StatusRowItem(label: String, count: Int, pct: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "$count (${(pct * 100).toInt()}%)",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TopSellersCard(productCounts: List<Pair<String, Int>>) {
    val displayMax = productCounts.firstOrNull()?.second ?: 1
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SciFiOrange.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TOP SELLING PRODUCTS - CAMPUS DEMAND",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SciFiOrange,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (productCounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NO PRODUCTS IN THE FEEDBACK LOOP", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    productCounts.take(5).forEachIndexed { index, (productName, count) ->
                        val ratio = count.toFloat() / displayMax.toFloat()
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = SciFiOrange,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = productName.uppercase(),
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "$count UNITS",
                                    color = SciFiPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = ratio)
                                        .fillMaxHeight()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(SciFiOrange, SciFiPrimary)
                                            ),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .border(0.5.dp, SciFiPrimary.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
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
fun AnalyticsStatCard(
    title: String,
    value: String,
    unit: String = "",
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit.uppercase(),
                        color = color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderAnalyticsScreen(
    assignedCollegeId: String?,
    isSuperAdmin: Boolean,
    onDismiss: () -> Unit
) {
    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    val yearsList = remember { (2024..2036).map { it.toString() } }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonthIndex by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) }
    
    var selectedCollegeItem by remember { mutableStateOf<OptionCollege?>(OptionCollege("ALL", "ALL CAMPUSES")) }
    
    var collegeOptions by remember { mutableStateOf<List<OptionCollege>>(emptyList()) }
    var ordersState by remember { mutableStateOf<List<com.example.data.model.OrderEntity>>(emptyList()) }
    var isLoadingOrders by remember { mutableStateOf(true) }
    var errorLoadingOrders by remember { mutableStateOf<String?>(null) }
    
    val firestore = FirebaseFirestore.getInstance()
    
    LaunchedEffect(isSuperAdmin) {
        if (isSuperAdmin) {
            firestore.collection("colleges").get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.map {
                        OptionCollege(it.id, it.getString("name") ?: "Unnamed Campus")
                    }
                    collegeOptions = listOf(OptionCollege("ALL", "ALL CAMPUSES")) + list
                }
        }
    }
    
    fun loadOrders() {
        isLoadingOrders = true
        errorLoadingOrders = null
        
        val query = if (isSuperAdmin) {
            val sel = selectedCollegeItem?.id ?: "ALL"
            if (sel == "ALL") {
                firestore.collection("orders")
            } else {
                firestore.collection("orders").whereEqualTo("selectedCollegeId", sel)
            }
        } else {
            if (!assignedCollegeId.isNullOrEmpty()) {
                firestore.collection("orders").whereEqualTo("selectedCollegeId", assignedCollegeId)
            } else {
                firestore.collection("orders")
            }
        }
        
        query.get()
            .addOnSuccessListener { snapshot ->
                var list = snapshot.documents.map { com.example.data.model.OrderEntity.fromMap(it.data) }
                
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.YEAR, selectedYear)
                calendar.set(java.util.Calendar.MONTH, selectedMonthIndex)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startMs = calendar.timeInMillis
                
                calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                calendar.set(java.util.Calendar.MINUTE, 59)
                calendar.set(java.util.Calendar.SECOND, 59)
                calendar.set(java.util.Calendar.MILLISECOND, 999)
                val endMs = calendar.timeInMillis
                
                list = list.filter { it.createdAt in startMs..endMs }
                ordersState = list
                isLoadingOrders = false
            }
            .addOnFailureListener { exception ->
                errorLoadingOrders = exception.message ?: "Failed metrics stream transmission"
                isLoadingOrders = false
            }
    }
    
    LaunchedEffect(selectedYear, selectedMonthIndex, selectedCollegeItem, assignedCollegeId) {
        loadOrders()
    }
    
    val monthNames = remember {
        listOf(
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
        )
    }
    val selectedMonthName = monthNames.getOrNull(selectedMonthIndex) ?: "UNKNOWN"
    
    val totalOrdersCount = ordersState.size
    val totalCoinsEconomy = ordersState.sumOf { it.coinPrice }
    
    var pendingCount = 0
    var processingCount = 0
    var ordersDelivered = 0
    var cancelledCount = 0
    ordersState.forEach { o ->
        when (o.orderStatus.lowercase()) {
            "pending" -> pendingCount++
            "delivered" -> ordersDelivered++
            "cancelled" -> cancelledCount++
            else -> processingCount++
        }
    }
    
    val productCounts = ordersState.groupBy { it.productName }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    
    val maxDays = remember(selectedYear, selectedMonthIndex) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.YEAR, selectedYear)
        calendar.set(java.util.Calendar.MONTH, selectedMonthIndex)
        calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }
    
    val dailyCounts = remember(ordersState, maxDays) {
        val counts = IntArray(maxDays) { 0 }
        ordersState.forEach { o ->
            val oCal = java.util.Calendar.getInstance()
            oCal.timeInMillis = o.createdAt
            val day = oCal.get(java.util.Calendar.DAY_OF_MONTH)
            if (day in 1..maxDays) {
                counts[day - 1]++
            }
        }
        counts
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SciFiBg)
            .padding(top = 16.dp)
            .testTag("order_analytics_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Return",
                            tint = SciFiPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "CHRONO ORDER ANALYTICS CENTER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "QUANTUM TEMPORAL METRICS PIPELINE",
                            fontSize = 9.sp,
                            color = SciFiPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(onClick = { loadOrders() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Signals",
                        tint = SciFiPrimary
                    )
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, SciFiPrimary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterDropdown(
                            label = "YEAR",
                            selectedValue = "$selectedYear",
                            options = yearsList,
                            onSelect = { selectedYear = it.toInt() },
                            modifier = Modifier.weight(1f)
                        )
                        FilterDropdown(
                            label = "MONTH",
                            selectedValue = selectedMonthName,
                            options = monthNames,
                            onSelect = { selectedMonthIndex = monthNames.indexOf(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (isSuperAdmin) {
                        CollegeFilterDropdown(
                            selectedValue = selectedCollegeItem?.name ?: "ALL CAMPUSES",
                            options = collegeOptions,
                            onSelect = { selectedCollegeItem = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SciFiBg, RoundedCornerShape(6.dp))
                                .border(0.5.dp, SciFiPrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(5.dp).background(SciFiGreen, CircleShape))
                                Text(
                                    text = "MONITORING SEGMENT ID: ${assignedCollegeId?.take(8)?.uppercase() ?: "UNKNOWN"}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingOrders) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = SciFiPrimary, modifier = Modifier.size(24.dp))
                        Text(
                            text = "SYNCING CHRONO METRICS...",
                            color = SciFiPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (errorLoadingOrders != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = SciFiSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "METRICS ENGINE FAULT: $errorLoadingOrders",
                            color = SciFiSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { loadOrders() },
                            colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RE-CONNECT TRANSMITTER", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                } else if (ordersState.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No data available",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No cargo operations registered on collegeId segment in $selectedMonthName $selectedYear.",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, SciFiPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "TEMPORAL CARGO DISPATCH CAPACITY",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$selectedMonthName -> $totalOrdersCount ORDERS",
                                        color = SciFiPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnalyticsStatCard(
                                    title = "TOTAL TRANSACTION VOLUME",
                                    value = "$totalCoinsEconomy",
                                    unit = "COINS",
                                    icon = Icons.Default.MonetizationOn,
                                    color = SciFiPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsStatCard(
                                    title = "DELIVERED SYSTEMS",
                                    value = "$ordersDelivered",
                                    unit = "UNITS",
                                    icon = Icons.Default.CheckCircle,
                                    color = SciFiGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        item {
                            DailyOrdersChart(
                                dailyCounts = dailyCounts,
                                selectedMonthName = selectedMonthName
                            )
                        }
                        
                        item {
                            OrderStatusAllocationPie(
                                pending = pendingCount,
                                processing = processingCount,
                                delivered = ordersDelivered,
                                cancelled = cancelledCount
                            )
                        }
                        
                        item {
                            TopSellersCard(
                                productCounts = productCounts
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsTabScreen(viewModel: AdminViewModel) {
    val reels by viewModel.adminReels.collectAsState()
    val searchQuery by viewModel.reelSearchQuery.collectAsState()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var editingReel by remember { mutableStateOf<com.example.data.model.ReelEntity?>(null) }

    // Dialog form state variables
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var publisherName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var mapsLink by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedLocations by remember { mutableStateOf(setOf<String>()) }
    var rewardCoins by remember { mutableStateOf("5") }
    var maximumViews by remember { mutableStateOf("10000") }
    var expiryDays by remember { mutableStateOf("30") }
    var status by remember { mutableStateOf("Active") }

    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedThumbnailUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var uploadStatusMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedVideoUri = uri
    }

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedThumbnailUri = uri
    }

    var colleges by remember { mutableStateOf<List<com.example.data.model.College>>(emptyList()) }
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val registration = db.collection("colleges").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                colleges = snapshot.documents.mapNotNull { com.example.data.model.College.fromMap(it.data) }
            }
        }
        onDispose {
            registration.remove()
        }
    }

    val resetForm = {
        title = ""
        description = ""
        publisherName = ""
        contactPerson = ""
        phone = ""
        whatsapp = ""
        email = ""
        website = ""
        mapsLink = ""
        thumbnailUrl = ""
        videoUrl = ""
        location = ""
        selectedLocations = emptySet()
        rewardCoins = "5"
        maximumViews = "10000"
        expiryDays = "30"
        status = "Active"
        editingReel = null
        selectedVideoUri = null
        selectedThumbnailUri = null
        uploadStatusMessage = ""
        isUploading = false
    }

    val fillForm = { reel: com.example.data.model.ReelEntity ->
        editingReel = reel
        title = reel.title
        description = reel.description
        publisherName = reel.publisherName
        contactPerson = reel.contactPerson
        phone = reel.phone
        whatsapp = reel.whatsapp
        email = reel.email
        website = reel.website
        mapsLink = reel.mapsLink
        thumbnailUrl = reel.thumbnailUrl
        videoUrl = reel.videoUrl
        location = reel.location
        selectedLocations = if (reel.location.isBlank()) emptySet() else reel.location.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        rewardCoins = (if (reel.rewardPerUser > 0) reel.rewardPerUser else reel.rewardCoins).toString()
        maximumViews = (if (reel.campaignCoins > 0) reel.campaignCoins else reel.maximumViews).toString()
        val daysLeft = ((reel.expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
        expiryDays = daysLeft.toString()
        status = reel.status
        selectedVideoUri = null
        selectedThumbnailUri = null
        uploadStatusMessage = ""
        isUploading = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm()
                    showDialog = true
                },
                containerColor = SciFiPrimary,
                contentColor = SciFiBg,
                modifier = Modifier.testTag("admin_reels_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reel")
            }
        },
        containerColor = SciFiBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header / Title & Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REELS CONTROLLER",
                        color = SciFiPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "MANAGE REELS, REWARDS & REACH",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Analytics Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Reels
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                    border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOTAL REELS",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${reels.size}",
                            color = SciFiPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Total Distributed
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                    border = BorderStroke(1.dp, SciFiAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOTAL DISTRIBUTED",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${reels.sumOf { it.distributedCoins }}",
                            color = SciFiAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Total Likes
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                    border = BorderStroke(1.dp, SciFiGreen.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOTAL LIKES",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${reels.sumOf { it.likes }}",
                            color = SciFiGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setReelSearchQuery(it) },
                label = { Text("Search title, publisher, or location", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_reels_search_input"),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SciFiPrimary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SciFiPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = SciFiPrimary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List of Reels
            if (reels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO REELS FOUND",
                        color = Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reels) { reel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_reel_card_${reel.reelId}"),
                            colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                            border = BorderStroke(1.dp, if (reel.status == "Active") SciFiPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reel.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "By ${reel.publisherName} • Location: ${reel.location}",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("${reel.rewardCoins} Coins", fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    labelColor = SciFiPrimary,
                                                    containerColor = SciFiPrimary.copy(alpha = 0.1f)
                                                ),
                                                border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.3f))
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(reel.status, fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    labelColor = if (reel.status == "Active") SciFiGreen else SciFiSecondary,
                                                    containerColor = (if (reel.status == "Active") SciFiGreen else SciFiSecondary).copy(alpha = 0.1f)
                                                ),
                                                border = BorderStroke(1.dp, (if (reel.status == "Active") SciFiGreen else SciFiSecondary).copy(alpha = 0.3f))
                                            )
                                        }
                                    }

                                    // Display direct Thumbnail or generic icon
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = reel.thumbnailUrl.ifBlank { "https://picsum.photos/seed/${reel.reelId}/100/100" }
                                            ),
                                            contentDescription = "Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Campaign progress stats
                                    val campaignCoins = if (reel.campaignCoins > 0) reel.campaignCoins else 1000
                                    val distributed = reel.distributedCoins
                                    Text(
                                        text = "Campaign: $distributed/$campaignCoins Coins • Likes: ${reel.likes}",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Action buttons row
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Status Toggle button
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleReelStatus(reel.reelId, reel.status) { success ->
                                                    val msg = if (success) "Reel status updated" else "Update failed"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (reel.status == "Active") Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Status",
                                                tint = if (reel.status == "Active") SciFiGreen else Color.LightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                fillForm(reel)
                                                showDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Reel",
                                                tint = SciFiPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteReel(reel.reelId) { success ->
                                                    val msg = if (success) "Reel deleted" else "Delete failed"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Reel",
                                                tint = SciFiSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Reel Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                resetForm()
            },
            title = {
                Text(
                    text = if (editingReel == null) "UPLOAD REEL" else "EDIT REEL",
                    color = SciFiPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            containerColor = SciFiSurface,
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_title_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_description_input"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = publisherName,
                            onValueChange = { publisherName = it },
                            label = { Text("Publisher Name") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_publisher_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Contact Person") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_contact_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_phone_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp Number") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_whatsapp_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_email_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Website Link") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_website_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = mapsLink,
                            onValueChange = { mapsLink = it },
                            label = { Text("Google Maps Link") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_maps_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Reel Video File",
                                color = SciFiPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isUploading) {
                                        videoPickerLauncher.launch("video/*")
                                    },
                                colors = CardDefaults.cardColors(containerColor = SciFiBg),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedVideoUri != null) SciFiGreen else Color.White.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = if (selectedVideoUri != null) SciFiGreen else SciFiPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (selectedVideoUri != null) "Video Selected" else "Select Video File",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (selectedVideoUri != null) {
                                                    selectedVideoUri?.lastPathSegment ?: "video.mp4"
                                                } else if (editingReel != null && videoUrl.isNotEmpty()) {
                                                    "Using uploaded video: $videoUrl"
                                                } else {
                                                    "No file selected (Required)"
                                                },
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (selectedVideoUri != null || (editingReel != null && videoUrl.isNotEmpty())) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Ready",
                                            tint = SciFiGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Reel Thumbnail Image",
                                color = SciFiPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isUploading) {
                                        thumbnailPickerLauncher.launch("image/*")
                                    },
                                colors = CardDefaults.cardColors(containerColor = SciFiBg),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedThumbnailUri != null) SciFiGreen else Color.White.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = if (selectedThumbnailUri != null) SciFiGreen else SciFiPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (selectedThumbnailUri != null) "Thumbnail Selected" else "Select Thumbnail Image",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (selectedThumbnailUri != null) {
                                                    selectedThumbnailUri?.lastPathSegment ?: "thumbnail.jpg"
                                                } else if (editingReel != null && thumbnailUrl.isNotEmpty()) {
                                                    "Using uploaded thumbnail: $thumbnailUrl"
                                                } else {
                                                    "No file selected (Required)"
                                                },
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (selectedThumbnailUri != null || (editingReel != null && thumbnailUrl.isNotEmpty())) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Ready",
                                            tint = SciFiGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isUploading) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    color = SciFiPrimary,
                                    trackColor = SciFiBg,
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uploadStatusMessage,
                                    color = SciFiPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Location dropdown simulation or text input
                    item {
                        Column {
                            Text("Location Selection (Select Multiple)", color = SciFiPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (colleges.isEmpty()) {
                                Text("No locations active. Please add locations in the management panel.", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colleges.forEach { col ->
                                        val isSelected = selectedLocations.contains(col.name)
                                        Button(
                                            onClick = {
                                                selectedLocations = if (isSelected) {
                                                    selectedLocations - col.name
                                                } else {
                                                    selectedLocations + col.name
                                                }
                                                location = selectedLocations.joinToString(",")
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) SciFiPrimary else SciFiBg,
                                                contentColor = if (isSelected) SciFiBg else Color.White
                                            ),
                                            border = BorderStroke(1.dp, if (isSelected) SciFiPrimary else Color.White.copy(alpha = 0.3f)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = SciFiBg,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(col.name, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = rewardCoins,
                            onValueChange = { rewardCoins = it },
                            label = { Text("Reward Coins Per User (rewardPerUser)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_reward_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = maximumViews,
                            onValueChange = { maximumViews = it },
                            label = { Text("Campaign Coins Budget (campaignCoins)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_views_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = expiryDays,
                            onValueChange = { expiryDays = it },
                            label = { Text("Expiry (Days from now)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_reels_expiry_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White)
                        )
                    }

                    item {
                        Column {
                            Text("Reel Status", color = SciFiPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { status = "Active" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (status == "Active") SciFiGreen else SciFiBg
                                    ),
                                    border = BorderStroke(1.dp, if (status == "Active") SciFiGreen else Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Active", fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = { status = "Disabled" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (status == "Disabled") SciFiSecondary else SciFiBg
                                    ),
                                    border = BorderStroke(1.dp, if (status == "Disabled") SciFiSecondary else Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Disabled", fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || publisherName.isBlank()) {
                            Toast.makeText(context, "Title and Publisher Name are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (editingReel == null) {
                            if (selectedVideoUri == null) {
                                Toast.makeText(context, "Please select a video file first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedThumbnailUri == null) {
                                Toast.makeText(context, "Please select a thumbnail image first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }

                        val coins = rewardCoins.toIntOrNull() ?: 5
                        val maxViews = maximumViews.toIntOrNull() ?: 10000
                        val days = expiryDays.toIntOrNull() ?: 30
                        val expiryTime = System.currentTimeMillis() + (days * 24L * 60 * 60 * 1000)

                        val id = editingReel?.reelId ?: "reel_${UUID.randomUUID().toString().take(6)}"

                        isUploading = true
                        uploadStatusMessage = "Starting upload..."

                        viewModel.uploadFilesAndSaveReel(
                            reelId = id,
                            videoUri = selectedVideoUri,
                            thumbnailUri = selectedThumbnailUri,
                            onProgress = { msg ->
                                uploadStatusMessage = msg
                            },
                            onFinished = { success, errorMsg ->
                                isUploading = false
                                if (success) {
                                    Toast.makeText(context, if (editingReel == null) "Reel Created Successfully" else "Reel Updated Successfully", Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                    resetForm()
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Failed to save reel", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) { finalVideoUrl, finalThumbnailUrl ->
                            val vUrl = if (finalVideoUrl.isNotEmpty()) finalVideoUrl else videoUrl
                            val tUrl = if (finalThumbnailUrl.isNotEmpty()) finalThumbnailUrl else thumbnailUrl

                            com.example.data.model.ReelEntity(
                                reelId = id,
                                title = title,
                                description = description,
                                publisherName = publisherName,
                                contactPerson = contactPerson,
                                phone = phone,
                                whatsapp = whatsapp,
                                email = email,
                                website = website,
                                mapsLink = mapsLink,
                                thumbnailUrl = tUrl,
                                videoUrl = vUrl,
                                location = location,
                                rewardCoins = coins,
                                duration = 10,
                                views = editingReel?.distributedCoins ?: 0,
                                likes = editingReel?.likes ?: 0,
                                maximumViews = maxViews,
                                expiryDate = expiryTime,
                                status = status,
                                createdBy = "Admin",
                                createdAt = editingReel?.createdAt ?: System.currentTimeMillis(),
                                campaignCoins = maxViews,
                                distributedCoins = editingReel?.distributedCoins ?: 0,
                                rewardPerUser = coins
                            )
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary, contentColor = SciFiBg)
                ) {
                    Text("SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                        resetForm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                ) {
                    Text("CANCEL", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun WellbeingTvSettingsTabScreen(viewModel: AdminViewModel) {
    val context = LocalContext.current
    var adsRequired by remember { mutableStateOf("2") }
    var coinsReward by remember { mutableStateOf("5") }
    var dailyRewardLimit by remember { mutableStateOf("30") }
    var dailyCoinLimit by remember { mutableStateOf("150") }
    var rewardEnabled by remember { mutableStateOf(true) }
    var updatedAt by remember { mutableStateOf(0L) }
    var updatedBy by remember { mutableStateOf("Admin") }
    var isSaving by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("settings").document("wellbeing_tv")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminScreens", "Error loading Wellbeing TV settings", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                adsRequired = (snapshot.getLong("adsRequired") ?: 2).toString()
                coinsReward = (snapshot.getLong("coinsReward") ?: 5).toString()
                dailyRewardLimit = (snapshot.getLong("dailyRewardLimit") ?: 30).toString()
                dailyCoinLimit = (snapshot.getLong("dailyCoinLimit") ?: 150).toString()
                rewardEnabled = snapshot.getBoolean("rewardEnabled") ?: true
                updatedAt = snapshot.getLong("updatedAt") ?: 0L
                updatedBy = snapshot.getString("updatedBy") ?: "Admin"
            }
        }
        onDispose {
            registration.remove()
        }
    }

    fun onSaveConfig() {
        val ads = adsRequired.toIntOrNull()
        val coins = coinsReward.toIntOrNull()
        val rewardLimit = dailyRewardLimit.toIntOrNull()
        val coinLimit = dailyCoinLimit.toIntOrNull()

        if (ads == null || ads <= 0) {
            Toast.makeText(context, "Please enter a valid positive number for Ads Required", Toast.LENGTH_SHORT).show()
        } else if (coins == null || coins <= 0) {
            Toast.makeText(context, "Please enter a valid positive number for Coins Rewarded", Toast.LENGTH_SHORT).show()
        } else if (rewardLimit == null || rewardLimit <= 0) {
            Toast.makeText(context, "Please enter a valid positive number for Daily Reward Limit", Toast.LENGTH_SHORT).show()
        } else if (coinLimit == null || coinLimit <= 0) {
            Toast.makeText(context, "Please enter a valid positive number for Daily Coin Limit", Toast.LENGTH_SHORT).show()
        } else {
            isSaving = true
            val db = FirebaseFirestore.getInstance()
            val docPath = "settings/wellbeing_tv"
            val savedValues = mapOf(
                "adsRequired" to ads,
                "coinsReward" to coins,
                "dailyRewardLimit" to rewardLimit,
                "dailyCoinLimit" to coinLimit,
                "rewardEnabled" to rewardEnabled,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "Super Admin"
            )
            Log.d("AdminScreens", "[DEBUG-ADMIN] Attempting to save configuration to Firestore path: $docPath")
            Log.d("AdminScreens", "[DEBUG-ADMIN] Saved values: $savedValues")
            
            db.collection("settings").document("wellbeing_tv")
                .set(savedValues)
                .addOnSuccessListener {
                    isSaving = false
                    Log.d("AdminScreens", "Configuration Saved successfully!")
                    Log.d("AdminScreens", "Firestore Path: $docPath")
                    Log.d("AdminScreens", "Ads Required: $ads")
                    Log.d("AdminScreens", "Coins Reward: $coins")
                    Toast.makeText(context, "Configuration updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    isSaving = false
                    Log.e("AdminScreens", "[DEBUG-ADMIN] Failed to save configuration to path $docPath", e)
                    Toast.makeText(context, "Failed to update configuration: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SciFiPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = SciFiPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "WELLBEING TV CONFIGURATION",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "MANAGE REWARD METRICS & LIMITS DYNAMICALLY",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "REWARD CONTROLS",
                        color = SciFiPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Reward Enabled Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reward Claiming Enabled",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Enable or disable users from claiming Wellbeing TV screen-time coins entirely.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = rewardEnabled,
                            onCheckedChange = { rewardEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SciFiPrimary,
                                checkedTrackColor = SciFiPrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                    // Ads Required Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Ads Required Per Target",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = adsRequired,
                            onValueChange = { adsRequired = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            placeholder = { Text("e.g. 2", color = Color.White.copy(alpha = 0.3f)) }
                        )
                        Text(
                            text = "Number of completed rewarded ads a user must watch to claim coins for each unlocked screen-time milestone.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                    // Coins Rewarded Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Coins Rewarded Per Claim",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = coinsReward,
                            onValueChange = { coinsReward = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            placeholder = { Text("e.g. 5", color = Color.White.copy(alpha = 0.3f)) }
                        )
                        Text(
                            text = "Number of coins credited to user's wallet for each completed screen-time milestone reward claim.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                    // Daily Max Rewards Limit Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Daily Maximum Rewards Claimable",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = dailyRewardLimit,
                            onValueChange = { dailyRewardLimit = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            placeholder = { Text("e.g. 30", color = Color.White.copy(alpha = 0.3f)) }
                        )
                        Text(
                            text = "Maximum number of screen-time reward targets a user can claim within a single calendar day.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                    // Daily Max Coins Limit Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Daily Maximum Coins Allowed",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = dailyCoinLimit,
                            onValueChange = { dailyCoinLimit = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            placeholder = { Text("e.g. 150", color = Color.White.copy(alpha = 0.3f)) }
                        )
                        Text(
                            text = "Maximum aggregate coins a user is allowed to earn from Wellbeing TV reward claims in a single day.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Metadata Info Row & Action Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SciFiSurface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LAST UPDATE METADATA",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Updated By: $updatedBy",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (updatedAt > 0) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val dateStr = sdf.format(java.util.Date(updatedAt))
                            Text(
                                text = "At: $dateStr",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onSaveConfig() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SciFiPrimary,
                    contentColor = SciFiBg,
                    disabledContainerColor = SciFiPrimary.copy(alpha = 0.3f)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = SciFiBg, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "SAVE CONFIGURATION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyControlTabScreen(currentUserUid: String) {
    val context = LocalContext.current
    var maintenanceEnabled by remember { mutableStateOf(false) }
    var maintenanceTitle by remember { mutableStateOf("System Under Maintenance") }
    var maintenanceMessage by remember { mutableStateOf("Lootra is temporarily unavailable.") }
    var estimatedEndTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var updatedAt by remember { mutableStateOf(0L) }
    var updatedBy by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isSuperAdmin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserUid) {
        if (currentUserUid.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("admins").document(currentUserUid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val role = doc.getString("role") ?: ""
                        isSuperAdmin = role == "super_admin"
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("settings").document("app_control")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminScreens", "Error loading App Control settings", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                maintenanceEnabled = snapshot.getBoolean("maintenanceEnabled") ?: false
                maintenanceTitle = snapshot.getString("maintenanceTitle") ?: "System Under Maintenance"
                maintenanceMessage = snapshot.getString("maintenanceMessage") ?: "Lootra is temporarily unavailable."
                estimatedEndTime = snapshot.getString("estimatedEndTime") ?: ""
                reason = snapshot.getString("reason") ?: ""
                updatedAt = snapshot.getLong("updatedAt") ?: 0L
                updatedBy = snapshot.getString("updatedBy") ?: ""
            }
        }
        onDispose {
            registration.remove()
        }
    }

    fun onSaveAppControl() {
        if (!isSuperAdmin) {
            Toast.makeText(context, "Permission Denied: Only Super Admins can write.", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        val db = FirebaseFirestore.getInstance()
        val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: currentUserUid
        
        db.collection("settings").document("app_control")
            .set(
                mapOf(
                    "maintenanceEnabled" to maintenanceEnabled,
                    "maintenanceTitle" to maintenanceTitle,
                    "maintenanceMessage" to maintenanceMessage,
                    "estimatedEndTime" to estimatedEndTime,
                    "reason" to reason,
                    "updatedAt" to System.currentTimeMillis(),
                    "updatedBy" to currentUserEmail
                )
            )
            .addOnSuccessListener {
                isSaving = false
                Toast.makeText(context, "Emergency Control updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Failed to update Emergency Control: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SciFiPrimary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("emergency_control_tab"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SciFiSecondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency Control",
                            tint = SciFiSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "EMERGENCY SYSTEM CONTROL",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Super Admin Maintenance Overrides",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (!isSuperAdmin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SciFiSecondary.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, SciFiSecondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = SciFiSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "READ-ONLY ACCESS: ONLY SUPER ADMINS CAN MODIFY EMERGENCY CONTROL.",
                                color = SciFiSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Maintenance Enabled Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Maintenance Mode",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Immediately lock the user application and show maintenance screen",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = maintenanceEnabled,
                                onCheckedChange = { maintenanceEnabled = it },
                                enabled = isSuperAdmin,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SciFiBg,
                                    checkedTrackColor = SciFiSecondary,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.05f)
                                ),
                                modifier = Modifier.testTag("maintenance_mode_switch")
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                        // Maintenance Title Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Maintenance Title",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = maintenanceTitle,
                                onValueChange = { maintenanceTitle = it },
                                enabled = isSuperAdmin,
                                modifier = Modifier.fillMaxWidth().testTag("maintenance_title_input"),
                                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SciFiSecondary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                placeholder = { Text("System Under Maintenance", color = Color.White.copy(alpha = 0.3f)) }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                        // Maintenance Message Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Maintenance Message",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = maintenanceMessage,
                                onValueChange = { maintenanceMessage = it },
                                enabled = isSuperAdmin,
                                modifier = Modifier.fillMaxWidth().testTag("maintenance_message_input"),
                                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SciFiSecondary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                placeholder = { Text("Lootra is temporarily unavailable.", color = Color.White.copy(alpha = 0.3f)) }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                        // Reason Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Reason",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                enabled = isSuperAdmin,
                                modifier = Modifier.fillMaxWidth().testTag("maintenance_reason_input"),
                                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SciFiSecondary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                placeholder = { Text("Security Maintenance", color = Color.White.copy(alpha = 0.3f)) }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                        // Estimated End Time Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Estimated End Time",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = estimatedEndTime,
                                onValueChange = { estimatedEndTime = it },
                                enabled = isSuperAdmin,
                                modifier = Modifier.fillMaxWidth().testTag("maintenance_end_time_input"),
                                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SciFiSecondary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                placeholder = { Text("e.g. 2 hours", color = Color.White.copy(alpha = 0.3f)) }
                            )
                        }
                    }
                }
            }

            // Metadata Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurface.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "EMERGENCY METADATA LOG",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Last Updated By: ${updatedBy.ifEmpty { "N/A" }}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (updatedAt > 0) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val dateStr = sdf.format(java.util.Date(updatedAt))
                            Text(
                                text = "Last Updated At: $dateStr",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Save configuration button
            if (isSuperAdmin) {
                item {
                    Button(
                        onClick = { onSaveAppControl() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_emergency_control_btn"),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SciFiSecondary,
                            contentColor = Color.White,
                            disabledContainerColor = SciFiSecondary.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "APPLY EMERGENCY CONFIG",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionPlansTabScreen(
    viewModel: PromotionPlanViewModel = viewModel(factory = PromotionPlanViewModel.Factory)
) {
    val plans by viewModel.plansFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showForm by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<PromotionPlan?>(null) }

    // Form inputs state variables
    var planName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var promotionType by remember { mutableStateOf("Banner") }
    var targetAudience by remember { mutableStateOf("All Users") }
    var targetViewsStr by remember { mutableStateOf("") }
    var rewardPerViewStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var buttonText by remember { mutableStateOf("Contact Now") }
    var badge by remember { mutableStateOf("") }
    var displayOrderStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("enabled") }
    var thumbnail by remember { mutableStateOf("") }

    // React to UI state changes for toasts
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is PromotionUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearUiState()
            }
            is PromotionUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    // Set form fields when editing
    LaunchedEffect(editingPlan) {
        if (editingPlan != null) {
            planName = editingPlan!!.displayTitle
            description = editingPlan!!.description
            promotionType = editingPlan!!.promotionType.ifBlank { "Banner" }
            targetAudience = editingPlan!!.targetAudience.ifBlank { "All Users" }
            targetViewsStr = editingPlan!!.targetViews.toString()
            rewardPerViewStr = editingPlan!!.rewardPerView.toString()
            priceStr = editingPlan!!.price.toInt().toString()
            contactPerson = editingPlan!!.contactPerson
            phone = editingPlan!!.phone
            whatsapp = editingPlan!!.whatsapp
            website = editingPlan!!.website
            buttonText = editingPlan!!.buttonText
            badge = editingPlan!!.badge
            displayOrderStr = editingPlan!!.displayOrder.toString()
            status = editingPlan!!.status
            thumbnail = editingPlan!!.thumbnail
        } else {
            planName = ""
            description = ""
            promotionType = "Banner"
            targetAudience = "All Users"
            targetViewsStr = ""
            rewardPerViewStr = ""
            priceStr = ""
            contactPerson = ""
            phone = ""
            whatsapp = ""
            website = ""
            buttonText = "Contact Now"
            badge = ""
            displayOrderStr = (plans.maxOfOrNull { it.displayOrder }?.plus(1) ?: 1).toString()
            status = "enabled"
            thumbnail = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SciFiBg)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC PROMOTION PLANS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = SciFiPrimary
                        )
                    )
                    Text(
                        text = "Realtime user home screen business promoter",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }

                Button(
                    onClick = {
                        editingPlan = null
                        showForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_add_plan_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Plan")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD PLAN", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            if (uiState is PromotionUiState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = SciFiPrimary,
                    trackColor = SciFiSurface
                )
            }

            if (plans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO PROMOTION PLANS DEFINED",
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create the first plan to display on User Home",
                            color = Color.White.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(plans) { index, plan ->
                        PromotionPlanAdminItem(
                            plan = plan,
                            isFirst = index == 0,
                            isLast = index == plans.size - 1,
                            onEdit = {
                                editingPlan = plan
                                showForm = true
                            },
                            onDelete = {
                                viewModel.deletePlan(plan.planId)
                            },
                            onDuplicate = {
                                viewModel.duplicatePlan(plan)
                            },
                            onToggleStatus = {
                                val newStatus = if (plan.status == "enabled") "disabled" else "enabled"
                                viewModel.updatePlan(plan.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
                            },
                            onMoveUp = { viewModel.movePlanUp(plan) },
                            onMoveDown = { viewModel.movePlanDown(plan) }
                        )
                    }
                }
            }
        }

        // Add/Edit Plan Form overlay Dialog
        if (showForm) {
            AlertDialog(
                onDismissRequest = { if (uiState !is PromotionUiState.Loading) showForm = false },
                containerColor = SciFiSurface,
                titleContentColor = Color.White,
                textContentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = if (editingPlan != null) "EDIT PROMOTION PLAN" else "ADD PROMOTION PLAN",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = SciFiPrimary
                    )
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Plan Title / Name
                        OutlinedTextField(
                            value = planName,
                            onValueChange = { planName = it },
                            label = { Text("Plan Name / Title", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_plan_name"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )

                        // Promotion Type & Target Audience Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = promotionType,
                                onValueChange = { promotionType = it },
                                label = { Text("Type (e.g. Banner)", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_promotion_type"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = targetAudience,
                                onValueChange = { targetAudience = it },
                                label = { Text("Target Audience", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_target_audience"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true
                            )
                        }

                        // Short Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Short Description", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_description"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            maxLines = 3
                        )

                        // Row: Target Views & Reward per View
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = targetViewsStr,
                                onValueChange = { targetViewsStr = it },
                                label = { Text("Target Views", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_target_views"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = rewardPerViewStr,
                                onValueChange = { rewardPerViewStr = it },
                                label = { Text("Reward/View (Coins)", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_reward_per_view"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        // Auto-calculating Campaign Budget preview card
                        val viewsVal = targetViewsStr.toIntOrNull() ?: 0
                        val rewardVal = rewardPerViewStr.toIntOrNull() ?: 0
                        val autoBudget = viewsVal * rewardVal

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x1A00FFCC)),
                            border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "CAMPAIGN BUDGET (AUTO CALCULATION)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SciFiPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = SciFiSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$autoBudget Coins ($viewsVal views × $rewardVal coins)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Row: Price & Display Order
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                label = { Text("Price (₹)", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_price"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = displayOrderStr,
                                onValueChange = { displayOrderStr = it },
                                label = { Text("Display Order", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_display_order"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        // Badge selection (Popular, Recommended, Best Value)
                        OutlinedTextField(
                            value = badge,
                            onValueChange = { badge = it },
                            label = { Text("Badge (Popular, Recommended, Best Value)", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_badge"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )

                        // Contact Person
                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Contact Person", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_contact_person"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )

                        // Contact Numbers
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_phone"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("WhatsApp", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f).testTag("form_whatsapp"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                        }

                        // Website
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Website (URL)", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_website"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )

                        // Custom Button Text & Thumbnail
                        OutlinedTextField(
                            value = buttonText,
                            onValueChange = { buttonText = it },
                            label = { Text("Contact Button Text", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_button_text"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = thumbnail,
                            onValueChange = { thumbnail = it },
                            label = { Text("Thumbnail URL (optional)", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().testTag("form_thumbnail"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SciFiPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (planName.isBlank()) {
                                Toast.makeText(context, "Plan Name / Title is required!", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val views = targetViewsStr.toIntOrNull() ?: 0
                            val reward = rewardPerViewStr.toIntOrNull() ?: 0
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val dispOrder = displayOrderStr.toIntOrNull() ?: 0
                            val calculatedBudget = views * reward

                            val planToSave = PromotionPlan(
                                planId = editingPlan?.planId ?: "",
                                title = planName,
                                planName = planName,
                                description = description,
                                promotionType = promotionType,
                                targetAudience = targetAudience,
                                targetViews = views,
                                rewardPerView = reward,
                                budget = calculatedBudget,
                                campaignBudget = calculatedBudget,
                                price = price,
                                contactPerson = contactPerson,
                                phone = phone,
                                whatsapp = whatsapp,
                                website = website,
                                buttonText = buttonText,
                                thumbnail = thumbnail,
                                badge = badge,
                                displayOrder = dispOrder,
                                status = status,
                                createdAt = editingPlan?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            if (editingPlan != null) {
                                viewModel.updatePlan(planToSave, onSuccess = {
                                    showForm = false
                                })
                            } else {
                                viewModel.addPlan(planToSave, onSuccess = {
                                    showForm = false
                                })
                            }
                        },
                        enabled = uiState !is PromotionUiState.Loading,
                        colors = ButtonDefaults.textButtonColors(contentColor = SciFiPrimary)
                    ) {
                        if (uiState is PromotionUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SciFiPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showForm = false },
                        enabled = uiState !is PromotionUiState.Loading
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@Composable
fun PromotionPlanAdminItem(
    plan: PromotionPlan,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleStatus: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .testTag("admin_plan_item_${plan.planId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SciFiSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.displayTitle.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    
                    if (plan.badge.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(SciFiSecondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(1.dp, SciFiSecondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.badge.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = SciFiSecondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (plan.status == "enabled") "ENABLED" else "DISABLED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.status == "enabled") SciFiGreen else Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = plan.status == "enabled",
                        onCheckedChange = { onToggleStatus() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SciFiGreen,
                            checkedTrackColor = SciFiGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.scale(0.8f).testTag("status_switch_${plan.planId}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (plan.description.isNotEmpty()) {
                Text(
                    text = plan.description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("VIEWS TARGET", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    Text("${plan.targetViews}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text("REWARD/VIEW", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    Text("${plan.rewardPerView} Coins", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text("CAMPAIGN BUDGET", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    Text("${plan.targetViews * plan.rewardPerView} Coins", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = SciFiPrimary)
                }
                Column {
                    Text("PRICE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    Text("₹${plan.price.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = SciFiSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Contact info string
            Text(
                text = "Contact: ${plan.contactPerson} | Phone: ${plan.phone} | WA: ${plan.whatsapp} | Web: ${plan.website}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reordering controls
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(28.dp).testTag("move_up_${plan.planId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (!isFirst) SciFiPrimary else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(28.dp).testTag("move_down_${plan.planId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (!isLast) SciFiPrimary else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "Order: ${plan.displayOrder}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp)
                    )
                }

                // Edit, Duplicate, Delete controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(28.dp).testTag("duplicate_${plan.planId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = SciFiPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("edit_${plan.planId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = SciFiSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_${plan.planId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 11: LEGAL CONTENT MANAGEMENT (SUPER ADMIN)
// ==========================================
@Composable
fun LegalContentManagementTabScreen(
    legalRepository: com.example.data.repository.LegalRepository = remember { com.example.data.repository.LegalRepository() }
) {
    var selectedDocId by remember { mutableStateOf("privacy_policy") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentEntity by legalRepository.getLegalPageFlow(selectedDocId).collectAsState(
        initial = com.example.data.repository.LegalDefaults.getDefault(selectedDocId)
    )

    var titleInput by remember(currentEntity) { mutableStateOf(currentEntity.title) }
    var versionInput by remember(currentEntity) { mutableStateOf(currentEntity.version) }
    var summaryInput by remember(currentEntity) { mutableStateOf(currentEntity.summary) }
    var contentInput by remember(currentEntity) { mutableStateOf(currentEntity.content) }
    var isPublished by remember(currentEntity) { mutableStateOf(currentEntity.published) }
    var isSaving by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    val pagesList = listOf(
        "privacy_policy" to "Privacy Policy",
        "terms_conditions" to "Terms & Conditions",
        "community_guidelines" to "Community Guidelines",
        "about_lootra" to "About Lootra",
        "help_support" to "Help & Support",
        "contact_us" to "Contact Us"
    )

    if (showPreview) {
        DynamicLegalDocumentViewer(
            docId = selectedDocId,
            onClose = { showPreview = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SciFiBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "SUPER ADMIN LEGAL CONTENT MANAGEMENT",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            ),
            color = SciFiAccent,
            modifier = Modifier.testTag("admin_legal_header_title")
        )

        Text(
            text = "Edit and publish dynamic legal documents. Changes take effect across all apps immediately.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Document Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = pagesList.indexOfFirst { it.first == selectedDocId }.coerceAtLeast(0),
            containerColor = SciFiSurface,
            contentColor = SciFiAccent,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            pagesList.forEach { (docId, label) ->
                Tab(
                    selected = selectedDocId == docId,
                    onClick = { selectedDocId = docId },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedDocId == docId) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedDocId == docId) SciFiAccent else Color.Gray
                        )
                    },
                    modifier = Modifier.testTag("tab_legal_admin_$docId")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editor Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SciFiSurface),
            border = BorderStroke(1.dp, SciFiPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document ID: $selectedDocId",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = SciFiSecondary
                    )

                    val formattedDate = remember(currentEntity.updatedAt) {
                        if (currentEntity.updatedAt > 0) {
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentEntity.updatedAt))
                        } else "Default"
                    }

                    Text(
                        text = "Last Updated: $formattedDate (${currentEntity.updatedBy})",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }

                HorizontalDivider(color = SciFiPrimary.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Document Title", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_legal_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiAccent,
                        unfocusedBorderColor = SciFiPrimary.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = versionInput,
                        onValueChange = { versionInput = it },
                        label = { Text("Version Tag (e.g. 1.0.1)", color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_legal_version"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SciFiAccent,
                            unfocusedBorderColor = SciFiPrimary.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = if (isPublished) "Published" else "Draft",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPublished) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isPublished,
                            onCheckedChange = { isPublished = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0x3310B981)
                            ),
                            modifier = Modifier.testTag("switch_legal_publish_status")
                        )
                    }
                }

                OutlinedTextField(
                    value = summaryInput,
                    onValueChange = { summaryInput = it },
                    label = { Text("Short Summary", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_legal_summary"),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiAccent,
                        unfocusedBorderColor = SciFiPrimary.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = contentInput,
                    onValueChange = { contentInput = it },
                    label = { Text("Full Legal Document Content", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .testTag("input_legal_content"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiAccent,
                        unfocusedBorderColor = SciFiPrimary.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPreview = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_preview_legal"),
                        border = BorderStroke(1.dp, SciFiSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Preview", color = SciFiSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val updatedEntity = com.example.data.model.LegalPageEntity(
                                    docId = selectedDocId,
                                    title = titleInput,
                                    summary = summaryInput,
                                    content = contentInput,
                                    version = versionInput,
                                    status = if (isPublished) "published" else "draft",
                                    published = isPublished,
                                    updatedAt = System.currentTimeMillis(),
                                    updatedBy = "Super Admin"
                                )
                                val res = legalRepository.saveLegalPage(updatedEntity)
                                isSaving = false
                                if (res.isSuccess) {
                                    android.widget.Toast.makeText(context, "Legal page published successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to save: ${res.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_legal"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPublished) Color(0xFF10B981) else SciFiAccent,
                            contentColor = Color(0xFF020617)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color(0xFF020617), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = if (isPublished) "Publish Page" else "Save Draft",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}



