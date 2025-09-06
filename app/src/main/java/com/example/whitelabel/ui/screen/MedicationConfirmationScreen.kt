package com.example.whitelabel.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.whitelabel.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationConfirmationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val prescriptionRepository = remember { PrescriptionRepository(database) }
    val scope = rememberCoroutineScope()
    
    var prescriptions by remember { mutableStateOf<List<PrescriptionEntity>>(emptyList()) }
    var upcomingEvents by remember { mutableStateOf<List<MedicationEventEntity>>(emptyList()) }
    var completedEvents by remember { mutableStateOf<List<MedicationEventEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load data on screen initialization
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val allPrescriptions = prescriptionRepository.getAllPrescriptions()
                prescriptions = allPrescriptions
                
                val currentTime = System.currentTimeMillis()
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val todayEnd = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                // Get today's events
                val todayEvents = prescriptionRepository.getEventsInTimeRangeSuspend(todayStart, todayEnd)
                upcomingEvents = todayEvents.filter { !it.isCompleted }
                completedEvents = todayEvents.filter { it.isCompleted }
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
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
                            "💊 Medication Confirmation",
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = VibrantOrange,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Card
                    item {
                        ConfirmationHeaderCard()
                    }
                    
                    // Upcoming Medications
                    if (upcomingEvents.isNotEmpty()) {
                        item {
                            Text(
                                "⏰ Upcoming Medications",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = DeepNavy,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        
                        items(upcomingEvents) { event ->
                            MedicationConfirmationCard(
                                event = event,
                                isCompleted = false,
                                onConfirmIntake = { eventId ->
                                    scope.launch {
                                        prescriptionRepository.markEventCompleted(eventId)
                                        // Refresh the data
                                        val currentTime = System.currentTimeMillis()
                                        val todayStart = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        
                                        val todayEnd = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }.timeInMillis
                                        
                                        val todayEvents = prescriptionRepository.getEventsInTimeRangeSuspend(todayStart, todayEnd)
                                        upcomingEvents = todayEvents.filter { !it.isCompleted }
                                        completedEvents = todayEvents.filter { it.isCompleted }
                                    }
                                }
                            )
                        }
                    }
                    
                    // Completed Medications
                    if (completedEvents.isNotEmpty()) {
                        item {
                            Text(
                                "✅ Completed Today",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = DeepNavy,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
                            )
                        }
                        
                        items(completedEvents) { event ->
                            MedicationConfirmationCard(
                                event = event,
                                isCompleted = true,
                                onConfirmIntake = { eventId ->
                                    scope.launch {
                                        prescriptionRepository.markEventIncomplete(eventId)
                                        // Refresh the data
                                        val currentTime = System.currentTimeMillis()
                                        val todayStart = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        
                                        val todayEnd = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }.timeInMillis
                                        
                                        val todayEvents = prescriptionRepository.getEventsInTimeRangeSuspend(todayStart, todayEnd)
                                        upcomingEvents = todayEvents.filter { !it.isCompleted }
                                        completedEvents = todayEvents.filter { it.isCompleted }
                                    }
                                }
                            )
                        }
                    }
                    
                    // Empty state
                    if (upcomingEvents.isEmpty() && completedEvents.isEmpty()) {
                        item {
                            EmptyStateCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmationHeaderCard() {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(MintGreen, GoldenYellow)
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
                        text = "💊",
                        fontSize = 28.sp
                    )
                }
                Column {
                    Text(
                        "Medication Intake Confirmation",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = DeepNavy
                    )
                    Text(
                        "Confirm your medication intake for today",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationConfirmationCard(
    event: MedicationEventEntity,
    isCompleted: Boolean,
    onConfirmIntake: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val completedDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isCompleted) MintGreen else Color(0xFF7F7F7F),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isCompleted) {
                            listOf(
                                Color(0xFFE8F8F5),
                                Color(0xFFD5F4E6)
                            )
                        } else {
                            listOf(
                                Color(0xFFFFE5E5),
                                Color(0xFFFFF0F0)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Medication,
                            contentDescription = null,
                            tint = if (isCompleted) MintGreen else VibrantOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = DeepNavy
                        )
                    }
                    
                    Text(
                        "⏰ ${dateFormat.format(Date(event.startTimeMillis))}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF7F8C8D)
                    )
                    
                    if (event.description.isNotBlank()) {
                        Text(
                            event.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF7F8C8D),
                            maxLines = 2
                        )
                    }
                    
                    if (isCompleted && event.completedAt != null) {
                        Text(
                            "✅ Completed at ${completedDateFormat.format(Date(event.completedAt))}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MintGreen
                        )
                    }
                }
                
                // Confirmation button
                Button(
                    onClick = { onConfirmIntake(event.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Color(0xFFBDC3C7) else MintGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        if (isCompleted) Icons.Filled.Check else Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isCompleted) "Undo" else "Confirm",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
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
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "📅",
                    fontSize = 48.sp
                )
                Text(
                    "No medications scheduled for today",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = DeepNavy
                )
                Text(
                    "Check your prescriptions or schedule new medications",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF7F8C8D)
                )
            }
        }
    }
}
