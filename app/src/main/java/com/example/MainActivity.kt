package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.navigation.WellbeingApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WellbeingViewModel
import com.example.ui.viewmodel.WellbeingViewModelFactory

class MainActivity : ComponentActivity() {

    private val forceLogoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_FORCE_LOGOUT") {
                Log.e("MainActivity", "Force logout broadcast received. Invalidate session.")
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error signing out firebase auth: ${e.message}")
                }
                
                val restartIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(restartIntent)
                finish()
            }
        }
    }

    private val midnightRolloverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_MIDNIGHT_ROLLOVER") {
                Log.i("MainActivity", "Midnight rollover broadcast received. Refreshing views.")
                try {
                    val appContainer = (application as WellbeingApplication).container
                    val factory = WellbeingViewModelFactory(application, appContainer.repository)
                    val viewModel = ViewModelProvider(this@MainActivity, factory)[WellbeingViewModel::class.java]
                    viewModel.checkPermission()
                    viewModel.refreshState(force = true)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error refreshing VM on rollover: ${e.message}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register Broadcast Receivers
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(forceLogoutReceiver, IntentFilter("com.example.ACTION_FORCE_LOGOUT"), Context.RECEIVER_EXPORTED)
            registerReceiver(midnightRolloverReceiver, IntentFilter("com.example.ACTION_MIDNIGHT_ROLLOVER"), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(forceLogoutReceiver, IntentFilter("com.example.ACTION_FORCE_LOGOUT"))
            registerReceiver(midnightRolloverReceiver, IntentFilter("com.example.ACTION_MIDNIGHT_ROLLOVER"))
        }

        // Retrieve the centralized app context database container
        val appContainer = (application as WellbeingApplication).container
        
        // Instantiate the Wellbeing ViewModel cleanly
        val factory = WellbeingViewModelFactory(application, appContainer.repository)
        val viewModel = ViewModelProvider(this, factory)[WellbeingViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WellbeingApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Force state checking dynamic poll in case they returned from Settings
        try {
            val appContainer = (application as WellbeingApplication).container
            val factory = WellbeingViewModelFactory(application, appContainer.repository)
            val viewModel = ViewModelProvider(this, factory)[WellbeingViewModel::class.java]
            viewModel.checkPermission()

            // Automatically start WellbeingForegroundService if user is logged in
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser != null && appContainer.repository.isPermissionGranted()) {
                val serviceIntent = Intent(this, com.example.service.WellbeingForegroundService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(forceLogoutReceiver)
            unregisterReceiver(midnightRolloverReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}
