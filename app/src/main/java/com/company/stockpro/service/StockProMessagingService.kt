package com.company.stockpro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.company.stockpro.MainActivity
import com.company.stockpro.R
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.NotificationEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Service to receive Firebase Cloud Messaging (FCM) push notifications.
 * Persists notifications to Room for the in-app notification center and
 * presents system status-bar notifications for operational alerts.
 */
class StockProMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "StockProMessaging"
        const val CHANNEL_ID = "stockpro_operational_alerts"
        const val CHANNEL_NAME = "StockPro Operational Alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM registration token received: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "StockPro Alert"
        val body = notification?.body ?: data["body"] ?: ""
        val type = data["type"] ?: "LOW_STOCK"
        val plantId = data["plantId"] ?: "plant_01"
        val entityRef = data["entityRef"]
        val now = System.currentTimeMillis()
        val notifId = UUID.randomUUID().toString()

        // 1. Persist to local Room database for in-app notification center
        serviceScope.launch {
            try {
                val db = StockProDatabase.getInstance(applicationContext)
                db.notificationDao().insert(
                    NotificationEntity(
                        id = notifId,
                        plantId = plantId,
                        type = type,
                        title = title,
                        body = body,
                        entityRef = entityRef,
                        isRead = false,
                        createdAt = now,
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist FCM notification to Room: ${e.message}", e)
            }
        }

        // 2. Post system notification
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Inventory thresholds, sync alerts, and tool calibration notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notificationBuilder.build())
    }
}
