package com.example

import android.app.Application

class TestWellbeingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Custom setup for tests can go here. Bypasses WellbeingApplication's production initializers.
    }
}
