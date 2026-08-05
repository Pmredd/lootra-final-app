package com.example.data.repository

import android.util.Log
import com.example.data.dao.ReelDao
import com.example.data.model.ReelEntity
import com.example.data.model.WatchedReelEntity
import com.example.data.model.ReelLikeEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReelRepository(
    private val reelDao: ReelDao,
    private val walletRepository: WalletRepository,
    private val firestore: FirebaseFirestore? = null
) {
    private val db: FirebaseFirestore
        get() = firestore ?: FirebaseFirestore.getInstance()

    val reelsReactive: Flow<List<ReelEntity>> = reelDao.getAllReelsReactive()

    private var reelsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        startRealtimeReelsListener()
    }

    fun startRealtimeReelsListener(selectedLocation: String? = null) {
        try {
            reelsListenerRegistration?.remove()
            Log.d("ReelRepository", "startRealtimeReelsListener: Listening to entire reels collection for real-time synchronization")
            
            val query = db.collection("reels")

            reelsListenerRegistration = query
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ReelRepository", "Firestore reels listener error: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        processSnapshot(snapshot, selectedLocation)
                    }
                }
        } catch (ex: Exception) {
            Log.e("ReelRepository", "Could not start realtime reels listener: ${ex.message}")
        }
    }

    private fun processSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot, selectedLocation: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ReelRepository", "[DEBUG] Snapshot updates received! Total documents in snapshot: ${snapshot.size()}")
                val uniqueReelsMap = mutableMapOf<String, ReelEntity>()
                for (doc in snapshot.documents) {
                    try {
                        val reel = ReelEntity.fromMap(doc.data)
                        if (reel.reelId.isNotBlank()) {
                            uniqueReelsMap[reel.reelId] = reel
                        }
                    } catch (mappingEx: Exception) {
                        Log.e("ReelRepository", "Mapping Error for document ${doc.id}: ${mappingEx.message}", mappingEx)
                    }
                }
                val list = uniqueReelsMap.values.toList()

                // Delete local reels that are no longer present in Firestore
                val localReels = reelDao.getAllReels()
                val remoteIds = uniqueReelsMap.keys
                for (local in localReels) {
                    if (local.reelId !in remoteIds) {
                        reelDao.deleteReel(local.reelId)
                    }
                }

                // Insert or update
                reelDao.insertReels(list)
                Log.d("ReelRepository", "[DEBUG] Room synchronization completed. Synced ${list.size} reels to Room database. Local DB Count: ${reelDao.getAllReels().size}")
            } catch (ex: Exception) {
                Log.e("ReelRepository", "Error syncing reels: ${ex.message}", ex)
            }
        }
    }

    private fun fallbackRealtimeListener(selectedLocation: String?) {
        // Obsolete as we now listen to the full collection
    }

    suspend fun getReelById(reelId: String): ReelEntity? = reelDao.getReel(reelId)

    fun getReelsForLocationReactive(location: String): Flow<List<ReelEntity>> {
        return reelDao.getReelsForLocationReactive(location)
    }

    suspend fun seedAndRefreshReels(selectedLocation: String? = null) {
        Log.d("ReelRepository", "seedAndRefreshReels called for selectedLocation: '$selectedLocation'")
        try {
            val query = db.collection("reels")
            val remoteReels = withTimeoutOrNull(5000) {
                query.get().await()
            }

            if (remoteReels != null) {
                val uniqueReelsMap = mutableMapOf<String, ReelEntity>()
                for (doc in remoteReels.documents) {
                    try {
                        val r = ReelEntity.fromMap(doc.data)
                        if (r.reelId.isNotBlank()) {
                            uniqueReelsMap[r.reelId] = r
                        }
                    } catch (mappingEx: Exception) {
                        Log.e("ReelRepository", "Mapping error in seedAndRefreshReels: ${mappingEx.message}", mappingEx)
                    }
                }
                val list = uniqueReelsMap.values.toList()

                val localReels = reelDao.getAllReels()
                val remoteIds = uniqueReelsMap.keys
                for (local in localReels) {
                    if (local.reelId !in remoteIds) {
                        reelDao.deleteReel(local.reelId)
                    }
                }
                reelDao.insertReels(list)
                Log.d("ReelRepository", "seedAndRefreshReels inserted ${list.size} reels. Local Database Count: ${reelDao.getAllReels().size}")
            } else {
                Log.w("ReelRepository", "No remote reels returned or query failed. Local reels not cleared to preserve cache.")
            }
        } catch (e: Exception) {
            Log.e("ReelRepository", "Failed to seed or refresh reels: ${e.message}", e)
        }
    }

    // Likes management
    fun getLikedReelsReactive(uid: String): Flow<List<ReelLikeEntity>> = reelDao.getLikedReelsReactive(uid)

    suspend fun toggleLikeReel(uid: String, reelId: String): Boolean {
        return try {
            val likeDocRef = db.collection("users").document(uid).collection("liked_reels").document(reelId)
            val remoteLike = likeDocRef.get().await()

            if (remoteLike.exists()) {
                // Unlike
                reelDao.deleteReelLike(uid, reelId)
                likeDocRef.delete().await()
                db.collection("reels").document(reelId)
                    .update("likes", com.google.firebase.firestore.FieldValue.increment(-1)).await()
            } else {
                // Like
                val like = ReelLikeEntity(uid, reelId, System.currentTimeMillis())
                reelDao.insertReelLike(like)
                likeDocRef.set(like.toMap()).await()
                db.collection("reels").document(reelId)
                    .update("likes", com.google.firebase.firestore.FieldValue.increment(1)).await()
            }
            true
        } catch (e: Exception) {
            Log.e("ReelRepository", "toggleLikeReel failed: ${e.message}", e)
            false
        }
    }

    // Unique view tracking
    suspend fun recordReelViewIfUnique(uid: String, reelId: String): Boolean {
        Log.d("ReelRepository", "[DEBUG] recordReelViewIfUnique called. Current Firebase UID: '$uid', Reel ID: '$reelId'")
        return try {
            val existingLocal = reelDao.getWatchedReel(uid, reelId)
            if (existingLocal != null) {
                Log.d("ReelRepository", "[DEBUG] Existing watched_reels document found in local Room DB for UID: '$uid', Reel ID: '$reelId'")
                return false
            }

            val docRef = db.collection("users").document(uid).collection("watched_reels").document(reelId)
            val remoteDoc = docRef.get().await()
            if (remoteDoc.exists()) {
                Log.d("ReelRepository", "[DEBUG] Existing watched_reels document found in remote Firestore for UID: '$uid', Reel ID: '$reelId'")
                val watched = WatchedReelEntity(
                    uid = uid,
                    reelId = reelId,
                    watchedAt = remoteDoc.getLong("watchedAt") ?: System.currentTimeMillis(),
                    rewardGiven = remoteDoc.getBoolean("rewardGiven") ?: false,
                    rewardCoins = (remoteDoc.getLong("rewardCoins") ?: 0L).toInt(),
                    isSynced = true
                )
                reelDao.insertWatchedReel(watched)
                return false
            }

            Log.d("ReelRepository", "[DEBUG] No existing watched_reels document found (neither local nor remote). Executing view increment for UID: '$uid', Reel ID: '$reelId'")
            val watched = WatchedReelEntity(
                uid = uid,
                reelId = reelId,
                watchedAt = System.currentTimeMillis(),
                rewardGiven = false,
                rewardCoins = 0,
                isSynced = true
            )
            docRef.set(watched.toMap()).await()
            reelDao.insertWatchedReel(watched)

            db.collection("reels").document(reelId)
                .update("views", com.google.firebase.firestore.FieldValue.increment(1)).await()
            Log.d("ReelRepository", "[DEBUG] View increment executed successfully in Firestore for Reel ID: '$reelId'")

            // Fetch and log the updated Firestore view count
            val updatedDoc = db.collection("reels").document(reelId).get().await()
            val newViews = updatedDoc.getLong("views") ?: 0L
            Log.d("ReelRepository", "[DEBUG] Updated Firestore view count retrieved for Reel ID '$reelId': $newViews views")

            true
        } catch (e: Exception) {
            Log.e("ReelRepository", "[DEBUG] Error in recordReelViewIfUnique for UID: '$uid', Reel ID: '$reelId': ${e.message}", e)
            false
        }
    }

    // Watched reels and rewards tracking
    fun getWatchedReelsReactive(uid: String): Flow<List<WatchedReelEntity>> = reelDao.getWatchedReelsReactive(uid)

    suspend fun getWatchedReel(uid: String, reelId: String): WatchedReelEntity? {
        return reelDao.getWatchedReel(uid, reelId)
    }

    suspend fun markReelCompletedAndReward(uid: String, reelId: String): Boolean {
        try {
            // Check if already rewarded to prevent multi-reward abuse
            val existingLocal = reelDao.getWatchedReel(uid, reelId)
            if (existingLocal != null && existingLocal.rewardGiven) {
                Log.d("ReelRepository", "User already rewarded for reel: $reelId (local)")
                return false
            }

            // Verify with remote firestore to prevent cheating / client-side clearing database exploit
            val remoteDoc = withTimeoutOrNull(4000) {
                db.collection("users").document(uid).collection("watched_reels").document(reelId).get().await()
            }
            if (remoteDoc != null && remoteDoc.exists()) {
                val rewarded = remoteDoc.getBoolean("rewardGiven") ?: false
                if (rewarded) {
                    Log.d("ReelRepository", "User already rewarded for reel: $reelId (firestore)")
                    // cache local
                    reelDao.insertWatchedReel(WatchedReelEntity(
                        uid = uid,
                        reelId = reelId,
                        watchedAt = remoteDoc.getLong("watchedAt") ?: System.currentTimeMillis(),
                        rewardGiven = true,
                        rewardCoins = (remoteDoc.getLong("rewardCoins") ?: 0L).toInt(),
                        isSynced = true
                    ))
                    return false
                }
            }

            val reel = reelDao.getReel(reelId) ?: return false

            // Rule check: campaign completed
            if (reel.campaignCoins > 0 && reel.remainingCoins < reel.rewardPerUser) {
                Log.d("ReelRepository", "[DEBUG] Campaign completed! remainingCoins (${reel.remainingCoins}) is less than rewardPerUser (${reel.rewardPerUser}). Cannot claim reward.")
                return false
            }

            // Rule check: expiry
            if (reel.expiryDate > 0 && System.currentTimeMillis() > reel.expiryDate) {
                Log.d("ReelRepository", "Reel expired!")
                return false
            }

            val rewardCoins = if (reel.rewardPerUser > 0) reel.rewardPerUser else reel.rewardCoins

            // 1. Grant coins in user wallet
            walletRepository.addCoins(
                uid = uid,
                amount = rewardCoins,
                type = "REELS_WATCH",
                description = "Completed watching Reel: ${reel.title}"
            )

            // 2. Mark complete locally
            val watched = WatchedReelEntity(
                uid = uid,
                reelId = reelId,
                watchedAt = System.currentTimeMillis(),
                rewardGiven = true,
                rewardCoins = rewardCoins,
                isSynced = true
            )
            reelDao.insertWatchedReel(watched)

            // 3. Mark complete in Firestore
            db.collection("users").document(uid).collection("watched_reels").document(reelId).set(watched.toMap()).await()

            // 4. Increment distributedCoins in Firestore
            db.collection("reels").document(reelId)
                .update("distributedCoins", com.google.firebase.firestore.FieldValue.increment(rewardCoins.toLong())).await()
            Log.d("ReelRepository", "[DEBUG] Successfully incremented distributedCoins by $rewardCoins for Reel ID: '$reelId'")

            // Fetch and log updated Firestore values
            val updatedDoc = db.collection("reels").document(reelId).get().await()
            val newDistributed = updatedDoc.getLong("distributedCoins") ?: 0L
            val campaignCoinsVal = updatedDoc.getLong("campaignCoins") ?: 0L
            Log.d("ReelRepository", "[DEBUG] Updated Firestore distributedCoins for Reel ID '$reelId': $newDistributed / $campaignCoinsVal coins")

            return true
        } catch (e: Exception) {
            Log.e("ReelRepository", "Error completing watch reward: ${e.message}")
            return false
        }
    }

    // Admin panel actions
    suspend fun uploadReel(reel: ReelEntity): Boolean {
        return try {
            db.collection("reels").document(reel.reelId).set(reel.toMap()).await()
            reelDao.insertReel(reel)
            true
        } catch (e: Exception) {
            Log.e("ReelRepository", "Upload reel failed: ${e.message}")
            false
        }
    }

    suspend fun deleteReel(reelId: String): Boolean {
        return try {
            db.collection("reels").document(reelId).delete().await()
            reelDao.deleteReel(reelId)
            true
        } catch (e: Exception) {
            Log.e("ReelRepository", "Delete reel failed: ${e.message}")
            false
        }
    }
}
