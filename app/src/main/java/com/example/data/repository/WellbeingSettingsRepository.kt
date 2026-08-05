package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.WellbeingTvConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WellbeingSettingsRepository(
    context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val sharedPrefs = context.getSharedPreferences("wellbeing_tv_settings", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadCachedConfig())
    val configFlow: StateFlow<WellbeingTvConfig> = _configFlow.asStateFlow()

    init {
        listenToConfigChanges()
    }

    private fun loadCachedConfig(): WellbeingTvConfig {
        return try {
            val config = WellbeingTvConfig(
                adsRequired = sharedPrefs.getInt("adsRequired", 2),
                coinsReward = sharedPrefs.getInt("coinsReward", 5),
                dailyRewardLimit = sharedPrefs.getInt("dailyRewardLimit", 30),
                dailyCoinLimit = sharedPrefs.getInt("dailyCoinLimit", 150),
                rewardEnabled = sharedPrefs.getBoolean("rewardEnabled", true),
                updatedAt = sharedPrefs.getLong("updatedAt", System.currentTimeMillis()),
                updatedBy = sharedPrefs.getString("updatedBy", "Admin") ?: "Admin"
            )
            Log.d("WellbeingSettingsRepo", "Configuration Loaded: $config")
            Log.d("WellbeingSettingsRepo", "Ads Required: ${config.adsRequired}")
            Log.d("WellbeingSettingsRepo", "Coins Reward: ${config.coinsReward}")
            config
        } catch (e: Exception) {
            Log.e("WellbeingSettingsRepo", "[DEBUG-USER] Error loading cached config, using defaults", e)
            Log.d("WellbeingSettingsRepo", "If the reward still gives 5 Coins, it might be due to initial default value fallback: 5")
            WellbeingTvConfig()
        }
    }

    private fun cacheConfig(config: WellbeingTvConfig) {
        try {
            sharedPrefs.edit().apply {
                putInt("adsRequired", config.adsRequired)
                putInt("coinsReward", config.coinsReward)
                putInt("dailyRewardLimit", config.dailyRewardLimit)
                putInt("dailyCoinLimit", config.dailyCoinLimit)
                putBoolean("rewardEnabled", config.rewardEnabled)
                putLong("updatedAt", config.updatedAt)
                putString("updatedBy", config.updatedBy)
                apply()
            }
            Log.d("WellbeingSettingsRepo", "[DEBUG-USER] Cached config locally: $config")
        } catch (e: Exception) {
            Log.e("WellbeingSettingsRepo", "[DEBUG-USER] Error caching config locally", e)
        }
    }

    private fun listenToConfigChanges() {
        val docPath = "settings/wellbeing_tv"
        Log.d("WellbeingSettingsRepo", "[DEBUG-USER] Real-time SnapshotListener observing Firestore document path: $docPath")
        firestore.collection("settings").document("wellbeing_tv")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("WellbeingSettingsRepo", "[DEBUG-USER] SnapshotListener error on path $docPath", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val adsReq = snapshot.getLong("adsRequired")?.toInt() ?: 2
                        val coinsRew = snapshot.getLong("coinsReward")?.toInt() ?: 5
                        val dailyLimit = snapshot.getLong("dailyRewardLimit")?.toInt() ?: 30
                        val dailyCoinLim = snapshot.getLong("dailyCoinLimit")?.toInt() ?: 150
                        val enabled = snapshot.getBoolean("rewardEnabled") ?: true
                        val uAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                        val uBy = snapshot.getString("updatedBy") ?: "Admin"

                        val newConfig = WellbeingTvConfig(
                            adsRequired = adsReq,
                            coinsReward = coinsRew,
                            dailyRewardLimit = dailyLimit,
                            dailyCoinLimit = dailyCoinLim,
                            rewardEnabled = enabled,
                            updatedAt = uAt,
                            updatedBy = uBy
                        )
                        _configFlow.value = newConfig
                        cacheConfig(newConfig)
                        Log.d("WellbeingSettingsRepo", "Snapshot Updated!")
                        Log.d("WellbeingSettingsRepo", "Ads Required: $adsReq")
                        Log.d("WellbeingSettingsRepo", "Coins Reward: $coinsRew")
                    } catch (e: Exception) {
                        Log.e("WellbeingSettingsRepo", "[DEBUG-USER] Error parsing snapshot from '$docPath'", e)
                    }
                } else {
                    Log.d("WellbeingSettingsRepo", "[DEBUG-USER] Document '$docPath' does not exist in Firestore. Using defaults.")
                }
            }
    }
}
