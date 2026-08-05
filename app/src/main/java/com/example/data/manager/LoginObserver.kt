package com.example.data.manager

import android.content.Context
import android.util.Log
import com.example.WellbeingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginObserver(
    private val context: Context,
    private val resetUseCase: DailyResetUseCase
) {
    private val observerScope = CoroutineScope(Dispatchers.Default)

    fun startObserving() {
        val app = context.applicationContext as? WellbeingApplication ?: return
        val authRepo = app.container.authRepository
        
        observerScope.launch {
            authRepo.currentUserState.collectLatest { user ->
                if (user != null) {
                    val uid = user.uid
                    Log.i("LoginObserver", "Auth state login detected for $uid. Triggering reset validation...")
                    try {
                        resetUseCase.checkAndExecuteReset(uid)
                    } catch (e: Exception) {
                        Log.e("LoginObserver", "Error executing validation reset during user login matching: ${e.message}")
                    }
                } else {
                    Log.d("LoginObserver", "Auth state logout: No active user.")
                }
            }
        }
    }
}
