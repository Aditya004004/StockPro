package com.company.stockpro

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
                // Initialize using google-services.json / XML resources if available
                FirebaseApp.initializeApp(this)
            } catch (e: Exception) {
                Log.w("StockProApp", "Standard FirebaseApp.initializeApp failed: ${e.message}")
            }

            // If still not initialized (e.g. google-services.json was not supplied), use fallback options
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:1021216414834:android:1be0a37cd5bf8fadeff6eb")
                        .setProjectId("door-a9b6f")
                        .setApiKey("AIzaSyCuUBLyShmPm6rN0TNz8NbbeSSeTxLW1NQ")
                        .setStorageBucket("door-a9b6f.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.i("StockProApp", "FirebaseApp initialized with fallback options for door-a9b6f.")
                } catch (e: Exception) {
                    Log.e("StockProApp", "Failed to initialize fallback FirebaseApp", e)
                }
            }

            // Subscribe to FCM topic for plant notifications
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("plant_plant_01")
            } catch (e: Exception) {
                Log.w("StockProApp", "Failed to subscribe to FCM topic plant_plant_01: ${e.message}")
            }
        }
    }
}
