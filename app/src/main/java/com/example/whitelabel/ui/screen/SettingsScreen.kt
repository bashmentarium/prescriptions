package com.example.whitelabel.ui.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whitelabel.data.SettingsManager
import com.example.whitelabel.data.UserSettings
import com.example.whitelabel.data.CalendarSync
import android.widget.Toast
import java.util.Calendar

// Import the vibrant colors from the theme
import com.example.whitelabel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.getSettings()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CreamWhite,
                        Color(0xFFFFF5E6)
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "⚙️ My Settings",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color.Black
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(VibrantOrange, BrightCoral)
                                    ),
                                    shape = CircleShape
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Card with Gradient
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            color = Color(0xFF7F7F7F),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFE5B4),
                                        Color(0xFFFFF0D6)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(VibrantOrange, BrightCoral)
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 3.dp,
                                        color = Color(0xFF7F7F7F),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️",
                                    fontSize = 28.sp
                                )
                            }
                            Column {
                                Text(
                                    "My Medication Settings",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    color = DeepNavy
                                )
                                Text(
                                    "Customize your preferences",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF7F8C8D)
                                )
                            }
                        }
                    }
                }
            
                // Time Range Settings
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            color = Color(0xFF7F7F7F),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE8F8F5),
                                        Color(0xFFD5F4E6)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "⏰",
                                    fontSize = 28.sp
                                )
                                Text(
                                    "Time Range Settings",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = DeepNavy
                                )
                            }
                    
                            // Earliest Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Earliest time:",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 18.sp
                                    ),
                                    color = DeepNavy
                                )
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
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SkyBlue
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(
                                        formatMinutesToTime(settings.earliestTimeMinutes),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                            
                            // Latest Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    " Latest time:",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 18.sp
                                    ),
                                    color = DeepNavy
                                )
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
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LavenderPurple
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(
                                        formatMinutesToTime(settings.latestTimeMinutes),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            
                // Event Settings
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            color = Color(0xFF7F7F7F),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFE5E5),
                                        Color(0xFFFFF0F0)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📅",
                                    fontSize = 28.sp
                                )
                                Text(
                                    "Calendar Events",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = DeepNavy
                                )
                            }
                            
                            // Event Duration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Event duration:",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 18.sp
                                    ),
                                    color = DeepNavy
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            if (settings.eventDurationMinutes > 5) {
                                                val newDuration = settings.eventDurationMinutes - 5
                                                settings = settings.copy(eventDurationMinutes = newDuration)
                                                settingsManager.updateEventDuration(newDuration)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BrightCoral
                                        ),
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp)
                                    ) { 
                                        Text(
                                            "-",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            ),
                                            color = Color.White
                                        ) 
                                    }
                                    Text(
                                        "${settings.eventDurationMinutes} min",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = DeepNavy,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Button(
                                        onClick = {
                                            val newDuration = settings.eventDurationMinutes + 5
                                            settings = settings.copy(eventDurationMinutes = newDuration)
                                            settingsManager.updateEventDuration(newDuration)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent
                                        ),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(53.dp)
                                            .border(
                                                width = 2.dp,
                                                color = MintGreen,
                                                shape = CircleShape
                                            )
                                    ) { 
                                        Text(
                                            "+",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            ),
                                            color = MintGreen
                                        ) 
                                    }
                                }
                            }
                        }
                    }
                }
            
                // Default Preferences
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(listOf(MintGreen, GoldenYellow)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE8F8F5),
                                        Color(0xFFFFF8DC)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "🍽️",
                                    fontSize = 28.sp
                                )
                                Text(
                                    "Default Preferences",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = DeepNavy
                                )
                            }
                            
                            // With Food Default
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🍎 Default: Take with food",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 18.sp
                                    ),
                                    color = DeepNavy
                                )
                                Switch(
                                    checked = settings.withFoodDefault,
                                    onCheckedChange = { checked ->
                                        settings = settings.copy(withFoodDefault = checked)
                                        settingsManager.updateWithFoodDefault(checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MintGreen,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFBDC3C7)
                                    )
                                )
                            }
                            
                            // Preferred Times
                            Text(
                                "⭐ Preferred times:",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                color = DeepNavy
                            )
                            Column(
                                modifier = Modifier.selectableGroup()
                            ) {
                                val timeOptions = listOf("morning", "afternoon", "evening", "night")
                                val timeEmojis = listOf("🌅", "☀️", "🌆", "🌙")
                                timeOptions.forEachIndexed { index, time ->
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
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = settings.preferredTimes.contains(time),
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MintGreen,
                                                unselectedColor = Color(0xFF7F8C8D)
                                            )
                                        )
                                        Text(
                                            text = "${timeEmojis[index]} ${time.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp
                                            ),
                                            modifier = Modifier.padding(start = 12.dp),
                                            color = DeepNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            
                // Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(listOf(LavenderPurple, SkyBlue)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF4E6FF),
                                        Color(0xFFE6F3FF)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "📋",
                                    fontSize = 28.sp
                                )
                                Text(
                                    "Current Settings Summary",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = DeepNavy
                                )
                            }
                            Text(
                                "⏰ Time range: ${formatMinutesToTime(settings.earliestTimeMinutes)} - ${formatMinutesToTime(settings.latestTimeMinutes)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = DeepNavy
                            )
                            Text(
                                "⏱️ Event duration: ${settings.eventDurationMinutes} minutes",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = DeepNavy
                            )
                            Text(
                                "🍎 With food: ${if (settings.withFoodDefault) "Yes" else "No"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = DeepNavy
                            )
                            Text(
                                "⭐ Preferred times: ${settings.preferredTimes.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = DeepNavy
                            )
                        }
                    }
                }
                
                // Debug Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(listOf(BrightCoral, VibrantOrange)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFE5E5),
                                        Color(0xFFFFF0F0)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "🐛",
                                    fontSize = 28.sp
                                )
                                Text(
                                    "Debug Calendar Access",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = DeepNavy
                                )
                            }
                            Button(
                                onClick = {
                                    val calendars = CalendarSync.getAvailableCalendars(context)
                                    if (calendars.isNotEmpty()) {
                                        val calendarList = calendars.joinToString("\n") { 
                                            "• ${it.name} (ID: ${it.id}, Primary: ${it.isPrimary})" 
                                        }
                                        Toast.makeText(context, "Found ${calendars.size} calendars:\n$calendarList", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "No calendars found! Check permissions.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SkyBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Filled.BugReport, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Test Calendar Access",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }
                            
                            Button(
                                onClick = {
                                    val calendars = CalendarSync.getAvailableCalendars(context)
                                    if (calendars.isNotEmpty()) {
                                        val calendarId = calendars.find { it.isPrimary }?.id ?: calendars.first().id
                                        val events = CalendarSync.getEventsInCalendar(context, calendarId)
                                        val calendarName = calendars.find { it.id == calendarId }?.name ?: "Unknown"
                                        
                                        if (events.isNotEmpty()) {
                                            val eventList = events.take(5).joinToString("\n") { 
                                                "• ${it.title} (${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.startTime))})" 
                                            }
                                            Toast.makeText(context, "Found ${events.size} events in '$calendarName':\n$eventList", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "No events found in '$calendarName'", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No calendars available", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Filled.BugReport, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Check Events",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
