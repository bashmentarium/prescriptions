package com.example.whitelabel.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveSettings(settings: UserSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString("settings", json).apply()
    }
    
    fun getSettings(): UserSettings {
        val json = prefs.getString("settings", null)
        return if (json != null) {
            try {
                gson.fromJson(json, UserSettings::class.java)
            } catch (e: Exception) {
                UserSettings() // Return default settings if parsing fails
            }
        } else {
            UserSettings() // Return default settings if no saved settings
        }
    }
    
    fun updateEarliestTime(minutes: Int) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(earliestTimeMinutes = minutes))
    }
    
    fun updateLatestTime(minutes: Int) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(latestTimeMinutes = minutes))
    }
    
    fun updateEventDuration(minutes: Int) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(eventDurationMinutes = minutes))
    }
    
    fun updateReminderMinutes(minutes: Int) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(reminderMinutes = minutes))
    }
    
    fun updateFoodTimingDefault(foodTiming: FoodTiming) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(foodTimingDefault = foodTiming))
    }
    
    fun updatePreferredTimes(times: List<String>) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(preferredTimes = times))
    }
}
