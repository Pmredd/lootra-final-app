package com.example.data.manager

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppControlManager {
    private const val TAG = "AppControlManager"
    
    private val _maintenanceEnabled = MutableStateFlow(false)
    val maintenanceEnabled: StateFlow<Boolean> = _maintenanceEnabled.asStateFlow()
    
    private val _maintenanceTitle = MutableStateFlow("System Under Maintenance")
    val maintenanceTitle: StateFlow<String> = _maintenanceTitle.asStateFlow()
    
    private val _maintenanceMessage = MutableStateFlow("Lootra is temporarily unavailable.")
    val maintenanceMessage: StateFlow<String> = _maintenanceMessage.asStateFlow()
    
    private val _estimatedEndTime = MutableStateFlow("")
    val estimatedEndTime: StateFlow<String> = _estimatedEndTime.asStateFlow()
    
    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()
    
    private val _updatedBy = MutableStateFlow("")
    val updatedBy: StateFlow<String> = _updatedBy.asStateFlow()
    
    private val _updatedAt = MutableStateFlow(0L)
    val updatedAt: StateFlow<Long> = _updatedAt.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun startListening() {
        if (listenerRegistration != null) return
        
        Log.d(TAG, "Starting real-time listener for settings/app_control")
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("settings").document("app_control")
        
        listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to app_control settings", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val enabled = snapshot.getBoolean("maintenanceEnabled") ?: false
                _maintenanceEnabled.value = enabled
                _maintenanceTitle.value = snapshot.getString("maintenanceTitle") ?: "System Under Maintenance"
                _maintenanceMessage.value = snapshot.getString("maintenanceMessage") ?: "Lootra is temporarily unavailable."
                _estimatedEndTime.value = snapshot.getString("estimatedEndTime") ?: ""
                _reason.value = snapshot.getString("reason") ?: ""
                _updatedBy.value = snapshot.getString("updatedBy") ?: ""
                _updatedAt.value = snapshot.getLong("updatedAt") ?: 0L
                Log.d(TAG, "AppControl settings updated: enabled=$enabled")
            } else {
                Log.d(TAG, "AppControl settings document does not exist, using default values.")
                _maintenanceEnabled.value = false
            }
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
