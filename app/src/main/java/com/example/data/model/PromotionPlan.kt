package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentSnapshot

@Keep
data class PromotionPlan(
    val planId: String = "",
    val title: String = "",
    val planName: String = "",
    val description: String = "",
    val promotionType: String = "Banner",
    val targetAudience: String = "All Users",
    val targetViews: Int = 0,
    val rewardPerView: Int = 0,
    val budget: Int = 0,
    val campaignBudget: Int = 0,
    val price: Double = 0.0,
    val contactPerson: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val website: String = "",
    val buttonText: String = "Contact Now",
    val thumbnail: String = "",
    val badge: String = "", // e.g. "Popular", "Recommended", "Best Value"
    val displayOrder: Int = 0,
    val status: String = "enabled", // "enabled" or "disabled"
    val startDate: String = "",
    val endDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Admin"
) {
    val displayTitle: String
        get() = title.ifBlank { planName.ifBlank { "Untitled Plan" } }

    val displayBudget: Int
        get() = if (budget > 0) budget else if (campaignBudget > 0) campaignBudget else (targetViews * rewardPerView)

    fun toMap(): Map<String, Any?> {
        val nameToSave = displayTitle
        val budgetToSave = displayBudget
        return mapOf(
            "planId" to planId,
            "title" to nameToSave,
            "planName" to nameToSave,
            "description" to description,
            "promotionType" to promotionType,
            "targetAudience" to targetAudience,
            "targetViews" to targetViews,
            "rewardPerView" to rewardPerView,
            "budget" to budgetToSave,
            "campaignBudget" to budgetToSave,
            "price" to price,
            "contactPerson" to contactPerson,
            "phone" to phone,
            "whatsapp" to whatsapp,
            "website" to website,
            "buttonText" to buttonText,
            "thumbnail" to thumbnail,
            "badge" to badge,
            "displayOrder" to displayOrder,
            "status" to status,
            "startDate" to startDate,
            "endDate" to endDate,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "createdBy" to createdBy
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): PromotionPlan {
            val id = doc.id
            val titleStr = doc.getString("title") ?: doc.getString("planName") ?: ""
            val desc = doc.getString("description") ?: ""
            val pType = doc.getString("promotionType") ?: "Banner"
            val targetAud = doc.getString("targetAudience") ?: "All Users"
            
            val tViews = getIntSafe(doc, "targetViews")
            val rPerView = getIntSafe(doc, "rewardPerView")
            val bgtRaw = getIntSafe(doc, "budget")
            val campBgtRaw = getIntSafe(doc, "campaignBudget")
            val finalBudget = if (bgtRaw > 0) bgtRaw else if (campBgtRaw > 0) campBgtRaw else (tViews * rPerView)

            val prc = getDoubleSafe(doc, "price")
            val cPerson = doc.getString("contactPerson") ?: ""
            val ph = doc.getString("phone") ?: ""
            val wa = doc.getString("whatsapp") ?: ""
            val web = doc.getString("website") ?: ""
            val btnText = doc.getString("buttonText") ?: "Contact Now"
            val thumb = doc.getString("thumbnail") ?: ""
            val bdg = doc.getString("badge") ?: ""
            val dOrder = getIntSafe(doc, "displayOrder")
            val st = doc.getString("status") ?: "enabled"
            val sDate = doc.getString("startDate") ?: ""
            val eDate = doc.getString("endDate") ?: ""
            val cAt = getLongSafe(doc, "createdAt", System.currentTimeMillis())
            val uAt = getLongSafe(doc, "updatedAt", System.currentTimeMillis())
            val cBy = doc.getString("createdBy") ?: "Admin"

            return PromotionPlan(
                planId = id,
                title = titleStr,
                planName = titleStr,
                description = desc,
                promotionType = pType,
                targetAudience = targetAud,
                targetViews = tViews,
                rewardPerView = rPerView,
                budget = finalBudget,
                campaignBudget = finalBudget,
                price = prc,
                contactPerson = cPerson,
                phone = ph,
                whatsapp = wa,
                website = web,
                buttonText = btnText,
                thumbnail = thumb,
                badge = bdg,
                displayOrder = dOrder,
                status = st,
                startDate = sDate,
                endDate = eDate,
                createdAt = cAt,
                updatedAt = uAt,
                createdBy = cBy
            )
        }

        private fun getIntSafe(doc: DocumentSnapshot, field: String): Int {
            return try {
                doc.getLong(field)?.toInt()
                    ?: doc.getDouble(field)?.toInt()
                    ?: doc.getString(field)?.toIntOrNull()
                    ?: 0
            } catch (_: Exception) {
                0
            }
        }

        private fun getDoubleSafe(doc: DocumentSnapshot, field: String): Double {
            return try {
                doc.getDouble(field)
                    ?: doc.getLong(field)?.toDouble()
                    ?: doc.getString(field)?.toDoubleOrNull()
                    ?: 0.0
            } catch (_: Exception) {
                0.0
            }
        }

        private fun getLongSafe(doc: DocumentSnapshot, field: String, default: Long): Long {
            return try {
                doc.getLong(field)
                    ?: doc.getDouble(field)?.toLong()
                    ?: doc.getString(field)?.toLongOrNull()
                    ?: default
            } catch (_: Exception) {
                default
            }
        }
    }
}
