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
import com.example.whitelabel.data.ParsedPrescription
import com.example.whitelabel.data.SettingsManager
import android.util.Log
import com.example.whitelabel.ui.screen.ChatDetailScreen
import com.example.whitelabel.ui.screen.ChatListScreen
import com.example.whitelabel.ui.screen.ScheduleBuilderScreen
import com.example.whitelabel.ui.screen.SettingsScreen
import com.example.whitelabel.ui.screen.PrescriptionListScreen
import com.example.whitelabel.ui.screen.PrescriptionDetailScreen
import com.example.whitelabel.ui.screen.MedicationConfirmationScreen
import com.example.whitelabel.ui.theme.WhitelabelTheme

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
    
    private fun buildDescription(prescription: ParsedPrescription?, userSettings: com.example.whitelabel.data.UserSettings): String {
        if (prescription == null) {
            return "Take prescribed medications"
        }
        
        val medications = prescription.medications.joinToString("\n") { med ->
            "• ${med.name}: ${med.dosage} - ${med.frequency}"
        }
        
        val schedule = prescription.schedule
        val scheduleInfo = buildString {
            append("Schedule: ${schedule.times_per_day} times per day")
            
            // Use user's default with food setting if prescription doesn't specify
            val withFood = schedule.with_food || (userSettings.withFoodDefault && !schedule.with_food)
            if (withFood) {
                append(" (with food)")
            }
            
            if (schedule.preferred_times.isNotEmpty()) {
                append("\nPreferred times: ${schedule.preferred_times.joinToString(", ")}")
            }
            
            // Add reminder info
            if (userSettings.reminderMinutes > 0) {
                append("\nReminder: ${userSettings.reminderMinutes} minutes before")
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
                        // Calendar integration removed
                        Toast.makeText(this, "Calendar integration has been removed", Toast.LENGTH_LONG).show()
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
            ChatListScreen(
                onOpenChat = { id -> navController.navigate("chat/$id") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenPrescriptions = { navController.navigate("prescriptions") },
                onOpenMedicationConfirmation = { navController.navigate("medication-confirmation") }
            )
        }
        composable("prescriptions") {
            PrescriptionListScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate("settings") },
                onOpenPrescriptionDetail = { id -> navController.navigate("prescription/$id") }
            )
        }
        composable("prescription/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            val prescriptionId = it.arguments?.getString("id") ?: ""
            PrescriptionDetailScreen(
                prescriptionId = prescriptionId,
                onBack = { navController.popBackStack() }
            )
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
            ScheduleBuilderScreen(
                onBack = { navController.popBackStack() }, 
                onConfirm = { start, earliest, latest, days ->
                    onInsertEvents(start, earliest, latest, days, parsedPrescription.value)
                    navController.popBackStack()
                },
                parsedPrescription = parsedPrescription.value
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("medication-confirmation") {
            MedicationConfirmationScreen(onBack = { navController.popBackStack() })
        }
    }
}