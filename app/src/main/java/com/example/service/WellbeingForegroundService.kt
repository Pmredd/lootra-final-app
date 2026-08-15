package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.WellbeingApplication
import com.example.data.database.WellbeingDatabase
import com.example.data.model.DailyReportEntity
import com.example.data.repository.FirestoreRepositoryImpl
import com.example.data.repository.WellbeingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WellbeingForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var trackingJob: Job? = null
    
    private val NOTIFICATION_ID = 4529
    private val CHANNEL_ID = "wellbeing_tracking_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WellbeingFGS", "Foreground tracking service starting...")
        
        // Build initial notification
        val notification = buildNotification("Monitoring screen usage", "Lootra Wellbeing is active")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("WellbeingFGS", "Failed to start foreground service: ${e.message}")
        }

        startTrackingLoop()
        return START_STICKY
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            var lastCheckedDateStr = getCurrentDateString()
            
            while (isActive) {
                val context = this@WellbeingForegroundService
                val app = applicationContext as? WellbeingApplication
                
                try {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.currentUser?.uid
                    
                    // 1. Validate Single Device Session
                    if (uid != null) {
                        val currentDeviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                        val firestoreRepo = app?.container?.firestoreRepository ?: FirestoreRepositoryImpl()
                        
                        val userResult = firestoreRepo.getUserProfile(uid)
                        if (userResult.isSuccess) {
                            val profile = userResult.getOrNull()
                            if (profile != null) {
                                // If mismatch exists and sessionActive is true
                                if (profile.sessionActive && profile.activeDeviceId.isNotEmpty() && profile.activeDeviceId != currentDeviceId) {
                                    Log.e("WellbeingFGS", "Session mismatch detected! Active device id: ${profile.activeDeviceId}, current: $currentDeviceId. FORCING LOGOUT.")
                                    
                                    // Trigger broad force-logout Broadcast to MainActivity to clear stack
                                    val forceLogoutIntent = Intent("com.example.ACTION_FORCE_LOGOUT")
                                    sendBroadcast(forceLogoutIntent)
                                    
                                    // Stop service and stop co-routine
                                    stopSelf()
                                    break
                                }
                            }
                        }
                    }

                    // 2. Refresh Wellbeing stats to Room database
                    val repository = app?.container?.repository
                    if (repository != null) {
                        val todayDate = Date()
                        repository.refreshUsageData(todayDate)
                        
                        // Perform reactive date validation reset if the date has changed
                        if (uid != null) {
                            app.container.dailyResetUseCase.checkAndExecuteReset(uid)
                        }

                        // Retrieve today's screen time
                        val currentDateStr = getCurrentDateString()
                        val dailyReport = if (uid != null) {
                            WellbeingDatabase.getDatabase(context).dailyReportDao.getDailyReportForDate(uid, currentDateStr)
                        } else {
                            null
                        }
                        if (dailyReport != null) {
                            val screenTimeMins = dailyReport.totalScreenTimeMs / 60000L
                            val productivityScore = dailyReport.productivityScore
                            updateNotification(
                                "Active Tracking • Today: ${screenTimeMins}m",
                                "Productivity rating: $productivityScore/100 • ${dailyReport.mostUsedApp}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WellbeingFGS", "Error in tracking loop iteration: ${e.message}")
                }

                // Poll every 15 seconds
                delay(15000L)
            }
        }
    }

    private fun application(block: (com.example.di.AppContainer) -> Unit): WellbeingApplication? {
        val app = applicationContext as? WellbeingApplication
        if (app != null) {
            block(app.container)
        }
        return app
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(com.example.R.mipmap.ic_launcher) // App's launcher icon as fallback
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Lootra Wellbeing Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device usage, screen goals, and productivity scores."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("WellbeingFGS", "Foreground tracking service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
