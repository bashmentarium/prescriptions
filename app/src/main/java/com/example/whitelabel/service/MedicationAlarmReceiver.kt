package com.example.whitelabel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.whitelabel.data.database.entities.MedicationEventEntity

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MedicationAlarmReceiver", "Alarm received: ${intent.action}")
        
        when (intent.action) {
            "com.example.whitelabel.MEDICATION_REMINDER" -> {
                handleMedicationReminder(context, intent)
            }
        }
    }
    
    private fun handleMedicationReminder(context: Context, intent: Intent) {
        try {
            val eventId = intent.getStringExtra("event_id") ?: return
            val prescriptionTitle = intent.getStringExtra("prescription_title") ?: "Medication Reminder"
            val description = intent.getStringExtra("description") ?: "Time to take your medication"
            val prescriptionId = intent.getStringExtra("prescription_id") ?: ""
            
            Log.d("MedicationAlarmReceiver", "Processing medication reminder for event: $eventId")
            
            // Create a temporary event for notification display
            val event = MedicationEventEntity(
                id = eventId,
                prescriptionId = prescriptionId,
                title = prescriptionTitle,
                description = description,
                startTimeMillis = System.currentTimeMillis(),
                endTimeMillis = System.currentTimeMillis() + 3600000, // 1 hour window
                isCompleted = false
            )
            
            // Show the notification
            MedicationNotificationService.showMedicationReminderNotification(
                context,
                event,
                prescriptionTitle
            )
            
            Log.d("MedicationAlarmReceiver", "Notification shown for event: $eventId")
            
        } catch (e: Exception) {
            Log.e("MedicationAlarmReceiver", "Error handling medication reminder: ${e.message}")
        }
    }
}
