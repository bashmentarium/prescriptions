package com.example.whitelabel.service

import android.content.Context
import android.util.Log
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.repository.PrescriptionRepository
import kotlinx.coroutines.*

class NotificationSystemTester(private val context: Context) {
    
    companion object {
        fun testSystemWideNotifications(context: Context) {
            val tester = NotificationSystemTester(context)
            tester.runComprehensiveTest()
        }
    }
    
    fun runComprehensiveTest() {
        Log.d("NotificationSystemTester", "Starting comprehensive notification system test...")
        
        // Test 1: Check if foreground service is running
        testForegroundService()
        
        // Test 2: Check battery optimization status
        testBatteryOptimization()
        
        // Test 3: Test immediate notification
        testImmediateNotification()
        
        // Test 4: Test scheduled notification (1 minute)
        testScheduledNotification()
        
        // Test 5: Test alarm-based notification (2 minutes)
        testAlarmBasedNotification()
        
        Log.d("NotificationSystemTester", "Comprehensive test completed - check logs for results")
    }
    
    private fun testForegroundService() {
        Log.d("NotificationSystemTester", "=== TEST 1: Foreground Service ===")
        val isRunning = MedicationForegroundService.isRunning()
        Log.d("NotificationSystemTester", "Foreground service running: $isRunning")
        
        if (!isRunning) {
            Log.w("NotificationSystemTester", "WARNING: Foreground service not running - starting it now")
            MedicationForegroundService.startService(context)
        }
    }
    
    private fun testBatteryOptimization() {
        Log.d("NotificationSystemTester", "=== TEST 2: Battery Optimization ===")
        val isOptimized = BatteryOptimizationHelper.isBatteryOptimizationDisabled(context)
        Log.d("NotificationSystemTester", "Battery optimization disabled: $isOptimized")
        
        if (!isOptimized) {
            Log.w("NotificationSystemTester", "WARNING: Battery optimization is enabled - notifications may not work when app is closed")
            Log.d("NotificationSystemTester", "Recommendation: Use the Settings screen to disable battery optimization")
        }
    }
    
    private fun testImmediateNotification() {
        Log.d("NotificationSystemTester", "=== TEST 3: Immediate Notification ===")
        try {
            val simpleTest = SimpleNotificationTest(context)
            simpleTest.showSimpleTestNotification()
            Log.d("NotificationSystemTester", "✓ Immediate notification test completed")
        } catch (e: Exception) {
            Log.e("NotificationSystemTester", "✗ Immediate notification test failed: ${e.message}")
        }
    }
    
    private fun testScheduledNotification() {
        Log.d("NotificationSystemTester", "=== TEST 4: Scheduled Notification (1 min) ===")
        try {
            val testService = TestNotificationService(context)
            testService.createTestMedicationEvent()
            Log.d("NotificationSystemTester", "✓ Scheduled notification test completed - check in 1 minute")
        } catch (e: Exception) {
            Log.e("NotificationSystemTester", "✗ Scheduled notification test failed: ${e.message}")
        }
    }
    
    private fun testAlarmBasedNotification() {
        Log.d("NotificationSystemTester", "=== TEST 5: Alarm-Based Notification (2 min) ===")
        try {
            // Create a test event for 2 minutes from now
            val testEvent = MedicationEventEntity(
                id = "test_alarm_${System.currentTimeMillis()}",
                prescriptionId = "test_prescription",
                title = "Test Alarm Notification",
                description = "This is a test alarm-based notification",
                startTimeMillis = System.currentTimeMillis() + (2 * 60 * 1000), // 2 minutes
                endTimeMillis = System.currentTimeMillis() + (3 * 60 * 1000), // 3 minutes
                isCompleted = false,
                reminderSent = false
            )
            
            val alarmScheduler = MedicationAlarmScheduler(context)
            alarmScheduler.scheduleMedicationReminder(testEvent, "Test Prescription")
            
            Log.d("NotificationSystemTester", "✓ Alarm-based notification test completed - check in 2 minutes")
        } catch (e: Exception) {
            Log.e("NotificationSystemTester", "✗ Alarm-based notification test failed: ${e.message}")
        }
    }
    
    fun testNotificationWhenAppClosed() {
        Log.d("NotificationSystemTester", "=== TEST: App Closed Scenario ===")
        Log.d("NotificationSystemTester", "To test notifications when app is closed:")
        Log.d("NotificationSystemTester", "1. Run this test")
        Log.d("NotificationSystemTester", "2. Close the app completely (swipe away from recent apps)")
        Log.d("NotificationSystemTester", "3. Wait for the scheduled time")
        Log.d("NotificationSystemTester", "4. Check if notification appears")
        Log.d("NotificationSystemTester", "5. If notification appears, the system is working correctly!")
        
        // Schedule a test notification for 3 minutes from now
        val testEvent = MedicationEventEntity(
            id = "test_closed_app_${System.currentTimeMillis()}",
            prescriptionId = "test_prescription",
            title = "App Closed Test",
            description = "This notification should appear even when the app is closed",
            startTimeMillis = System.currentTimeMillis() + (3 * 60 * 1000), // 3 minutes
            endTimeMillis = System.currentTimeMillis() + (4 * 60 * 1000), // 4 minutes
            isCompleted = false,
            reminderSent = false
        )
        
        val alarmScheduler = MedicationAlarmScheduler(context)
        alarmScheduler.scheduleMedicationReminder(testEvent, "App Closed Test")
        
        Log.d("NotificationSystemTester", "Test notification scheduled for 3 minutes from now")
        Log.d("NotificationSystemTester", "Close the app and wait for the notification!")
    }
}
