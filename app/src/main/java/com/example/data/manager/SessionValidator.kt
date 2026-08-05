package com.example.data.manager

import java.text.SimpleDateFormat
import java.util.*

class SessionValidator(
    private val sessionManager: DailySessionManager
) {
    fun isSessionValid(uid: String): Boolean {
        if (uid.isEmpty()) return false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val stored = sessionManager.getStoredDate(uid)
        return stored == todayStr
    }
}
