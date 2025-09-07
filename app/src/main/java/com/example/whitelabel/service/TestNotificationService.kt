package com.example.whitelabel.service

import android.content.Context
import android.util.Log
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.repository.PrescriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class TestNotificationService(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val repository = PrescriptionRepository(database)
    private val reminderScheduler = MedicationReminderScheduler(context)
    
    init {
        repository.setReminderScheduler(reminderScheduler)
    }
    
    fun createTestMedicationEvent() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a test medication event for 1 minute from now
                val testEvent = MedicationEventEntity(
                    prescriptionId = "test-prescription-123",
                    title = "Test Medication",
                    description = "This is a test medication reminder. Take 1 tablet of TestMed with food.",
                    startTimeMillis = System.currentTimeMillis() + 60000, // 1 minute from now
                    endTimeMillis = System.currentTimeMillis() + 4200000, // 1 hour window
                    isCompleted = false
                )
                
                // Insert the test event
                repository.eventDao.insertEvent(testEvent)
                
                // Schedule the reminder
                reminderScheduler.scheduleSpecificReminder(testEvent, "Test Prescription")
                
                Log.d("TestNotificationService", "Created test medication event: ${testEvent.id}")
                
            } catch (e: Exception) {
                Log.e("TestNotificationService", "Failed to create test medication event: ${e.message}")
            }
        }
    }
    
    fun showImmediateTestNotification() {
        // Create a test event for immediate notification
        val testEvent = MedicationEventEntity(
            prescriptionId = "test-prescription-immediate",
            title = "Immediate Test Medication",
            description = "This is an immediate test medication reminder. Take 1 tablet of TestMed with food.\n\nInstructions: Take with a full glass of water. Do not crush or chew.",
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis() + 3600000, // 1 hour window
            isCompleted = false
        )
        
        // Show notification immediately
        MedicationNotificationService.showMedicationReminderNotification(
            context,
            testEvent,
            "Test Prescription - Immediate"
        )
        
        Log.d("TestNotificationService", "Showed immediate test notification")
    }
}
