package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.WellbeingDatabase
import com.example.data.datastore.WellbeingPreferences
import com.example.data.manager.DeviceUsageStatsService
import com.example.data.repository.FirestoreRepositoryImpl
import com.example.data.repository.WellbeingRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull

class WellbeingSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (com.example.data.manager.AppControlManager.maintenanceEnabled.value) {
            Log.w("WellbeingSyncWorker", "Emergency Maintenance Mode is ACTIVE. Aborting sync task.")
            return Result.failure()
        }
        return try {
            val app = applicationContext as? com.example.WellbeingApplication
            val database = WellbeingDatabase.getDatabase(applicationContext)
            val repository = app?.container?.repository ?: WellbeingRepository(
                context = applicationContext,
                database = database,
                preferences = com.example.data.datastore.WellbeingPreferences(applicationContext),
                statsService = DeviceUsageStatsService()
            )
            val firestoreRepo = app?.container?.firestoreRepository ?: FirestoreRepositoryImpl()
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

            // 1. Refresh usage data (local write)
            repository.refreshUsageData(Date())

            // 2. Perform sync from Room to Firestore
            val unsyncedReports = database.dailyReportDao.getUnsyncedDailyReports(uid)
            
            var allSuccessful = true
            for (report in unsyncedReports) {
                // Use a short timeout for network operations
                val result = withTimeoutOrNull(5000L) {
                    firestoreRepo.syncDailyReport(report)
                }
                
                if (result != null && result.isSuccess) {
                    database.dailyReportDao.markDailyReportSynced(report.userId, report.date)
                } else {
                    allSuccessful = false
                    Log.e("WellbeingSyncWorker", "Failed to sync report for ${report.date}")
                }
            }

            // Sync Monthly Reports
            val unsyncedMonthly = database.monthlyReportDao.getUnsyncedMonthlyReports(uid)
            for (monthly in unsyncedMonthly) {
                val result = withTimeoutOrNull(5000L) {
                    firestoreRepo.syncMonthlyReport(monthly)
                }
                if (result != null && result.isSuccess) {
                    database.monthlyReportDao.markMonthlyReportSynced(monthly.userId, monthly.monthKey)
                } else {
                    allSuccessful = false
                    Log.e("WellbeingSyncWorker", "Failed to sync monthly report for ${monthly.monthKey}")
                }
            }

            if (allSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("WellbeingSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
