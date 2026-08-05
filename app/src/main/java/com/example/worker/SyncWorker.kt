package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.WellbeingApplication

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (com.example.data.manager.AppControlManager.maintenanceEnabled.value) {
            Log.w("SyncWorker", "Emergency Maintenance Mode is ACTIVE. Aborting sync task.")
            return Result.failure()
        }
        Log.d("SyncWorker", "Executing daily usage and state sync to remote Firestone...")
        return try {
            val app = applicationContext as? WellbeingApplication
            val usageRepo = app?.container?.usageRepository
            
            if (usageRepo != null) {
                usageRepo.syncUnsyncedStates()
                Log.i("SyncWorker", "Periodic DailyState synchronization finished successfully.")
                Result.success()
            } else {
                Log.e("SyncWorker", "AppContainer or UsageRepository is not initialized yet.")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed execution of state synchronization: ${e.message}", e)
            Result.retry()
        }
    }
}
