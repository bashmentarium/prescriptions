package com.example.whitelabel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.repository.PrescriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "CONFIRM_MEDICATION" -> {
                val eventId = intent.getStringExtra("event_id")
                if (eventId != null) {
                    confirmMedicationIntake(context, eventId)
                }
            }
        }
    }
    
    private fun confirmMedicationIntake(context: Context, eventId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = PrescriptionRepository(database)
                
                // Mark the event as completed
                repository.markEventCompleted(eventId)
                
                Log.d("MedicationNotification", "Medication intake confirmed for event: $eventId")
                
                // Cancel the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(eventId.hashCode())
                
            } catch (e: Exception) {
                Log.e("MedicationNotification", "Failed to confirm medication intake: ${e.message}")
            }
        }
    }
}
