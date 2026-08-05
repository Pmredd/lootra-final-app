package com.example.data.repository

import android.util.Log
import com.example.data.dao.OrderDao
import com.example.data.model.OrderEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class OrderRepository(
    private val orderDao: OrderDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getOrdersReactive(uid: String): Flow<List<OrderEntity>> = orderDao.getOrdersForUserReactive(uid)

    suspend fun createOrder(
        uid: String,
        productId: String,
        productName: String,
        coinPrice: Int,
        selectedCollegeId: String,
        selectedCollegeName: String,
        pickupLocation: String,
        pricingType: String = "COINS_ONLY",
        coinsUsed: Int = 0,
        moneyPaid: Int = 0,
        paymentStatus: String = "Paid"
    ): OrderEntity {
        val orderId = "ORD_${UUID.randomUUID().toString().take(8).uppercase()}"
        val order = OrderEntity(
            orderId = orderId,
            uid = uid,
            productId = productId,
            productName = productName,
            coinPrice = coinPrice,
            orderStatus = "Confirmed", // Default status per prompt purchase success
            createdAt = System.currentTimeMillis(),
            selectedCollegeId = selectedCollegeId,
            selectedCollegeName = selectedCollegeName,
            pickupLocation = pickupLocation,
            isSynced = false,
            pricingType = pricingType,
            coinsUsed = coinsUsed,
            moneyPaid = moneyPaid,
            paymentStatus = paymentStatus
        )

        // Save locally fast
        orderDao.insertOrder(order)
        Log.d("OrderRepository", "Created local order: $orderId")

        // Sync with Firestore
        syncOrderToRemote(order)
        return order
    }

    suspend fun fetchOrdersFromRemote(uid: String) {
        try {
            val snapshot = withTimeoutOrNull(5000) {
                firestore.collection("orders")
                    .whereEqualTo("uid", uid)
                    .get()
                    .await()
            }
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val order = OrderEntity.fromMap(doc.data)
                    val status = if (order.orderStatus == "Delivered") "Delivered Successfully" else order.orderStatus
                    orderDao.insertOrder(order.copy(orderStatus = status))
                }
                Log.d("OrderRepository", "Successfully fetched ${snapshot.size()} orders from Firestore for user $uid")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error fetching user orders from Firestore: ${e.message}")
        }
    }

    private var ordersListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListeningToOrders(uid: String) {
        if (uid.isEmpty()) return
        Log.d("OrderRepository", "Starting real-time Firestore listener for orders of user: $uid")
        ordersListener?.remove()
        ordersListener = firestore.collection("orders")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("OrderRepository", "Real-time subscription failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        for (doc in snapshot.documents) {
                            val order = OrderEntity.fromMap(doc.data)
                            val status = if (order.orderStatus == "Delivered") "Delivered Successfully" else order.orderStatus
                            orderDao.insertOrder(order.copy(orderStatus = status))
                        }
                    }
                }
            }
    }

    fun stopListeningToOrders() {
        ordersListener?.remove()
        ordersListener = null
    }

    suspend fun syncOrderToRemote(order: OrderEntity): Boolean {
        return try {
            val success = withTimeoutOrNull(5000) {
                firestore.collection("orders")
                    .document(order.orderId)
                    .set(order.toMap())
                    .await()
                true
            }
            if (success == true) {
                orderDao.markOrderSynced(order.orderId)
                Log.d("OrderRepository", "Synced order ${order.orderId} to Firestore successfully")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Failed to sync order ${order.orderId} to Firestore: ${e.message}")
            false
        }
    }

    suspend fun syncUnsyncedOrders(uid: String) {
        try {
            val unsynced = orderDao.getUnsyncedOrders()
            for (order in unsynced) {
                if (order.uid == uid) {
                    syncOrderToRemote(order)
                }
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error syncing unsynced orders: ${e.message}")
        }
    }

    suspend fun deleteLocalOrdersForUser(uid: String) {
        try {
            orderDao.deleteOrdersForUser(uid)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error deleting local orders cache: ${e.message}")
        }
    }
}
