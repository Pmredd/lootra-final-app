package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity

@Keep
@Entity(tableName = "watched_reels", primaryKeys = ["uid", "reelId"])
data class WatchedReelEntity(
    val uid: String = "",
    val reelId: String = "",
    val watchedAt: Long = 0L,
    val rewardGiven: Boolean = false,
    val rewardCoins: Int = 0,
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "reelId" to reelId,
            "watchedAt" to watchedAt,
            "rewardGiven" to rewardGiven,
            "rewardCoins" to rewardCoins
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?, uid: String): WatchedReelEntity {
            if (map == null) return WatchedReelEntity()
            return WatchedReelEntity(
                uid = uid,
                reelId = map["reelId"] as? String ?: "",
                watchedAt = (map["watchedAt"] as? Number)?.toLong() ?: 0L,
                rewardGiven = map["rewardGiven"] as? Boolean ?: false,
                rewardCoins = (map["rewardCoins"] as? Number)?.toInt() ?: 0,
                isSynced = true
            )
        }
    }
}
