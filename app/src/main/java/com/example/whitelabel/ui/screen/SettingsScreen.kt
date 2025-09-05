package com.example.whitelabel.ui.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.whitelabel.data.SettingsManager
import com.example.whitelabel.data.UserSettings
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.getSettings()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Medication Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Time Range Settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "⏰ Time Range",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Earliest Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Earliest time:")
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, settings.earliestTimeMinutes / 60)
                                    set(Calendar.MINUTE, settings.earliestTimeMinutes % 60)
                                }
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val newMinutes = hour * 60 + minute
                                        settings = settings.copy(earliestTimeMinutes = newMinutes)
                                        settingsManager.updateEarliestTime(newMinutes)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }
                        ) {
                            Text(formatMinutesToTime(settings.earliestTimeMinutes))
                        }
                    }
                    
                    // Latest Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Latest time:")
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, settings.latestTimeMinutes / 60)
                                    set(Calendar.MINUTE, settings.latestTimeMinutes % 60)
                                }
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val newMinutes = hour * 60 + minute
                                        settings = settings.copy(latestTimeMinutes = newMinutes)
                                        settingsManager.updateLatestTime(newMinutes)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }
                        ) {
                            Text(formatMinutesToTime(settings.latestTimeMinutes))
                        }
                    }
                }
            }
            
            // Event Settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "📅 Calendar Events",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Event Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Event duration:")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (settings.eventDurationMinutes > 5) {
                                        val newDuration = settings.eventDurationMinutes - 5
                                        settings = settings.copy(eventDurationMinutes = newDuration)
                                        settingsManager.updateEventDuration(newDuration)
                                    }
                                }
                            ) { Text("-") }
                            Text("${settings.eventDurationMinutes} min")
                            Button(
                                onClick = {
                                    val newDuration = settings.eventDurationMinutes + 5
                                    settings = settings.copy(eventDurationMinutes = newDuration)
                                    settingsManager.updateEventDuration(newDuration)
                                }
                            ) { Text("+") }
                        }
                    }
                    
                }
            }
            
            // Default Preferences
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "🍽️ Default Preferences",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // With Food Default
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default: Take with food")
                        Switch(
                            checked = settings.withFoodDefault,
                            onCheckedChange = { checked ->
                                settings = settings.copy(withFoodDefault = checked)
                                settingsManager.updateWithFoodDefault(checked)
                            }
                        )
                    }
                    
                    // Preferred Times
                    Text("Preferred times:")
                    Column(
                        modifier = Modifier.selectableGroup()
                    ) {
                        val timeOptions = listOf("morning", "afternoon", "evening", "night")
                        timeOptions.forEach { time ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = settings.preferredTimes.contains(time),
                                        onClick = {
                                            val newTimes = if (settings.preferredTimes.contains(time)) {
                                                settings.preferredTimes - time
                                            } else {
                                                settings.preferredTimes + time
                                            }
                                            settings = settings.copy(preferredTimes = newTimes)
                                            settingsManager.updatePreferredTimes(newTimes)
                                        },
                                        role = Role.Checkbox
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.preferredTimes.contains(time),
                                    onClick = null
                                )
                                Text(
                                    text = time.replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "📋 Current Settings Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("• Time range: ${formatMinutesToTime(settings.earliestTimeMinutes)} - ${formatMinutesToTime(settings.latestTimeMinutes)}")
                    Text("• Event duration: ${settings.eventDurationMinutes} minutes")
                    Text("• With food: ${if (settings.withFoodDefault) "Yes" else "No"}")
                    Text("• Preferred times: ${settings.preferredTimes.joinToString(", ")}")
                }
            }
        }
    }
}

private fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val period = if (hours >= 12) "PM" else "AM"
    val displayHours = when {
        hours == 0 -> 12
        hours > 12 -> hours - 12
        else -> hours
    }
    return String.format("%d:%02d %s", displayHours, mins, period)
}
