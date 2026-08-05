package com.example.data.manager

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.example.data.model.UsageStatisticEntity
import java.text.SimpleDateFormat
import java.util.*

class DeviceUsageStatsService {

    private var lastPermissionCheckTime: Long = 0
    private var lastPermissionResult: Boolean = false

    private fun getAttributionContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.createAttributionContext("default")
        } else {
            context
        }
    }

    fun isUsageAccessPermissionGranted(context: Context): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPermissionCheckTime < 5000) {
            return lastPermissionResult
        }

        val attrContext = getAttributionContext(context)
        val appOps = attrContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val result = if (appOps != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    attrContext.packageName
                ) == AppOpsManager.MODE_ALLOWED
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    attrContext.packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
        } else {
            false
        }

        lastPermissionCheckTime = currentTime
        lastPermissionResult = result
        return result
    }

    fun fetchDeviceUsageStats(context: Context, date: Date, userId: String = "unknown"): List<UsageStatisticEntity> {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

        if (!isUsageAccessPermissionGranted(context)) {
            return emptyList()
        }

        val attrContext = getAttributionContext(context)
        val usageStatsManager = attrContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = startTime + 24 * 60 * 60 * 1000L

        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        if (statsMap.isNullOrEmpty()) {
            return emptyList()
        }

        val pm = context.packageManager
        val entities = mutableListOf<UsageStatisticEntity>()

        for ((packageName, usageStats) in statsMap) {
            val durationMs = usageStats.totalTimeInForeground
            if (durationMs > 1000) {
                val appLabel = getAppName(pm, packageName)
                val category = determineAppCategory(pm, packageName)
                val openCount = getOpenCount(usageStats)

                entities.add(
                    UsageStatisticEntity(
                        userId = userId,
                        packageName = packageName,
                        appName = appLabel,
                        durationMs = durationMs,
                        openCount = openCount,
                        date = dateString,
                        category = category
                    )
                )
            }
        }

        entities.sortByDescending { it.durationMs }

        return entities.ifEmpty { emptyList() }
    }

    private fun getAppName(pm: PackageManager, packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        }
    }

    private fun determineAppCategory(pm: PackageManager, packageName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> return "GAMING"
                    ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> return "ENTERTAINMENT"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "PRODUCTIVITY"
                    ApplicationInfo.CATEGORY_MAPS -> return "PRODUCTIVITY"
                    ApplicationInfo.CATEGORY_SOCIAL -> return "SOCIAL"
                    ApplicationInfo.CATEGORY_NEWS -> return "EDUCATION"
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // ignore
            }
        }

        val pkg = packageName.lowercase()
        return when {
            pkg.contains("game") || pkg.contains("pubg") || pkg.contains("clash") || pkg.contains("fortnite") || pkg.contains("minecraft") -> "GAMING"
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") || pkg.contains("disney") || pkg.contains("hulu") || pkg.contains("tiktok") -> "ENTERTAINMENT"
            pkg.contains("slack") || pkg.contains("teams") || pkg.contains("whatsapp") || pkg.contains("skype") || pkg.contains("telegram") || pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("twitter") || pkg.contains("linkedin") -> "SOCIAL"
            pkg.contains("duolingo") || pkg.contains("coursera") || pkg.contains("edx") || pkg.contains("udemy") || pkg.contains("quizlet") || pkg.contains("khan") || pkg.contains("dictionary") -> "EDUCATION"
            pkg.contains("excel") || pkg.contains("word") || pkg.contains("docs") || pkg.contains("drive") || pkg.contains("dropbox") || pkg.contains("keep") || pkg.contains("todo") || pkg.contains("calendar") || pkg.contains("email") || pkg.contains("gmail") || pkg.contains("productivity") || pkg.contains("notion") -> "PRODUCTIVITY"
            else -> "OTHER"
        }
    }

    private fun getOpenCount(usageStats: UsageStats): Int {
        return try {
            val field = usageStats.javaClass.getDeclaredField("mLaunchCount")
            field.isAccessible = true
            field.getInt(usageStats)
        } catch (e: Exception) {
            try {
                val method = usageStats.javaClass.getDeclaredMethod("getAppLaunchCount")
                method.isAccessible = true
                (method.invoke(usageStats) as? Int) ?: 1
            } catch (e2: Exception) {
                val mins = usageStats.totalTimeInForeground / 60000L
                (mins / 5L + 1).toInt().coerceAtLeast(1)
            }
        }
    }

    fun calculateProductivityScore(stats: List<UsageStatisticEntity>): Int {
        if (stats.isEmpty()) return 100 
        var totalDuration = 0L
        var productiveDuration = 0L
        var educationalDuration = 0L
        var socialDuration = 0L
        var gamingDuration = 0L
        var entertainmentDuration = 0L

        for (stat in stats) {
            totalDuration += stat.durationMs
            when (stat.category) {
                "PRODUCTIVITY" -> productiveDuration += stat.durationMs
                "EDUCATION" -> educationalDuration += stat.durationMs
                "SOCIAL" -> socialDuration += stat.durationMs
                "GAMING" -> gamingDuration += stat.durationMs
                "ENTERTAINMENT" -> entertainmentDuration += stat.durationMs
            }
        }

        if (totalDuration == 0L) return 100

        val pProd = productiveDuration.toDouble() / totalDuration
        val pEdu = educationalDuration.toDouble() / totalDuration
        val pSocial = socialDuration.toDouble() / totalDuration
        val pGame = gamingDuration.toDouble() / totalDuration
        val pEnt = entertainmentDuration.toDouble() / totalDuration

        val delta = (pProd * 45) + (pEdu * 50) - (pSocial * 35) - (pGame * 45) - (pEnt * 15)
        val score = (60.0 + delta).toInt()

        return score.coerceIn(0, 100)
    }

    fun generateSimulationStats(dateString: String): List<UsageStatisticEntity> {
        return emptyList()
    }
}
