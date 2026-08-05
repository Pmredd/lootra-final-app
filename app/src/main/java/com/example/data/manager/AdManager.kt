package com.example.data.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AdManager {
    val isAdLoaded: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val loadError: StateFlow<String?>

    fun loadRewardedAd(context: Context)
    fun showRewardedAd(
        context: Context,
        onAdClosedOrFailed: () -> Unit = {},
        onRewardEarned: () -> Unit
    )
}

class MockAdManagerImpl : AdManager {
    private val _isAdLoaded = MutableStateFlow(true)
    override val isAdLoaded = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    override val loadError = _loadError.asStateFlow()

    override fun loadRewardedAd(context: Context) {
        Log.d("MockAdManager", "Loading mock rewarded ad...")
        _isAdLoaded.value = true
        _isLoading.value = false
        _loadError.value = null
    }

    override fun showRewardedAd(
        context: Context,
        onAdClosedOrFailed: () -> Unit,
        onRewardEarned: () -> Unit
    ) {
        // Mock showing ad
        Log.d("MockAdManager", "Showing mock rewarded ad... completed!")
        onRewardEarned()
    }
}
