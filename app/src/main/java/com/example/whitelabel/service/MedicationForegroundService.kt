package com.example.whitelabel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whitelabel.MainActivity
import com.example.whitelabel.R
import kotlinx.coroutines.*

class MedicationForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "medication_foreground_service"
        private const val CHANNEL_NAME = "Medication Service"
        private const val CHANNEL_DESCRIPTION = "Background service for medication reminders"
        
        private var serviceJob: Job? = null
        private var isServiceRunning = false
        
        fun startService(context: Context) {
            if (!isServiceRunning) {
                val intent = Intent(context, MedicationForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d("MedicationForegroundService", "Starting foreground service")
            }
        }
        
        fun stopService(context: Context) {
            if (isServiceRunning) {
                val intent = Intent(context, MedicationForegroundService::class.java)
                context.stopService(intent)
                Log.d("MedicationForegroundService", "Stopping foreground service")
            }
        }
        
        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MedicationForegroundService", "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MedicationForegroundService", "Service started")
        
        // Create notification for foreground service
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        isServiceRunning = true
        
        // Start background monitoring
        startBackgroundMonitoring()
        
        return START_STICKY // Restart if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MedicationForegroundService", "Service destroyed")
        isServiceRunning = false
        serviceJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("MedicationForegroundService", "Notification channel created")
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Medication Reminders Active")
            .setContentText("Monitoring your medication schedule")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startBackgroundMonitoring() {
        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive && isServiceRunning) {
                try {
                    Log.d("MedicationForegroundService", "Background monitoring cycle")
                    
                    // Check for upcoming medication events
                    checkUpcomingMedications()
                    
                    // Wait 5 minutes before next check
                    delay(5 * 60 * 1000)
                } catch (e: Exception) {
                    Log.e("MedicationForegroundService", "Error in background monitoring: ${e.message}")
                    delay(60 * 1000) // Wait 1 minute on error
                }
            }
        }
    }

    private suspend fun checkUpcomingMedications() {
        try {
            val database = com.example.whitelabel.data.database.AppDatabase.getDatabase(this)
            val repository = com.example.whitelabel.data.repository.PrescriptionRepository(database)
            
            val currentTime = System.currentTimeMillis()
            val upcomingTime = currentTime + (30 * 60 * 1000) // Next 30 minutes
            
            // Get upcoming medication events
            val upcomingEvents = repository.getEventsInTimeRangeSuspend(currentTime, upcomingTime)
                .filter { !it.isCompleted && !it.reminderSent }
            
            Log.d("MedicationForegroundService", "Found ${upcomingEvents.size} upcoming events")
            
            for (event in upcomingEvents) {
                val prescription = repository.getPrescriptionById(event.prescriptionId)
                val prescriptionTitle = prescription?.title ?: "Medication Reminder"
                
                // Schedule notification using AlarmManager for reliability
                MedicationAlarmScheduler.scheduleNotification(
                    this,
                    event,
                    prescriptionTitle
                )
                
                // Mark reminder as sent
                repository.markReminderSent(event.id)
                
                Log.d("MedicationForegroundService", "Scheduled notification for event: ${event.id}")
            }
        } catch (e: Exception) {
            Log.e("MedicationForegroundService", "Error checking upcoming medications: ${e.message}")
        }
    }
}
