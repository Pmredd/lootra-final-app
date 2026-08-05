package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.DailyReportDao
import com.example.data.dao.MonthlyReportDao
import com.example.data.dao.UsageStatisticDao
import com.example.data.dao.WeeklyReportDao
import com.example.data.dao.WalletDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.RewardDao
import com.example.data.model.DailyReportEntity
import com.example.data.model.MonthlyReportEntity
import com.example.data.model.UsageStatisticEntity
import com.example.data.model.WeeklyReportEntity
import com.example.data.model.WalletEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.RewardEntity
import com.example.data.model.UserRewardSummaryEntity
import com.example.data.model.ProductEntity
import com.example.data.model.OrderEntity
import com.example.data.dao.ProductDao
import com.example.data.dao.OrderDao
import com.example.data.dao.DailyStateDao
import com.example.data.dao.AdHistoryDao
import com.example.data.dao.ReelDao
import com.example.data.model.DailyStateEntity
import com.example.data.model.AdHistoryEntity
import com.example.data.model.ReelEntity
import com.example.data.model.WatchedReelEntity
import com.example.data.model.ReelLikeEntity

@Database(
    entities = [
        UsageStatisticEntity::class,
        DailyReportEntity::class,
        WeeklyReportEntity::class,
        MonthlyReportEntity::class,
        WalletEntity::class,
        TransactionEntity::class,
        RewardEntity::class,
        UserRewardSummaryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        DailyStateEntity::class,
        AdHistoryEntity::class,
        ReelEntity::class,
        WatchedReelEntity::class,
        ReelLikeEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class WellbeingDatabase : RoomDatabase() {
    abstract val usageStatisticDao: UsageStatisticDao
    abstract val dailyReportDao: DailyReportDao
    abstract val weeklyReportDao: WeeklyReportDao
    abstract val monthlyReportDao: MonthlyReportDao
    abstract val walletDao: WalletDao
    abstract val transactionDao: TransactionDao
    abstract val rewardDao: RewardDao
    abstract val productDao: ProductDao
    abstract val orderDao: OrderDao
    abstract val dailyStateDao: DailyStateDao
    abstract val adHistoryDao: AdHistoryDao
    abstract val reelDao: ReelDao

    companion object {
        @Volatile
        private var INSTANCE: WellbeingDatabase? = null

        fun getDatabase(context: Context): WellbeingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WellbeingDatabase::class.java,
                    "loot_wellbeing_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
