package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val reelId: String = "",
    val title: String = "",
    val description: String = "",
    val publisherName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val website: String = "",
    val mapsLink: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val location: String = "",
    val rewardCoins: Int = 0,
    val duration: Int = 0,
    val views: Int = 0,
    val likes: Int = 0,
    val maximumViews: Int = 0,
    val expiryDate: Long = 0L,
    val status: String = "Active",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val isSynced: Boolean = false,
    val campaignCoins: Int = 0,
    val distributedCoins: Int = 0,
    val rewardPerUser: Int = 0
) {
    val remainingCoins: Int
        get() = maxOf(0, campaignCoins - distributedCoins)

    val campaignStatus: String
        get() = if (remainingCoins <= 0) "Completed" else "Active"

    val locations: List<String>
        get() = if (location.isBlank()) emptyList() else location.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "reelId" to reelId,
            "title" to title,
            "description" to description,
            "publisherName" to publisherName,
            "contactPerson" to contactPerson,
            "phone" to phone,
            "whatsapp" to whatsapp,
            "email" to email,
            "website" to website,
            "mapsLink" to mapsLink,
            "thumbnailUrl" to thumbnailUrl,
            "videoUrl" to videoUrl,
            "location" to location,
            "locations" to locations,
            "rewardCoins" to rewardCoins,
            "duration" to duration,
            "views" to views,
            "likes" to likes,
            "maximumViews" to maximumViews,
            "expiryDate" to expiryDate,
            "status" to status,
            "createdBy" to createdBy,
            "createdAt" to createdAt,
            "campaignCoins" to campaignCoins,
            "distributedCoins" to distributedCoins,
            "rewardPerUser" to rewardPerUser,
            "remainingCoins" to remainingCoins,
            "campaignStatus" to campaignStatus
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): ReelEntity {
            if (map == null) return ReelEntity()
            val locationsList = map["locations"] as? List<*>
            val resolvedLocation = if (locationsList != null) {
                locationsList.filterIsInstance<String>().joinToString(",")
            } else {
                map["location"] as? String ?: ""
            }

            val rCoins = (map["rewardCoins"] as? Number)?.toInt() ?: 0
            val maxV = (map["maximumViews"] as? Number)?.toInt() ?: 0
            val vCount = (map["views"] as? Number)?.toInt() ?: 0

            val rewardPerUserVal = (map["rewardPerUser"] as? Number)?.toInt() ?: rCoins
            val campaignCoinsVal = (map["campaignCoins"] as? Number)?.toInt() ?: maxV
            val distributedCoinsVal = (map["distributedCoins"] as? Number)?.toInt() ?: (vCount * rewardPerUserVal)

            return ReelEntity(
                reelId = map["reelId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                publisherName = map["publisherName"] as? String ?: "",
                contactPerson = map["contactPerson"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                whatsapp = map["whatsapp"] as? String ?: "",
                email = map["email"] as? String ?: "",
                website = map["website"] as? String ?: "",
                mapsLink = map["mapsLink"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                videoUrl = map["videoUrl"] as? String ?: "",
                location = resolvedLocation,
                rewardCoins = rewardPerUserVal,
                duration = (map["duration"] as? Number)?.toInt() ?: 0,
                views = if (rewardPerUserVal > 0) distributedCoinsVal / rewardPerUserVal else distributedCoinsVal,
                likes = (map["likes"] as? Number)?.toInt() ?: 0,
                maximumViews = campaignCoinsVal,
                expiryDate = (map["expiryDate"] as? Number)?.toLong() ?: 0L,
                status = map["status"] as? String ?: "Active",
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                isSynced = true,
                campaignCoins = campaignCoinsVal,
                distributedCoins = distributedCoinsVal,
                rewardPerUser = rewardPerUserVal
            )
        }
    }
}
