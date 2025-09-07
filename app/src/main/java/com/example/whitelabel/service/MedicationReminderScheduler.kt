package com.example.whitelabel.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.repository.PrescriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MedicationReminderScheduler(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    private val database = AppDatabase.getDatabase(context)
    private val repository = PrescriptionRepository(database)
    
    fun scheduleMedicationReminders() {
        Log.d("MedicationReminderScheduler", "Starting to schedule medication reminders...")
        
        // Cancel existing work
        workManager.cancelUniqueWork("medication_reminders")
        Log.d("MedicationReminderScheduler", "Cancelled existing medication reminder work")
        
        // Schedule new reminders with minimal constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false) // Allow even when battery is low
            .setRequiresCharging(false) // Don't require charging
            .setRequiresDeviceIdle(false) // Don't require device to be idle
            .build()
        
        Log.d("MedicationReminderScheduler", "Constraints: Network=${constraints.requiredNetworkType}, Battery=${constraints.requiresBatteryNotLow()}, Charging=${constraints.requiresCharging()}, Idle=${constraints.requiresDeviceIdle()}")
        
        val reminderWork = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // Start checking in 1 minute
            .build()
        
        workManager.enqueueUniqueWork(
            "medication_reminders",
            ExistingWorkPolicy.REPLACE,
            reminderWork
        )
        
        Log.d("MedicationReminderScheduler", "Scheduled medication reminder checks with work ID: ${reminderWork.id}")
        
        // Check if work was actually enqueued
        workManager.getWorkInfosForUniqueWork("medication_reminders").get().forEach { workInfo ->
            Log.d("MedicationReminderScheduler", "Work status: ${workInfo.state}, ID: ${workInfo.id}")
        }
    }
    
    fun scheduleSpecificReminder(event: MedicationEventEntity, prescriptionTitle: String) {
        val delay = event.startTimeMillis - System.currentTimeMillis()
        
        if (delay > 0) {
            // Use AlarmManager for reliable notifications when app is closed
            val alarmScheduler = MedicationAlarmScheduler(context)
            alarmScheduler.scheduleMedicationReminder(event, prescriptionTitle)
            
            Log.d("MedicationReminderScheduler", "Scheduled alarm-based reminder for event ${event.id} in ${delay}ms")
        }
    }
    
    fun cancelReminder(eventId: String) {
        // Cancel both WorkManager and AlarmManager reminders
        workManager.cancelUniqueWork("medication_reminder_$eventId")
        
        val alarmScheduler = MedicationAlarmScheduler(context)
        alarmScheduler.cancelMedicationReminder(eventId)
        
        Log.d("MedicationReminderScheduler", "Cancelled reminder for event $eventId")
    }
}

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("MedicationReminderWorker", "Starting medication reminder check...")
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = PrescriptionRepository(database)
            
            val currentTime = System.currentTimeMillis()
            val upcomingTime = currentTime + (30 * 60 * 1000) // Next 30 minutes
            
            // Get upcoming medication events
            val upcomingEvents = repository.getEventsInTimeRangeSuspend(currentTime, upcomingTime)
                .filter { !it.isCompleted && !it.reminderSent }
            
            Log.d("MedicationReminderWorker", "Found ${upcomingEvents.size} upcoming events")
            
            for (event in upcomingEvents) {
                val prescription = repository.getPrescriptionById(event.prescriptionId)
                val prescriptionTitle = prescription?.title ?: "Medication Reminder"
                
                Log.d("MedicationReminderWorker", "Scheduling reminder for event: ${event.id}")
                
                // Schedule specific reminder for this event
                val scheduler = MedicationReminderScheduler(applicationContext)
                scheduler.scheduleSpecificReminder(event, prescriptionTitle)
                
                // Mark reminder as sent
                repository.markReminderSent(event.id)
            }
            
            // Schedule next check in 15 minutes
            val nextCheckWork = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "medication_reminders",
                ExistingWorkPolicy.REPLACE,
                nextCheckWork
            )
            
            Result.success()
        } catch (e: Exception) {
            Log.e("MedicationReminderWorker", "Failed to check medication reminders: ${e.message}")
            Result.retry()
        }
    }
}

class SpecificMedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("SpecificMedicationReminderWorker", "Starting specific medication reminder...")
        try {
            val eventId = inputData.getString("event_id") ?: return@withContext Result.failure()
            val prescriptionId = inputData.getString("prescription_id") ?: ""
            val prescriptionTitle = inputData.getString("prescription_title") ?: "Medication Reminder"
            val description = inputData.getString("description") ?: "Time to take your medication"
            
            Log.d("SpecificMedicationReminderWorker", "Processing reminder for event: $eventId")
            
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = PrescriptionRepository(database)
            
            // Check if event is still not completed
            val event = repository.getEventById(eventId)
            if (event != null && !event.isCompleted) {
                // Show notification
                MedicationNotificationService.showMedicationReminderNotification(
                    applicationContext,
                    event,
                    prescriptionTitle
                )
                
                Log.d("SpecificMedicationReminderWorker", "Showed reminder for event $eventId")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("SpecificMedicationReminderWorker", "Failed to show medication reminder: ${e.message}")
            Result.failure()
        }
    }
}
