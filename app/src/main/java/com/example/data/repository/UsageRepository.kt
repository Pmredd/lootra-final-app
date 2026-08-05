package com.example.data.repository

import com.example.data.dao.DailyStateDao
import com.example.data.model.DailyStateEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import android.util.Log

class UsageRepository(
    private val dailyStateDao: DailyStateDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getDailyState(uid: String, date: String): DailyStateEntity? {
        val compositeId = "${uid}_${date}"
        return dailyStateDao.getDailyState(compositeId)
    }

    fun getDailyStateReactive(uid: String, date: String): Flow<DailyStateEntity?> {
        return dailyStateDao.getDailyStateReactive(uid, date)
    }

    suspend fun saveDailyState(state: DailyStateEntity) {
        dailyStateDao.insertDailyState(state)
    }

    suspend fun syncDailyStateToRemote(state: DailyStateEntity): Boolean {
        return try {
            val data = mapOf(
                "uid" to state.uid,
                "date" to state.date,
                "dailyScreenTimeMs" to state.dailyScreenTimeMs,
                "rewardTvTimeMs" to state.rewardTvTimeMs,
                "adsWatchedCount" to state.adsWatchedCount,
                "dailyUsageCacheJson" to state.dailyUsageCacheJson,
                "temporaryAnalyticsJson" to state.temporaryAnalyticsJson,
                "lastUpdated" to state.lastUpdated
            )
            firestore.collection("user_daily_state")
                .document(state.uid)
                .collection("days")
                .document(state.date)
                .set(data)
                .await()
            dailyStateDao.markDailyStateSynced(state.compositeId)
            true
        } catch (e: Exception) {
            Log.e("UsageRepository", "Failed to sync DailyState to remote firestore: ${e.message}")
            false
        }
    }

    suspend fun syncUnsyncedStates() {
        try {
            val unsynced = dailyStateDao.getUnsyncedDailyStates()
            for (state in unsynced) {
                syncDailyStateToRemote(state)
            }
        } catch (e: Exception) {
            Log.e("UsageRepository", "Error running sync for unsynced states: ${e.message}")
        }
    }
}
