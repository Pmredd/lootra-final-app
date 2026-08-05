package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProductEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.WalletRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    data class Success(val orderId: String, val productName: String) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val walletRepository: WalletRepository,
    private val orderRepository: OrderRepository,
    private val preferences: com.example.data.datastore.WellbeingPreferences
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState = _purchaseState.asStateFlow()

    private val _colleges = MutableStateFlow<List<com.example.data.model.College>>(emptyList())
    val colleges = _colleges.asStateFlow()

    private val _pickupLocations = MutableStateFlow<List<com.example.data.model.PickupLocation>>(emptyList())
    val pickupLocations = _pickupLocations.asStateFlow()

    val selectedCollegeId: StateFlow<String> = preferences.selectedCollegeIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedCollegeName: StateFlow<String> = preferences.selectedCollegeNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val uidFlow: StateFlow<String> = authRepository.currentUserState
        .map { it?.uid ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Filtered list of products (only loads products assigned to user's selected college!)
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        productRepository.productsReactive,
        _searchQuery,
        _selectedCategory,
        selectedCollegeId
    ) { catalog, query, category, collId ->
        val uniqueCatalog = catalog.distinctBy { it.productId }
            .filter { it.isActive && it.stock > 0 }
        
        // Filter by user's selected college ID
        val collegeCatalog = uniqueCatalog.filter { it.collegeId == collId }

        collegeCatalog.filter { product ->
            val matchesSearch = product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || product.category.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = productRepository.productsReactive
        .map { catalog ->
            val uniqueCatalog = catalog.distinctBy { it.productId }
                .filter { it.isActive && it.stock > 0 }
            listOf("All") + uniqueCatalog.map { it.category }.distinct().filter { it.isNotEmpty() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val userWallet = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                walletRepository.getWalletReactive(uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshCatalog()
        loadCollegesAndLocations()
        viewModelScope.launch {
            productRepository.collegesReactive.collect { list ->
                _colleges.value = list
            }
        }
    }

    fun loadCollegesAndLocations() {
        viewModelScope.launch {
            _colleges.value = productRepository.getAllColleges()
            _pickupLocations.value = productRepository.getAllPickupLocations()
        }
    }

    fun selectCollege(collegeId: String, collegeName: String) {
        viewModelScope.launch {
            preferences.setSelectedCollege(collegeId, collegeName)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                productRepository.seedAndRefreshProducts()
                loadCollegesAndLocations()
            } catch (e: Exception) {
                Log.e("ShopViewModel", "Failed to refresh products: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun purchaseProduct(product: ProductEntity) {
        val uid = uidFlow.value
        if (uid.isEmpty()) {
            _purchaseState.value = PurchaseState.Error("User session not found. Please log in.")
            return
        }

        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            try {
                // Double check stock first
                val currentProduct = productRepository.getProductById(product.productId)
                if (currentProduct == null || currentProduct.stock <= 0) {
                    _purchaseState.value = PurchaseState.Error("Product is currently out of stock.")
                    return@launch
                }

                // Pricing parameters
                val pricingType = product.pricingType
                val coinsToDeduct = if (pricingType == "MONEY_ONLY") 0 else (if (product.coinsRequired > 0) product.coinsRequired else product.coinPrice)
                val moneyToPay = if (pricingType == "COINS_ONLY") 0 else product.moneyPrice
                
                // For money/hybrid pricing, initial status is Pending
                val paymentStatus = if (pricingType == "MONEY_ONLY" || pricingType == "HYBRID") "Pending" else "Paid"

                // Check and deduct coins if needed
                if (coinsToDeduct > 0) {
                    val wallet = walletRepository.getOrCreateLocalWallet(uid)
                    if (wallet.totalCoins < coinsToDeduct) {
                        _purchaseState.value = PurchaseState.Error("Insufficient coins. Earn more by reducing screen time!")
                        return@launch
                    }

                    // Step 1: Deduct coins from wallet & save transaction
                    val successDeduction = walletRepository.deductCoins(
                        uid = uid,
                        amount = coinsToDeduct,
                        type = "FUTURE_PURCHASE",
                        description = "Purchased ${product.name}"
                    )

                    if (!successDeduction) {
                        _purchaseState.value = PurchaseState.Error("Transaction failed. Insufficient funds.")
                        return@launch
                    }
                }

                // Step 2: Decrease stock of product
                productRepository.updateStock(product.productId, 1)

                // Step 3: Create confirmed order with college and pickup location from product
                val order = orderRepository.createOrder(
                    uid = uid,
                    productId = product.productId,
                    productName = product.name,
                    coinPrice = coinsToDeduct,
                    selectedCollegeId = product.collegeId,
                    selectedCollegeName = product.collegeName,
                    pickupLocation = product.pickupLocation,
                    pricingType = pricingType,
                    coinsUsed = coinsToDeduct,
                    moneyPaid = moneyToPay,
                    paymentStatus = paymentStatus
                )

                _purchaseState.value = PurchaseState.Success(order.orderId, product.name)
            } catch (e: Exception) {
                Log.e("ShopViewModel", "Purchase transaction failed: ${e.message}")
                _purchaseState.value = PurchaseState.Error("An error occurred: ${e.message}")
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val productRepository: ProductRepository,
        private val walletRepository: WalletRepository,
        private val orderRepository: OrderRepository,
        private val preferences: com.example.data.datastore.WellbeingPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShopViewModel(
                authRepository,
                productRepository,
                walletRepository,
                orderRepository,
                preferences
            ) as T
        }
    }
}
