package com.example.data.dao

import androidx.room.*
import com.example.data.model.AdHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdHistory(entity: AdHistoryEntity): Long

    @Query("SELECT * FROM ad_history WHERE uid = :uid ORDER BY watchedAt DESC")
    fun getAdHistoryReactive(uid: String): Flow<List<AdHistoryEntity>>

    @Query("SELECT * FROM ad_history WHERE uid = :uid AND status = :status ORDER BY watchedAt DESC")
    suspend fun getAdHistoryByStatus(uid: String, status: String): List<AdHistoryEntity>

    @Query("DELETE FROM ad_history WHERE uid = :uid")
    suspend fun deleteAdHistoryForUser(uid: String)
}
