package com.example.data.dao

import androidx.room.*
import com.example.data.model.DailyReportEntity
import com.example.data.model.MonthlyReportEntity
import com.example.data.model.UsageStatisticEntity
import com.example.data.model.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageStatisticDao {
    @Query("SELECT * FROM usage_statistics WHERE userId = :userId AND date = :date ORDER BY durationMs DESC")
    fun getUsageStatsForDate(userId: String, date: String): Flow<List<UsageStatisticEntity>>

    @Query("SELECT * FROM usage_statistics WHERE userId = :userId ORDER BY durationMs DESC")
    fun getAllUsageStats(userId: String): Flow<List<UsageStatisticEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStats(stats: List<UsageStatisticEntity>)

    @Query("DELETE FROM usage_statistics WHERE userId = :userId AND date = :date")
    suspend fun deleteUsageStatsForDate(userId: String, date: String)
}

@Dao
interface DailyReportDao {
    @Query("SELECT * FROM daily_reports WHERE userId = :userId ORDER BY date DESC")
    fun getAllDailyReportsReactive(userId: String): Flow<List<DailyReportEntity>>

    @Query("SELECT * FROM daily_reports WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getDailyReportForDate(userId: String, date: String): DailyReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReport(report: DailyReportEntity)

    @Query("SELECT * FROM daily_reports WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyReports(userId: String, limit: Int): Flow<List<DailyReportEntity>>

    @Query("SELECT * FROM daily_reports WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedDailyReports(userId: String): List<DailyReportEntity>

    @Query("UPDATE daily_reports SET isSynced = 1 WHERE userId = :userId AND date = :date")
    suspend fun markDailyReportSynced(userId: String, date: String)

    @Query("DELETE FROM daily_reports WHERE userId = :userId AND date = :date")
    suspend fun deleteDailyReportForDate(userId: String, date: String)
}

@Dao
interface WeeklyReportDao {
    @Query("SELECT * FROM weekly_reports WHERE userId = :userId ORDER BY weekStartDate DESC")
    fun getAllWeeklyReports(userId: String): Flow<List<WeeklyReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyReport(report: WeeklyReportEntity)
}

@Dao
interface MonthlyReportDao {
    @Query("SELECT * FROM monthly_reports WHERE userId = :userId ORDER BY monthKey DESC")
    fun getAllMonthlyReports(userId: String): Flow<List<MonthlyReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyReport(report: MonthlyReportEntity)

    @Query("SELECT * FROM monthly_reports WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedMonthlyReports(userId: String): List<MonthlyReportEntity>

    @Query("UPDATE monthly_reports SET isSynced = 1 WHERE userId = :userId AND monthKey = :monthKey")
    suspend fun markMonthlyReportSynced(userId: String, monthKey: String)
}
