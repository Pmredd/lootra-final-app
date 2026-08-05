package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_statistics")
data class UsageStatisticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String = "unknown",
    val packageName: String,
    val appName: String,
    val durationMs: Long,
    val openCount: Int,
    val date: String, // format "yyyy-MM-dd"
    val category: String, // "PRODUCTIVITY", "SOCIAL", "ENTERTAINMENT", "EDUCATION", "OTHER"
    val isSynced: Boolean = false
)

@Entity(tableName = "daily_reports", primaryKeys = ["userId", "date"])
data class DailyReportEntity(
    val userId: String = "unknown",
    val date: String, // "yyyy-MM-dd"
    val totalScreenTimeMs: Long,
    val productivityScore: Int,
    val mostUsedApp: String,
    val appOpenCount: Int,
    val insights: String, // Semicolon-separated or JSON list of general insights
    val isSynced: Boolean = false
)

@Entity(tableName = "weekly_reports", primaryKeys = ["userId", "weekStartDate"])
data class WeeklyReportEntity(
    val userId: String = "unknown",
    val weekStartDate: String, // "yyyy-MM-dd"
    val totalScreenTimeMs: Long,
    val averageScreenTimeMs: Long,
    val productivityScore: Int,
    val mostUsedApp: String,
    val insightsList: String, // Semicolon-separated List of weekly insights
    val isSynced: Boolean = false
)

@Entity(tableName = "monthly_reports", primaryKeys = ["userId", "monthKey"])
data class MonthlyReportEntity(
    val userId: String = "unknown",
    val monthKey: String, // "yyyy-MM-dd"
    val totalScreenTimeMs: Long,
    val averageScreenTimeMs: Long,
    val productivityScore: Int,
    val mostUsedApp: String,
    val insightsList: String, // Semicolon-separated List of monthly insights
    val isSynced: Boolean = false
)
