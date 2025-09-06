package com.example.whitelabel.ui.screen

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.content.ContentResolver
import android.database.Cursor
import android.provider.OpenableColumns
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.example.whitelabel.ui.theme.*
import coil.compose.rememberAsyncImagePainter
import com.example.whitelabel.data.LlmService
import com.example.whitelabel.data.RealAnthropicService
import com.example.whitelabel.data.ParsedPrescription
import com.example.whitelabel.data.CalendarSync
import com.example.whitelabel.data.SettingsManager
import com.example.whitelabel.data.UserSettings
import com.example.whitelabel.data.CalendarInfo
import com.example.whitelabel.data.EventInfo
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.repository.PrescriptionRepository
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.util.Calendar
import android.widget.Toast

data class CourseItem(val name: String, val prescriptionPreview: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onOpenChat: (String) -> Unit, onOpenSettings: () -> Unit, onOpenPrescriptions: () -> Unit = {}, onOpenMedicationConfirmation: () -> Unit = {}) {
    val items = remember { 
        mutableStateListOf(
            CourseItem("Course 1", "Amoxicillin 500mg - Take 3 times daily with food for 7 days. Complete the full course even if you feel better."),
            CourseItem("Course 2", "Ibuprofen 400mg - Take as needed for pain, maximum 3 times per day. Take with food to reduce stomach irritation.")
        ) 
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            topBar = { 
                TopAppBar(
                    title = { 
                        Text(
                            "My Treatment Courses",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = Color.Black,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(
                            onClick = onOpenPrescriptions
                        ) {
                            Icon(
                                Icons.Filled.Medication, 
                                contentDescription = "Saved Prescriptions",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                Icons.Filled.Settings, 
                                contentDescription = "Settings",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenMedicationConfirmation
                        ) {
                            Icon(
                                Icons.Filled.Check, 
                                contentDescription = "Medication Confirmation",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val newCourse = CourseItem("Course ${items.size + 1}", "New prescription - details will be added after processing")
                        items.add(newCourse)
                        onOpenChat(newCourse.name)
                    },
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            width = 1.dp,
                            color = BorderGray,
                            shape = CircleShape
                        )
                ) { 
                    Icon(
                        Icons.Filled.Add, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { courseItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                            .clickable { onOpenChat(courseItem.name) }
                            .border(
                                width = 1.dp,
                                color = BorderGray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Treatment course icon - simple black and white
                                Icon(
                                    Icons.Filled.Medication,
                                    contentDescription = "Treatment",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = courseItem.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        ),
                                        color = Color.Black
                                    )
                                    Text(
                                        text = courseItem.prescriptionPreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Arrow without circle background
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Open",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(180f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ChatMessage(val fromUser: Boolean, val text: String = "", val image: Uri? = null, val isProcessing: Boolean = false)

// Function to get image name from URI
private fun getImageName(context: android.content.Context, uri: Uri): String {
    var name = "Unknown Image"
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex) ?: "Unknown Image"
            }
        }
    }
    return name
}

// Function to create a temporary file for camera photos
private fun createImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

// Function to create calendar events directly from prescription using user settings
private fun createCalendarEventsFromPrescription(
    context: android.content.Context,
    prescription: ParsedPrescription,
    userSettings: UserSettings
): Int {
    val cal = Calendar.getInstance()
    var created = 0
    
    repeat(prescription.schedule.duration_days) { dayIndex ->
        val dayCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayIndex)
        }
        
        // Create multiple events per day based on prescription and user settings
        val timesPerDay = prescription.schedule.times_per_day
        val prescriptionStartTime = prescription.schedule.start_time_minutes
        val prescriptionEndTime = prescription.schedule.end_time_minutes
        
        // Use user settings as bounds, but respect prescription timing if it's within bounds
        val startTime = maxOf(prescriptionStartTime, userSettings.earliestTimeMinutes)
        val endTime = minOf(prescriptionEndTime, userSettings.latestTimeMinutes)
        
        // Calculate time slots
        val timeSlots = if (timesPerDay <= 1) {
            listOf(startTime)
        } else {
            val timeRange = endTime - startTime
            val interval = timeRange / (timesPerDay - 1)
            (0 until timesPerDay).map { index ->
                startTime + (interval * index)
            }
        }
        
        timeSlots.forEach { timeInMinutes ->
            val eventCal = (dayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, timeInMinutes / 60)
                set(Calendar.MINUTE, timeInMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val start = eventCal.timeInMillis
            val end = start + userSettings.eventDurationMinutes * 60 * 1000 // Use user's preferred duration
            
            // Create event title and description from prescription
            val title = buildEventTitle(prescription)
            val description = buildEventDescription(prescription, userSettings)
            
            // Get available calendars and use the first one if default doesn't work
            val availableCalendars = CalendarSync.getAvailableCalendars(context)
            val calendarId = if (availableCalendars.isNotEmpty()) {
                // Try to find primary calendar first, otherwise use the first available
                availableCalendars.find { it.isPrimary }?.id ?: availableCalendars.first().id
            } else {
                userSettings.defaultCalendarId
            }
            
            Log.d("ChatScreens", "Using calendar ID: $calendarId")
            
            CalendarSync.insertEvent(
                context = context,
                title = title,
                description = description,
                startMillis = start,
                endMillis = end,
                calendarId = calendarId
            )?.let { 
                created++
                Log.d("ChatScreens", "Successfully created event $created")
            } ?: run {
                Log.e("ChatScreens", "Failed to create event at time $timeInMinutes")
            }
        }
    }
    
    // Verify events were actually created
    if (created > 0) {
        val availableCalendars = CalendarSync.getAvailableCalendars(context)
        val calendarId = if (availableCalendars.isNotEmpty()) {
            availableCalendars.find { it.isPrimary }?.id ?: availableCalendars.first().id
        } else {
            userSettings.defaultCalendarId
        }
        
        val events = CalendarSync.getEventsInCalendar(context, calendarId)
        Log.d("ChatScreens", "Verification: Found ${events.size} events in calendar $calendarId")
        
        // Log recent events to help debug
        events.take(5).forEach { event ->
            Log.d("ChatScreens", "Recent event: ${event.title} at ${event.startTime}")
        }
    }
    
    return created
}

private fun buildEventTitle(prescription: ParsedPrescription): String {
    val medications = prescription.medications
    return when {
        medications.isEmpty() -> "Medication"
        medications.size == 1 -> medications.first().name
        medications.size <= 3 -> medications.joinToString(", ") { it.name }
        else -> "${medications.first().name} + ${medications.size - 1} more"
    }
}

private fun buildEventDescription(prescription: ParsedPrescription, userSettings: UserSettings): String {
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

private fun buildHumanReadableMessage(prescription: ParsedPrescription, sourceType: String): String {
    val medications = prescription.medications.joinToString("\n") { med ->
        "• ${med.name}: ${med.dosage} - ${med.frequency}"
    }
    
    val schedule = prescription.schedule
    val startTime = formatMinutesToTime(schedule.start_time_minutes)
    val endTime = formatMinutesToTime(schedule.end_time_minutes)
    
    val scheduleInfo = buildString {
        append("📅 Schedule: ${schedule.times_per_day} times per day for ${schedule.duration_days} days")
        append("\n⏰ Timing: Between $startTime and $endTime")
        if (schedule.with_food) {
            append("\n🍽️ Take with food")
        }
        if (schedule.preferred_times.isNotEmpty()) {
            append("\n⭐ Preferred times: ${schedule.preferred_times.joinToString(", ")}")
        }
    }
    
    return "✅ Prescription parsed successfully from $sourceType!\n\n" +
           "💊 Medications:\n$medications\n\n" +
           "$scheduleInfo\n\n" +
           "🎯 Ready to create calendar events! Tap 'Set Timing & Approve' to add to your calendar."
}

fun formatMinutesToTime(minutes: Int): String {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(onBack: () -> Unit, onOpenSchedule: (ParsedPrescription?) -> Unit) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val input = remember { mutableStateOf("") }
    val context = LocalContext.current
    val llm: LlmService = remember { RealAnthropicService(context) }
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val parsedPrescription = remember { mutableStateOf<ParsedPrescription?>(null) }
    val settingsManager = remember { SettingsManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val prescriptionRepository = remember { PrescriptionRepository(database) }
    
    // Calendar permissions
    val hasCalendarPermission = remember { mutableStateOf(false) }
    val requester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCalendarPermission.value = result[Manifest.permission.READ_CALENDAR] == true &&
            result[Manifest.permission.WRITE_CALENDAR] == true
    }
    
    // Camera permissions
    val hasCameraPermission = remember { mutableStateOf(false) }
    val cameraPermissionRequester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission.value = isGranted
    }
    
    // State for image upload functionality
    val selectedImage = remember { mutableStateOf<Uri?>(null) }
    val isImageUploading = remember { mutableStateOf(false) }
    val imageName = remember { mutableStateOf<String?>(null) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImage.value = uri
            isImageUploading.value = true
            
            // Get image name for preview
            val name = getImageName(context, uri)
            imageName.value = name
            
            // Simulate image processing delay and add to messages
            scope.launch {
                kotlinx.coroutines.delay(1000) // Simulate processing time
                messages.add(ChatMessage(true, image = uri))
                isImageUploading.value = false
                // Keep the preview visible - don't clear selectedImage and imageName
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri.value != null) {
            selectedImage.value = cameraImageUri.value
            isImageUploading.value = true
            imageName.value = "Camera Photo"
            
            // Simulate image processing delay and add to messages
            scope.launch {
                kotlinx.coroutines.delay(1000) // Simulate processing time
                messages.add(ChatMessage(true, image = cameraImageUri.value))
                isImageUploading.value = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CreamWhite,
                        LightCream
                    )
                )
            )
    ) {
        Scaffold(
            topBar = { 
                TopAppBar(
                    title = { 
                        Text(
                            "💬 Prescription Chat",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = DeepNavy
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = { 
                        IconButton(
                            onClick = onBack
                        ) { 
                            Icon(
                                Icons.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color.Black,
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
            ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!m.fromUser) {
                            // AI avatar - simple icon
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = "AI",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        Column(
                            modifier = Modifier.widthIn(max = 280.dp),
                            horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start
                        ) {
                            if (m.image != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 2.dp,
                                            brush = if (m.fromUser) 
                                                Brush.linearGradient(listOf(VibrantOrange, GoldenYellow))
                                            else 
                                                Brush.linearGradient(listOf(MintGreen, SkyBlue)),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (m.fromUser) 
                                            WarmPeach 
                                        else 
                                            LightCream
                                    )
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(m.image),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }
                            
                            if (m.text.isNotBlank()) {
                                Card(
                                    modifier = Modifier
                                        .border(
                                            width = 2.dp,
                                            brush = if (m.fromUser) 
                                                Brush.linearGradient(listOf(VibrantOrange, GoldenYellow))
                                            else 
                                                Brush.linearGradient(listOf(MintGreen, SkyBlue)),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (m.fromUser) 16.dp else 4.dp,
                                                bottomEnd = if (m.fromUser) 4.dp else 16.dp
                                            )
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (m.fromUser) 
                                            WarmPeach 
                                        else 
                                            LightCream
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (m.fromUser) 16.dp else 4.dp,
                                        bottomEnd = if (m.fromUser) 4.dp else 16.dp
                                    )
                                ) {
                                    if (m.isProcessing) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = if (m.fromUser) 
                                                    MaterialTheme.colorScheme.onPrimary 
                                                else 
                                                    MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Processing prescription...", 
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (m.fromUser) 
                                                    MaterialTheme.colorScheme.onPrimary 
                                                else 
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Text(
                                            m.text, 
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (m.fromUser) 
                                                DeepNavy 
                                            else 
                                                CharcoalGray,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (m.fromUser) {
                            Spacer(modifier = Modifier.width(12.dp))
                            // User avatar - simple icon
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "User",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Image preview area
            if (selectedImage.value != null && imageName.value != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImage.value),
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = imageName.value!!,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (isImageUploading.value) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Uploading...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Uploaded",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Uploaded successfully",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Close button to clear the preview
                        IconButton(
                            onClick = {
                                selectedImage.value = null
                                imageName.value = null
                                isImageUploading.value = false
                            }
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear preview",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Input area with vibrant design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(VibrantOrange, SkyBlue)),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFF3E0),
                                    Color(0xFFFFE0B2)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image upload buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Gallery button
                        Box {
                            IconButton(
                                onClick = {
                                    if (!isImageUploading.value) {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                when {
                                    isImageUploading.value -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    selectedImage.value != null -> {
                                        Image(
                                            painter = rememberAsyncImagePainter(selectedImage.value),
                                            contentDescription = "Selected image",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    else -> {
                                        Icon(Icons.Filled.Image, contentDescription = "Upload from gallery", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            
                            // Show check mark when image is being processed
                            if (isImageUploading.value) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Processing",
                                    modifier = Modifier
                                        .size(12.dp)
                                        .offset(x = 16.dp, y = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // Camera button
                        IconButton(
                            onClick = {
                                if (!isImageUploading.value) {
                                    if (hasCameraPermission.value) {
                                        val photoFile = createImageFile(context)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        cameraImageUri.value = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionRequester.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Take photo", modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    // Text input field
                    OutlinedTextField(
                        value = input.value, 
                        onValueChange = { input.value = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your prescription here...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    
                    // Send button
                    Button(
                        enabled = !isImageUploading.value && (input.value.isNotBlank() || selectedImage.value != null),
                        onClick = {
                            // Allow sending if there's text OR if there's a selected image
                            if ((input.value.isBlank() && selectedImage.value == null) || isImageUploading.value) return@Button
                            
                            val userInput = input.value // Store the input before clearing
                            val userMessage = ChatMessage(true, text = userInput, image = selectedImage.value)
                            messages.add(userMessage)
                            
                            // Clear the image preview after sending
                            selectedImage.value = null
                            imageName.value = null
                            
                            val processingMessage = ChatMessage(false, text = "", isProcessing = true)
                            messages.add(processingMessage)
                            
                            input.value = "" // Clear input immediately after storing
                            
                            scope.launch {
                                try {
                                    val result = if (userMessage.image != null) {
                                        // Process image
                                        llm.parseFromImage(userMessage.image)
                                    } else {
                                        // Process text
                                        llm.parseFromText(userInput)
                                    }
                                    messages.remove(processingMessage)
                                    
                                    when (result) {
                                        is com.example.whitelabel.data.LlmResult.Success -> {
                                            val jsonText = result.normalizedSchedule
                                            try {
                                                // Parse the JSON response into ParsedPrescription
                                                val prescription = gson.fromJson(jsonText, ParsedPrescription::class.java)
                                                parsedPrescription.value = prescription
                                                
                                                // Create human-readable message
                                                val sourceType = if (userMessage.image != null) "image" else "text"
                                                val humanReadableMessage = buildHumanReadableMessage(prescription, sourceType)
                                                messages.add(ChatMessage(false, text = humanReadableMessage))
                                            } catch (e: Exception) {
                                                // If parsing fails, show the raw response
                                                val sourceType = if (userMessage.image != null) "image" else "text"
                                                messages.add(ChatMessage(false, text = "Prescription parsed from $sourceType (raw):\n\n$jsonText\n\nTap 'Set Timing & Approve' to continue."))
                                                parsedPrescription.value = null
                                            }
                                        }
                                        is com.example.whitelabel.data.LlmResult.Error -> {
                                            messages.add(ChatMessage(false, text = "Error: ${result.message}"))
                                            parsedPrescription.value = null
                                        }
                                    }
                                } catch (e: Exception) {
                                    messages.remove(processingMessage)
                                    messages.add(ChatMessage(false, text = "Error processing prescription: ${e.message}"))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VibrantOrange,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(VibrantOrange, GoldenYellow)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) { 
                        Text(
                            "Send", 
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                    }
                }
                }
            }
            // Set Timing & Approve button with vibrant design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        width = 3.dp,
                        brush = if (parsedPrescription.value != null) 
                            Brush.linearGradient(listOf(MintGreen, SkyBlue))
                        else 
                            Brush.linearGradient(listOf(CharcoalGray, WarmBrown)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = if (parsedPrescription.value != null) 
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE8F8F5),
                                        Color(0xFFD5F4E6)
                                    )
                                )
                            else 
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF5F5F5),
                                        Color(0xFFE0E0E0)
                                    )
                                ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                Button(
                    onClick = { 
                        parsedPrescription.value?.let { prescription ->
                            scope.launch {
                                try {
                                    val userSettings = settingsManager.getSettings()
                                    val prescriptionId = prescriptionRepository.createPrescriptionWithEvents(
                                        parsedPrescription = prescription,
                                        userSettings = userSettings,
                                        title = "Prescription ${System.currentTimeMillis()}"
                                    )
                                    
                                    Toast.makeText(context, "Prescription saved successfully with ID: $prescriptionId", Toast.LENGTH_LONG).show()
                                    
                                    // Optionally sync to calendar if permissions are available
                                    if (hasCalendarPermission.value) {
                                        try {
                                            val created = createCalendarEventsFromPrescription(context, prescription, userSettings)
                                            Toast.makeText(context, "Also synced $created events to calendar", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.w("ChatScreens", "Failed to sync to calendar: ${e.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error saving prescription: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } ?: run {
                            Toast.makeText(context, "No prescription data available. Please process a prescription first.", Toast.LENGTH_LONG).show()
                        }
                    }, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = parsedPrescription.value != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (parsedPrescription.value != null) 
                            DeepNavy 
                        else 
                            CharcoalGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✅",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Save Prescription", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                    }
                }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBuilderScreen(
    onBack: () -> Unit, 
    onConfirm: (startMillis: Long, earliest: Int, latest: Int, days: Int) -> Unit,
    parsedPrescription: ParsedPrescription? = null
) {
    val startMillis = remember { mutableStateOf(nowAtMidnight()) }
    val earliest = remember { mutableStateOf(8 * 60) }
    val latest = remember { mutableStateOf(20 * 60) }
    val days = remember { mutableStateOf(7) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Pre-populate values from parsed prescription
    LaunchedEffect(parsedPrescription) {
        parsedPrescription?.let { prescription ->
            // Set duration days from prescription
            days.value = prescription.schedule.duration_days
            
            // Set time range based on preferred times
            val schedule = prescription.schedule
            when {
                schedule.preferred_times.contains("morning") && schedule.preferred_times.contains("evening") -> {
                    earliest.value = 8 * 60 // 8:00 AM
                    latest.value = 20 * 60  // 8:00 PM
                }
                schedule.preferred_times.contains("morning") -> {
                    earliest.value = 7 * 60  // 7:00 AM
                    latest.value = 12 * 60   // 12:00 PM
                }
                schedule.preferred_times.contains("afternoon") -> {
                    earliest.value = 12 * 60 // 12:00 PM
                    latest.value = 17 * 60   // 5:00 PM
                }
                schedule.preferred_times.contains("evening") -> {
                    earliest.value = 17 * 60 // 5:00 PM
                    latest.value = 22 * 60   // 10:00 PM
                }
                else -> {
                    // Default to morning if no specific times
                    earliest.value = 8 * 60
                    latest.value = 20 * 60
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Schedule") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Display parsed prescription information
            parsedPrescription?.let { prescription ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Parsed Prescription", style = MaterialTheme.typography.titleMedium)
                        prescription.medications.forEach { med ->
                            Text("• ${med.name}: ${med.dosage} - ${med.frequency}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Duration: ${prescription.schedule.duration_days} days", style = MaterialTheme.typography.bodySmall)
                        Text("Times per day: ${prescription.schedule.times_per_day}", style = MaterialTheme.typography.bodySmall)
                        if (prescription.schedule.with_food) {
                            Text("Take with food", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Start: ${formatDate(startMillis.value)}")
                Button(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = startMillis.value }
                    DatePickerDialog(context, { _, y, m, d ->
                        val c = Calendar.getInstance().apply {
                            set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, d)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        startMillis.value = c.timeInMillis
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text("Change") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Earliest: ${formatMinutes(earliest.value)}")
                Button(onClick = {
                    TimePickerDialog(context, { _, h, m -> earliest.value = h * 60 + m }, earliest.value / 60, earliest.value % 60, true).show()
                }) { Text("Change") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Latest: ${formatMinutes(latest.value)}")
                Button(onClick = {
                    TimePickerDialog(context, { _, h, m -> latest.value = h * 60 + m }, latest.value / 60, latest.value % 60, true).show()
                }) { Text("Change") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Days: ${days.value}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (days.value > 1) days.value -= 1 }) { Text("-") }
                    Button(onClick = { days.value += 1 }) { Text("+") }
                }
            }
            Button(onClick = { onConfirm(startMillis.value, earliest.value, latest.value, days.value) }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirm & Sync")
            }
        }
    }
}

private fun nowAtMidnight(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun formatMinutes(total: Int) = String.format("%02d:%02d", total / 60, total % 60)
private fun formatDate(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}
