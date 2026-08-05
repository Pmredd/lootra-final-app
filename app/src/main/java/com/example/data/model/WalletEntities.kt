package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val uid: String = "",
    val totalCoins: Int = 0,
    val todayCoins: Int = 0,
    val lifetimeCoins: Int = 0,
    val pendingCoins: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "totalCoins" to totalCoins,
            "todayCoins" to todayCoins,
            "lifetimeCoins" to lifetimeCoins,
            "pendingCoins" to pendingCoins,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): WalletEntity {
            if (map == null) return WalletEntity()
            return WalletEntity(
                uid = map["uid"] as? String ?: "",
                totalCoins = (map["totalCoins"] as? Number)?.toInt() ?: 0,
                todayCoins = (map["todayCoins"] as? Number)?.toInt() ?: 0,
                lifetimeCoins = (map["lifetimeCoins"] as? Number)?.toInt() ?: 0,
                pendingCoins = (map["pendingCoins"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isSynced = true // Pulled from remote
            )
        }
    }
}

@Keep
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val transactionId: String = "",
    val uid: String = "",
    val type: String = "REWARD_EARNED", // "REWARD_EARNED", "BONUS_EARNED", "FUTURE_PURCHASE", "FUTURE_REFUND"
    val amount: Int = 0,
    val description: String = "",
    val createdAt: Long = 0L,
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "transactionId" to transactionId,
            "uid" to uid,
            "type" to type,
            "amount" to amount,
            "description" to description,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): TransactionEntity {
            if (map == null) return TransactionEntity()
            return TransactionEntity(
                transactionId = map["transactionId"] as? String ?: "",
                uid = map["uid"] as? String ?: "",
                type = map["type"] as? String ?: "REWARD_EARNED",
                amount = (map["amount"] as? Number)?.toInt() ?: 0,
                description = map["description"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                isSynced = true
            )
        }
    }
}

@Keep
@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val rewardId: String = "", // formatted: "uid_date_threshold"
    val uid: String = "",
    val thresholdMinutes: Int = 0, // e.g., 30, 60, 120, 300
    val coins: Int = 0,
    val date: String = "", // "yyyy-MM-dd"
    val isUnlocked: Boolean = false,
    val adsWatchedCount: Int = 0, // 0, 1, 2
    val isClaimed: Boolean = false,
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "rewardId" to rewardId,
            "uid" to uid,
            "thresholdMinutes" to thresholdMinutes,
            "coins" to coins,
            "date" to date,
            "isUnlocked" to isUnlocked,
            "adsWatchedCount" to adsWatchedCount,
            "isClaimed" to isClaimed
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): RewardEntity {
            if (map == null) return RewardEntity()
            return RewardEntity(
                rewardId = map["rewardId"] as? String ?: "",
                uid = map["uid"] as? String ?: "",
                thresholdMinutes = (map["thresholdMinutes"] as? Number)?.toInt() ?: 0,
                coins = (map["coins"] as? Number)?.toInt() ?: 0,
                date = map["date"] as? String ?: "",
                isUnlocked = map["isUnlocked"] as? Boolean ?: false,
                adsWatchedCount = (map["adsWatchedCount"] as? Number)?.toInt() ?: 0,
                isClaimed = map["isClaimed"] as? Boolean ?: false,
                isSynced = true
            )
        }
    }
}

@Keep
@Entity(tableName = "user_rewards_summary")
data class UserRewardSummaryEntity(
    @PrimaryKey val uid: String = "",
    val screenTimeMinutes: Int = 0,
    val unlockedCoins: Int = 0,
    val adsWatched: Int = 0,
    val claimedRewards: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "screenTimeMinutes" to screenTimeMinutes,
            "unlockedCoins" to unlockedCoins,
            "adsWatched" to adsWatched,
            "claimedRewards" to claimedRewards,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): UserRewardSummaryEntity {
            if (map == null) return UserRewardSummaryEntity()
            return UserRewardSummaryEntity(
                uid = map["uid"] as? String ?: "",
                screenTimeMinutes = (map["screenTimeMinutes"] as? Number)?.toInt() ?: 0,
                unlockedCoins = (map["unlockedCoins"] as? Number)?.toInt() ?: 0,
                adsWatched = (map["adsWatched"] as? Number)?.toInt() ?: 0,
                claimedRewards = (map["claimedRewards"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isSynced = true
            )
        }
    }
}
