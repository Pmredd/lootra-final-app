package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ProductEntity
import com.example.data.model.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProductsReactive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE productId = :productId LIMIT 1")
    suspend fun getProduct(productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY createdAt DESC")
    fun getProductsByCategoryReactive(category: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET stock = :stock WHERE productId = :productId")
    suspend fun updateProductStock(productId: String, stock: Int)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM products WHERE productId = :productId")
    suspend fun deleteProduct(productId: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE uid = :uid ORDER BY createdAt DESC")
    fun getOrdersForUserReactive(uid: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE uid = :uid ORDER BY createdAt DESC")
    suspend fun getOrdersForUser(uid: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrder(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<OrderEntity>

    @Query("UPDATE orders SET isSynced = 1 WHERE orderId = :orderId")
    suspend fun markOrderSynced(orderId: String)

    @Query("DELETE FROM orders WHERE uid = :uid")
    suspend fun deleteOrdersForUser(uid: String)
}
