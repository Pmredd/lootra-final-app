package com.example.data.model

import androidx.annotation.Keep

@Keep
data class College(
    val collegeId: String = "",
    val name: String = "",
    val available: Boolean = true
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "collegeId" to collegeId,
            "name" to name,
            "available" to available
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): College {
            if (map == null) return College()
            return College(
                collegeId = map["collegeId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                available = map["available"] as? Boolean ?: true
            )
        }
    }
}

@Keep
data class PickupLocation(
    val locationId: String = "",
    val name: String = "",
    val collegeId: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "locationId" to locationId,
            "name" to name,
            "collegeId" to collegeId
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): PickupLocation {
            if (map == null) return PickupLocation()
            return PickupLocation(
                locationId = map["locationId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                collegeId = map["collegeId"] as? String ?: ""
            )
        }
    }
}
