package com.example.data.model

import androidx.annotation.Keep

@Keep
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profileImage: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isActive: Boolean = true,
    val activeDeviceId: String = "",
    val deviceModel: String = "",
    val lastLoginTime: Long = 0L,
    val sessionActive: Boolean = false,
    val legalAccepted: Boolean = false,
    val acceptedAt: Long = 0L,
    val privacyVersion: String = "1.0.0",
    val termsVersion: String = "1.0.0",
    val communityVersion: String = "1.0.0"
) {
    // Helper to map easily to Firestore map structure
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "email" to email,
            "profileImage" to profileImage,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isActive" to isActive,
            "activeDeviceId" to activeDeviceId,
            "deviceModel" to deviceModel,
            "lastLoginTime" to lastLoginTime,
            "sessionActive" to sessionActive,
            "legalAccepted" to legalAccepted,
            "acceptedAt" to acceptedAt,
            "privacyVersion" to privacyVersion,
            "termsVersion" to termsVersion,
            "communityVersion" to communityVersion
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): UserProfile {
            if (map == null) return UserProfile()
            return UserProfile(
                uid = map["uid"] as? String ?: "",
                username = map["username"] as? String ?: "",
                email = map["email"] as? String ?: "",
                profileImage = map["profileImage"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isActive = map["isActive"] as? Boolean ?: true,
                activeDeviceId = map["activeDeviceId"] as? String ?: "",
                deviceModel = map["deviceModel"] as? String ?: "",
                lastLoginTime = (map["lastLoginTime"] as? Number)?.toLong() ?: 0L,
                sessionActive = map["sessionActive"] as? Boolean ?: false,
                legalAccepted = map["legalAccepted"] as? Boolean ?: map["legalAcceptance.accepted"] as? Boolean ?: false,
                acceptedAt = (map["acceptedAt"] as? Number)?.toLong() ?: 0L,
                privacyVersion = map["privacyVersion"] as? String ?: "1.0.0",
                termsVersion = map["termsVersion"] as? String ?: "1.0.0",
                communityVersion = map["communityVersion"] as? String ?: "1.0.0"
            )
        }
    }
}
