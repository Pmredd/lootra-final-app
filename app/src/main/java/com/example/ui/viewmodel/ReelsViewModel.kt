package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReelEntity
import com.example.data.model.WatchedReelEntity
import com.example.data.model.ReelLikeEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.ReelRepository
import com.example.data.repository.ProductRepository
import com.example.data.datastore.WellbeingPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ReelsViewModel(
    private val authRepository: AuthRepository,
    private val reelRepository: ReelRepository,
    private val productRepository: ProductRepository,
    private val preferences: WellbeingPreferences
) : ViewModel() {

    private val _colleges = MutableStateFlow<List<com.example.data.model.College>>(emptyList())
    val colleges = _colleges.asStateFlow()

    val selectedCollegeId: StateFlow<String> = preferences.selectedCollegeIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedCollegeName: StateFlow<String> = preferences.selectedCollegeNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val uidFlow: StateFlow<String> = authRepository.currentUserState
        .map { it?.uid ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val filteredReels: StateFlow<List<ReelEntity>> = combine(
        reelRepository.reelsReactive,
        selectedCollegeName
    ) { reels, locationName ->
        val now = System.currentTimeMillis()
        Log.d("ReelsViewModel", "filteredReels combining: reels count = ${reels.size}, selectedCollegeName = '$locationName'")
        if (locationName.isBlank()) {
            emptyList()
        } else {
            val filtered = reels.filter { r ->
                val isStatusActive = r.status.equals("Active", ignoreCase = true)
                val isLocationMatch = r.locations.any { it.equals(locationName, ignoreCase = true) }
                val isNotExpired = (r.expiryDate == 0L || now < r.expiryDate)
                
                Log.d("ReelsViewModel", "Reel ID: ${r.reelId}, Title: ${r.title}, " +
                        "Active: $isStatusActive, LocationMatch: $isLocationMatch, " +
                        "NotExpired: $isNotExpired, Locations: ${r.locations}")
                
                isStatusActive && isLocationMatch && isNotExpired
            }
            Log.d("ReelsViewModel", "filteredReels output count: ${filtered.size}")
            filtered
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe liked and watched statuses
    val userLikedReels: StateFlow<List<ReelLikeEntity>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                reelRepository.getLikedReelsReactive(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWatchedReels: StateFlow<List<WatchedReelEntity>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) {
                reelRepository.getWatchedReelsReactive(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.collegesReactive.collect { list ->
                _colleges.value = list
            }
        }
        viewModelScope.launch {
            selectedCollegeName.collect { locationName ->
                reelRepository.startRealtimeReelsListener(locationName)
                if (locationName.isNotBlank()) {
                    _isRefreshing.value = true
                    try {
                        reelRepository.seedAndRefreshReels(locationName)
                    } catch (e: Exception) {
                        Log.e("ReelsViewModel", "Error syncing location reels: ${e.message}")
                    } finally {
                        _isRefreshing.value = false
                    }
                }
            }
        }
        refreshReels()
    }

    fun loadLocations() {
        viewModelScope.launch {
            try {
                _colleges.value = productRepository.getAllColleges()
            } catch (e: Exception) {
                Log.e("ReelsViewModel", "Error loading locations: ${e.message}")
            }
        }
    }

    fun selectLocation(locationId: String, locationName: String) {
        viewModelScope.launch {
            preferences.setSelectedCollege(locationId, locationName)
        }
    }

    fun refreshReels() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                reelRepository.seedAndRefreshReels(selectedCollegeName.value)
                loadLocations()
            } catch (e: Exception) {
                Log.e("ReelsViewModel", "Error refreshing reels: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleLike(reelId: String) {
        val uid = uidFlow.value
        if (uid.isEmpty()) return
        viewModelScope.launch {
            reelRepository.toggleLikeReel(uid, reelId)
        }
    }

    fun recordReelView(reelId: String) {
        // Obsoleted by the Campaign Coins Progress system
        Log.d("ReelsViewModel", "[DEBUG] recordReelView called for '$reelId' - Obsoleted by Campaign Coins Progress system.")
    }

    fun claimReelReward(reelId: String, onResult: (Boolean, String) -> Unit) {
        val uid = uidFlow.value
        if (uid.isEmpty()) {
            onResult(false, "User session not found.")
            return
        }
        viewModelScope.launch {
            try {
                val success = reelRepository.markReelCompletedAndReward(uid, reelId)
                if (success) {
                    onResult(true, "Successfully claimed reward!")
                } else {
                    onResult(false, "Reward already claimed or limit reached.")
                }
            } catch (e: Exception) {
                onResult(false, "Error claiming reward: ${e.message}")
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val reelRepository: ReelRepository,
        private val productRepository: ProductRepository,
        private val preferences: WellbeingPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReelsViewModel(
                authRepository,
                reelRepository,
                productRepository,
                preferences
            ) as T
        }
    }
}
