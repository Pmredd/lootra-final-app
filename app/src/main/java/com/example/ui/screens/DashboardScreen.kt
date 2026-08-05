package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyReportEntity
import com.example.data.model.UsageStatisticEntity
import com.example.ui.components.CategoryDonutDial
import com.example.ui.components.MetricBarChart
import com.example.ui.viewmodel.WellbeingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WellbeingViewModel,
    onNavigateToAppsDetail: () -> Unit,
    onNavigateToInsightsDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyUsageList by viewModel.dailyUsageStats.collectAsState()
    val dailyReports by viewModel.dailyReports.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Determine today's report
    val todayReport = remember(dailyReports) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dailyReports.find { it.date == todayStr } ?: dailyReports.firstOrNull()
    }

    // Prepare trend history data for bar chart
    val historyDataPoints = remember(dailyReports) {
        dailyReports.take(7).reversed().map { report ->
            val dayName = try {
                val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(report.date) ?: Date()
                SimpleDateFormat("EEE", Locale.getDefault()).format(parsedDate)
            } catch (e: Exception) {
                report.date.substringAfterLast("-")
            }
            Pair(dayName, report.totalScreenTimeMs.toFloat() / 3600000f) // values in hours
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Lootra Wellbeing",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshState() },
                        modifier = Modifier.testTag("sync_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Statistics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isLoading && dailyUsageList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Today's Master Screen Time Indicator
                item {
                    val totalMs = todayReport?.totalScreenTimeMs ?: dailyUsageList.sumOf { it.durationMs }
                    MainScreenTimeCard(
                        totalMs = totalMs,
                        goalMinutes = dailyGoalMinutes,
                        onGoalChange = { viewModel.setDailyGoal(it) }
                    )
                }

                // Split metrics: Productivity Score and Insights Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ProductivityCircleGauge(
                                score = todayReport?.productivityScore ?: 72,
                                modifier = Modifier.height(180.dp)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            QuickMetricsCard(
                                reportsCount = dailyReports.size,
                                appOpenCount = todayReport?.appOpenCount ?: dailyUsageList.sumOf { it.openCount },
                                notificationsEnabled = viewModel.notificationsEnabled.collectAsState().value,
                                onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                modifier = Modifier.height(180.dp)
                            )
                        }
                    }
                }

                // Category Donut Distribution layout
                item {
                    CategoryDonutDial(
                        statistics = dailyUsageList,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Canvas Usage Trend Block
                item {
                    MetricBarChart(
                        dataPoints = historyDataPoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // AI Insight Bullet Panel
                item {
                    if (todayReport != null && todayReport.insights.isNotEmpty()) {
                        InsightPanelCard(
                            insights = todayReport.insights.split(";"),
                            onNavigateToInsightsDetail = onNavigateToInsightsDetail
                        )
                    }
                }

                // Top Visited Applications Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Most Used Applications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = onNavigateToAppsDetail,
                            modifier = Modifier.testTag("view_all_apps_button")
                        ) {
                            Text("See Details")
                        }
                    }
                }

                items(dailyUsageList.take(4)) { stat ->
                    AppUsageItemRow(stat = stat)
                }
            }
        }
    }
}

@Composable
fun MainScreenTimeCard(
    totalMs: Long,
    goalMinutes: Int,
    onGoalChange: (Int) -> Unit
) {
    val totalMinutes = (totalMs / 60000L).toInt()
    val hours = totalMinutes / 60
    val remMins = totalMinutes % 60
    val progress = (totalMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
    var showGoalSettings by remember { mutableStateOf(false) }

    val gradientTheme = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_screen_time_card")
    ) {
        Column(
            modifier = Modifier
                .background(brush = gradientTheme)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DAILY SCREEN LIMITS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                IconButton(
                    onClick = { showGoalSettings = !showGoalSettings },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Change Limit Settings",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${hours}h ${remMins}m",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.onSecondary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% of daily goal utilized",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = "Goal limit: $goalMinutes mins",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            AnimatedVisibility(
                visible = showGoalSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Set Personalized Goal Limit",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(60, 120, 180, 240, 300).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (goalMinutes == mins) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(
                                            alpha = 0.15f
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onGoalChange(mins)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins / 60}h",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (goalMinutes == mins) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
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
fun ProductivityCircleGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val rating = when {
        score >= 75 -> "Productive"
        score in 50..74 -> "Balanced"
        else -> "Distracted"
    }

    val themeColor = when {
        score >= 75 -> Color(0xFF4CAF50)
        score in 50..74 -> Color(0xFF2196F3)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Productivity Index",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        color = themeColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = themeColor
                    )
                    Text(
                        "/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = rating,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = themeColor
            )
        }
    }
}

@Composable
fun QuickMetricsCard(
    reportsCount: Int,
    appOpenCount: Int,
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Quick Analysis",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Launch Opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$appOpenCount sessions",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Historical Days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$reportsCount cycles",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onToggleNotifications(!notificationsEnabled) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Wellness Alerts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (notificationsEnabled) "ON" else "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun InsightPanelCard(
    insights: List<String>,
    onNavigateToInsightsDetail: () -> Unit
) {
    if (insights.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToInsightsDetail() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Personalized Wellness Insight",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insights.firstOrNull() ?: "Insights are being calculated securely in your repository database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to view full productivity breakdown",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AppUsageItemRow(
    stat: UsageStatisticEntity
) {
    val hrs = stat.durationMs / 3600000L
    val mins = (stat.durationMs % 3600000) / 60000L

    val categoryColor = when (stat.category) {
        "PRODUCTIVITY" -> Color(0xFF4CAF50)
        "EDUCATION" -> Color(0xFF2196F3)
        "SOCIAL" -> Color(0xFFFF9800)
        "GAMING" -> Color(0xFFE91E63)
        "ENTERTAINMENT" -> Color(0xFF9C27B0)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.0f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = categoryColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stat.appName.firstOrNull()?.toString() ?: "A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stat.appName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = categoryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stat.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = categoryColor
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stat.openCount} opens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
