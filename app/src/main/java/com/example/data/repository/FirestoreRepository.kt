package com.example.data.repository

import android.util.Log
import com.example.data.model.DailyReportEntity
import com.example.data.model.MonthlyReportEntity
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface FirestoreRepository {
    suspend fun createUserProfile(userProfile: UserProfile): Result<Unit>
    suspend fun getUserProfile(uid: String): Result<UserProfile?>
    suspend fun updateUserProfile(uid: String, username: String, profileImage: String): Result<Unit>
    suspend fun testFirestoreConnection(): Result<Unit>
    suspend fun syncDailyReport(report: DailyReportEntity): Result<Unit>
    suspend fun syncMonthlyReport(report: MonthlyReportEntity): Result<Unit>
    suspend fun deleteUserData(uid: String): Result<Unit>
}

class FirestoreRepositoryImpl(
    private val firestore: FirebaseFirestore? = null
) : FirestoreRepository {

    private val db: FirebaseFirestore
        get() = firestore ?: FirebaseFirestore.getInstance()

    override suspend fun createUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                db.collection("users")
                    .document(userProfile.uid)
                    .set(userProfile.toMap())
                    .await()
                true
            }
            if (success == true) {
                Log.d("FirestoreRepository", "Successfully created user profile for ${userProfile.uid}")
                Result.success(Unit)
            } else {
                Log.e("FirestoreRepository", "Timeout creating user profile for ${userProfile.uid}")
                Result.failure(Exception("Timeout setting user profile"))
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to create user profile for ${userProfile.uid}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val doc = kotlinx.coroutines.withTimeoutOrNull(10000) {
                db.collection("users")
                    .document(uid)
                    .get()
                    .await()
            }
            if (doc != null && doc.exists()) {
                val data = doc.data
                Log.d("FirestoreRepository", "Successfully loaded user profile for $uid")
                Result.success(UserProfile.fromMap(data))
            } else {
                Log.d("FirestoreRepository", "No user profile found for $uid of timeout/nonexistent")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to get user profile for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(
        uid: String,
        username: String,
        profileImage: String
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "username" to username,
                "updatedAt" to System.currentTimeMillis()
            )
            if (profileImage.isNotEmpty()) {
                updates["profileImage"] = profileImage
            }
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                db.collection("users")
                    .document(uid)
                    .update(updates)
                    .await()
                true
            }
            if (success == true) {
                Log.d("FirestoreRepository", "Successfully updated user profile for $uid")
                Result.success(Unit)
            } else {
                Log.e("FirestoreRepository", "Timeout updating user profile for $uid")
                Result.failure(Exception("Timeout updating profile"))
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to update user profile for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun syncDailyReport(report: DailyReportEntity): Result<Unit> {
        return try {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("Not authenticated"))
            val data = mapOf(
                "uid" to currentUid,
                "date" to report.date,
                "totalScreenTimeMs" to report.totalScreenTimeMs,
                "productivityScore" to report.productivityScore,
                "mostUsedApp" to report.mostUsedApp,
                "appOpenCount" to report.appOpenCount,
                "insights" to report.insights
            )
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                db.collection("daily_reports")
                    .document(report.date)
                    .set(data)
                    .await()
                true
            }
            if (success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Timeout syncing daily report"))
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to sync daily report", e)
            Result.failure(e)
        }
    }

    override suspend fun syncMonthlyReport(report: MonthlyReportEntity): Result<Unit> {
        return try {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("Not authenticated"))
            val data = mapOf(
                "uid" to currentUid,
                "monthKey" to report.monthKey,
                "totalScreenTimeMs" to report.totalScreenTimeMs,
                "averageScreenTimeMs" to report.averageScreenTimeMs,
                "productivityScore" to report.productivityScore,
                "mostUsedApp" to report.mostUsedApp,
                "insightsList" to report.insightsList
            )
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                db.collection("monthly_reports")
                    .document(report.monthKey)
                    .set(data)
                    .await()
                true
            }
            if (success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Timeout syncing monthly report"))
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to sync monthly report", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteUserData(uid: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val firestore = db

            // 1. Delete users/{uid}
            try { firestore.collection("users").document(uid).delete().await() } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting users/$uid: ${e.message}") }

            // 2. Delete wallet/{uid}
            try { firestore.collection("wallet").document(uid).delete().await() } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting wallet/$uid: ${e.message}") }

            // 3. Delete rewards/{uid}
            try { firestore.collection("rewards").document(uid).delete().await() } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting rewards/$uid: ${e.message}") }

            // 4. Delete transactions/{uid} and user transactions
            try { firestore.collection("transactions").document(uid).delete().await() } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting transactions/$uid: ${e.message}") }
            try {
                val transQuery = firestore.collection("transactions").whereEqualTo("userId", uid).get().await()
                for (doc in transQuery.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting user transactions query: ${e.message}") }

            // 5. Delete user_daily_state/{uid}
            try { firestore.collection("user_daily_state").document(uid).delete().await() } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting user_daily_state/$uid: ${e.message}") }

            // 6. Delete device_bindings where userId == uid
            try {
                val bindQuery = firestore.collection("device_bindings").whereEqualTo("userId", uid).get().await()
                for (doc in bindQuery.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) { Log.w("FirestoreRepo", "Error deleting device_bindings: ${e.message}") }

            // 7. Delete watched_reels and liked_reels
            try {
                val watchedQuery = firestore.collection("watched_reels").whereEqualTo("userId", uid).get().await()
                for (doc in watchedQuery.documents) { doc.reference.delete().await() }
            } catch (_: Exception) {}

            try {
                val likedQuery = firestore.collection("liked_reels").whereEqualTo("userId", uid).get().await()
                for (doc in likedQuery.documents) { doc.reference.delete().await() }
            } catch (_: Exception) {}

            // Subcollections users/{uid}/watched_reels & liked_reels
            try {
                val userWatched = firestore.collection("users").document(uid).collection("watched_reels").get().await()
                for (doc in userWatched.documents) { doc.reference.delete().await() }
                val userLiked = firestore.collection("users").document(uid).collection("liked_reels").get().await()
                for (doc in userLiked.documents) { doc.reference.delete().await() }
            } catch (_: Exception) {}

            // 8. Delete orders where userId == uid
            try {
                val ordersQuery = firestore.collection("orders").whereEqualTo("userId", uid).get().await()
                for (doc in ordersQuery.documents) { doc.reference.delete().await() }
            } catch (_: Exception) {}

            // 9. Delete daily_reports and monthly_reports for user
            try {
                val dailyQuery = firestore.collection("daily_reports").whereEqualTo("uid", uid).get().await()
                for (doc in dailyQuery.documents) { doc.reference.delete().await() }
            } catch (_: Exception) {}

            try {
                val monthlyQuery = firestore.collection("monthly_reports").whereEqualTo("uid", uid).get().await()
                for (doc in monthlyQuery.documents) { doc.reference.delete().await() }
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "deleteUserData exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun testFirestoreConnection(): Result<Unit> {
        return try {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid == null) {
                Log.d("LootraFirebase", "Firebase Connection Test: Not signed in, skipping connection check.")
                return Result.success(Unit)
            }
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                db.collection("users")
                    .document(currentUid)
                    .get()
                    .await()
                true
            }
            if (success == true) {
                Log.d("LootraFirebase", "Firebase Connected Successfully (authenticated read)")
                Result.success(Unit)
            } else {
                Log.e("LootraFirebase", "Firebase connection timeout")
                Result.failure(Exception("Timeout testing connection"))
            }
        } catch (e: Exception) {
            Log.e("LootraFirebase", "Firebase Connection Failure", e)
            Result.failure(e)
        }
    }
}
