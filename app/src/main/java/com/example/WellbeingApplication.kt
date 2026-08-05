package com.example

import android.app.Application
import android.os.FileObserver
import androidx.work.*
import com.example.di.AppContainer
import com.example.di.AppContainerImpl
import com.example.worker.WellbeingSyncWorker
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WellbeingApplication : Application() {

    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Proactively clear any leftover notifications from old service processes to prevent System UI I/O errors
        try {
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(4529)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize dependency container
        container = AppContainerImpl(this)

        // Start LoginObserver to trigger Date Validation Resets and handle account isolation
        container.loginObserver.startObserving()

        // Initialize Firebase automatically when the application starts
        try {
            FirebaseApp.initializeApp(this)
            com.example.data.manager.AppControlManager.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Cloudinary MediaManager securely for Reels upload
        try {
            val config = mapOf(
                "cloud_name" to "ywc2lsz9",
                "secure" to true
            )
            com.cloudinary.android.MediaManager.init(this, config)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Google Mobile Ads SDK for AdMob Integration
        try {
            com.google.android.gms.ads.MobileAds.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Test Firestore connection on launch and seed database if empty
        applicationScope.launch {
            try {
                container.firestoreRepository.testFirestoreConnection()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                container.repository.seedMockDatabaseDataIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                container.reelRepository.seedAndRefreshReels()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Schedule WorkManager periodic synchronization
        schedulePeriodicUsageSync()
    }

    private fun schedulePeriodicUsageSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) 
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<WellbeingSyncWorker>(
            1, TimeUnit.HOURS // Repeat every hour
        )
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            10000L,
            TimeUnit.MILLISECONDS
        )
        .build()

        try {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "wellbeing_stats_sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (e: IllegalStateException) {
            // Fallback gracefully under JVM test environments
            e.printStackTrace()
        }
    }
}
