package com.company.stockpro

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StockProApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized()
    }

    private fun ensureFirebaseInitialized() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this)
                Log.i("StockProApp", "FirebaseApp successfully initialized.")
            } catch (e: Exception) {
                Log.w("StockProApp", "Standard FirebaseApp.initializeApp failed: ${e.message}")
            }
        }
    }
}
