package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.screens.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.WellbeingViewModel
import com.example.ui.viewmodel.WalletViewModel
import com.example.ui.viewmodel.RewardTVViewModel
import com.example.ui.viewmodel.ShopViewModel
import com.example.ui.viewmodel.OrderViewModel
import com.example.WellbeingApplication

private sealed class Screen(val route: String, val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Wallet : Screen("wallet", "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object Shop : Screen("shop_home", "Shop", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
    object RewardTV : Screen("reward_tv", "TV", Icons.Filled.Tv, Icons.Outlined.Tv)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object DailyReports : Screen("daily_reports", "Daily", Icons.Filled.Analytics, Icons.Outlined.Analytics)
    object WeeklyReports : Screen("weekly_reports", "Weekly", Icons.Filled.Analytics, Icons.Outlined.Analytics)
    object MonthlyReports : Screen("monthly_reports", "Monthly", Icons.Filled.Analytics, Icons.Outlined.Analytics)
    object AppsDetail : Screen("apps_detail", "App Audit", Icons.Filled.List, Icons.Outlined.List)
    object InsightsDetail : Screen("insights_detail", "Productivity", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
}

@Composable
fun WellbeingApp(
    viewModel: WellbeingViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val maintenanceEnabled by com.example.data.manager.AppControlManager.maintenanceEnabled.collectAsState()
    val maintenanceTitle by com.example.data.manager.AppControlManager.maintenanceTitle.collectAsState()
    val maintenanceMessage by com.example.data.manager.AppControlManager.maintenanceMessage.collectAsState()
    val maintenanceReason by com.example.data.manager.AppControlManager.reason.collectAsState()
    val estimatedEndTime by com.example.data.manager.AppControlManager.estimatedEndTime.collectAsState()

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val isSplashFinished by authViewModel.isSplashFinished.collectAsState()

    val context = LocalContext.current
    val application = context.applicationContext as WellbeingApplication
    val container = application.container

    val walletViewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.Factory(
            authRepository = container.authRepository,
            walletRepository = container.walletRepository
        )
    )

    val rewardTVViewModel: RewardTVViewModel = viewModel(
        factory = RewardTVViewModel.Factory(
            authRepository = container.authRepository,
            rewardRepository = container.rewardRepository,
            wellbeingRepository = container.repository,
            walletRepository = container.walletRepository,
            adManager = container.adManager
        )
    )

    val shopViewModel: ShopViewModel = viewModel(
        factory = ShopViewModel.Factory(
            authRepository = container.authRepository,
            productRepository = container.productRepository,
            walletRepository = container.walletRepository,
            orderRepository = container.orderRepository,
            preferences = container.preferences
        )
    )

    val orderViewModel: OrderViewModel = viewModel(
        factory = OrderViewModel.Factory(
            authRepository = container.authRepository,
            orderRepository = container.orderRepository
        )
    )

    val reelsViewModel: com.example.ui.viewmodel.ReelsViewModel = viewModel(
        factory = com.example.ui.viewmodel.ReelsViewModel.Factory(
            authRepository = container.authRepository,
            reelRepository = container.reelRepository,
            productRepository = container.productRepository,
            preferences = container.preferences
        )
    )

    // Handle seamless state-driven Splash page auto navigation delay
    LaunchedEffect(isSplashFinished, currentRoute) {
        if (isSplashFinished && currentRoute == "splash") {
            if (authViewModel.isUserLoggedIn()) {
                val role = authViewModel.adminRole.value
                val isActive = authViewModel.isAdmin.value
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "Unknown"
                val destination = if (isActive && role == "super_admin") {
                    "admin_dashboard"
                } else if (isActive && role == "sub_admin") {
                    "sub_admin_dashboard"
                } else {
                    "home"
                }
                val permissionLevel = if (isActive && role == "super_admin") {
                    "Super Admin (Level 2)"
                } else if (isActive && role == "sub_admin") {
                    "Sub Admin (Level 1)"
                } else {
                    "Normal User (Level 0)"
                }
                
                android.util.Log.d(
                    "WellbeingApp",
                    "[ROLE RESTORATION] Session Restored: Yes, Firebase UID: $uid, Detected Role: $role, Permission Level: $permissionLevel, Navigation Destination: $destination"
                )

                if (isActive && role == "super_admin") {
                    navController.navigate("admin_dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else if (isActive && role == "sub_admin") {
                    navController.navigate("sub_admin_dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            } else {
                android.util.Log.d(
                    "WellbeingApp",
                    "[ROLE RESTORATION] Session Restored: No, Firebase UID: null, Detected Role: null, Permission Level: Guest (Level 0), Navigation Destination: signin"
                )
                navController.navigate("signin") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    // Restrict permission prompt checks until user completely finishes authentication flows
    val authRoutes = listOf("splash", "signin", "signup", "forgot_password", "edit_profile")
    val isAuthScreen = currentRoute == null || currentRoute in authRoutes
    
    val showBottomBar = currentRoute in listOf("home", "wallet", "shop_home", "reward_tv", "profile")

    val isSuperAdmin = authViewModel.isAdmin.value && authViewModel.adminRole.value == "super_admin"
    val needsPolicyUpdate by authViewModel.needsPolicyUpdate.collectAsState()
    var showPolicyReviewScreen by remember { mutableStateOf(false) }

    if (needsPolicyUpdate && !isAuthScreen) {
        if (showPolicyReviewScreen) {
            val isLegalAccepting by authViewModel.isLegalAccepting.collectAsState()
            val legalError by authViewModel.legalAgreementError.collectAsState()
            LegalAgreementScreen(
                onAcceptAndContinue = { privacyVer, termsVer, communityVer ->
                    authViewModel.saveLegalAcceptance(privacyVer, termsVer, communityVer)
                    showPolicyReviewScreen = false
                },
                onBack = {
                    showPolicyReviewScreen = false
                },
                isLoading = isLegalAccepting,
                errorMessage = legalError,
                onClearError = { authViewModel.clearLegalError() }
            )
        } else {
            PolicyUpdateDialog(
                onReviewAndAccept = {
                    showPolicyReviewScreen = true
                },
                onLogout = {
                    authViewModel.performSignOut()
                }
            )
        }
    } else if (maintenanceEnabled && !isSuperAdmin && currentRoute != "splash" && currentRoute != "signin") {
        MaintenanceScreen(
            title = maintenanceTitle,
            message = maintenanceMessage,
            reason = maintenanceReason,
            estimatedEndTime = estimatedEndTime,
            onRetry = {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("settings").document("app_control")
                    .get()
            }
        )
    } else if (!isPermissionGranted && !isAuthScreen) {
        UsagePermissionScreen(viewModel = viewModel, modifier = modifier.fillMaxSize())
    } else {
        Scaffold(
            bottomBar = {
                // Bottom layout is only visible on the main premium tabs
                if (showBottomBar) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .testTag("app_navigation_bar"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xEE020617) else Color(0xEEFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = if (isSystemInDarkTheme()) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.04f))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val navigationItems = listOf(
                                Screen.Home,
                                Screen.Wallet,
                                Screen.Shop,
                                Screen.RewardTV,
                                Screen.Profile
                            )

                            navigationItems.forEach { screen ->
                                val isSelected = currentRoute == screen.route
                                
                                // Beautiful bouncy scale transition
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.2f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "tabScale"
                                )
                                
                                val activeColor = MaterialTheme.colorScheme.primary
                                val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                val iconColor by animateColorAsState(
                                    targetValue = if (isSelected) activeColor else inactiveColor,
                                    label = "tabIconColor"
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("nav_item_${screen.route}")
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(
                                                bounded = false,
                                                radius = 28.dp
                                            )
                                        ) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(30.dp)
                                            .width(52.dp)
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) screen.activeIcon else screen.inactiveIcon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.scale(scale),
                                            tint = iconColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(innerPadding)
            ) {
                // 1. Splash Screen
                composable("splash") {
                    SplashScreen()
                }

                // 2. Sign In Screen
                composable("signin") {
                    SignInScreen(
                        viewModel = authViewModel,
                        onNavigateToSignUp = {
                            authViewModel.forceResetAuthStates()
                            navController.navigate("signup")
                        },
                        onNavigateToForgotPassword = {
                            authViewModel.forceResetAuthStates()
                            navController.navigate("forgot_password")
                        },
                        onLoginSuccess = {
                            val role = authViewModel.adminRole.value
                            val isActive = authViewModel.isAdmin.value
                            authViewModel.forceResetAuthStates()
                            if (isActive && role == "super_admin") {
                                navController.navigate("admin_dashboard") {
                                    popUpTo("signin") { inclusive = true }
                                }
                            } else if (isActive && role == "sub_admin") {
                                navController.navigate("sub_admin_dashboard") {
                                    popUpTo("signin") { inclusive = true }
                                }
                            } else {
                                navController.navigate("home") {
                                    popUpTo("signin") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // 3. Sign Up Screen
                composable("signup") {
                    SignUpScreen(
                        viewModel = authViewModel,
                        onNavigateToSignIn = {
                            authViewModel.forceResetAuthStates()
                            navController.navigate("signin") {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        onSignUpSuccess = {
                            authViewModel.forceResetAuthStates()
                            navController.navigate("home") {
                                popUpTo("signup") { inclusive = true }
                            }
                        }
                    )
                }

                // 4. Forgot Password Screen
                composable("forgot_password") {
                    ForgotPasswordScreen(
                        viewModel = authViewModel,
                        onNavigateBack = {
                            authViewModel.forceResetAuthStates()
                            navController.navigate("signin") {
                                popUpTo("forgot_password") { inclusive = true }
                            }
                        }
                    )
                }

                // 5. Home Screen
                composable("home") {
                    HomeScreen(
                        viewModel = authViewModel,
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) }
                    )
                }

                composable("wallet") {
                    WalletScreen(viewModel = walletViewModel)
                }

                composable("reward_tv") {
                    RewardTVScreen(
                        viewModel = rewardTVViewModel,
                        reelsViewModel = reelsViewModel
                    )
                }

                // --- E-Commerce Shop Screens ---
                composable("shop_home") {
                    ShopHomeScreen(
                        viewModel = shopViewModel,
                        onNavigateToProduct = { productId -> navController.navigate("product_detail/$productId") },
                        onNavigateToCategories = { navController.navigate("shop_categories") },
                        onNavigateToSearch = { navController.navigate("shop_search") },
                        onNavigateToCart = { navController.navigate("shop_cart") },
                        onNavigateToOrders = { navController.navigate("shop_orders") }
                    )
                }

                composable("product_detail/{productId}") { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: ""
                    ProductDetailsScreen(
                        productId = productId,
                        viewModel = shopViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToCheckout = { pId -> navController.navigate("shop_checkout/$pId") },
                        onNavigateToCart = { navController.navigate("shop_cart") }
                    )
                }

                composable("shop_categories") {
                    CategoriesScreen(
                        viewModel = shopViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateHome = {
                            navController.navigate("shop_home") {
                                popUpTo("shop_home") { inclusive = false }
                            }
                        }
                    )
                }

                composable("shop_search") {
                    SearchProductsScreen(
                        viewModel = shopViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToProduct = { productId -> navController.navigate("product_detail/$productId") }
                    )
                }

                composable("shop_cart") {
                    CartScreen(
                        viewModel = shopViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToCheckout = { productId -> navController.navigate("shop_checkout/$productId") }
                    )
                }

                composable("shop_checkout/{productId}") { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: ""
                    CheckoutScreen(
                        productId = productId,
                        viewModel = shopViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToOrders = { navController.navigate("shop_orders") }
                    )
                }

                composable("shop_orders") {
                    OrdersScreen(
                        viewModel = orderViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToOrderDetail = { orderId -> navController.navigate("shop_order_detail/$orderId") }
                    )
                }

                composable("shop_order_detail/{orderId}") { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                    OrderDetailsScreen(
                        orderId = orderId,
                        viewModel = orderViewModel,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // 6. Profile Screen
                composable("profile") {
                    ProfileScreen(
                        viewModel = authViewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToEditProfile = { navController.navigate("edit_profile") },
                        onNavigateToLegalInfo = { navController.navigate("legal_info") },
                        onLogoutFinished = {
                            navController.navigate("signin") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToAdmin = {
                            val role = authViewModel.adminRole.value
                            if (role == "sub_admin") {
                                navController.navigate("sub_admin_dashboard")
                            } else {
                                navController.navigate("admin_dashboard")
                            }
                        }
                    )
                }

                // 6b. Legal & Information Screen
                composable("legal_info") {
                    LegalInfoScreen(
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // --- Secure Admin Panel ---
                composable("admin_dashboard") {
                    val adminViewModel: com.example.ui.viewmodel.AdminViewModel = viewModel(
                        factory = com.example.ui.viewmodel.AdminViewModel.Factory
                    )
                    val uidFlow = authViewModel.userProfile.collectAsState()
                    val uid = uidFlow.value?.uid ?: ""
                    AdminDashboardScreen(
                        viewModel = adminViewModel,
                        currentUserUid = uid,
                        onNavigateBack = { navController.navigateUp() },
                        onSignOut = {
                            authViewModel.performSignOut()
                            navController.navigate("signin") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // --- Sub-Admin Panel ---
                composable("sub_admin_dashboard") {
                    val adminViewModel: com.example.ui.viewmodel.AdminViewModel = viewModel(
                        factory = com.example.ui.viewmodel.AdminViewModel.Factory
                    )
                    val collegeIdFlow = authViewModel.adminCollegeId.collectAsState()
                    val collegeId = collegeIdFlow.value
                    SubAdminDashboardScreen(
                        viewModel = adminViewModel,
                        assignedCollegeId = collegeId,
                        onSignOut = {
                            authViewModel.performSignOut()
                            navController.navigate("signin") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // 7. Edit Profile Screen
                composable("edit_profile") {
                    EditProfileScreen(
                        viewModel = authViewModel,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // --- Wellbeing Application screens (Post-Login) ---
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAppsDetail = { navController.navigate(Screen.AppsDetail.route) },
                        onNavigateToInsightsDetail = { navController.navigate(Screen.InsightsDetail.route) }
                    )
                }

                composable(Screen.DailyReports.route) {
                    DailyReportScreen(
                        viewModel = viewModel,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.WeeklyReports.route) {
                    WeeklyReportScreen(
                        viewModel = viewModel,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.MonthlyReports.route) {
                    MonthlyReportScreen(
                        viewModel = viewModel,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.AppsDetail.route) {
                    MostUsedAppsScreen(
                        viewModel = viewModel,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(Screen.InsightsDetail.route) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProductivityInsightsScreen(
                            viewModel = viewModel,
                            onBack = { navController.navigateUp() },
                            modifier = Modifier.weight(1f)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { navController.navigate(Screen.WeeklyReports.route) },
                                modifier = Modifier.weight(1f).testTag("btn_weekly_reports"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Text("Weekly Reports", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { navController.navigate(Screen.MonthlyReports.route) },
                                modifier = Modifier.weight(1f).testTag("btn_monthly_reports"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                            ) {
                                Text("Monthly Reports", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
