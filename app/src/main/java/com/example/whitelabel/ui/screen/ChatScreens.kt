package com.example.whitelabel.ui.screen

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.content.ContentResolver
import android.database.Cursor
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.example.whitelabel.data.LlmService
import com.example.whitelabel.data.RealAnthropicService
import com.example.whitelabel.data.ParsedPrescription
import com.example.whitelabel.data.CalendarSync
import com.example.whitelabel.data.SettingsManager
import com.example.whitelabel.data.UserSettings
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.util.Calendar
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onOpenChat: (String) -> Unit, onOpenSettings: () -> Unit) {
    val items = remember { mutableStateListOf("Course 1", "Course 2") }
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Treatment Courses") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val new = "Course ${items.size + 1}"
                items.add(new)
                onOpenChat(new)
            }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(items) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenChat(title) }.padding(16.dp)
                )
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
            
            CalendarSync.insertEvent(
                context = context,
                title = title,
                description = description,
                startMillis = start,
                endMillis = end,
                calendarId = userSettings.defaultCalendarId
            )?.let { created++ }
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
    
    // Calendar permissions
    val hasCalendarPermission = remember { mutableStateOf(false) }
    val requester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCalendarPermission.value = result[Manifest.permission.READ_CALENDAR] == true &&
            result[Manifest.permission.WRITE_CALENDAR] == true
    }
    
    // State for image upload functionality
    val selectedImage = remember { mutableStateOf<Uri?>(null) }
    val isImageUploading = remember { mutableStateOf(false) }
    val imageName = remember { mutableStateOf<String?>(null) }

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chat") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                items(messages) { m ->
                    if (m.image != null) {
                        Image(
                            painter = rememberAsyncImagePainter(m.image),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    if (m.text.isNotBlank()) {
                        if (m.isProcessing) {
                            Text("Processing prescription...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(m.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
            
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Image upload button with preview and loading state
                Box {
                    IconButton(
                        onClick = {
                            if (!isImageUploading.value) {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                    ) {
                        when {
                            isImageUploading.value -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            selectedImage.value != null -> {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedImage.value),
                                    contentDescription = "Selected image",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Icon(Icons.Filled.Image, contentDescription = "Upload image")
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
                
                TextField(value = input.value, onValueChange = { input.value = it }, modifier = Modifier.weight(1f))
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
                }) { Text("Send") }
            }
            Button(
                onClick = { 
                    parsedPrescription.value?.let { prescription ->
                        if (hasCalendarPermission.value) {
                            scope.launch {
                                try {
                                    val userSettings = settingsManager.getSettings()
                                    val created = createCalendarEventsFromPrescription(context, prescription, userSettings)
                                    Toast.makeText(context, "$created calendar events created successfully!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error creating calendar events: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // Request calendar permissions
                            requester.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                        }
                    } ?: run {
                        Toast.makeText(context, "No prescription data available. Please process a prescription first.", Toast.LENGTH_LONG).show()
                    }
                }, 
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                enabled = parsedPrescription.value != null
            ) { 
                Text("Set Timing & Approve") 
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
