package com.example.data.manager

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.dao.AdHistoryDao
import com.example.data.model.AdHistoryEntity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class RewardedAdManager(
    private val context: Context,
    private val adHistoryDao: AdHistoryDao
) : AdManager {

    private val _isAdLoaded = MutableStateFlow(false)
    override val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    override val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private val productionAdUnitId = "ca-app-pub-7517042272316969/9970193668" // Production AdMob Rewarded Ad Unit ID

    private val managerScope = CoroutineScope(Dispatchers.Default)

    // Anti-Abuse variables
    private var lastShowTime = 0L
    private val cooldownDurationMs = 15000L // 15 seconds cooldown between ad shows
    private var isShowingAdLock = false // Transaction lock to prevent duplicate shows & rewards
    private var retryAttempt = 0
    private val maxRetryAttempts = 5

    override fun loadRewardedAd(context: Context) {
        if (_isLoading.value) {
            Log.d("RewardedAdManager", "Ad loading is already in progress.")
            return
        }

        managerScope.launch {
            loadAdWithRetry(context)
        }
    }

    private suspend fun loadAdWithRetry(context: Context) {
        _isLoading.value = true
        _loadError.value = null
        _isAdLoaded.value = false

        var success = false
        val delayTime = getRetryDelay(retryAttempt)

        if (retryAttempt > 0) {
            Log.d("RewardedAdManager", "Retrying ad load in ${delayTime / 1000} seconds (Attempt: $retryAttempt)")
            delay(delayTime)
        }

        // Verify Network Connectivity before attempting load
        if (!isNetworkAvailable(context)) {
            Log.e("RewardedAdManager", "Offline! Unable to load ad. Queueing for retry.")
            _isLoading.value = false
            _loadError.value = "Device is offline"
            scheduleRetry(context)
            return
        }

        withContext(Dispatchers.Main) {
            try {
                // Timeout loading after 15 seconds to prevent hanging the loader UX
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    productionAdUnitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            val errorCode = adError.code
                            val errorDomain = adError.domain
                            val errorMessage = adError.message
                            val errorCause = adError.cause?.toString() ?: "None"
                            
                            val displayMsg = when (errorCode) {
                                3 -> "Ad temporary unavailable (NO FILL - Code 3). This is expected for new production ad units or inactive accounts and takes some time to resolve on AdMob servers."
                                2 -> "Network connection error (Code 2). Please verify your internet connectivity and try again."
                                1 -> "Configuration configuration request issue (Code 1). Invalid Ad Unit ID or layout request."
                                0 -> "Internal SDK/AdMob servers error (Code 0)."
                                else -> "$errorMessage (Code $errorCode)"
                            }
                            
                            Log.e("RewardedAdManager", "Ad failed to load in production mode:\n" +
                                    "Error Code: $errorCode\n" +
                                    "Domain: $errorDomain\n" +
                                    "Message: $errorMessage\n" +
                                    "Cause: $errorCause\n" +
                                    "Resolution Hint: $displayMsg"
                            )
                            rewardedAd = null
                            _isAdLoaded.value = false
                            _isLoading.value = false
                            _loadError.value = displayMsg
                            
                            // Log Failed callback event in Room for anti-abuse & profiling
                            logAdEvent(status = "FAILED", completedAdsCount = 0)

                            scheduleRetry(context)
                        }

                        override fun onAdLoaded(ad: RewardedAd) {
                            Log.d("RewardedAdManager", "Ad loaded successfully.")
                            rewardedAd = ad
                            _isAdLoaded.value = true
                            _isLoading.value = false
                            _loadError.value = null
                            retryAttempt = 0 // Reset retry count upon successful loading
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("RewardedAdManager", "Exception during ad load initialization: ${e.message}")
                _isLoading.value = false
                _loadError.value = e.message
                scheduleRetry(context)
            }
        }
    }

    private fun scheduleRetry(context: Context) {
        if (retryAttempt < maxRetryAttempts) {
            retryAttempt++
            managerScope.launch {
                loadAdWithRetry(context)
            }
        } else {
            Log.e("RewardedAdManager", "Max retry attempts reached. Stopping ad load retries.")
            _loadError.value = "Failed to load ad after multiple retries"
        }
    }

    private fun getRetryDelay(attempt: Int): Long {
        // Exponential backoff logic
        return when (attempt) {
            0 -> 0L
            1 -> 3000L
            2 -> 6000L
            3 -> 12000L
            4 -> 24000L
            else -> 30000L
        }
    }

    override fun showRewardedAd(
        context: Context,
        onAdClosedOrFailed: () -> Unit,
        onRewardEarned: () -> Unit
    ) {
        val currentTimestamp = System.currentTimeMillis()

        // 1. Cooldown Anti-Abuse Check
        if (currentTimestamp - lastShowTime < cooldownDurationMs) {
            val remaining = (cooldownDurationMs - (currentTimestamp - lastShowTime)) / 1000
            Log.w("RewardedAdManager", "Ad show request rejected due to cooldown. Wait $remaining seconds.")
            _loadError.value = "Please wait $remaining seconds before watching another ad"
            onAdClosedOrFailed()
            return
        }

        // 2. Transaction Lock to prevent concurrent show calls
        if (isShowingAdLock) {
            Log.w("RewardedAdManager", "Ad show rejected: Ad display is already locked.")
            onAdClosedOrFailed()
            return
        }

        val ad = rewardedAd
        val activity = findActivity(context)

        if (ad != null && activity != null) {
            isShowingAdLock = true
            _loadError.value = null

            var rewardCallbackDispatched = false

            activity.runOnUiThread {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("RewardedAdManager", "Ad dismissed by user.")
                        rewardedAd = null
                        _isAdLoaded.value = false
                        isShowingAdLock = false
                        
                        // User closed/dismissed the ad (not completed or rewarded yet unless completion callback triggers first)
                        logAdEvent(status = "CLOSED", completedAdsCount = 0)
                        
                        // Automatically preload the next ad in the background
                        loadRewardedAd(context)
                        onAdClosedOrFailed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        val errorCode = adError.code
                        val errorDomain = adError.domain
                        val errorMessage = adError.message
                        Log.e("RewardedAdManager", "Ad failed to show: Code $errorCode, Domain: $errorDomain, Message: $errorMessage")
                        rewardedAd = null
                        _isAdLoaded.value = false
                        isShowingAdLock = false
                        _loadError.value = "Ad Show Failed: $errorMessage (Code $errorCode)"

                        logAdEvent(status = "FAILED", completedAdsCount = 0)

                        // Reload
                        loadRewardedAd(context)
                        onAdClosedOrFailed()
                    }
                }

                ad.show(activity) { rewardItem ->
                    // 3. User Watched Complete - grant reward callback safely
                    if (!rewardCallbackDispatched) {
                        rewardCallbackDispatched = true
                        lastShowTime = System.currentTimeMillis()
                        Log.i("RewardedAdManager", "Ad successfully watched completely. Processing reward callback.")

                        // Anti-abuse completed ad validation and persistence
                        managerScope.launch {
                            logAdEvent(status = "COMPLETED", completedAdsCount = 1)
                        }

                        onRewardEarned()
                    } else {
                        Log.w("RewardedAdManager", "Duplicate reward callback detected and ignored.")
                    }
                }
            }
        } else {
            Log.e("RewardedAdManager", "Show failed. Ad loaded: ${ad != null}, Activity parsed: ${activity != null}")
            _loadError.value = "Ad not ready for display yet"
            if (ad == null) {
                loadRewardedAd(context)
            }
            onAdClosedOrFailed()
        }
    }

    private fun logAdEvent(status: String, completedAdsCount: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        managerScope.launch {
            try {
                val entity = AdHistoryEntity(
                    uid = uid,
                    rewardAmount = if (status == "COMPLETED") 5 else 0,
                    completedAdsCount = completedAdsCount,
                    watchedAt = System.currentTimeMillis(),
                    status = status
                )
                adHistoryDao.insertAdHistory(entity)
                Log.d("RewardedAdManager", "Recorded Ad History event locally in Room: status=$status")
            } catch (e: Exception) {
                Log.e("RewardedAdManager", "Error storing ad history event locally: ${e.message}")
            }
        }
    }

    private fun findActivity(context: Context): Activity? {
        var tempContext = context
        while (tempContext is ContextWrapper) {
            if (tempContext is Activity) {
                return tempContext
            }
            tempContext = tempContext.baseContext
        }
        return null
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
