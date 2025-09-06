package com.example.whitelabel.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
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
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.MedicationEventEntity
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
                        PrescriptionInfoCard(prescription = prescription!!)
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
fun PrescriptionInfoCard(prescription: PrescriptionEntity) {
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
                
                // Medications
                Text(
                    "Medications:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = DeepNavy
                )
                prescription.medications.forEach { medication ->
                    Text(
                        "• ${medication.name}: ${medication.dosage} - ${medication.frequency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                // Schedule details
                Text(
                    "Schedule: ${prescription.timesPerDay} times per day for ${prescription.durationDays} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7F8C8D)
                )
                
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
