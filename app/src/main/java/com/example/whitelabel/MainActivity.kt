package com.example.whitelabel

import android.Manifest
import android.os.Build
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
import com.example.whitelabel.service.MedicationNotificationService
import com.example.whitelabel.service.MedicationReminderScheduler
import com.example.whitelabel.service.MedicationForegroundService
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.repository.PrescriptionRepository
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
            Toast.makeText(this, "Notification permission is required for medication reminders", Toast.LENGTH_LONG).show()
        }
    }
    
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
            else -> medications.joinToString(", ") { it.name }
        }
    }
    
    private fun buildDescription(prescription: ParsedPrescription?, userSettings: com.example.whitelabel.data.UserSettings): String {
        if (prescription == null) {
            return "Take prescribed medications"
        }
        
        val medications = prescription.medications.joinToString("\n") { med ->
            "• ${med.name}: ${med.dosage} - ${med.frequency}"
        }
        
        val schedule = prescription.schedule ?: com.example.whitelabel.data.ScheduleAggregator.aggregateScheduleFromMedications(prescription.medications)
        val scheduleInfo = buildString {
            append("Schedule: ${schedule.times_per_day} times per day")
            
            // Use prescription's food timing or user's default if prescription is neutral
            val foodTiming = if (schedule.food_timing == com.example.whitelabel.data.FoodTiming.NEUTRAL) {
                userSettings.foodTimingDefault
            } else {
                schedule.food_timing
            }
            
            val foodTimingText = when (foodTiming) {
                com.example.whitelabel.data.FoodTiming.BEFORE_MEAL -> " (before meal)"
                com.example.whitelabel.data.FoodTiming.DURING_MEAL -> " (during meal)"
                com.example.whitelabel.data.FoodTiming.AFTER_MEAL -> " (after meal)"
                com.example.whitelabel.data.FoodTiming.NEUTRAL -> ""
            }
            append(foodTimingText)
            
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
        
        // Initialize notification services (with error handling)
        try {
            MedicationNotificationService.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to create notification channel: ${e.message}")
        }
        
        // Initialize Firebase Messaging (with error handling)
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                
                // Get new FCM registration token
                val token = task.result
                Log.d("MainActivity", "FCM Registration Token: $token")
                
                // TODO: Send token to your server if needed
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize Firebase Messaging: ${e.message}")
        }
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("MainActivity", "Requesting notification permission for Android 13+")
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Log.d("MainActivity", "Android version < 13, no notification permission needed")
        }
        
        // Start medication reminder scheduler (with error handling)
        try {
            val reminderScheduler = MedicationReminderScheduler(this)
            reminderScheduler.scheduleMedicationReminders()
            
            // Start foreground service for reliable background notifications
            MedicationForegroundService.startService(this)
            
            // Set up repository with reminder scheduler
            val database = AppDatabase.getDatabase(this)
            val repository = PrescriptionRepository(database)
            repository.setReminderScheduler(reminderScheduler)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize services: ${e.message}")
            // Continue with app launch even if services fail
        }
        
        setContent {
            WhitelabelTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(
                        onInsertEvents = { startMillis, earliest, latest, days, prescription ->
                            // Calendar integration removed
                            Toast.makeText(this, "Calendar integration has been removed", Toast.LENGTH_LONG).show()
                        },
                        initialDestination = getInitialDestination()
                    )
                }
            }
        }
    }
    
    private fun getInitialDestination(): String {
        val intent = intent
        val navigateTo = intent.getStringExtra("navigate_to")
        return when (navigateTo) {
            "medication-confirmation" -> "medication-confirmation"
            else -> "chats"
        }
    }
}

@Composable
fun AppNav(
    onInsertEvents: (startMillis: Long, earliestMinutes: Int, latestMinutes: Int, days: Int, prescription: ParsedPrescription?) -> Unit,
    initialDestination: String = "chats"
) {
    val navController = rememberNavController()
    val parsedPrescription = remember { mutableStateOf<ParsedPrescription?>(null) }
    
    NavHost(navController = navController, startDestination = initialDestination) {
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
            val conversationId = it.arguments?.getString("id") ?: ""
            ChatDetailScreen(
                conversationId = conversationId,
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