package com.example.data.dao

import androidx.room.*
import com.example.data.model.ReelEntity
import com.example.data.model.WatchedReelEntity
import com.example.data.model.ReelLikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {
    @Query("SELECT * FROM reels ORDER BY createdAt DESC")
    fun getAllReelsReactive(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels ORDER BY createdAt DESC")
    suspend fun getAllReels(): List<ReelEntity>

    @Query("SELECT * FROM reels WHERE location = :location ORDER BY createdAt DESC")
    fun getReelsForLocationReactive(location: String): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE reelId = :reelId LIMIT 1")
    suspend fun getReel(reelId: String): ReelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: ReelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReels(reels: List<ReelEntity>)

    @Query("DELETE FROM reels WHERE reelId = :reelId")
    suspend fun deleteReel(reelId: String)

    @Query("DELETE FROM reels")
    suspend fun clearReels()

    // Watched Reels (History and rewards prevention)
    @Query("SELECT * FROM watched_reels WHERE uid = :uid AND reelId = :reelId LIMIT 1")
    suspend fun getWatchedReel(uid: String, reelId: String): WatchedReelEntity?

    @Query("SELECT * FROM watched_reels WHERE uid = :uid")
    fun getWatchedReelsReactive(uid: String): Flow<List<WatchedReelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedReel(watchedReel: WatchedReelEntity)

    // Reel Likes
    @Query("SELECT * FROM reel_likes WHERE uid = :uid AND reelId = :reelId LIMIT 1")
    suspend fun getReelLike(uid: String, reelId: String): ReelLikeEntity?

    @Query("SELECT * FROM reel_likes WHERE uid = :uid")
    fun getLikedReelsReactive(uid: String): Flow<List<ReelLikeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReelLike(reelLike: ReelLikeEntity)

    @Query("DELETE FROM reel_likes WHERE uid = :uid AND reelId = :reelId")
    suspend fun deleteReelLike(uid: String, reelId: String)
}
