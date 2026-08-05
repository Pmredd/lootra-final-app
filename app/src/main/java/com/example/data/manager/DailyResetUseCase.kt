package com.example.data.manager

import android.content.Context
import android.util.Log
import com.example.data.database.WellbeingDatabase
import com.example.data.model.DailyStateEntity
import com.example.data.model.DailyReportEntity
import com.example.data.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.*

class DailyResetUseCase(
    private val context: Context,
    private val database: WellbeingDatabase,
    private val usageRepository: UsageRepository,
    private val sessionManager: DailySessionManager
) {
    suspend fun checkAndExecuteReset(uid: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val storedDate = sessionManager.getStoredDate(uid)

        Log.d("DailyResetUseCase", "Checking daily reset for $uid. Stored: $storedDate, Today: $todayStr")

        if (storedDate != todayStr) {
            Log.i("DailyResetUseCase", "Date validation mismatch! Performing Daily Reset for $uid from $storedDate to $todayStr.")
            
            // 1. Create a fresh clean DailyStateEntity for today
            val compositeId = "${uid}_${todayStr}"
            val freshDailyState = DailyStateEntity(
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
            usageRepository.saveDailyState(freshDailyState)
            
            // 2. Clear previous unclaimed rewards to prevent carryover
            database.rewardDao.deleteUnclaimedRewardsForPreviousDays(uid, todayStr)
            
            // 3. Reset UserRewardSummary daily metrics
            val summary = database.rewardDao.getUserRewardSummary(uid)
            if (summary != null) {
                val resetSummary = summary.copy(
                    screenTimeMinutes = 0,
                    unlockedCoins = 0,
                    adsWatched = 0,
                    claimedRewards = 0,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                database.rewardDao.insertUserRewardSummary(resetSummary)
            }
            
            // 4. Force inject a fresh today's DailyReportEntity
            val newReport = DailyReportEntity(
                userId = uid,
                date = todayStr,
                totalScreenTimeMs = 0L,
                productivityScore = 100,
                mostUsedApp = "None",
                appOpenCount = 0,
                insights = "No screen usage logged yet today.",
                isSynced = false
            )
            database.dailyReportDao.insertDailyReport(newReport)

            // 5. Store/Save new verified date to commit
            sessionManager.saveStoredDate(uid, todayStr)
            Log.i("DailyResetUseCase", "Confirmed daily session validation reset for date $todayStr completed successfully.")
            
            // Sync DailyState to Firestore
            usageRepository.syncDailyStateToRemote(freshDailyState)
        } else {
            Log.d("DailyResetUseCase", "Same day detected. Skipping reset.")
        }
    }
}
