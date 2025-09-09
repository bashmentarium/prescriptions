package com.example.whitelabel.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import android.app.DatePickerDialog
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whitelabel.data.SettingsManager
import com.example.whitelabel.data.UserSettings
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.database.entities.ParsedMedicationEntity
import com.example.whitelabel.data.database.entities.PrescriptionEntity
import com.example.whitelabel.data.repository.PrescriptionRepository
import com.example.whitelabel.data.repository.PrescriptionStats
import com.example.whitelabel.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionDetailScreen(
    prescriptionId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val prescriptionRepository = remember { PrescriptionRepository(database) }
    val scope = rememberCoroutineScope()
    
    var prescription by remember { mutableStateOf<PrescriptionEntity?>(null) }
    val events by prescriptionRepository.getEventsByPrescriptionId(prescriptionId).collectAsState(initial = emptyList())
    var stats by remember { mutableStateOf<PrescriptionStats?>(null) }
    
    LaunchedEffect(prescriptionId) {
        prescription = prescriptionRepository.getPrescriptionById(prescriptionId)
        stats = prescriptionRepository.getPrescriptionStats(prescriptionId)
    }
    
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
                            prescription?.title ?: "Prescription Details",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Black,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
            if (prescription == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prescription info card
                    item {
                        PrescriptionInfoCard(
                            prescription = prescription!!,
                            prescriptionRepository = prescriptionRepository,
                            userSettings = remember { SettingsManager(context).getSettings() }
                        )
                    }
                    
                    // Statistics card
                    stats?.let { prescriptionStats ->
                        item {
                            StatisticsCard(stats = prescriptionStats)
                        }
                    }
                    
                    // Events header
                    item {
                        Text(
                            "Medication Events",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = DeepNavy
                        )
                    }
                    
                    // Events list
                    items(events) { event ->
                        EventCard(
                            event = event,
                            onToggleCompleted = { eventId ->
                                scope.launch {
                                    if (event.isCompleted) {
                                        prescriptionRepository.markEventIncomplete(eventId)
                                    } else {
                                        prescriptionRepository.markEventCompleted(eventId)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrescriptionInfoCard(
    prescription: PrescriptionEntity,
    prescriptionRepository: PrescriptionRepository,
    userSettings: UserSettings
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedDuration by remember { mutableStateOf(prescription.durationDays.toString()) }
    var editedFrequency by remember { mutableStateOf(prescription.timesPerDay.toString()) }
    var editedInterval by remember { mutableStateOf(prescription.intervalDays.toString()) }
    var editedStartDate by remember { mutableStateOf(prescription.startDateMillis) }
    var editedMedications by remember { mutableStateOf(prescription.medications.toMutableList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = editedStartDate
        }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                editedStartDate = newCalendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    fun addMedication() {
        editedMedications.add(
            ParsedMedicationEntity(
                prescriptionId = prescription.id,
                name = "",
                dosage = "",
                frequency = "",
                duration = "",
                instructions = ""
            )
        )
    }
    
    fun removeMedication(index: Int) {
        if (editedMedications.size > 1) {
            editedMedications.removeAt(index)
        }
    }
    
    fun updateMedication(index: Int, field: String, value: String) {
        val medication = editedMedications[index]
        editedMedications[index] = when (field) {
            "name" -> medication.copy(name = value)
            "dosage" -> medication.copy(dosage = value)
            "frequency" -> medication.copy(frequency = value)
            "duration" -> medication.copy(duration = value)
            "instructions" -> medication.copy(instructions = value)
            else -> medication
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color(0xFF7F7F7F),
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
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFE5B4),
                            Color(0xFFFFF0D6)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Medication,
                            contentDescription = "Prescription",
                            tint = DeepNavy,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Prescription Information",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = DeepNavy
                        )
                    }
                    
                    // Edit/Save button
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                // Save changes
                                scope.launch {
                                    try {
                                        val updatedPrescription = prescription.copy(
                                            durationDays = editedDuration.toIntOrNull() ?: prescription.durationDays,
                                            timesPerDay = editedFrequency.toIntOrNull() ?: prescription.timesPerDay,
                                            intervalDays = editedInterval.toIntOrNull() ?: prescription.intervalDays,
                                            startDateMillis = editedStartDate,
                                            medications = editedMedications
                                        )
                                        prescriptionRepository.updatePrescriptionAndRecalculateEvents(
                                            updatedPrescription,
                                            userSettings,
                                            preservePastEvents = true
                                        )
                                        isEditing = false
                                    } catch (e: Exception) {
                                        // Handle error - could show a snackbar
                                    }
                                }
                            } else {
                                isEditing = true
                            }
                        }
                    ) {
                        Icon(
                            if (isEditing) Icons.Filled.Save else Icons.Filled.Edit,
                            contentDescription = if (isEditing) "Save changes" else "Edit prescription",
                            tint = DeepNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Medications section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Medications:",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = DeepNavy
                    )
                    
                    if (isEditing) {
                        IconButton(
                            onClick = { addMedication() }
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add medication",
                                tint = DeepNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                if (isEditing) {
                    // Editable medications
                    editedMedications.forEachIndexed { index, medication ->
                        MedicationEditCard(
                            medication = medication,
                            index = index,
                            onUpdate = { field, value -> updateMedication(index, field, value) },
                            onRemove = { removeMedication(index) },
                            canRemove = editedMedications.size > 1
                        )
                    }
                } else {
                    // Display medications
                    prescription.medications.forEach { medication ->
                        Text(
                            "• ${medication.name}: ${medication.dosage} - ${medication.frequency}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
                
                // Editable fields
                if (isEditing) {
                    // Duration field
                    OutlinedTextField(
                        value = editedDuration,
                        onValueChange = { editedDuration = it },
                        label = { Text("Duration (days)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    
                    // Frequency field
                    OutlinedTextField(
                        value = editedFrequency,
                        onValueChange = { editedFrequency = it },
                        label = { Text("Times per day") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    
                    // Interval field
                    OutlinedTextField(
                        value = editedInterval,
                        onValueChange = { editedInterval = it },
                        label = { Text("Interval (days)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        supportingText = { Text("1 = daily, 2 = every 2 days, 3 = every 3 days, etc.") }
                    )
                    
                    // Start date field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Start Date:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = DeepNavy
                        )
                        OutlinedTextField(
                            value = dateFormat.format(Date(editedStartDate)),
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showDatePicker() }
                                ) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = "Select date",
                                        tint = DeepNavy
                                    )
                                }
                            }
                        )
                    }
                } else {
                    // Display mode
                    val intervalText = if (prescription.intervalDays == 1) {
                        "daily"
                    } else {
                        "every ${prescription.intervalDays} days"
                    }
                    
                    Text(
                        "Schedule: ${prescription.timesPerDay} times per day, $intervalText for ${prescription.durationDays} days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7F8C8D)
                    )
                    
                    Text(
                        "Start Date: ${dateFormat.format(Date(prescription.startDateMillis))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                if (prescription.preferredTimes.isNotEmpty()) {
                    Text(
                        "Preferred times: ${prescription.preferredTimes.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                if (prescription.withFood) {
                    Text(
                        "🍽️ Take with food",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(stats: PrescriptionStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(MintGreen, SkyBlue)),
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
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE8F8F5),
                            Color(0xFFD5F4E6)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = "Statistics",
                        tint = DeepNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Progress Statistics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = DeepNavy
                    )
                }
                
                Text(
                    "Total Events: ${stats.totalEvents}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7F8C8D)
                )
                
                Text(
                    "Completed: ${stats.completedEvents}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7F8C8D)
                )
                
                Text(
                    "Completion Rate: ${stats.completionRate}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (stats.completionRate >= 80) MintGreen else VibrantOrange
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: MedicationEventEntity,
    onToggleCompleted: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    val isPast = event.startTimeMillis < System.currentTimeMillis()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (event.isCompleted) MintGreen else if (isPast) BrightCoral else Color(0xFF7F7F7F),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isCompleted) 
                Color(0xFFE8F8F5) 
            else if (isPast) 
                Color(0xFFFFE5E5) 
            else 
                Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = DeepNavy
                )
                
                Text(
                    dateFormat.format(Date(event.startTimeMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7F8C8D)
                )
                
                if (event.description.isNotBlank()) {
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7F8C8D),
                        maxLines = 2
                    )
                }
            }
            
            // Completion toggle
            IconButton(
                onClick = { onToggleCompleted(event.id) }
            ) {
                Icon(
                    if (event.isCompleted) Icons.Filled.Check else Icons.Filled.Schedule,
                    contentDescription = if (event.isCompleted) "Mark incomplete" else "Mark complete",
                    tint = if (event.isCompleted) MintGreen else Color(0xFF7F8C8D),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MedicationEditCard(
    medication: ParsedMedicationEntity,
    index: Int,
    onUpdate: (String, String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Medication ${index + 1}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = DeepNavy
                )
                
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove medication",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = medication.name,
                onValueChange = { onUpdate("name", it) },
                label = { Text("Medication Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = medication.dosage,
                    onValueChange = { onUpdate("dosage", it) },
                    label = { Text("Dosage") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = medication.frequency,
                    onValueChange = { onUpdate("frequency", it) },
                    label = { Text("Frequency") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            OutlinedTextField(
                value = medication.duration,
                onValueChange = { onUpdate("duration", it) },
                label = { Text("Duration") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = medication.instructions,
                onValueChange = { onUpdate("instructions", it) },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }
    }
}
