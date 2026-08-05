package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UsageStatisticEntity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Capsule-styled Bar Chart to show screen-time over a series of days.
 */
@Composable
fun MetricBarChart(
    dataPoints: List<Pair<String, Float>>, // Label to value in Hours
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data points recorded for this cycle.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val maxValue = dataPoints.maxOf { it.second }.coerceAtLeast(1.0f)
    var triggerAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(dataPoints) {
        triggerAnimation = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (triggerAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "bar_chart_anim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Usage History (Hours)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val spacing = 28f
            val barsCount = dataPoints.size
            val individualBarWidth = (canvasWidth - (spacing * (barsCount + 1))) / barsCount
            
            // Draw horizontal coordinate helper grid lines
            val gridLineCount = 3
            for (i in 0..gridLineCount) {
                val y = canvasHeight * (i.toFloat() / gridLineCount)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 2f
                )
            }

            // Render bars
            for (idx in dataPoints.indices) {
                val point = dataPoints[idx]
                val pct = (point.second / maxValue) * animatedProgress
                val barHeight = canvasHeight * pct
                val originalX = spacing + idx * (individualBarWidth + spacing)
                val originalY = canvasHeight - barHeight

                // Gradient design
                val barGradient = Brush.verticalGradient(
                    colors = listOf(
                        barColor,
                        accentColor
                    )
                )

                // Capsule geometry
                drawRoundRect(
                    brush = barGradient,
                    topLeft = Offset(originalX, originalY),
                    size = Size(individualBarWidth, barHeight.coerceAtLeast(6f)),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display Bottom labels representing weeks or dates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (point in dataPoints) {
                Text(
                    text = point.first,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.width(32.dp),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * A beautiful Donut Chart representing Screen Time Category distribution.
 */
@Composable
fun CategoryDonutDial(
    statistics: List<UsageStatisticEntity>,
    modifier: Modifier = Modifier
) {
    val categoryTotals = remember(statistics) {
        val distribution = mutableMapOf<String, Long>()
        for (stat in statistics) {
            distribution[stat.category] = (distribution[stat.category] ?: 0L) + stat.durationMs
        }
        distribution
    }

    val totalDuration = categoryTotals.values.sum().coerceAtLeast(1L)
    
    // Sort categories consistently
    val categoriesOrder = listOf("PRODUCTIVITY", "EDUCATION", "SOCIAL", "GAMING", "ENTERTAINMENT", "OTHER")
    val colorsMap = mapOf(
        "PRODUCTIVITY" to Color(0xFF4CAF50),  // Bright Emerald green
        "EDUCATION" to Color(0xFF2196F3),     // Tech Blue
        "SOCIAL" to Color(0xFFFF9800),        // Deep Warm Orange
        "GAMING" to Color(0xFFE91E63),        // Coral Pink Alert
        "ENTERTAINMENT" to Color(0xFF9C27B0), // Royal Purple
        "OTHER" to Color(0xFF9E9E9E)          // Cool Grey
    )

    val listItems = remember(categoryTotals) {
        categoriesOrder.mapNotNull { cat ->
            val ms = categoryTotals[cat] ?: 0L
            if (ms > 0) {
                val pct = (ms.toFloat() / totalDuration) * 100
                CategoryItem(cat, pct, colorsMap[cat] ?: Color.Cyan, ms)
            } else null
        }
    }

    if (listItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No app categories parsed for today.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Canvas Donut Ring
        Canvas(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp)
        ) {
            var currentStartAngle = -90f
            val strokeWidthPx = 22f

            for (item in listItems) {
                val sweepAngle = (item.percentage / 100f) * 360f
                drawArc(
                    color = item.color,
                    startAngle = currentStartAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                    size = size
                )
                currentStartAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Right side: Custom Legend labels
        Column(
            modifier = Modifier.weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (item in listItems) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(item.color, shape = RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${item.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private data class CategoryItem(
    val name: String,
    val percentage: Float,
    val color: Color,
    val durationMs: Long
)
