package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity

@Keep
@Entity(tableName = "reel_likes", primaryKeys = ["uid", "reelId"])
data class ReelLikeEntity(
    val uid: String = "",
    val reelId: String = "",
    val likedAt: Long = 0L,
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "reelId" to reelId,
            "likedAt" to likedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?, uid: String): ReelLikeEntity {
            if (map == null) return ReelLikeEntity()
            return ReelLikeEntity(
                uid = uid,
                reelId = map["reelId"] as? String ?: "",
                likedAt = (map["likedAt"] as? Number)?.toLong() ?: 0L,
                isSynced = true
            )
        }
    }
}
