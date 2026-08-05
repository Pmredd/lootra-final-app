package com.example.data.repository

import android.util.Log
import com.example.data.dao.WalletDao
import com.example.data.dao.TransactionDao
import com.example.data.model.WalletEntity
import com.example.data.model.TransactionEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WalletRepository(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getWalletReactive(uid: String): Flow<WalletEntity?> = walletDao.getWalletReactive(uid)

    fun getTransactionsReactive(uid: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsReactive(uid)

    suspend fun getOrCreateLocalWallet(uid: String): WalletEntity {
        val existing = walletDao.getWallet(uid)
        if (existing != null) {
            if (!existing.isSynced) {
                // Background retry for unsynced wallet when loading
                syncWalletToRemote(existing)
            }
            return existing
        }
        
        // Try fetching from Firestore first in case it was created on signup or another device
        val remoteWallet = fetchWalletFromRemote(uid)
        if (remoteWallet != null) {
            walletDao.insertWallet(remoteWallet)
            fetchTransactionsFromRemote(uid)
            return remoteWallet
        }
        
        val newWallet = WalletEntity(
            uid = uid,
            totalCoins = 0,
            todayCoins = 0,
            lifetimeCoins = 0,
            pendingCoins = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        walletDao.insertWallet(newWallet)
        syncWalletToRemote(newWallet) // Ensure it is created on Firestore immediately (e.g. on signup)
        return newWallet
    }

    suspend fun addCoins(uid: String, amount: Int, type: String, description: String) {
        val wallet = getOrCreateLocalWallet(uid)
        val updatedWallet = wallet.copy(
            totalCoins = wallet.totalCoins + amount,
            todayCoins = wallet.todayCoins + amount,
            lifetimeCoins = wallet.lifetimeCoins + amount,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        walletDao.insertWallet(updatedWallet)

        val txId = UUID.randomUUID().toString()
        val tx = TransactionEntity(
            transactionId = txId,
            uid = uid,
            type = type,
            amount = amount,
            description = description,
            createdAt = System.currentTimeMillis(),
            isSynced = false
        )
        transactionDao.insertTransaction(tx)

        // Attempt Firestore upload immediately
        syncWalletToRemote(updatedWallet)
        syncTransactionToRemote(tx)
    }

    suspend fun deductCoins(uid: String, amount: Int, type: String, description: String): Boolean {
        val wallet = getOrCreateLocalWallet(uid)
        if (wallet.totalCoins < amount) {
            return false
        }
        val updatedWallet = wallet.copy(
            totalCoins = wallet.totalCoins - amount,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        walletDao.insertWallet(updatedWallet)

        val txId = UUID.randomUUID().toString()
        val tx = TransactionEntity(
            transactionId = txId,
            uid = uid,
            type = type,
            amount = -amount,
            description = description,
            createdAt = System.currentTimeMillis(),
            isSynced = false
        )
        transactionDao.insertTransaction(tx)

        // Attempt Firestore upload immediately
        syncWalletToRemote(updatedWallet)
        syncTransactionToRemote(tx)
        return true
    }

    suspend fun fetchWalletFromRemote(uid: String): WalletEntity? {
        return try {
            kotlinx.coroutines.withTimeoutOrNull(10000) {
                val doc = firestore.collection("wallet")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    val data = doc.data
                    val wallet = WalletEntity.fromMap(data)
                    walletDao.insertWallet(wallet)
                    Log.d("WalletRepository", "Fetched and cached remote wallet for $uid")
                    wallet
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error fetching remote wallet for $uid: ${e.message}")
            null
        }
    }

    suspend fun fetchTransactionsFromRemote(uid: String) {
        try {
            kotlinx.coroutines.withTimeoutOrNull(10000) {
                val snapshot = firestore.collection("transactions")
                    .document(uid)
                    .collection("history")
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    val data = doc.data
                    if (data != null) {
                        val tx = TransactionEntity.fromMap(data)
                        transactionDao.insertTransaction(tx)
                    }
                }
                Log.d("WalletRepository", "Fetched and cached remote transaction history for $uid")
            }
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error fetching remote transactions for $uid: ${e.message}")
        }
    }

    suspend fun fetchWalletAndTransactionsFromRemote(uid: String) {
        fetchWalletFromRemote(uid)
        fetchTransactionsFromRemote(uid)
    }

    suspend fun syncWalletToRemote(wallet: WalletEntity): Boolean {
        return try {
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                firestore.collection("wallet")
                    .document(wallet.uid)
                    .set(wallet.toMap())
                    .await()
                true
            }
            if (success == true) {
                walletDao.markWalletSynced(wallet.uid)
                Log.d("WalletRepository", "Synced wallet to Firestore for ${wallet.uid}")
                true
            } else {
                Log.e("WalletRepository", "Firestore wallet sync timed out for ${wallet.uid}")
                false
            }
        } catch (e: Exception) {
            Log.e("WalletRepository", "Firestore wallet sync failed: ${e.message}")
            false
        }
    }

    suspend fun syncTransactionToRemote(tx: TransactionEntity): Boolean {
        return try {
            val success = kotlinx.coroutines.withTimeoutOrNull(10000) {
                firestore.collection("transactions")
                    .document(tx.uid)
                    .collection("history")
                    .document(tx.transactionId)
                    .set(tx.toMap())
                    .await()
                true
            }
            if (success == true) {
                transactionDao.markTransactionSynced(tx.transactionId)
                Log.d("WalletRepository", "Synced transaction ${tx.transactionId} to Firestore under transactions/${tx.uid}/history")
                true
            } else {
                Log.e("WalletRepository", "Firestore tx sync timed out for ${tx.transactionId}")
                false
            }
        } catch (e: Exception) {
            Log.e("WalletRepository", "Firestore tx sync failed: ${e.message}")
            false
        }
    }

    suspend fun syncUnsyncedData(uid: String) {
        try {
            val unsyncedWallets = walletDao.getUnsyncedWallets()
            for (w in unsyncedWallets) {
                if (w.uid == uid) {
                    syncWalletToRemote(w)
                }
            }
            val unsyncedTxs = transactionDao.getUnsyncedTransactions()
            for (tx in unsyncedTxs) {
                if (tx.uid == uid) {
                    syncTransactionToRemote(tx)
                }
            }
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error syncing unsynced data: ${e.message}")
        }
    }

    suspend fun deleteLocalWalletAndTransactionsForUser(uid: String) {
        try {
            walletDao.deleteWalletForUser(uid)
            transactionDao.deleteTransactionsForUser(uid)
            Log.d("WalletRepository", "Successfully cleared local wallet and transactions for $uid")
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error clearing wallet cache: ${e.message}")
        }
    }
}
