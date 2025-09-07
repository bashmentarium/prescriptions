package com.example.whitelabel.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.whitelabel.data.database.entities.MedicationEventEntity

class MedicationAlarmScheduler(private val context: Context) {

    companion object {
        private const val ACTION_MEDICATION_REMINDER = "com.example.whitelabel.MEDICATION_REMINDER"
        private const val EXTRA_EVENT_ID = "event_id"
        private const val EXTRA_PRESCRIPTION_TITLE = "prescription_title"
        private const val EXTRA_DESCRIPTION = "description"
        private const val EXTRA_PRESCRIPTION_ID = "prescription_id"
        
        fun scheduleNotification(
            context: Context,
            event: MedicationEventEntity,
            prescriptionTitle: String
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                action = ACTION_MEDICATION_REMINDER
                putExtra(EXTRA_EVENT_ID, event.id)
                putExtra(EXTRA_PRESCRIPTION_TITLE, prescriptionTitle)
                putExtra(EXTRA_DESCRIPTION, event.description)
                putExtra(EXTRA_PRESCRIPTION_ID, event.prescriptionId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val triggerTime = event.startTimeMillis
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Use setExactAndAllowWhileIdle for Android 6+ to work in Doze mode
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                
                Log.d("MedicationAlarmScheduler", "Scheduled alarm for event ${event.id} at ${java.util.Date(triggerTime)}")
            } catch (e: Exception) {
                Log.e("MedicationAlarmScheduler", "Failed to schedule alarm: ${e.message}")
            }
        }
        
        fun cancelNotification(context: Context, eventId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, MedicationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            Log.d("MedicationAlarmScheduler", "Cancelled alarm for event $eventId")
        }
    }
    
    fun scheduleMedicationReminder(event: MedicationEventEntity, prescriptionTitle: String) {
        scheduleNotification(context, event, prescriptionTitle)
    }
    
    fun cancelMedicationReminder(eventId: String) {
        cancelNotification(context, eventId)
    }
}
