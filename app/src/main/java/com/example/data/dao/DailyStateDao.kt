package com.example.data.dao

import androidx.room.*
import com.example.data.model.DailyStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStateDao {
    @Query("SELECT * FROM user_daily_states WHERE compositeId = :compositeId LIMIT 1")
    suspend fun getDailyState(compositeId: String): DailyStateEntity?

    @Query("SELECT * FROM user_daily_states WHERE uid = :uid AND date = :date LIMIT 1")
    fun getDailyStateReactive(uid: String, date: String): Flow<DailyStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyState(state: DailyStateEntity)

    @Query("SELECT * FROM user_daily_states WHERE isSynced = 0")
    suspend fun getUnsyncedDailyStates(): List<DailyStateEntity>

    @Query("UPDATE user_daily_states SET isSynced = 1 WHERE compositeId = :compositeId")
    suspend fun markDailyStateSynced(compositeId: String)
    
    @Query("DELETE FROM user_daily_states WHERE uid = :uid")
    suspend fun deleteDailyStatesForUser(uid: String)
}
