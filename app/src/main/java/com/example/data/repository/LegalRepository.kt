package com.example.data.repository

import android.util.Log
import com.example.data.model.LegalPageEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LegalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collectionRef = firestore.collection("legal_pages")

    fun getLegalPageFlow(docId: String): Flow<LegalPageEntity> = callbackFlow {
        val listener = collectionRef.document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LegalRepository", "Error fetching legal page $docId: ${error.message}")
                    trySend(LegalDefaults.getDefault(docId))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val entity = LegalPageEntity.fromMap(docId, snapshot.data)
                    trySend(entity)
                } else {
                    trySend(LegalDefaults.getDefault(docId))
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllLegalPagesFlow(): Flow<List<LegalPageEntity>> = callbackFlow {
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("LegalRepository", "Error fetching legal pages: ${error.message}")
                val defaultList = LegalDefaults.ALL_DOC_IDS.map { LegalDefaults.getDefault(it) }
                trySend(defaultList)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val map = snapshot.documents.associate { doc ->
                    doc.id to LegalPageEntity.fromMap(doc.id, doc.data)
                }
                val list = LegalDefaults.ALL_DOC_IDS.map { docId ->
                    map[docId] ?: LegalDefaults.getDefault(docId)
                }
                trySend(list)
            } else {
                val defaultList = LegalDefaults.ALL_DOC_IDS.map { LegalDefaults.getDefault(it) }
                trySend(defaultList)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getLegalPageOnce(docId: String): LegalPageEntity = withContext(Dispatchers.IO) {
        try {
            val doc = collectionRef.document(docId).get().await()
            if (doc.exists()) {
                LegalPageEntity.fromMap(docId, doc.data)
            } else {
                LegalDefaults.getDefault(docId)
            }
        } catch (e: Exception) {
            Log.e("LegalRepository", "Failed once fetch for $docId: ${e.message}")
            LegalDefaults.getDefault(docId)
        }
    }

    suspend fun saveLegalPage(entity: LegalPageEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docData = entity.toMap()
            collectionRef.document(entity.docId).set(docData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LegalRepository", "Failed saving legal page ${entity.docId}: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserLegalAcceptance(
        uid: String,
        privacyVersion: String,
        termsVersion: String,
        communityVersion: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val acceptanceData = mapOf(
                "legalAccepted" to true,
                "acceptedAt" to System.currentTimeMillis(),
                "privacyVersion" to privacyVersion,
                "termsVersion" to termsVersion,
                "communityVersion" to communityVersion,
                "updatedAt" to System.currentTimeMillis()
            )
            val success = kotlinx.coroutines.withTimeoutOrNull(8000) {
                firestore.collection("users").document(uid)
                    .set(acceptanceData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                true
            }
            if (success == true) {
                Result.success(Unit)
            } else {
                Log.w("LegalRepository", "Timeout writing legal acceptance to Firestore for $uid. Proceeding locally.")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("LegalRepository", "Failed updating user legal acceptance for $uid: ${e.message}")
            Result.failure(e)
        }
    }
}
