package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.WellbeingApplication
import com.example.data.model.DailyReportEntity
import com.example.data.model.MonthlyReportEntity
import com.example.data.model.UsageStatisticEntity
import com.example.data.model.WeeklyReportEntity
import com.example.data.repository.WellbeingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class WellbeingViewModel(
    private val application: Application,
    private val repository: WellbeingRepository
) : AndroidViewModel(application) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // UI Local Selection States
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(repository.isPermissionGranted())
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isUsageGranted = MutableStateFlow(false)
    val isUsageGranted: StateFlow<Boolean> = _isUsageGranted.asStateFlow()

    private val _isNotificationsGranted = MutableStateFlow(false)
    val isNotificationsGranted: StateFlow<Boolean> = _isNotificationsGranted.asStateFlow()

    private val _isBatteryIgnoring = MutableStateFlow(false)
    val isBatteryIgnoring: StateFlow<Boolean> = _isBatteryIgnoring.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // Preferences
    val dailyGoalMinutes: StateFlow<Int> = repository.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 180)

    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Room Database Streams
    val dailyUsageStats: StateFlow<List<UsageStatisticEntity>> = _selectedDate
        .flatMapLatest { dateStr -> repository.getUsageStatsForDate(dateStr) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyReports: StateFlow<List<DailyReportEntity>> = repository.getAllDailyReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyReports: StateFlow<List<WeeklyReportEntity>> = repository.getAllWeeklyReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyReports: StateFlow<List<MonthlyReportEntity>> = repository.getAllMonthlyReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically check/update database records on load
        refreshState(force = false)
    }

    /**
     * Checks permission status dynamically.
     */
    fun checkPermission() {
        val context = application.applicationContext
        
        // 1. Check Usage stats
        val usage = repository.isUsageAccessGranted()
        _isUsageGranted.value = usage
        
        // 2. Check Notifications
        val notifications = if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _isNotificationsGranted.value = notifications
        
        // 3. Check Battery Optimization Exclusions
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        val battery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && pm != null) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
        _isBatteryIgnoring.value = battery

        _isPermissionGranted.value = usage && notifications
        
        if (usage && notifications) {
            try {
                val serviceIntent = android.content.Intent(context, com.example.service.WellbeingForegroundService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("WellbeingVM", "FGS auto-start failed: ${e.message}")
            }
            refreshState(force = true)
        }
    }

    /**
     * Refreshes usage stats from System UsageStatsManager.
     */
    fun refreshState(force: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _isPermissionGranted.value = repository.isPermissionGranted()
                // Fetch stats for the selected date
                val date = try {
                    dateFormat.parse(_selectedDate.value) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
                repository.refreshUsageData(date)
                _uiMessage.emit("Wellbeing statistics synchronized!")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiMessage.emit("Synchronization error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates local date index for historical drill down.
     */
    fun changeSelectedDate(dateString: String) {
        _selectedDate.value = dateString
        refreshState(force = true)
    }

    /**
     * Modifies current user customizable screen timer limit.
     */
    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            repository.saveDailyGoalMinutes(minutes)
            _uiMessage.emit("Screen goal set to $minutes minutes")
        }
    }

    /**
     * Updates notification configurations.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveNotificationsEnabled(enabled)
            _uiMessage.emit(if (enabled) "Notifications enabled" else "Notifications muted")
        }
    }
}

/**
 * Custom Factory Provider to resolve dynamic Repository constructors.
 */
class WellbeingViewModelFactory(
    private val application: Application,
    private val repository: WellbeingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WellbeingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WellbeingViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
