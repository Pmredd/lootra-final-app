package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val productId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val publicId: String = "",
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val uploadedAt: Long = 0L,
    val coinPrice: Int = 0,
    val stock: Int = 0,
    val category: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val collegeId: String = "",
    val collegeName: String = "",
    val pickupLocation: String = "",
    val isSynced: Boolean = false,
    val pricingType: String = "COINS_ONLY",
    val coinsRequired: Int = 0,
    val moneyPrice: Int = 0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "productId" to productId,
            "name" to name,
            "description" to description,
            "imageUrl" to imageUrl,
            "publicId" to publicId,
            "imageWidth" to imageWidth,
            "imageHeight" to imageHeight,
            "uploadedAt" to uploadedAt,
            "coinPrice" to coinPrice,
            "stock" to stock,
            "category" to category,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "collegeId" to collegeId,
            "collegeName" to collegeName,
            "pickupLocation" to pickupLocation,
            "pricingType" to pricingType,
            "coinsRequired" to coinsRequired,
            "moneyPrice" to moneyPrice
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): ProductEntity {
            if (map == null) return ProductEntity()
            val cPrice = (map["coinPrice"] as? Number)?.toInt() ?: 0
            val pType = map["pricingType"] as? String ?: "COINS_ONLY"
            val cReq = (map["coinsRequired"] as? Number)?.toInt() ?: cPrice
            val mPrice = (map["moneyPrice"] as? Number)?.toInt() ?: 0
            return ProductEntity(
                productId = map["productId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                publicId = map["publicId"] as? String ?: "",
                imageWidth = (map["imageWidth"] as? Number)?.toInt() ?: 0,
                imageHeight = (map["imageHeight"] as? Number)?.toInt() ?: 0,
                uploadedAt = (map["uploadedAt"] as? Number)?.toLong() ?: 0L,
                coinPrice = cPrice,
                stock = (map["stock"] as? Number)?.toInt() ?: 0,
                category = map["category"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                collegeId = map["collegeId"] as? String ?: "",
                collegeName = map["collegeName"] as? String ?: "",
                pickupLocation = map["pickupLocation"] as? String ?: "",
                isSynced = true,
                pricingType = pType,
                coinsRequired = if (cReq == 0 && pType == "COINS_ONLY") cPrice else cReq,
                moneyPrice = mPrice
            )
        }
    }
}

@Keep
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String = "",
    val uid: String = "",
    val productId: String = "",
    val productName: String = "",
    val coinPrice: Int = 0,
    val orderStatus: String = "Pending", // "Pending", "Confirmed", "Shipped", "Delivered", "Cancelled"
    val createdAt: Long = 0L,
    val selectedCollegeId: String = "",
    val selectedCollegeName: String = "",
    val pickupLocation: String = "",
    val isSynced: Boolean = false,
    val pricingType: String = "COINS_ONLY",
    val coinsUsed: Int = 0,
    val moneyPaid: Int = 0,
    val paymentStatus: String = "Pending"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "orderId" to orderId,
            "uid" to uid,
            "productId" to productId,
            "productName" to productName,
            "coinPrice" to coinPrice,
            "orderStatus" to orderStatus,
            "createdAt" to createdAt,
            "selectedCollegeId" to selectedCollegeId,
            "selectedCollegeName" to selectedCollegeName,
            "pickupLocation" to pickupLocation,
            "pricingType" to pricingType,
            "coinsUsed" to coinsUsed,
            "moneyPaid" to moneyPaid,
            "paymentStatus" to paymentStatus
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): OrderEntity {
            if (map == null) return OrderEntity()
            val cPrice = (map["coinPrice"] as? Number)?.toInt() ?: 0
            val pType = map["pricingType"] as? String ?: "COINS_ONLY"
            val cUsed = (map["coinsUsed"] as? Number)?.toInt() ?: cPrice
            val mPaid = (map["moneyPaid"] as? Number)?.toInt() ?: 0
            val pStatus = map["paymentStatus"] as? String ?: (if (pType == "COINS_ONLY") "Completed" else "Pending")
            return OrderEntity(
                orderId = map["orderId"] as? String ?: "",
                uid = map["uid"] as? String ?: "",
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                coinPrice = cPrice,
                orderStatus = map["orderStatus"] as? String ?: "Pending",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                selectedCollegeId = map["selectedCollegeId"] as? String ?: "",
                selectedCollegeName = map["selectedCollegeName"] as? String ?: "",
                pickupLocation = map["pickupLocation"] as? String ?: "",
                isSynced = true,
                pricingType = pType,
                coinsUsed = cUsed,
                moneyPaid = mPaid,
                paymentStatus = pStatus
            )
        }
    }
}
