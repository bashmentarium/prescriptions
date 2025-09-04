package com.example.whitelabel

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.whitelabel.data.CalendarSync
import com.example.whitelabel.data.ParsedPrescription
import com.example.whitelabel.ui.screen.ChatDetailScreen
import com.example.whitelabel.ui.screen.ChatListScreen
import com.example.whitelabel.ui.screen.ScheduleBuilderScreen
import com.example.whitelabel.ui.theme.WhitelabelTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    
    private fun calculateTimeSlots(earliest: Int, latest: Int, timesPerDay: Int, prescription: ParsedPrescription?): List<Int> {
        if (timesPerDay <= 1) {
            return listOf(earliest)
        }
        
        // Try to use preferred times from prescription if available
        val preferredTimes = prescription?.schedule?.preferred_times
        if (preferredTimes != null && preferredTimes.isNotEmpty()) {
            val preferredTimeSlots = preferredTimes.map { time ->
                when (time.lowercase()) {
                    "morning" -> 8 * 60 // 8:00 AM
                    "afternoon" -> 14 * 60 // 2:00 PM
                    "evening" -> 20 * 60 // 8:00 PM
                    "night" -> 22 * 60 // 10:00 PM
                    else -> earliest
                }
            }.filter { it >= earliest && it <= latest }
            
            // If we have enough preferred times, use them
            if (preferredTimeSlots.size >= timesPerDay) {
                return preferredTimeSlots.take(timesPerDay)
            }
            // If we have some preferred times but not enough, use them and fill the rest
            else if (preferredTimeSlots.isNotEmpty()) {
                val remaining = timesPerDay - preferredTimeSlots.size
                val timeRange = latest - earliest
                val interval = timeRange / (remaining + 1)
                
                val additionalSlots = (1..remaining).map { index ->
                    earliest + (interval * index)
                }
                
                return (preferredTimeSlots + additionalSlots).sorted()
            }
        }
        
        // Fallback to evenly distributed times
        val timeRange = latest - earliest
        val interval = timeRange / (timesPerDay - 1)
        
        return (0 until timesPerDay).map { index ->
            earliest + (interval * index)
        }
    }
    
    private fun buildEventTitle(prescription: ParsedPrescription?): String {
        if (prescription == null) {
            return "Medication"
        }
        
        val medications = prescription.medications
        return when {
            medications.isEmpty() -> "Medication"
            medications.size == 1 -> medications.first().name
            medications.size <= 3 -> medications.joinToString(", ") { it.name }
            else -> "${medications.first().name} + ${medications.size - 1} more"
        }
    }
    
    private fun buildDescription(prescription: ParsedPrescription?): String {
        if (prescription == null) {
            return "Take prescribed medications"
        }
        
        val medications = prescription.medications.joinToString("\n") { med ->
            "• ${med.name}: ${med.dosage} - ${med.frequency}"
        }
        
        val schedule = prescription.schedule
        val scheduleInfo = buildString {
            append("Schedule: ${schedule.times_per_day} times per day")
            if (schedule.with_food) {
                append(" (with food)")
            }
            if (schedule.preferred_times.isNotEmpty()) {
                append("\nPreferred times: ${schedule.preferred_times.joinToString(", ")}")
            }
        }
        
        return "$medications\n\n$scheduleInfo"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhitelabelTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(onInsertEvents = { startMillis, earliest, latest, days, prescription ->
                        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
                        var created = 0
                        
                        repeat(days) { dayIndex ->
                            val dayCal = (cal.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_YEAR, dayIndex)
                            }
                            
                            // Create multiple events per day based on prescription
                            val timesPerDay = prescription?.schedule?.times_per_day ?: 1
                            val timeSlots = calculateTimeSlots(earliest, latest, timesPerDay, prescription)
                            
                            timeSlots.forEach { timeInMinutes ->
                                val eventCal = (dayCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, timeInMinutes / 60)
                                    set(Calendar.MINUTE, timeInMinutes % 60)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                
                                val start = eventCal.timeInMillis
                                val end = start + 30 * 60 * 1000 // 30 minutes duration
                                
                                // Create event title and description from prescription
                                val title = buildEventTitle(prescription)
                                val description = buildDescription(prescription)
                                
                                CalendarSync.insertEvent(
                                    context = this,
                                    title = title,
                                    description = description,
                                    startMillis = start,
                                    endMillis = end
                                )?.let { created++ }
                            }
                        }
                        Toast.makeText(this, "$created events added to Calendar", Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
    }
}

@Composable
fun AppNav(onInsertEvents: (startMillis: Long, earliestMinutes: Int, latestMinutes: Int, days: Int, prescription: ParsedPrescription?) -> Unit) {
    val navController = rememberNavController()
    val parsedPrescription = remember { mutableStateOf<ParsedPrescription?>(null) }
    
    NavHost(navController = navController, startDestination = "chats") {
        composable("chats") {
            ChatListScreen(onOpenChat = { id -> navController.navigate("chat/$id") })
        }
        composable("chat/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            ChatDetailScreen(
                onBack = { navController.popBackStack() }, 
                onOpenSchedule = { prescription -> 
                    parsedPrescription.value = prescription
                    navController.navigate("schedule") 
                }
            )
        }
        composable("schedule") {
            val hasCalendarPermission = remember { mutableStateOf(false) }
            val requester = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                hasCalendarPermission.value = result[Manifest.permission.READ_CALENDAR] == true &&
                    result[Manifest.permission.WRITE_CALENDAR] == true
            }
            LaunchedEffect(Unit) {
                requester.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            }
            ScheduleBuilderScreen(
                onBack = { navController.popBackStack() }, 
                onConfirm = { start, earliest, latest, days ->
                    if (hasCalendarPermission.value) onInsertEvents(start, earliest, latest, days, parsedPrescription.value)
                    navController.popBackStack()
                },
                parsedPrescription = parsedPrescription.value
            )
        }
    }
}