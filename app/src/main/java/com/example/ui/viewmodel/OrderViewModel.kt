package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.OrderEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModel(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val uidFlow: StateFlow<String> = authRepository.currentUserState
        .map { it?.uid ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val orders: StateFlow<List<OrderEntity>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                orderRepository.getOrdersReactive(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            uidFlow.collectLatest { uid ->
                if (uid.isNotEmpty()) {
                    orderRepository.startListeningToOrders(uid)
                    refreshOrders()
                } else {
                    orderRepository.stopListeningToOrders()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        orderRepository.stopListeningToOrders()
    }

    fun refreshOrders() {
        val uid = uidFlow.value
        if (uid.isEmpty()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                orderRepository.fetchOrdersFromRemote(uid)
                orderRepository.syncUnsyncedOrders(uid)
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Failed to refresh orders: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val orderRepository: OrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderViewModel(authRepository, orderRepository) as T
        }
    }
}
