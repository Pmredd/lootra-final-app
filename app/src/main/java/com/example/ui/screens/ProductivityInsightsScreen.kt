package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.WellbeingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityInsightsScreen(
    viewModel: WellbeingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyReports = viewModel.dailyReports.collectAsState().value
    val weeklyReports = viewModel.weeklyReports.collectAsState().value
    val monthlyReports = viewModel.monthlyReports.collectAsState().value

    // 1. Calculate historical metrics
    val statsCalculations = remember(dailyReports, weeklyReports, monthlyReports) {
        val dailyAvgMs = if (dailyReports.isNotEmpty()) dailyReports.map { it.totalScreenTimeMs }.average().toLong() else 0L
        val weeklyAvgMs = if (weeklyReports.isNotEmpty()) weeklyReports.map { it.totalScreenTimeMs }.average().toLong() else 0L
        val monthlyAvgMs = if (monthlyReports.isNotEmpty()) monthlyReports.map { it.totalScreenTimeMs }.average().toLong() else 0L

        // Determine most productive day based on highest daily productivity score
        val mostProductiveDay = if (dailyReports.isNotEmpty()) {
            val bestReport = dailyReports.maxByOrNull { it.productivityScore }
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(bestReport?.date ?: "") ?: Date()
                SimpleDateFormat("EEEE (MMM dd)", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                bestReport?.date ?: "N/A"
            }
        } else {
            "N/A"
        }

        val avgProductivityScore = if (dailyReports.isNotEmpty()) dailyReports.map { it.productivityScore }.average().toInt() else 0

        InsightMetrics(
            dailyAvgMs = dailyAvgMs,
            weeklyAvgMs = weeklyAvgMs,
            monthlyAvgMs = monthlyAvgMs,
            mostProductiveDay = mostProductiveDay,
            averageProductivityScore = avgProductivityScore
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productivity Diagnostics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button_insights")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Balance & Productivity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Audit overall trends, benchmark metrics, and check tips generated dynamically by the local wellness compiler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Dynamic Calculations Widgets
            Text(
                "Aggregated Benchmark Metrics",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ScoreWidget(
                        title = "Daily Screen Avg",
                        value = formatMsToHrsMins(statsCalculations.dailyAvgMs),
                        icon = Icons.Outlined.Timer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ScoreWidget(
                        title = "Weekly Screen Avg",
                        value = formatMsToHrsMins(statsCalculations.weeklyAvgMs),
                        icon = Icons.Outlined.Timer,
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ScoreWidget(
                        title = "Productive Zenith",
                        value = statsCalculations.mostProductiveDay,
                        icon = Icons.Outlined.Event,
                        contentColor = Color(0xFF4CAF50)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ScoreWidget(
                        title = "Productivity Avg",
                        value = "${statsCalculations.averageProductivityScore}/100",
                        icon = Icons.Outlined.CheckCircle,
                        contentColor = Color(0xFF2196F3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actionable Wellness Suggestions/Tips
            Text(
                "Actionable Diagnostics Recommendations",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            RecommendationItem(
                icon = Icons.Default.Highlight,
                title = "Reduce Evening Screen Exposure",
                advice = "Analyzing past evening checks shows high social media engagement near bedtime. Mute app pushes past 10 PM in wellbeing settings to stabilize your rest."
            )

            Spacer(modifier = Modifier.height(10.dp))

            RecommendationItem(
                icon = Icons.Default.Highlight,
                title = "Take Screen Fatigue Pauses",
                advice = "After 45 continuous minutes of Slack/Notion productivity sessions, take a quick 5-minute visual recess. It relieves optical fatigue and boosts sustained stamina."
            )

            Spacer(modifier = Modifier.height(10.dp))

            RecommendationItem(
                icon = Icons.Default.Highlight,
                title = "Dedicate Time to Educational Learning",
                advice = "Sustaining a daily focus of 15 minutes of language and quiz workflows (like Duolingo) consistently boosts the local cognitive index above 80 points."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ScoreWidget(
    title: String,
    value: String,
    icon: ImageVector,
    contentColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecommendationItem(
    icon: ImageVector,
    title: String,
    advice: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatMsToHrsMins(ms: Long): String {
    if (ms <= 0L) return "0 mins"
    val hrs = ms / 3600000L
    val mins = (ms % 3600000) / 60000L
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}

private data class InsightMetrics(
    val dailyAvgMs: Long,
    val weeklyAvgMs: Long,
    val monthlyAvgMs: Long,
    val mostProductiveDay: String,
    val averageProductivityScore: Int
)
