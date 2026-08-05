package com.example.data.repository

import android.util.Log
import com.example.data.dao.ProductDao
import com.example.data.dao.OrderDao
import com.example.data.model.UserProfile
import com.example.data.model.ProductEntity
import com.example.data.model.OrderEntity
import com.example.data.model.WalletEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class AdminRepository(
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // --- Admin Verification ---
    suspend fun verifyAdminStatus(uid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("admins").document(uid).get().await()
            if (doc.exists()) {
                val isActive = doc.getBoolean("isActive") ?: false
                Result.success(isActive)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to verify admin status: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Product Management ---
    suspend fun addProduct(product: ProductEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Update Firestore
            firestore.collection("products")
                .document(product.productId)
                .set(product.toMap())
                .await()

            // Update local Room database in case the admin is also a user/debugging locally
            productDao.insertProduct(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to add product: ${e.message}")
            Result.failure(e)
        }
    }

    // In ShopDetails / admin panels we edit products. All changes immediately sync.
    suspend fun editProduct(product: ProductEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("products")
                .document(product.productId)
                .set(product.toMap())
                .await()

            productDao.insertProduct(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to edit product: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("products")
                .document(productId)
                .delete()
                .await()

            // Delete locally
            productDao.deleteProduct(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to delete product: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setProductActiveState(productId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("products")
                .document(productId)
                .update("isActive", isActive)
                .await()

            val existing = productDao.getProduct(productId)
            if (existing != null) {
                productDao.insertProduct(existing.copy(isActive = isActive))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to set product state: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Order Management ---
    suspend fun getAllOrders(): Result<List<OrderEntity>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("orders").get().await()
            val list = snapshot.documents.map { doc ->
                OrderEntity.fromMap(doc.data)
            }.sortedByDescending { it.createdAt }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to get all orders: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("orders")
                .document(orderId)
                .update("orderStatus", newStatus)
                .await()

            // Update user's local order in room database if we have it
            val order = orderDao.getOrder(orderId)
            if (order != null) {
                orderDao.insertOrder(order.copy(orderStatus = newStatus))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to update order status: ${e.message}")
            Result.failure(e)
        }
    }

    // --- User Management ---
    suspend fun getAllUsers(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").get().await()
            val list = snapshot.documents.map { doc ->
                UserProfile.fromMap(doc.data)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserWallet(uid: String): Result<WalletEntity?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("wallet").document(uid).get().await()
            if (doc.exists()) {
                Result.success(WalletEntity.fromMap(doc.data))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserOrdersCount(uid: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("uid", uid)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRewardStats(uid: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("rewards").document(uid).get().await()
            if (doc.exists()) {
                val data = doc.data ?: emptyMap()
                Result.success(data)
            } else {
                Result.success(emptyMap())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Analytics ---
    suspend fun getAnalytics(): Result<AdminAnalyticsData> = withContext(Dispatchers.IO) {
        try {
            val usersSnapshot = firestore.collection("users").get().await()
            val productsSnapshot = firestore.collection("products").get().await()
            val ordersSnapshot = firestore.collection("orders").get().await()
            val walletsSnapshot = firestore.collection("wallet").get().await()

            val totalUsers = usersSnapshot.size()
            val totalProducts = productsSnapshot.size()
            
            val ordersList = ordersSnapshot.documents.map { OrderEntity.fromMap(it.data) }
            val totalOrders = ordersList.size
            val pendingOrders = ordersList.count { it.orderStatus == "Pending" }
            val deliveredOrders = ordersList.count { it.orderStatus == "Delivered" || it.orderStatus == "Delivered Successfully" }

            // Sum of lifetimeCoins of all wallets
            var totalCoinsEarned = 0
            for (doc in walletsSnapshot.documents) {
                val lifetime = (doc.get("lifetimeCoins") as? Number)?.toInt() ?: 0
                totalCoinsEarned += lifetime
            }

            // Sum of coinPrice of all placed orders
            val totalCoinsSpent = ordersList.sumOf { it.coinPrice }

            // Most Purchased Products calculation
            val purchaseCounts = ordersList.groupBy { it.productId }
                .mapValues { it.value.size }
            val mostPurchased = purchaseCounts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { entry ->
                    val prodId = entry.key
                    val count = entry.value
                    val prodDoc = productsSnapshot.documents.find { it.id == prodId }
                    val name = prodDoc?.getString("name") ?: ordersList.find { it.productId == prodId }?.productName ?: prodId
                    name to count
                }

            // Daily Active Users: let's fetch daily_reports collection count or active user updates
            val reportsSnapshot = firestore.collection("daily_reports").get().await()
            val reportsCount = reportsSnapshot.size()
            // Estimate or fallback to reports count, minimum of 1 if there's any user
            val dailyActive = maxOf(reportsCount, if (totalUsers > 0) 1 else 0)

            val analytics = AdminAnalyticsData(
                totalUsers = totalUsers,
                totalProducts = totalProducts,
                totalOrders = totalOrders,
                totalCoinsDistributed = totalCoinsEarned,
                pendingOrders = pendingOrders,
                deliveredOrders = deliveredOrders,
                totalCoinsEarned = totalCoinsEarned,
                totalCoinsSpent = totalCoinsSpent,
                mostPurchasedProducts = mostPurchased,
                dailyActiveUsers = dailyActive
            )
            Result.success(analytics)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to compute admin analytics: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Sub-Admin Management ---
    suspend fun getAllSubAdmins(): Result<List<SubAdminUser>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("admins")
                .whereEqualTo("role", "sub_admin")
                .get()
                .await()
            val list = snapshot.documents.map { doc ->
                SubAdminUser.fromMap(doc.data).copy(uid = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to get all sub-admins: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createSubAdmin(
        email: String,
        password: String,
        name: String,
        collegeId: String,
        collegeName: String,
        isActive: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
            val secondaryAppName = "SecondarySubAdminAuth"
            val secondaryApp = try {
                com.google.firebase.FirebaseApp.getInstance(secondaryAppName)
            } catch (e: Exception) {
                val options = com.google.firebase.FirebaseApp.getInstance().options
                com.google.firebase.FirebaseApp.initializeApp(context, options, secondaryAppName)
            }
            val secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp)

            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Auth UID creation returned null")

            secondaryAuth.signOut()

            val subAdmin = SubAdminUser(
                uid = uid,
                email = email,
                role = "sub_admin",
                collegeId = collegeId,
                collegeName = collegeName,
                isActive = isActive,
                createdAt = System.currentTimeMillis(),
                name = name
            )

            firestore.collection("admins")
                .document(uid)
                .set(subAdmin.toMap())
                .await()

            Result.success(uid)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to create sub-admin: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateSubAdmin(subAdmin: SubAdminUser): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("admins")
                .document(subAdmin.uid)
                .set(subAdmin.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to update sub-admin: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteSubAdmin(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("admins")
                .document(uid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to delete sub-admin: ${e.message}")
            Result.failure(e)
        }
    }
}

@androidx.annotation.Keep
data class SubAdminUser(
    val uid: String = "",
    val email: String = "",
    val role: String = "sub_admin",
    val collegeId: String = "",
    val collegeName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val name: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "collegeId" to collegeId,
            "collegeName" to collegeName,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "name" to name
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): SubAdminUser {
            if (map == null) return SubAdminUser()
            return SubAdminUser(
                uid = map["uid"] as? String ?: map["userId"] as? String ?: "",
                email = map["email"] as? String ?: "",
                role = map["role"] as? String ?: "sub_admin",
                collegeId = map["collegeId"] as? String ?: "",
                collegeName = map["collegeName"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                name = map["name"] as? String ?: ""
            )
        }
    }
}


data class AdminAnalyticsData(
    val totalUsers: Int = 0,
    val totalProducts: Int = 0,
    val totalOrders: Int = 0,
    val totalCoinsDistributed: Int = 0,
    val pendingOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val totalCoinsEarned: Int = 0,
    val totalCoinsSpent: Int = 0,
    val mostPurchasedProducts: List<Pair<String, Int>> = emptyList(),
    val dailyActiveUsers: Int = 0
)
