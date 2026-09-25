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
                        .setApplicationId(packageName)
                        .setProjectId("stockpro-production")
                        .setApiKey("AIzaSyB-dummyKeyForOfflineLocalExecution")
                        .setDatabaseUrl("https://stockpro-production.firebaseio.com")
                        .setStorageBucket("stockpro-production.appspot.com")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.i("StockProApp", "FirebaseApp initialized with fallback options for offline mode.")
                } catch (e: Exception) {
                    Log.e("StockProApp", "Failed to initialize fallback FirebaseApp", e)
                }
            }
        }
    }
}
