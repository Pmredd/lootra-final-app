package com.example.data.repository

import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUserState: StateFlow<FirebaseUser?>
    fun getCurrentUser(): FirebaseUser?
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    suspend fun signUp(email: String, password: String): Result<FirebaseUser>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    fun signOut()
    fun isUserLoggedIn(): Boolean
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth? = null
) : AuthRepository {

    private val auth: FirebaseAuth
        get() = firebaseAuth ?: FirebaseAuth.getInstance()

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
    override val currentUserState: StateFlow<FirebaseUser?> = _currentUserState

    init {
        try {
            _currentUserState.value = auth.currentUser
            auth.addAuthStateListener { listenerAuth ->
                _currentUserState.value = listenerAuth.currentUser
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCurrentUser(): FirebaseUser? {
        return try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                _currentUserState.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Authentication failed: User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                _currentUserState.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: User is null"))
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            // Elegant specific mapping for standard duplicate email constraints
            Result.failure(Exception("This email is already registered. Please sign in."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.delete().await()
                _currentUserState.value = null
                Result.success(Unit)
            } else {
                Result.failure(Exception("No active user session found to delete."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUserState.value = null
    }

    override fun isUserLoggedIn(): Boolean {
        return try {
            auth.currentUser != null
        } catch (e: Exception) {
            false
        }
    }
}
