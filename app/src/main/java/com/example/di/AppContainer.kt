package com.example.di

import android.content.Context
import com.example.data.database.WellbeingDatabase
import com.example.data.datastore.WellbeingPreferences
import com.example.data.manager.DeviceUsageStatsService
import com.example.data.repository.WellbeingRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.FirestoreRepositoryImpl
import com.example.data.repository.StorageRepository
import com.example.data.repository.StorageRepositoryImpl
import com.example.data.repository.WalletRepository
import com.example.data.repository.RewardRepository
import com.example.data.repository.WellbeingSettingsRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.UsageRepository
import com.example.data.manager.DailySessionManager
import com.example.data.manager.DailyResetUseCase
import com.example.data.manager.SessionValidator
import com.example.data.manager.LoginObserver
import com.example.data.manager.RewardTvResetManager

interface AppContainer {
    val repository: WellbeingRepository
    val authRepository: AuthRepository
    val firestoreRepository: FirestoreRepository
    val storageRepository: StorageRepository
    val walletRepository: WalletRepository
    val rewardRepository: RewardRepository
    val wellbeingSettingsRepository: WellbeingSettingsRepository
    val productRepository: ProductRepository
    val orderRepository: OrderRepository
    val adminRepository: com.example.data.repository.AdminRepository
    val reelRepository: com.example.data.repository.ReelRepository
    val preferences: WellbeingPreferences
    val adManager: com.example.data.manager.AdManager
    val usageRepository: UsageRepository
    val dailySessionManager: DailySessionManager
    val dailyResetUseCase: DailyResetUseCase
    val sessionValidator: SessionValidator
    val loginObserver: LoginObserver
    val rewardTvResetManager: RewardTvResetManager
    val promotionPlanRepository: com.example.data.repository.PromotionPlanRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {

    private val database: WellbeingDatabase by lazy {
        WellbeingDatabase.getDatabase(context)
    }

    override val usageRepository: UsageRepository by lazy {
        UsageRepository(database.dailyStateDao)
    }

    override val dailySessionManager: DailySessionManager by lazy {
        DailySessionManager(context)
    }

    override val dailyResetUseCase: DailyResetUseCase by lazy {
        DailyResetUseCase(
            context = context,
            database = database,
            usageRepository = usageRepository,
            sessionManager = dailySessionManager
        )
    }

    override val sessionValidator: SessionValidator by lazy {
        SessionValidator(dailySessionManager)
    }

    override val loginObserver: LoginObserver by lazy {
        LoginObserver(context, dailyResetUseCase)
    }

    override val rewardTvResetManager: RewardTvResetManager by lazy {
        RewardTvResetManager(context, database)
    }

    override val adManager: com.example.data.manager.AdManager by lazy {
        com.example.data.manager.RewardedAdManager(context, database.adHistoryDao)
    }

    override val preferences: WellbeingPreferences by lazy {
        WellbeingPreferences(context)
    }

    private val statsService: DeviceUsageStatsService by lazy {
        DeviceUsageStatsService()
    }

    override val repository: WellbeingRepository by lazy {
        WellbeingRepository(
            context = context,
            database = database,
            preferences = preferences,
            statsService = statsService
        )
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }

    override val firestoreRepository: FirestoreRepository by lazy {
        FirestoreRepositoryImpl()
    }

    override val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl()
    }

    override val walletRepository: WalletRepository by lazy {
        WalletRepository(
            walletDao = database.walletDao,
            transactionDao = database.transactionDao
        )
    }

    override val wellbeingSettingsRepository: WellbeingSettingsRepository by lazy {
        WellbeingSettingsRepository(context)
    }

    override val rewardRepository: RewardRepository by lazy {
        RewardRepository(
            rewardDao = database.rewardDao,
            dailyReportDao = database.dailyReportDao,
            walletRepository = walletRepository,
            wellbeingSettingsRepository = wellbeingSettingsRepository
        )
    }

    override val productRepository: ProductRepository by lazy {
        ProductRepository(
            productDao = database.productDao
        )
    }

    override val orderRepository: OrderRepository by lazy {
        OrderRepository(
            orderDao = database.orderDao
        )
    }

    override val adminRepository: com.example.data.repository.AdminRepository by lazy {
        com.example.data.repository.AdminRepository(
            productDao = database.productDao,
            orderDao = database.orderDao
        )
    }

    override val reelRepository: com.example.data.repository.ReelRepository by lazy {
        com.example.data.repository.ReelRepository(
            reelDao = database.reelDao,
            walletRepository = walletRepository
        )
    }

    override val promotionPlanRepository: com.example.data.repository.PromotionPlanRepository by lazy {
        com.example.data.repository.PromotionPlanRepository(context)
    }
}
