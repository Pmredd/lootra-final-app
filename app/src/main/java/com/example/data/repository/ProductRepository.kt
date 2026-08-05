package com.example.data.repository

import android.util.Log
import com.example.data.dao.ProductDao
import com.example.data.model.ProductEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductRepository(
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore? = null
) {
    private val db: FirebaseFirestore
        get() = firestore ?: FirebaseFirestore.getInstance()

    // Expose local reactive flow of products
    val productsReactive: Flow<List<ProductEntity>> = productDao.getAllProductsReactive()

    private val _collegesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.model.College>>(emptyList())
    val collegesReactive: Flow<List<com.example.data.model.College>> = _collegesFlow

    private var productsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var collegesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        startRealtimeProductListener()
        startRealtimeCollegesListener()
    }

    fun startRealtimeCollegesListener() {
        try {
            collegesListenerRegistration?.remove()
            collegesListenerRegistration = db.collection("colleges")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ProductRepository", "Firestore colleges listener error: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            com.example.data.model.College.fromMap(doc.data)
                        }
                        _collegesFlow.value = list
                        Log.d("ProductRepository", "Real-time synced ${list.size} colleges from Firestore.")
                    }
                }
        } catch (ex: Exception) {
            Log.e("ProductRepository", "Could not start realtime colleges listener: ${ex.message}")
        }
    }

    fun startRealtimeProductListener() {
        try {
            productsListenerRegistration?.remove()
            productsListenerRegistration = db.collection("products")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ProductRepository", "Firestore listener error: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val uniqueProductsMap = mutableMapOf<String, ProductEntity>()
                                for (doc in snapshot.documents) {
                                    val prod = ProductEntity.fromMap(doc.data)
                                    if (prod.productId.isNotBlank()) {
                                        uniqueProductsMap[prod.productId] = prod
                                    }
                                }
                                val list = uniqueProductsMap.values.toList()

                                // Delete any local product that is no longer in Firestore (representing an admin deletion)
                                val localProducts = productDao.getAllProducts()
                                val remoteIds = uniqueProductsMap.keys
                                for (local in localProducts) {
                                    if (local.productId !in remoteIds) {
                                        productDao.deleteProduct(local.productId)
                                    }
                                }

                                // Insert or update remaining products
                                productDao.insertProducts(list)
                                Log.d("ProductRepository", "Real-time synced ${list.size} unique products from Firestore.")
                            } catch (ex: Exception) {
                                Log.e("ProductRepository", "Error updating local cache from snapshot: ${ex.message}", ex)
                            }
                        }
                    }
                }
        } catch (ex: Exception) {
            Log.e("ProductRepository", "Could not start realtime product listener: ${ex.message}")
        }
    }

    suspend fun getProductById(productId: String): ProductEntity? = productDao.getProduct(productId)

    // Seed default products to local and remote databases if empty
    suspend fun seedAndRefreshProducts() {
        try {
            // Attempt fetching from Remote Firestore first
            val remoteProducts = withTimeoutOrNull(5000) {
                db.collection("products")
                    .get()
                    .await()
            }

            if (remoteProducts != null && !remoteProducts.isEmpty) {
                val uniqueProductsMap = mutableMapOf<String, ProductEntity>()
                for (doc in remoteProducts.documents) {
                    val prod = ProductEntity.fromMap(doc.data)
                    if (prod.productId.isNotBlank()) {
                        uniqueProductsMap[prod.productId] = prod
                    }
                }
                val list = uniqueProductsMap.values.toList()

                // Keep Room synchronized with Firestore
                val localProducts = productDao.getAllProducts()
                val remoteIds = uniqueProductsMap.keys
                for (local in localProducts) {
                    if (local.productId !in remoteIds) {
                        productDao.deleteProduct(local.productId)
                    }
                }

                productDao.insertProducts(list)
                Log.d("ProductRepository", "Successfully fetched and loaded ${list.size} unique products from Firestore")
            } else {
                // If remote is empty or null, see if local is empty
                val localProducts = productDao.getAllProducts()
                if (localProducts.isEmpty()) {
                    val defaultCatalog = createDefaultCatalog()
                    productDao.insertProducts(defaultCatalog)
                    
                    // Upload/seed to Firestore running in background
                    for (p in defaultCatalog) {
                        try {
                            withTimeoutOrNull(3000) {
                                db.collection("products")
                                    .document(p.productId)
                                    .set(p.toMap())
                                    .await()
                            }
                        } catch (ex: Exception) {
                            Log.e("ProductRepository", "Failed to seed ${p.name} to Firestore: ${ex.message}")
                        }
                    }
                    Log.d("ProductRepository", "Local database initialized with standard product catalog.")
                }
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error refreshing product list, fallback to local: ${e.message}")
            // Fallback: If local is empty, populate with template catalog
            val localProducts = productDao.getAllProducts()
            if (localProducts.isEmpty()) {
                val defaultCatalog = createDefaultCatalog()
                productDao.insertProducts(defaultCatalog)
            }
        }
    }

    suspend fun updateStock(productId: String, subtractCount: Int): Boolean {
        return try {
            val product = productDao.getProduct(productId) ?: return false
            if (product.stock < subtractCount) return false

            val newStock = product.stock - subtractCount
            productDao.updateProductStock(productId, newStock)

            // Update in Firestore
            withTimeoutOrNull(5000) {
                db.collection("products")
                    .document(productId)
                    .update("stock", newStock)
                    .await()
            }
            Log.d("ProductRepository", "Updated stock for $productId locally and on Firestore. New stock: $newStock")
            true
        } catch (e: Exception) {
            Log.e("ProductRepository", "Failed to sync stock after purchase: ${e.message}")
            // Return true even if Firestore fails so the transaction completes locally. We'll try to retry next time
            true
        }
    }

    private fun createDefaultCatalog(): List<ProductEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ProductEntity(
                productId = "steam_50",
                name = "Steam $50 Digital Code",
                description = "Instantly unlock and buy thousands of PC games on Steam. Deliverable via secure code post-checkout.",
                imageUrl = "https://images.unsplash.com/photo-1612287230202-1bf1d85d1bdf?w=600&q=80",
                coinPrice = 500,
                stock = 25,
                category = "Gaming",
                isActive = true,
                createdAt = now
            ),
            ProductEntity(
                productId = "ps5_controller",
                name = "DualSense Wireless Controller",
                description = "Experience lightning-fast immersive haptic feedback, dynamic triggers, and built-in mic inside an iconic design.",
                imageUrl = "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=600&q=80",
                coinPrice = 1500,
                stock = 5,
                category = "Gaming",
                isActive = true,
                createdAt = now - 1000
            ),
            ProductEntity(
                productId = "galaxy_buds",
                name = "SoundPulse ANC Earbuds",
                description = "Premium lightweight wireless earbuds featuring immersive SoundStage, active noise isolation, and up to 18 hours duration.",
                imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&q=80",
                coinPrice = 1200,
                stock = 8,
                category = "Gadgets",
                isActive = true,
                createdAt = now - 2000
            ),
            ProductEntity(
                productId = "smarttracker_v2",
                name = "Lootra Fit Tracker Band",
                description = "High-precision active fitness band to calculate resting statistics, sleep quality, and exercise duration metrics.",
                imageUrl = "https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?w=600&q=80",
                coinPrice = 900,
                stock = 12,
                category = "Gadgets",
                isActive = true,
                createdAt = now - 3000
            ),
            ProductEntity(
                productId = "cyberpunk_hoodie",
                name = "Lootra Neon Synth Hoodie",
                description = "Custom streetwear edition heavy hoodie with deep fleece comfort lining and stylish aesthetic visual graphics.",
                imageUrl = "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&q=80",
                coinPrice = 650,
                stock = 15,
                category = "Fashion",
                isActive = true,
                createdAt = now - 4000
            ),
            ProductEntity(
                productId = "amazon_20",
                name = "Amazon $20 Gift Card",
                description = "The world's most versatile digital gift card. Perfect for shopping, movie rentals, or cloud storage upgrades.",
                imageUrl = "https://images.unsplash.com/photo-1523474253046-8cd2748b5fd2?w=600&q=80",
                coinPrice = 200,
                stock = 45,
                category = "Vouchers",
                isActive = true,
                createdAt = now - 5000
            ),
            ProductEntity(
                productId = "netflix_3m",
                name = "Netflix 3-Month Voucher",
                description = "Enjoy cinema-grade entertainment in 4K resolution. Perfect for weekend show binging, ads-free.",
                imageUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8edd86?w=600&q=80",
                coinPrice = 350,
                stock = 30,
                category = "Vouchers",
                isActive = true,
                createdAt = now - 6000
            )
        )
    }

    // --- College and Pickup Location Management ---

    suspend fun getAllColleges(): List<com.example.data.model.College> {
        return try {
            val snapshot = db.collection("colleges").get().await()
            snapshot.documents.map { com.example.data.model.College.fromMap(it.data) }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting colleges: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveCollege(college: com.example.data.model.College): Boolean {
        return try {
            db.collection("colleges").document(college.collegeId).set(college.toMap()).await()
            true
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error saving college: ${e.message}")
            false
        }
    }

    suspend fun deleteCollege(collegeId: String): Boolean {
        return try {
            db.collection("colleges").document(collegeId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error deleting college: ${e.message}")
            false
        }
    }

    suspend fun getAllPickupLocations(): List<com.example.data.model.PickupLocation> {
        return try {
            val snapshot = db.collection("pickup_locations").get().await()
            snapshot.documents.map { com.example.data.model.PickupLocation.fromMap(it.data) }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting pickup locations: ${e.message}")
            emptyList()
        }
    }

    suspend fun savePickupLocation(location: com.example.data.model.PickupLocation): Boolean {
        return try {
            db.collection("pickup_locations").document(location.locationId).set(location.toMap()).await()
            true
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error saving pickup location: ${e.message}")
            false
        }
    }

    suspend fun deletePickupLocation(locationId: String): Boolean {
        return try {
            db.collection("pickup_locations").document(locationId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error deleting pickup location: ${e.message}")
            false
        }
    }
}
