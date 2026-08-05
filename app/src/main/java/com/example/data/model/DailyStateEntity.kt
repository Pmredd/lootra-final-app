package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_daily_states")
data class DailyStateEntity(
    @PrimaryKey val compositeId: String, // format: "uid_date"
    val uid: String,
    val date: String, // "yyyy-MM-dd"
    val dailyScreenTimeMs: Long = 0L,
    val rewardTvTimeMs: Long = 0L,
    val adsWatchedCount: Int = 0,
    val dailyUsageCacheJson: String = "{}",
    val temporaryAnalyticsJson: String = "{}",
    val isSynced: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
