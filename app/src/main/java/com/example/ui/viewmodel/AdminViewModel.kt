package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.WellbeingApplication
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.example.data.model.UserProfile
import com.example.data.model.WalletEntity
import com.example.data.repository.AdminAnalyticsData
import com.example.data.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AdminUiState<out T> {
    object Loading : AdminUiState<Nothing>()
    data class Success<out T>(val data: T) : AdminUiState<T>()
    data class Error(val message: String) : AdminUiState<Nothing>()
}

class AdminViewModel(
    private val adminRepository: AdminRepository,
    val reelRepository: com.example.data.repository.ReelRepository,
    val storageRepository: com.example.data.repository.StorageRepository
) : ViewModel() {

    // --- Reels States ---
    private val _reelSearchQuery = MutableStateFlow("")
    val reelSearchQuery = _reelSearchQuery.asStateFlow()

    init {
        refreshAdminReels()
    }

    fun refreshAdminReels() {
        viewModelScope.launch {
            try {
                reelRepository.seedAndRefreshReels()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error refreshing reels: ${e.message}")
            }
        }
    }

    val adminReels: StateFlow<List<com.example.data.model.ReelEntity>> = combine(
        reelRepository.reelsReactive,
        reelSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true) ||
                it.publisherName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReelSearchQuery(query: String) {
        _reelSearchQuery.value = query
    }

    fun uploadFilesAndSaveReel(
        reelId: String,
        videoUri: android.net.Uri?,
        thumbnailUri: android.net.Uri?,
        onProgress: (String) -> Unit,
        onFinished: (Boolean, String?) -> Unit,
        reelBuilder: (String, String) -> com.example.data.model.ReelEntity
    ) {
        viewModelScope.launch {
            try {
                var finalVideoUrl = ""
                var finalThumbnailUrl = ""

                if (videoUri != null) {
                    onProgress("Uploading video file...")
                    val videoResult = storageRepository.uploadReelVideo(reelId, videoUri)
                    if (videoResult.isSuccess) {
                        finalVideoUrl = videoResult.getOrThrow()
                    } else {
                        onFinished(false, "Failed to upload video: ${videoResult.exceptionOrNull()?.message}")
                        return@launch
                    }
                }

                if (thumbnailUri != null) {
                    onProgress("Uploading thumbnail image...")
                    val thumbResult = storageRepository.uploadReelThumbnail(reelId, thumbnailUri)
                    if (thumbResult.isSuccess) {
                        finalThumbnailUrl = thumbResult.getOrThrow()
                    } else {
                        onFinished(false, "Failed to upload thumbnail: ${thumbResult.exceptionOrNull()?.message}")
                        return@launch
                    }
                }

                onProgress("Saving reel info...")
                val newReel = reelBuilder(finalVideoUrl, finalThumbnailUrl)
                val success = reelRepository.uploadReel(newReel)
                if (success) {
                    onFinished(true, null)
                } else {
                    onFinished(false, "Failed to save reel meta to database")
                }
            } catch (e: Exception) {
                onFinished(false, e.message)
            }
        }
    }

    fun uploadOrUpdateReel(reel: com.example.data.model.ReelEntity, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = reelRepository.uploadReel(reel)
            onFinished(success)
        }
    }

    fun deleteReel(reelId: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = reelRepository.deleteReel(reelId)
            onFinished(success)
        }
    }

    fun toggleReelStatus(reelId: String, currentStatus: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val reel = reelRepository.getReelById(reelId)
            if (reel != null) {
                val newStatus = if (currentStatus == "Active") "Disabled" else "Active"
                val updatedReel = reel.copy(status = newStatus)
                val success = reelRepository.uploadReel(updatedReel)
                onFinished(success)
            } else {
                onFinished(false)
            }
        }
    }

    // --- Admin Verification State ---
    private val _isAdmin = MutableStateFlow<Boolean?>(null)
    val isAdmin = _isAdmin.asStateFlow()

    private val _isVerifyingAdmin = MutableStateFlow(false)
    val isVerifyingAdmin = _isVerifyingAdmin.asStateFlow()

    // --- Analytics States ---
    private val _analyticsState = MutableStateFlow<AdminUiState<AdminAnalyticsData>>(AdminUiState.Loading)
    val analyticsState = _analyticsState.asStateFlow()

    // --- Products States ---
    private val _productsState = MutableStateFlow<AdminUiState<List<ProductEntity>>>(AdminUiState.Loading)
    val productsState = _productsState.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        productsState,
        productSearchQuery
    ) { state, query ->
        if (state is AdminUiState.Success) {
            val list = state.data
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.productId.contains(query, ignoreCase = true) ||
                    it.collegeName.contains(query, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Users States ---
    private val _usersState = MutableStateFlow<AdminUiState<List<UserProfile>>>(AdminUiState.Loading)
    val usersState = _usersState.asStateFlow()

    // --- Orders States ---
    private val _ordersState = MutableStateFlow<AdminUiState<List<OrderEntity>>>(AdminUiState.Loading)
    val ordersState = _ordersState.asStateFlow()

    private val _orderSearchQuery = MutableStateFlow("")
    val orderSearchQuery = _orderSearchQuery.asStateFlow()

    private val _orderFilterStatus = MutableStateFlow("All")
    val orderFilterStatus = _orderFilterStatus.asStateFlow()

    val filteredOrders: StateFlow<List<OrderEntity>> = combine(
        ordersState,
        orderSearchQuery,
        orderFilterStatus,
        usersState
    ) { state, query, status, uState ->
        if (state is AdminUiState.Success) {
            var list = state.data
            if (status != "All") {
                list = list.filter { it.orderStatus.equals(status, ignoreCase = true) }
            }
            if (query.isNotBlank()) {
                val userList = if (uState is AdminUiState.Success) uState.data else emptyList()
                list = list.filter { order ->
                    val user = userList.find { it.uid == order.uid }
                    val userName = user?.username ?: ""
                    val userEmail = user?.email ?: ""
                    
                    order.orderId.contains(query, ignoreCase = true) ||
                    order.productName.contains(query, ignoreCase = true) ||
                    order.uid.contains(query, ignoreCase = true) ||
                    userName.contains(query, ignoreCase = true) ||
                    userEmail.contains(query, ignoreCase = true) ||
                    order.paymentStatus.contains(query, ignoreCase = true) ||
                    order.pricingType.contains(query, ignoreCase = true)
                }
            }
            list
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery = _userSearchQuery.asStateFlow()

    val filteredUsers: StateFlow<List<UserProfile>> = combine(
        usersState,
        userSearchQuery
    ) { state, query ->
        if (state is AdminUiState.Success) {
            val list = state.data
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.username.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.uid.contains(query, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selected User Metrics for Dialog / details ---
    private val _selectedUserWallet = MutableStateFlow<WalletEntity?>(null)
    val selectedUserWallet = _selectedUserWallet.asStateFlow()

    private val _selectedUserOrdersCount = MutableStateFlow(0)
    val selectedUserOrdersCount = _selectedUserOrdersCount.asStateFlow()

    private val _selectedUserRewards = MutableStateFlow<Map<String, Any>>(emptyMap())
    val selectedUserRewards = _selectedUserRewards.asStateFlow()

    private val _isLoadingUserMetrics = MutableStateFlow(false)
    val isLoadingUserMetrics = _isLoadingUserMetrics.asStateFlow()

    // --- Sub-Admin States ---
    private val _subAdminsState = MutableStateFlow<AdminUiState<List<com.example.data.repository.SubAdminUser>>>(AdminUiState.Loading)
    val subAdminsState = _subAdminsState.asStateFlow()

    private val _subAdminSearchQuery = MutableStateFlow("")
    val subAdminSearchQuery = _subAdminSearchQuery.asStateFlow()

    val filteredSubAdmins: StateFlow<List<com.example.data.repository.SubAdminUser>> = combine(
        subAdminsState,
        subAdminSearchQuery
    ) { state, query ->
        if (state is AdminUiState.Success) {
            val list = state.data
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.collegeName.contains(query, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Firestore Listener registrations for real-time dashboard synching
    private var ordersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var productsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var usersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // --- Admin Authentication Check ---
    fun checkAdminPermission(uid: String, onFinished: (Boolean) -> Unit = {}) {
        if (uid.isEmpty()) {
            _isAdmin.value = false
            onFinished(false)
            return
        }
        viewModelScope.launch {
            _isVerifyingAdmin.value = true
            val result = adminRepository.verifyAdminStatus(uid)
            if (result.isSuccess) {
                val hasAccess = result.getOrNull() ?: false
                _isAdmin.value = hasAccess
                onFinished(hasAccess)
                if (hasAccess) {
                    startRealtimeListeners()
                }
            } else {
                _isAdmin.value = false
                onFinished(false)
            }
            _isVerifyingAdmin.value = false
        }
    }

    private fun startRealtimeListeners() {
        val firestore = FirebaseFirestore.getInstance()

        // 1. Listen to products
        productsListenerRegistration?.remove()
        productsListenerRegistration = firestore.collection("products")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _productsState.value = AdminUiState.Error(e.message ?: "Failed listening products")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        ProductEntity.fromMap(doc.data)
                    }.sortedByDescending { it.createdAt }
                    _productsState.value = AdminUiState.Success(list)
                    refreshAnalytics() // refresh counts in database
                }
            }

        // 2. Listen to orders
        ordersListenerRegistration?.remove()
        ordersListenerRegistration = firestore.collection("orders")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _ordersState.value = AdminUiState.Error(e.message ?: "Failed listening orders")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        OrderEntity.fromMap(doc.data)
                    }.sortedByDescending { it.createdAt }
                    _ordersState.value = AdminUiState.Success(list)
                    refreshAnalytics()
                }
            }

        // 3. Listen to users
        usersListenerRegistration?.remove()
        usersListenerRegistration = firestore.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _usersState.value = AdminUiState.Error(e.message ?: "Failed listening users")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        UserProfile.fromMap(doc.data)
                    }
                    _usersState.value = AdminUiState.Success(list)
                    refreshAnalytics()
                }
            }
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            val result = adminRepository.getAnalytics()
            if (result.isSuccess) {
                _analyticsState.value = AdminUiState.Success(result.getOrDefault(AdminAnalyticsData()))
            } else {
                _analyticsState.value = AdminUiState.Error(result.exceptionOrNull()?.message ?: "Analytics Fetch Error")
            }
        }
    }

    // --- Product Management Actions ---
    fun addProduct(product: ProductEntity, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentState = _productsState.value
            if (currentState is AdminUiState.Success) {
                val currentProducts = currentState.data
                val trimmedName = product.name.trim()
                if (currentProducts.any { it.productId == product.productId }) {
                    onFinished(false, "Error: Product ID already exists.")
                    return@launch
                }
                if (currentProducts.any { it.name.trim().equals(trimmedName, ignoreCase = true) }) {
                    onFinished(false, "Error: Product name '${product.name}' already exists.")
                    return@launch
                }
            }
            val result = adminRepository.addProduct(product)
            if (result.isSuccess) {
                onFinished(true, "Successfully added ${product.name}")
            } else {
                onFinished(false, result.exceptionOrNull()?.message ?: "Adding failed")
            }
        }
    }

    fun editProduct(product: ProductEntity, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentState = _productsState.value
            if (currentState is AdminUiState.Success) {
                val currentProducts = currentState.data
                val trimmedName = product.name.trim()
                if (currentProducts.any { it.productId != product.productId && it.name.trim().equals(trimmedName, ignoreCase = true) }) {
                    onFinished(false, "Error: Another product with name '${product.name}' already exists.")
                    return@launch
                }
            }
            val result = adminRepository.editProduct(product)
            if (result.isSuccess) {
                onFinished(true, "Successfully updated product information")
            } else {
                onFinished(false, result.exceptionOrNull()?.message ?: "Update failed")
            }
        }
    }

    fun deleteProduct(productId: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteProduct(productId)
            if (result.isSuccess) {
                onFinished(true, "Product successfully deleted")
            } else {
                onFinished(false, result.exceptionOrNull()?.message ?: "Deletion failed")
            }
        }
    }

    fun toggleProductActive(productId: String, currentActive: Boolean) {
        viewModelScope.launch {
            adminRepository.setProductActiveState(productId, !currentActive)
        }
    }

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    // --- Order Management Actions ---
    fun updateOrderStatus(orderId: String, newStatus: String, onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = adminRepository.updateOrderStatus(orderId, newStatus)
            if (result.isSuccess) {
                onFinished(true)
            } else {
                onFinished(false)
            }
        }
    }

    fun setOrderSearchQuery(query: String) {
        _orderSearchQuery.value = query
    }

    fun setOrderFilterStatus(status: String) {
        _orderFilterStatus.value = status
    }

    // --- User Management Actions ---
    fun setUserSearchQuery(query: String) {
        _userSearchQuery.value = query
    }

    fun loadUserMetrics(uid: String) {
        viewModelScope.launch {
            _isLoadingUserMetrics.value = true
            val walletResult = adminRepository.getUserWallet(uid)
            val ordersCountResult = adminRepository.getUserOrdersCount(uid)
            val rewardsResult = adminRepository.getUserRewardStats(uid)

            _selectedUserWallet.value = walletResult.getOrNull()
            _selectedUserOrdersCount.value = ordersCountResult.getOrDefault(0)
            _selectedUserRewards.value = rewardsResult.getOrDefault(emptyMap())
            _isLoadingUserMetrics.value = false
        }
    }

    // --- Sub-Admin Management Actions ---
    fun setSubAdminSearchQuery(query: String) {
        _subAdminSearchQuery.value = query
    }

    fun loadSubAdmins() {
        viewModelScope.launch {
            _subAdminsState.value = AdminUiState.Loading
            val result = adminRepository.getAllSubAdmins()
            if (result.isSuccess) {
                _subAdminsState.value = AdminUiState.Success(result.getOrDefault(emptyList()))
            } else {
                _subAdminsState.value = AdminUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load sub-admins")
            }
        }
    }

    fun createSubAdmin(
        email: String,
        password: String,
        name: String,
        collegeId: String,
        collegeName: String,
        isActive: Boolean,
        onFinished: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.createSubAdmin(email, password, name, collegeId, collegeName, isActive)
            if (result.isSuccess) {
                loadSubAdmins()
            }
            onFinished(result)
        }
    }

    fun updateSubAdmin(
        subAdmin: com.example.data.repository.SubAdminUser,
        onFinished: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateSubAdmin(subAdmin)
            if (result.isSuccess) {
                loadSubAdmins()
            }
            onFinished(result)
        }
    }

    fun deleteSubAdmin(
        uid: String,
        onFinished: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.deleteSubAdmin(uid)
            if (result.isSuccess) {
                loadSubAdmins()
            }
            onFinished(result)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ordersListenerRegistration?.remove()
        productsListenerRegistration?.remove()
        usersListenerRegistration?.remove()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WellbeingApplication
                return AdminViewModel(
                    adminRepository = application.container.adminRepository,
                    reelRepository = application.container.reelRepository,
                    storageRepository = application.container.storageRepository
                ) as T
            }
        }
    }
}
