package com.example.data.repository

import android.content.Context
import com.example.data.database.WellbeingDatabase
import com.example.data.datastore.WellbeingPreferences
import com.example.data.manager.DeviceUsageStatsService
import com.example.data.model.DailyReportEntity
import com.example.data.model.MonthlyReportEntity
import com.example.data.model.UsageStatisticEntity
import com.example.data.model.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class WellbeingRepository(
    private val context: Context,
    private val database: WellbeingDatabase,
    private val preferences: WellbeingPreferences,
    private val statsService: DeviceUsageStatsService
) {
    // Standard DAOs
    private val usageDao = database.usageStatisticDao
    private val dailyDao = database.dailyReportDao
    private val weeklyDao = database.weeklyReportDao
    private val monthlyDao = database.monthlyReportDao

    // DataStore Configurations
    val dailyGoalMinutes: Flow<Int> = preferences.dailyGoalMinutesFlow
    val notificationsEnabled: Flow<Boolean> = preferences.notificationsEnabledFlow
    val lastRefreshTimestamp: Flow<Long> = preferences.lastRefreshTimestampFlow

    suspend fun saveDailyGoalMinutes(minutes: Int) = preferences.setDailyGoalMinutes(minutes)
    suspend fun saveNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)

    /**
     * Checks if usage permission is granted.
     */
    fun isUsageAccessGranted(): Boolean = statsService.isUsageAccessPermissionGranted(context)

    /**
     * Standard overall permission validation constraint
     */
    fun isPermissionGranted(): Boolean {
        val usage = isUsageAccessGranted()
        val notifications = if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return usage && notifications
    }

    /**
     * Reactive feeds from local database tables
     */
    private fun getUid(): String = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

    /**
     * Reactive feeds from local database tables
     */
    fun getUsageStatsForDate(dateString: String): Flow<List<UsageStatisticEntity>> =
        usageDao.getUsageStatsForDate(getUid(), dateString)

    fun getAllDailyReports(): Flow<List<DailyReportEntity>> =
        dailyDao.getAllDailyReportsReactive(getUid())

    fun getRecentDailyReports(limit: Int): Flow<List<DailyReportEntity>> =
        dailyDao.getRecentDailyReports(getUid(), limit)

    fun getAllWeeklyReports(): Flow<List<WeeklyReportEntity>> =
        weeklyDao.getAllWeeklyReports(getUid())

    fun getAllMonthlyReports(): Flow<List<MonthlyReportEntity>> =
        monthlyDao.getAllMonthlyReports(getUid())

    /**
     * Refreshes the local database data for a modern date by pulling from Android UsageStatsManager
     * and auto-generating aggregated screen time, productivity score, and text insights.
     */
    suspend fun refreshUsageData(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(date)
        val uid = getUid()

        // 1. Fetch real application stats
        val freshStats = statsService.fetchDeviceUsageStats(context, date, uid)

        // 2. Clear out older stats for this date and write fresh ones
        usageDao.deleteUsageStatsForDate(uid, dateString)
        usageDao.insertUsageStats(freshStats)

        // 3. Compute summaries and insights
        var totalMs = 0L
        var totalOpens = 0
        var mostUsedName = "None"
        var maxTime = 0L

        for (stat in freshStats) {
            totalMs += stat.durationMs
            totalOpens += stat.openCount
            if (stat.durationMs > maxTime) {
                maxTime = stat.durationMs
                mostUsedName = stat.appName
            }
        }

        val prodScore = statsService.calculateProductivityScore(freshStats)
        val insightsString = generateInsightsList(freshStats, totalMs, prodScore, mostUsedName).joinToString(";")

        // 4. Create Daily Report Entity
        val dailyReport = DailyReportEntity(
            userId = uid,
            date = dateString,
            totalScreenTimeMs = totalMs,
            productivityScore = prodScore,
            mostUsedApp = mostUsedName,
            appOpenCount = totalOpens,
            insights = insightsString
        )
        dailyDao.insertDailyReport(dailyReport)

        // 5. Update Weekly and Monthly aggregates
        updateWeeklyAggregate(date, dailyReport)
        updateMonthlyAggregate(date, dailyReport)

        // 6. Record last refresh timing
        preferences.setLastRefreshTimestamp(System.currentTimeMillis())
    }

    /**
     * Aggregates daily reports in the same week to build/override a Weekly Report.
     */
    private suspend fun updateWeeklyAggregate(date: Date, currentDaily: DailyReportEntity) {
        val cal = Calendar.getInstance()
        cal.time = date
        // Set calendar to Monday of this week as the key
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val mondayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        // Fetch recent daily reports
        val currentReports = dailyDao.getAllDailyReportsReactive(currentDaily.userId).first()
        // Filter those in the last 7 days of this Monday key
        val weekReports = currentReports.filter { report ->
            try {
                val reportDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(report.date) ?: return@filter false
                val diffDays = (reportDate.time - cal.time.time) / (1000 * 60 * 60 * 24)
                diffDays in 0..6
            } catch (e: Exception) {
                false
            }
        }

        if (weekReports.isNotEmpty()) {
            val totalScreenTime = weekReports.sumOf { it.totalScreenTimeMs }
            val avgScreenTime = totalScreenTime / weekReports.size
            val avgProdScore = weekReports.sumOf { it.productivityScore } / weekReports.size
            
            // Determine weekly most used app
            val appCountMap = weekReports.groupBy { it.mostUsedApp }.mapValues { entry -> entry.value.size }
            val weeklyMostUsed = appCountMap.maxByOrNull { it.value }?.key ?: currentDaily.mostUsedApp

            val weeklyInsights = listOf(
                "Weekly screen average sits around ${(avgScreenTime / 3600000L)}h ${(avgScreenTime % 3600000L) / 60000L}m.",
                "Your weekly productivity score average is $avgProdScore/100.",
                "Most visited application across this week is $weeklyMostUsed."
            ).joinToString(";")

            weeklyDao.insertWeeklyReport(
                WeeklyReportEntity(
                    userId = currentDaily.userId,
                    weekStartDate = mondayDateString,
                    totalScreenTimeMs = totalScreenTime,
                    averageScreenTimeMs = avgScreenTime,
                    productivityScore = avgProdScore,
                    mostUsedApp = weeklyMostUsed,
                    insightsList = weeklyInsights
                )
            )
        }
    }

    /**
     * Aggregates daily reports in the same month to build/override a Monthly Report.
     */
    private suspend fun updateMonthlyAggregate(date: Date, currentDaily: DailyReportEntity) {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)

        val currentReports = dailyDao.getAllDailyReportsReactive(currentDaily.userId).first()
        val monthReports = currentReports.filter { it.date.startsWith(monthKey) }

        if (monthReports.isNotEmpty()) {
            val totalScreenTime = monthReports.sumOf { it.totalScreenTimeMs }
            val avgScreenTime = totalScreenTime / monthReports.size
            val avgProdScore = monthReports.sumOf { it.productivityScore } / monthReports.size

            val appCountMap = monthReports.groupBy { it.mostUsedApp }.mapValues { entry -> entry.value.size }
            val monthlyMostUsed = appCountMap.maxByOrNull { it.value }?.key ?: currentDaily.mostUsedApp

            val monthlyInsights = listOf(
                "Monthly total screen duration accumulated ${(totalScreenTime / 3600000L)} hours.",
                "Overall Monthly Productivity standing is healthy at $avgProdScore/100.",
                "Dominant application category is balanced, with $monthlyMostUsed on top."
            ).joinToString(";")

            monthlyDao.insertMonthlyReport(
                MonthlyReportEntity(
                    userId = currentDaily.userId,
                    monthKey = monthKey,
                    totalScreenTimeMs = totalScreenTime,
                    averageScreenTimeMs = avgScreenTime,
                    productivityScore = avgProdScore,
                    mostUsedApp = monthlyMostUsed,
                    insightsList = monthlyInsights
                )
            )
        }
    }

    /**
     * Resets current session wellbeing counters on logout.
     */
    suspend fun clearUsageStatsAndReportsOnLogout(uid: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 1. Clear today's usage stats cache
        usageDao.deleteUsageStatsForDate(uid, todayStr)
        
        // 2. Delete today's DailyReportEntity
        dailyDao.deleteDailyReportForDate(uid, todayStr)
        
        // 3. Reset DailyStateEntity for today so it starts from zero
        val compositeId = "${uid}_${todayStr}"
        val emptyState = com.example.data.model.DailyStateEntity(
            compositeId = compositeId,
            uid = uid,
            date = todayStr,
            dailyScreenTimeMs = 0L,
            rewardTvTimeMs = 0L,
            adsWatchedCount = 0,
            dailyUsageCacheJson = "{}",
            temporaryAnalyticsJson = "{}",
            isSynced = false,
            lastUpdated = System.currentTimeMillis()
        )
        database.dailyStateDao.insertDailyState(emptyState)
    }

    /**
     * Dynamic smart generator of wellbeing analytics based on daily app metrics.
     */
    private fun generateInsightsList(
        stats: List<UsageStatisticEntity>,
        totalMs: Long,
        score: Int,
        mostUsedApp: String
    ): List<String> {
        val insightsList = mutableListOf<String>()

        val hours = totalMs / (1000 * 60 * 60)
        val minutes = (totalMs % (1000 * 60 * 60)) / (1000 * 60)

        insightsList.add("Today's Screen Time: ${hours}h ${minutes}m.")

        if (score >= 75) {
            insightsList.add("Excellent balance! Your productivity is outstanding ($score/100). Focus apps were prioritized.")
        } else if (score in 50..74) {
            insightsList.add("Moderate balance ($score/100). Try trimming down usage of $mostUsedApp.")
        } else {
            insightsList.add("Screen saturation is high. Social/Gaming apps lead today's usage. Consider taking a outdoor break.")
        }

        // Search for specific app categories to generate tailored advice
        var educationTime = 0L
        var socialTime = 0L
        var productivityTime = 0L

        for (s in stats) {
            when (s.category) {
                "EDUCATION" -> educationTime += s.durationMs
                "SOCIAL" -> socialTime += s.durationMs
                "PRODUCTIVITY" -> productivityTime += s.durationMs
            }
        }

        if (educationTime > 20 * 60 * 1000L) {
            insightsList.add("Awesome! You dedicated ${educationTime / 60000L} minutes to educational learning.")
        }
        if (socialTime > 60 * 60 * 1000L) {
            insightsList.add("Social platforms occupied ${(socialTime / 3600000L)} hours. Setting screen limits might free up your evening.")
        }
        if (productivityTime > 45 * 60 * 1000L) {
            insightsList.add("High utility! Your productivity workspace apps were actively used for ${productivityTime / 60000L} minutes.")
        }

        if (insightsList.size < 3) {
            insightsList.add("Try configuring app limits in settings to boost wellness points.")
        }

        return insightsList
    }

    /**
     * Seeds initial mock data for past days so charts are visually beautiful
     * when the system starts fresh on an empty device database.
     */
    suspend fun seedMockDatabaseDataIfEmpty() {
        // Only refresh today's data to ensure real initial stats exist
        refreshUsageData(Date())
    }
}
