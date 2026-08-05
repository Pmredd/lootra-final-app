package com.example.data.dao

import androidx.room.*
import com.example.data.model.WalletEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.RewardEntity
import com.example.data.model.UserRewardSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE uid = :uid LIMIT 1")
    fun getWalletReactive(uid: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE uid = :uid LIMIT 1")
    suspend fun getWallet(uid: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE isSynced = 0")
    suspend fun getUnsyncedWallets(): List<WalletEntity>

    @Query("UPDATE wallets SET isSynced = 1 WHERE uid = :uid")
    suspend fun markWalletSynced(uid: String)

    @Query("DELETE FROM wallets WHERE uid = :uid")
    suspend fun deleteWalletForUser(uid: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE uid = :uid ORDER BY createdAt DESC")
    fun getTransactionsReactive(uid: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1 WHERE transactionId = :transactionId")
    suspend fun markTransactionSynced(transactionId: String)

    @Query("DELETE FROM transactions WHERE uid = :uid")
    suspend fun deleteTransactionsForUser(uid: String)
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE uid = :uid AND date = :date")
    fun getRewardsForDateReactive(uid: String, date: String): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE uid = :uid AND date = :date")
    suspend fun getRewardsForDate(uid: String, date: String): List<RewardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewards(rewards: List<RewardEntity>)

    @Query("SELECT * FROM rewards WHERE isSynced = 0")
    suspend fun getUnsyncedRewards(): List<RewardEntity>

    @Query("UPDATE rewards SET isSynced = 1 WHERE rewardId = :rewardId")
    suspend fun markRewardSynced(rewardId: String)

    @Query("DELETE FROM rewards WHERE uid = :uid AND date != :currentDate AND isClaimed = 0")
    suspend fun deleteUnclaimedRewardsForPreviousDays(uid: String, currentDate: String)

    @Query("DELETE FROM rewards WHERE uid = :uid")
    suspend fun deleteRewardsForUser(uid: String)

    @Query("DELETE FROM rewards WHERE uid = :uid AND date = :date")
    suspend fun deleteRewardsForDate(uid: String, date: String)

    @Query("SELECT * FROM user_rewards_summary WHERE uid = :uid LIMIT 1")
    fun getUserRewardSummaryReactive(uid: String): Flow<UserRewardSummaryEntity?>

    @Query("SELECT * FROM user_rewards_summary WHERE uid = :uid LIMIT 1")
    suspend fun getUserRewardSummary(uid: String): UserRewardSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserRewardSummary(summary: UserRewardSummaryEntity)

    @Query("SELECT * FROM user_rewards_summary WHERE isSynced = 0")
    suspend fun getUnsyncedUserRewardSummaries(): List<UserRewardSummaryEntity>

    @Query("UPDATE user_rewards_summary SET isSynced = 1 WHERE uid = :uid")
    suspend fun markUserRewardSummarySynced(uid: String)

    @Query("DELETE FROM user_rewards_summary WHERE uid = :uid")
    suspend fun deleteUserRewardSummaryForUser(uid: String)
}
