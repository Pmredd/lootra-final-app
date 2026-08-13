package com.example.data.repository

import android.util.Log
import com.example.data.dao.RewardDao
import com.example.data.dao.DailyReportDao
import com.example.data.model.RewardEntity
import com.example.data.model.UserRewardSummaryEntity
import com.example.data.model.WellbeingTvConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RewardRepository(
    private val rewardDao: RewardDao,
    private val dailyReportDao: DailyReportDao,
    private val walletRepository: WalletRepository,
    private val wellbeingSettingsRepository: WellbeingSettingsRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val configFlow: kotlinx.coroutines.flow.StateFlow<WellbeingTvConfig> = wellbeingSettingsRepository.configFlow

    fun getTodayRewardsReactive(uid: String): Flow<List<RewardEntity>> {
        val today = getTodayDateString()
        return flow {
            // First ensure levels are initialized
            prepareAndRefreshTodayRewards(uid)
            rewardDao.getRewardsForDateReactive(uid, today).collect {
                emit(it)
            }
        }
    }

    suspend fun getTodayRewardsList(uid: String): List<RewardEntity> {
        val today = getTodayDateString()
        prepareAndRefreshTodayRewards(uid)
        return rewardDao.getRewardsForDate(uid, today)
    }

    private suspend fun prepareAndRefreshTodayRewards(uid: String) {
        val today = getTodayDateString()
        
        // 1. Clean previous days unclaimed rewards
        rewardDao.deleteUnclaimedRewardsForPreviousDays(uid, today)

        // 2. Load daily report to check today's screen time
        val report = dailyReportDao.getDailyReportForDate(uid, today)
        val screenTimeMinutes = if (report != null) {
            report.totalScreenTimeMs / 60_000L
        } else {
            0L
        }

        // 3. Load or generate today's reward list
        val existing = rewardDao.getRewardsForDate(uid, today)
        val currentCoinsReward = configFlow.value.coinsReward
        
        Log.d("RewardRepository", "Reward Calculation")
        Log.d("RewardRepository", "Current Firestore Coins Reward: $currentCoinsReward")
        Log.d("RewardRepository", "Current Firestore Ads Required: ${configFlow.value.adsRequired}")
        if (currentCoinsReward == 5) {
            Log.d("RewardRepository", "If the reward still gives 5 Coins, the value 5 originates from: WellbeingTvConfig default parameter or initial cache fallback inside WellbeingSettingsRepository.")
        }

        val targets = listOf(
            30 to currentCoinsReward,
            60 to currentCoinsReward,
            90 to currentCoinsReward,
            120 to currentCoinsReward,
            150 to currentCoinsReward,
            180 to currentCoinsReward,
            210 to currentCoinsReward,
            240 to currentCoinsReward,
            270 to currentCoinsReward,
            300 to currentCoinsReward,
            330 to currentCoinsReward,
            360 to currentCoinsReward,
            390 to currentCoinsReward,
            420 to currentCoinsReward,
            450 to currentCoinsReward,
            480 to currentCoinsReward,
            510 to currentCoinsReward,
            540 to currentCoinsReward
        )

        val updatedList = mutableListOf<RewardEntity>()

        if (existing.isEmpty()) {
            // Seed fresh daily rewards
            for ((threshold, coins) in targets) {
                val rId = "${uid}_${today}_${threshold}"
                val unlocked = screenTimeMinutes >= threshold
                val newReward = RewardEntity(
                    rewardId = rId,
                    uid = uid,
                    thresholdMinutes = threshold,
                    coins = coins,
                    date = today,
                    isUnlocked = unlocked,
                    adsWatchedCount = 0,
                    isClaimed = false,
                    isSynced = false
                )
                updatedList.add(newReward)
            }
            rewardDao.insertRewards(updatedList)
        } else {
            // Check if any can be dynamic updated to unlocked or coins updated if not claimed
            for (r in existing) {
                val unlocked = screenTimeMinutes >= r.thresholdMinutes
                val updatedCoins = if (!r.isClaimed) currentCoinsReward else r.coins
                if (r.isUnlocked != unlocked || r.coins != updatedCoins) {
                    updatedList.add(r.copy(isUnlocked = unlocked, coins = updatedCoins, isSynced = false))
                } else {
                    updatedList.add(r)
                }
            }
            if (updatedList.any { it.isSynced == false }) {
                rewardDao.insertRewards(updatedList)
            }
        }

        try {
            val sum = getOrCreateLocalRewardSummary(uid)
            val updatedSum = sum.copy(
                screenTimeMinutes = screenTimeMinutes.toInt(),
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            rewardDao.insertUserRewardSummary(updatedSum)
            syncRewardSummaryToRemote(updatedSum)
        } catch (e: Exception) {
            Log.e("RewardRepository", "Error updating reward summary screenTimeMinutes: ${e.message}")
        }
    }

    suspend fun watchAdForReward(uid: String, rewardId: String): Boolean {
        val config = configFlow.value
        if (!config.rewardEnabled) {
            Log.d("RewardRepository", "Reward TV is currently disabled by Super Admin.")
            return false
        }

        val today = getTodayDateString()
        val existing = rewardDao.getRewardsForDate(uid, today)
        val reward = existing.find { it.rewardId == rewardId } ?: return false

        if (!reward.isUnlocked || reward.isClaimed) return false
        val adsReq = config.adsRequired
        if (reward.adsWatchedCount >= adsReq) return true

        val updated = reward.copy(
            adsWatchedCount = reward.adsWatchedCount + 1,
            isSynced = false
        )
        rewardDao.insertRewards(listOf(updated))
        
        try {
            val sum = getOrCreateLocalRewardSummary(uid)
            val updatedSum = sum.copy(
                adsWatched = sum.adsWatched + 1,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            rewardDao.insertUserRewardSummary(updatedSum)
            syncRewardSummaryToRemote(updatedSum)
        } catch (e: Exception) {
            Log.e("RewardRepository", "Error updating reward summary adsWatched: ${e.message}")
        }
        
        // Trigger background sync for reward entity
        syncRewardToRemote(updated)
        return true
    }

    suspend fun claimRewardCoins(uid: String, rewardId: String): Boolean {
        val config = configFlow.value
        if (!config.rewardEnabled) {
            Log.d("RewardRepository", "Reward TV is currently disabled by Super Admin.")
            return false
        }

        val today = getTodayDateString()
        val existing = rewardDao.getRewardsForDate(uid, today)
        val reward = existing.find { it.rewardId == rewardId } ?: return false

        val adsReq = config.adsRequired
        if (reward.isClaimed || reward.adsWatchedCount < adsReq) return false

        // Check Daily Limits
        val todayClaimedCount = existing.count { it.isClaimed }
        if (todayClaimedCount >= config.dailyRewardLimit) {
            Log.w("RewardRepository", "Cannot claim reward: Daily reward claim limit reached ($todayClaimedCount >= ${config.dailyRewardLimit})")
            return false
        }

        val todayClaimedCoins = existing.filter { it.isClaimed }.sumOf { it.coins }
        if (todayClaimedCoins + reward.coins > config.dailyCoinLimit) {
            Log.w("RewardRepository", "Cannot claim reward: Daily coin limit reached ($todayClaimedCoins + ${reward.coins} > ${config.dailyCoinLimit})")
            return false
        }

        // 1. Mark claimed in Room
        val updated = reward.copy(
            isClaimed = true,
            isSynced = false
        )
        rewardDao.insertRewards(listOf(updated))

        try {
            val sum = getOrCreateLocalRewardSummary(uid)
            val updatedSum = sum.copy(
                unlockedCoins = sum.unlockedCoins + reward.coins,
                claimedRewards = sum.claimedRewards + 1,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            rewardDao.insertUserRewardSummary(updatedSum)
            syncRewardSummaryToRemote(updatedSum)
        } catch (e: Exception) {
            Log.e("RewardRepository", "Error updating reward summary stats: ${e.message}")
        }

        // 2. Add Coins to Wallet (handling local insert, transaction, and immediate firestore post)
        Log.d("RewardRepository", "Wallet Update")
        Log.d("RewardRepository", "Coins granted to wallet: ${reward.coins}")
        Log.d("RewardRepository", "Current Firestore Coins Reward: ${config.coinsReward}")
        Log.d("RewardRepository", "Current Firestore Ads Required: ${config.adsRequired}")
        if (reward.coins == 5) {
            Log.d("RewardRepository", "If the reward still gives 5 Coins, the value 5 originates from: WellbeingTvConfig default parameter or initial cache fallback inside WellbeingSettingsRepository.")
        }

        walletRepository.addCoins(
            uid = uid,
            amount = reward.coins,
            type = "REWARD_EARNED",
            description = "Claimed TV screen time target: ${reward.thresholdMinutes} mins"
        )

        // 3. Attempt sync
        syncRewardToRemote(updated)
        return true
    }

    suspend fun fetchRewardsFromRemote(uid: String) {
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000) {
                val snapshot = firestore.collection("rewards")
                    .document(uid)
                    .collection("daily_rewards")
                    .get()
                    .await()
                val rewardsList = mutableListOf<RewardEntity>()
                for (doc in snapshot.documents) {
                    val data = doc.data
                    if (data != null) {
                        rewardsList.add(RewardEntity.fromMap(data))
                    }
                }
                if (rewardsList.isNotEmpty()) {
                    rewardDao.insertRewards(rewardsList)
                    Log.d("RewardRepository", "Fetched and cached ${rewardsList.size} remote rewards for $uid")
                }
            }
        } catch (e: Exception) {
            Log.e("RewardRepository", "Error fetching remote rewards for $uid: ${e.message}")
        }
    }

    suspend fun syncRewardToRemote(reward: RewardEntity): Boolean {
        return try {
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                firestore.collection("rewards")
                    .document(reward.uid)
                    .collection("daily_rewards")
                    .document(reward.rewardId)
                    .set(reward.toMap())
                    .await()
                true
            }
            if (success == true) {
                rewardDao.markRewardSynced(reward.rewardId)
                Log.d("RewardRepository", "Synced reward ${reward.rewardId} to Firestore under rewards/${reward.uid}/daily_rewards")
                true
            } else {
                Log.e("RewardRepository", "Firestore reward sync timed out for ${reward.rewardId}")
                false
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("RewardRepository", "Firestore reward sync failed: ${e.message}")
            false
        }
    }

    suspend fun syncUnsyncedRewards(uid: String) {
        try {
            val unsynced = rewardDao.getUnsyncedRewards()
            for (r in unsynced) {
                if (r.uid == uid) {
                    syncRewardToRemote(r)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("RewardRepository", "Error syncing unsynced rewards: ${e.message}")
        }
    }

    suspend fun getOrCreateLocalRewardSummary(uid: String): UserRewardSummaryEntity {
        val existing = rewardDao.getUserRewardSummary(uid)
        if (existing != null) {
            if (!existing.isSynced) {
                // Retry syncing previously unsynced local summary
                syncRewardSummaryToRemote(existing)
            }
            return existing
        }

        val remoteSummary = fetchRewardSummaryFromRemote(uid)
        if (remoteSummary != null) {
            rewardDao.insertUserRewardSummary(remoteSummary)
            return remoteSummary
        }

        val newSummary = UserRewardSummaryEntity(
            uid = uid,
            screenTimeMinutes = 0,
            unlockedCoins = 0,
            adsWatched = 0,
            claimedRewards = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        rewardDao.insertUserRewardSummary(newSummary)
        syncRewardSummaryToRemote(newSummary)
        return newSummary
    }

    suspend fun fetchRewardSummaryFromRemote(uid: String): UserRewardSummaryEntity? {
        return try {
            kotlinx.coroutines.withTimeoutOrNull(10000) {
                val doc = firestore.collection("rewards")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    val data = doc.data
                    val summary = UserRewardSummaryEntity.fromMap(data)
                    rewardDao.insertUserRewardSummary(summary)
                    Log.d("RewardRepository", "Fetched and cached remote rewards summary for $uid")
                    summary
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("RewardRepository", "Error fetching remote rewards summary for $uid: ${e.message}")
            null
        }
    }

    suspend fun syncRewardSummaryToRemote(summary: UserRewardSummaryEntity): Boolean {
        return try {
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                firestore.collection("rewards")
                    .document(summary.uid)
                    .set(summary.toMap())
                    .await()
                true
            }
            if (success == true) {
                rewardDao.markUserRewardSummarySynced(summary.uid)
                Log.d("RewardRepository", "Synced rewards summary to Firestore for ${summary.uid}")
                true
            } else {
                Log.e("RewardRepository", "Firestore reward summary sync timed out for ${summary.uid}")
                false
            }
        } catch (e: Exception) {
            Log.e("RewardRepository", "Firestore reward summary sync failed: ${e.message}")
            false
        }
    }

    suspend fun deleteLocalRewardsForUser(uid: String) {
        try {
            rewardDao.deleteRewardsForUser(uid)
            rewardDao.deleteUserRewardSummaryForUser(uid)
            Log.d("RewardRepository", "Successfully cleared local rewards cache for $uid")
        } catch (e: Exception) {
            Log.e("RewardRepository", "Error clearing local rewards cache: ${e.message}")
        }
    }

    suspend fun clearRewardSessionOnLogout(uid: String) {
        val today = getTodayDateString()
        // 1. Delete today's reward status so they regenerates
        rewardDao.deleteRewardsForDate(uid, today)
        // 2. Clear reward summary active metrics
        val summary = rewardDao.getUserRewardSummary(uid)
        if (summary != null) {
            val emptySummary = summary.copy(
                screenTimeMinutes = 0,
                unlockedCoins = 0,
                adsWatched = 0,
                claimedRewards = 0,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            rewardDao.insertUserRewardSummary(emptySummary)
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
