package com.example.data.manager

import android.content.Context
import android.util.Log
import com.example.data.database.WellbeingDatabase
import java.text.SimpleDateFormat
import java.util.*

class RewardTvResetManager(
    private val context: Context,
    private val database: WellbeingDatabase
) {
    suspend fun resetRewardTvForUser(uid: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.i("RewardTvResetManager", "Resetting Reward TV state for $uid on date $todayStr...")
        try {
            // Delete historic unclaimed rewards
            database.rewardDao.deleteUnclaimedRewardsForPreviousDays(uid, todayStr)
            
            // Re-initialize or clear ads count & screen time in summary
            val summary = database.rewardDao.getUserRewardSummary(uid)
            if (summary != null) {
                val cleanSummary = summary.copy(
                    screenTimeMinutes = 0,
                    unlockedCoins = 0,
                    adsWatched = 0,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                database.rewardDao.insertUserRewardSummary(cleanSummary)
            }
        } catch (e: Exception) {
            Log.e("RewardTvResetManager", "Error resetting Reward TV stats: ${e.message}")
        }
    }
}
