package com.example.data.model

import androidx.annotation.Keep

@Keep
data class LegalPageEntity(
    val docId: String = "", // privacy_policy, terms_conditions, community_guidelines, about_lootra, help_support, contact_us
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val version: String = "1.0.0",
    val status: String = "published", // published, draft
    val published: Boolean = true,
    val updatedAt: Long = 0L,
    val updatedBy: String = "Super Admin"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "docId" to docId,
            "title" to title,
            "summary" to summary,
            "content" to content,
            "version" to version,
            "status" to status,
            "published" to published,
            "updatedAt" to updatedAt,
            "updatedBy" to updatedBy
        )
    }

    companion object {
        fun fromMap(docId: String, map: Map<String, Any>?): LegalPageEntity {
            if (map == null) return LegalPageEntity(docId = docId)
            return LegalPageEntity(
                docId = docId,
                title = map["title"] as? String ?: "",
                summary = map["summary"] as? String ?: "",
                content = map["content"] as? String ?: "",
                version = map["version"] as? String ?: "1.0.0",
                status = map["status"] as? String ?: "published",
                published = map["published"] as? Boolean ?: true,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                updatedBy = map["updatedBy"] as? String ?: "Super Admin"
            )
        }
    }
}
