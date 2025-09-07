package com.example.whitelabel.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Battery optimization doesn't exist on older versions
        }
    }
    
    fun requestDisableBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isBatteryOptimizationDisabled(context)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    Log.d("BatteryOptimizationHelper", "Requested to disable battery optimization")
                } catch (e: Exception) {
                    Log.e("BatteryOptimizationHelper", "Failed to request battery optimization disable: ${e.message}")
                    // Fallback to general battery optimization settings
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        Log.e("BatteryOptimizationHelper", "Failed to open battery optimization settings: ${e2.message}")
                    }
                }
            } else {
                Log.d("BatteryOptimizationHelper", "Battery optimization already disabled")
            }
        }
    }
    
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
            Log.d("BatteryOptimizationHelper", "Opened battery optimization settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimizationHelper", "Failed to open battery optimization settings: ${e.message}")
        }
    }
}
