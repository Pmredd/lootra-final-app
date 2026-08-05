package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "ad_history")
data class AdHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val rewardAmount: Int, // e.g., 5 coins
    val completedAdsCount: Int, // current count (1 or 2)
    val watchedAt: Long, // timestamp
    val status: String // "COMPLETED", "SKIPPED", "CLOSED", "FAILED"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "uid" to uid,
            "rewardAmount" to rewardAmount,
            "completedAdsCount" to completedAdsCount,
            "watchedAt" to watchedAt,
            "status" to status
        )
    }
}
