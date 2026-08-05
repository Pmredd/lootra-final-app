package com.example.data.manager

import android.content.Context

class DailySessionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("daily_session_prefs", Context.MODE_PRIVATE)

    fun getStoredDate(uid: String): String {
        return prefs.getString("stored_date_$uid", "") ?: ""
    }

    fun saveStoredDate(uid: String, dateStr: String) {
        prefs.edit().putString("stored_date_$uid", dateStr).apply()
    }
}
