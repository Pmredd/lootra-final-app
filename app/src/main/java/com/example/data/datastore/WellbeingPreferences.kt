package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wellbeing_preferences")

class WellbeingPreferences(private val context: Context) {

    companion object {
        val DAILY_SCREEN_TIME_GOAL_MINUTES = intPreferencesKey("daily_screen_time_goal_minutes")
        val SCREEN_TIME_NOTIFICATIONS_ENABLED = booleanPreferencesKey("screen_time_notifications_enabled")
        val LAST_REFRESH_TIMESTAMP = longPreferencesKey("last_refresh_timestamp")
        val SELECTED_COLLEGE_ID = stringPreferencesKey("selected_college_id")
        val SELECTED_COLLEGE_NAME = stringPreferencesKey("selected_college_name")
    }

    val dailyGoalMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_SCREEN_TIME_GOAL_MINUTES] ?: 180 // 3 hours default goal
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SCREEN_TIME_NOTIFICATIONS_ENABLED] ?: true
    }

    val lastRefreshTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_REFRESH_TIMESTAMP] ?: 0L
    }

    val selectedCollegeIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_COLLEGE_ID] ?: ""
    }

    val selectedCollegeNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_COLLEGE_NAME] ?: ""
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_SCREEN_TIME_GOAL_MINUTES] = minutes
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCREEN_TIME_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setLastRefreshTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_REFRESH_TIMESTAMP] = timestamp
        }
    }

    suspend fun setSelectedCollege(collegeId: String, collegeName: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_COLLEGE_ID] = collegeId
            preferences[SELECTED_COLLEGE_NAME] = collegeName
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
