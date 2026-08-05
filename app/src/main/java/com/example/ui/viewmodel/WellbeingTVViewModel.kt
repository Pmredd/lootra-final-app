package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.WellbeingSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WellbeingTVViewModel(
    private val wellbeingSettingsRepository: WellbeingSettingsRepository
) : ViewModel() {

    val config = wellbeingSettingsRepository.configFlow

    val adsRequired: StateFlow<Int> = config
        .map { 
            val value = it.adsRequired
            Log.d("WellbeingTVViewModel", "[DEBUG-USER] Observed adsRequired changed: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val coinsReward: StateFlow<Int> = config
        .map { 
            val value = it.coinsReward
            Log.d("WellbeingTVViewModel", "[DEBUG-USER] Observed coinsReward changed: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val dailyRewardLimit: StateFlow<Int> = config
        .map { 
            val value = it.dailyRewardLimit
            Log.d("WellbeingTVViewModel", "[DEBUG-USER] Observed dailyRewardLimit changed: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val dailyCoinLimit: StateFlow<Int> = config
        .map { 
            val value = it.dailyCoinLimit
            Log.d("WellbeingTVViewModel", "[DEBUG-USER] Observed dailyCoinLimit changed: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val rewardEnabled: StateFlow<Boolean> = config
        .map { 
            val value = it.rewardEnabled
            Log.d("WellbeingTVViewModel", "[DEBUG-USER] Observed rewardEnabled changed: $value")
            value
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
