package com.example.whitelabel.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.whitelabel.R

class SimpleNotificationTest(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "simple_test_channel"
        private const val CHANNEL_NAME = "Simple Test Notifications"
        private const val NOTIFICATION_ID = 9999
    }
    
    fun createTestChannel() {
        Log.d("SimpleNotificationTest", "Creating test notification channel...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Simple test notifications"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("SimpleNotificationTest", "Test notification channel created successfully")
        } else {
            Log.d("SimpleNotificationTest", "Android version < O, no channel needed")
        }
    }
    
    fun showSimpleTestNotification() {
        Log.d("SimpleNotificationTest", "Attempting to show simple test notification...")
        
        createTestChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔔 Simple Test Notification")
            .setContentText("This is a simple test notification to verify the system works.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("This is a simple test notification to verify that the notification system is working correctly. If you can see this, the basic notification functionality is working."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            // Check if notifications are enabled
            val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
            Log.d("SimpleNotificationTest", "Notifications enabled: $areNotificationsEnabled")
            
            if (areNotificationsEnabled) {
                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d("SimpleNotificationTest", "Simple test notification posted successfully")
            } else {
                Log.e("SimpleNotificationTest", "Notifications are disabled for this app")
            }
        } catch (e: SecurityException) {
            Log.e("SimpleNotificationTest", "Failed to show simple test notification: ${e.message}")
        } catch (e: Exception) {
            Log.e("SimpleNotificationTest", "Unexpected error showing simple test notification: ${e.message}")
        }
    }
}
