package com.example.data.model

import androidx.annotation.Keep

@Keep
data class WellbeingTvConfig(
    val adsRequired: Int = 2,
    val coinsReward: Int = 5,
    val dailyRewardLimit: Int = 30,
    val dailyCoinLimit: Int = 150,
    val rewardEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Admin"
)
