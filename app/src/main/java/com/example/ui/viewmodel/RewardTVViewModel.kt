package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.manager.AdManager
import com.example.data.manager.MockAdManagerImpl
import com.example.data.model.RewardEntity
import com.example.data.model.WellbeingTvConfig
import com.example.data.repository.AuthRepository
import com.example.data.repository.RewardRepository
import com.example.data.repository.WellbeingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class RewardTVViewModel(
    private val authRepository: AuthRepository,
    private val rewardRepository: RewardRepository,
    private val wellbeingRepository: WellbeingRepository,
    private val walletRepository: com.example.data.repository.WalletRepository,
    val adManager: AdManager
) : ViewModel() {

    val config: StateFlow<WellbeingTvConfig> = rewardRepository.configFlow

    val adsRequired: StateFlow<Int> = config
        .map { 
            val value = it.adsRequired
            Log.d("RewardTVViewModel", "[DEBUG-USER] Current adsRequired value: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val coinsReward: StateFlow<Int> = config
        .map { 
            val value = it.coinsReward
            Log.d("RewardTVViewModel", "[DEBUG-USER] Current coinsReward value: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val dailyRewardLimit: StateFlow<Int> = config
        .map { 
            val value = it.dailyRewardLimit
            Log.d("RewardTVViewModel", "[DEBUG-USER] Current dailyRewardLimit value: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val dailyCoinLimit: StateFlow<Int> = config
        .map { 
            val value = it.dailyCoinLimit
            Log.d("RewardTVViewModel", "[DEBUG-USER] Current dailyCoinLimit value: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val rewardEnabled: StateFlow<Boolean> = config
        .map { 
            val value = it.rewardEnabled
            Log.d("RewardTVViewModel", "[DEBUG-USER] Current rewardEnabled value: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _adPlayingRewardId = MutableStateFlow<String?>(null)
    val adPlayingRewardId = _adPlayingRewardId.asStateFlow()

    private val _rewardClaimedSuccessAmount = MutableSharedFlow<Int>()
    val rewardClaimedSuccessAmount = _rewardClaimedSuccessAmount.asSharedFlow()

    val isAdLoaded: StateFlow<Boolean> = adManager.isAdLoaded
    val isLoadingAd: StateFlow<Boolean> = adManager.isLoading
    val loadAdError: StateFlow<String?> = adManager.loadError

    val uidFlow: StateFlow<String> = authRepository.currentUserState
        .map { it?.uid ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val todayScreenTimeMinutes: StateFlow<Long> = wellbeingRepository.getAllDailyReports()
        .map { reports ->
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val report = reports.find { it.date == todayStr }
            report?.totalScreenTimeMs?.div(60_000L) ?: 0L
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val rewards: StateFlow<List<RewardEntity>> = combine(uidFlow, config) { uid, _ -> uid }
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                rewardRepository.getTodayRewardsReactive(uid)
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
                        rewardRepository.fetchRewardsFromRemote(uid)
                        rewardRepository.getOrCreateLocalRewardSummary(uid)
                        rewardRepository.fetchRewardSummaryFromRemote(uid)
                        rewardRepository.getTodayRewardsList(uid)
                        rewardRepository.syncUnsyncedRewards(uid)
                    } catch (e: Exception) {
                        Log.e("RewardTVViewModel", "Error fetching/syncing remote reward data on uid update: ${e.message}")
                    } finally {
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    fun refreshRewards() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val uid = authRepository.getCurrentUser()?.uid
            if (uid != null) {
                rewardRepository.fetchRewardsFromRemote(uid)
                rewardRepository.getOrCreateLocalRewardSummary(uid)
                rewardRepository.fetchRewardSummaryFromRemote(uid)
                rewardRepository.getTodayRewardsList(uid)
                rewardRepository.syncUnsyncedRewards(uid)
            }
            _isRefreshing.value = false
        }
    }

    fun preloadRewardedAd(context: Context) {
        adManager.loadRewardedAd(context)
    }

    fun watchRewardedAd(context: Context, rewardId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        
        // Safety pre-buffer check: if the ad is not pre-buffered, request/preload it and do not block the user
        if (!isAdLoaded.value) {
            Log.e("RewardTVViewModel", "Active watch request rejected: Ad is not fully pre-buffered yet.")
            preloadRewardedAd(context)
            return
        }
        
        _adPlayingRewardId.value = rewardId
        
        adManager.showRewardedAd(
            context = context,
            onAdClosedOrFailed = {
                _adPlayingRewardId.value = null
            },
            onRewardEarned = {
                viewModelScope.launch {
                    try {
                        // Increment the ads watched state in database to persist progress
                        rewardRepository.watchAdForReward(uid, rewardId)
                        Log.i("RewardTVViewModel", "Successfully incremented ads watched progress for target: $rewardId")
                    } catch (e: Exception) {
                        Log.e("RewardTVViewModel", "Error while recording ad view progress: ${e.message}")
                    } finally {
                        _adPlayingRewardId.value = null
                    }
                }
            }
        )
    }

    fun claimReward(rewardId: String, coins: Int) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            val success = rewardRepository.claimRewardCoins(uid, rewardId)
            if (success) {
                _rewardClaimedSuccessAmount.emit(coins)
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val rewardRepository: RewardRepository,
        private val wellbeingRepository: WellbeingRepository,
        private val walletRepository: com.example.data.repository.WalletRepository,
        private val adManager: AdManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RewardTVViewModel(authRepository, rewardRepository, wellbeingRepository, walletRepository, adManager) as T
        }
    }
}
