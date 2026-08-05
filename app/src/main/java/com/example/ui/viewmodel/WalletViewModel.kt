package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.WalletEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModel(
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _coinsClaimedAnimationTrigger = MutableSharedFlow<Int>()
    val coinsClaimedAnimationTrigger = _coinsClaimedAnimationTrigger.asSharedFlow()

    val uidFlow: StateFlow<String> = authRepository.currentUserState
        .map { it?.uid ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val wallet: StateFlow<WalletEntity?> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                walletRepository.getWalletReactive(uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<TransactionEntity>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                walletRepository.getTransactionsReactive(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            uidFlow.collectLatest { uid ->
                if (uid.isNotEmpty()) {
                    _isRefreshing.value = true
                    try {
                        walletRepository.getOrCreateLocalWallet(uid)
                        walletRepository.fetchWalletAndTransactionsFromRemote(uid)
                        walletRepository.syncUnsyncedData(uid)
                    } catch (e: Exception) {
                        Log.e("WalletViewModel", "Error fetching/syncing remote wallet data on uid update: ${e.message}")
                    } finally {
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    fun refreshWallet() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val uid = authRepository.getCurrentUser()?.uid
            if (uid != null) {
                walletRepository.getOrCreateLocalWallet(uid)
                walletRepository.syncUnsyncedData(uid)
            }
            _isRefreshing.value = false
        }
    }

    fun claimWelcomeBonus() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            val txs = walletRepository.getTransactionsReactive(uid).first()
            if (txs.none { it.type == "BONUS_EARNED" }) {
                walletRepository.addCoins(
                    uid = uid,
                    amount = 20, // 20 welcome coins reward
                    type = "BONUS_EARNED",
                    description = "Premium Welcoming Gift"
                )
                _coinsClaimedAnimationTrigger.emit(20)
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val walletRepository: WalletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(authRepository, walletRepository) as T
        }
    }
}
