package com.example.whitelabel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "Device booted or app updated, restarting medication reminders")
                
                // Restart medication reminder scheduler
                val reminderScheduler = MedicationReminderScheduler(context)
                reminderScheduler.scheduleMedicationReminders()
                
                // Start foreground service for reliable background processing
                MedicationForegroundService.startService(context)
            }
        }
    }
}
