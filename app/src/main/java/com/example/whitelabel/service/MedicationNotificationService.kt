package com.example.whitelabel.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.whitelabel.MainActivity
import com.example.whitelabel.R
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MedicationNotificationService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "medication_reminders"
        private const val CHANNEL_NAME = "Medication Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for medication reminders"
        private const val NOTIFICATION_ID_BASE = 1000
        
        fun createNotificationChannel(context: Context) {
            Log.d("MedicationNotification", "Creating notification channel...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MAX
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d("MedicationNotification", "Notification channel created successfully")
                
                // Verify channel was created
                val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (createdChannel != null) {
                    Log.d("MedicationNotification", "Channel verification: ${createdChannel.id}, importance: ${createdChannel.importance}")
                } else {
                    Log.e("MedicationNotification", "Channel verification failed - channel not found!")
                }
            } else {
                Log.d("MedicationNotification", "Android version < O, no channel needed")
            }
        }
        
        fun diagnoseNotificationSystem(context: Context): String {
            val notificationManager = NotificationManagerCompat.from(context)
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val diagnostics = StringBuilder()
            diagnostics.appendLine("=== NOTIFICATION SYSTEM DIAGNOSTICS ===")
            diagnostics.appendLine("Android Version: ${Build.VERSION.SDK_INT}")
            diagnostics.appendLine("Notifications Enabled: ${notificationManager.areNotificationsEnabled()}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = systemNotificationManager.getNotificationChannel(CHANNEL_ID)
                if (channel != null) {
                    diagnostics.appendLine("Channel ID: ${channel.id}")
                    diagnostics.appendLine("Channel Importance: ${channel.importance}")
                    diagnostics.appendLine("Channel Enabled: ${channel.importance != NotificationManager.IMPORTANCE_NONE}")
                } else {
                    diagnostics.appendLine("ERROR: Notification channel not found!")
                }
            }
            
            diagnostics.appendLine("=====================================")
            Log.d("MedicationNotification", diagnostics.toString())
            return diagnostics.toString()
        }
        
        fun showMedicationReminderNotification(
            context: Context,
            event: MedicationEventEntity,
            prescriptionTitle: String
        ) {
            Log.d("MedicationNotification", "Attempting to show notification for event: ${event.id}")
            
            // Run diagnostics first
            diagnoseNotificationSystem(context)
            
            createNotificationChannel(context)
            
            // Create intent to open medication confirmation screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "medication-confirmation")
                putExtra("event_id", event.id)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                event.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💊 Time for your medication!")
                .setContentText("$prescriptionTitle - ${event.description}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$prescriptionTitle\n\n${event.description}\n\nTap to open the app."))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            val notificationManager = NotificationManagerCompat.from(context)
            try {
                // Check if notifications are enabled
                val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
                Log.d("MedicationNotification", "Notifications enabled: $areNotificationsEnabled")
                
                if (areNotificationsEnabled) {
                    notificationManager.notify(event.id.hashCode(), notification)
                    Log.d("MedicationNotification", "Notification posted successfully with ID: ${event.id.hashCode()}")
                } else {
                    Log.e("MedicationNotification", "Notifications are disabled for this app")
                }
            } catch (e: SecurityException) {
                Log.e("MedicationNotification", "Failed to show notification: ${e.message}")
            } catch (e: Exception) {
                Log.e("MedicationNotification", "Unexpected error showing notification: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("MedicationNotification", "From: ${remoteMessage.from}")
        Log.d("MedicationNotification", "Message data payload: ${remoteMessage.data}")
        
        // Handle data payload
        val eventId = remoteMessage.data["event_id"]
        val prescriptionTitle = remoteMessage.data["prescription_title"] ?: "Medication Reminder"
        val description = remoteMessage.data["description"] ?: "Time to take your medication"
        
        if (eventId != null) {
            // Create a temporary event for notification display
            val event = MedicationEventEntity(
                id = eventId,
                prescriptionId = remoteMessage.data["prescription_id"] ?: "",
                title = prescriptionTitle,
                description = description,
                startTimeMillis = System.currentTimeMillis(),
                endTimeMillis = System.currentTimeMillis() + 3600000, // 1 hour window
                isCompleted = false
            )
            
            showMedicationReminderNotification(this, event, prescriptionTitle)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MedicationNotification", "Refreshed token: $token")
        
        // TODO: Send token to your server if needed
        // sendRegistrationToServer(token)
    }
}
