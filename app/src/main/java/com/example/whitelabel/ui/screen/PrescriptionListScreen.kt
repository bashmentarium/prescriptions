package com.example.whitelabel.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
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
import com.example.whitelabel.data.database.entities.PrescriptionEntity
import com.example.whitelabel.data.repository.PrescriptionRepository
import com.example.whitelabel.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionListScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrescriptionDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val prescriptionRepository = remember { PrescriptionRepository(database) }
    val prescriptions by prescriptionRepository.getAllActivePrescriptions().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
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
                            "My Saved Prescriptions",
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
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Filled.Settings, 
                                contentDescription = "Settings",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (prescriptions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Medication,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "No prescriptions saved yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                        Text(
                            "Process a prescription in the chat to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(prescriptions) { prescription ->
                        PrescriptionCard(
                            prescription = prescription,
                            onOpenDetail = { onOpenPrescriptionDetail(prescription.id) },
                            onDelete = {
                                scope.launch {
                                    prescriptionRepository.deletePrescription(prescription.id)
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
fun PrescriptionCard(
    prescription: PrescriptionEntity,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
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
                            Color(0xFFE8F8F5),
                            Color(0xFFD5F4E6)
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
                        prescription.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = DeepNavy
                    )
                }
                
                // Medication summary
                Text(
                    "Medications: ${prescription.medications.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7F8C8D)
                )
                
                // Schedule info
                Text(
                    "Schedule: ${prescription.timesPerDay} times/day for ${prescription.durationDays} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7F8C8D)
                )
                
                // Preferred times
                if (prescription.preferredTimes.isNotEmpty()) {
                    Text(
                        "Preferred times: ${prescription.preferredTimes.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                // Food timing indicator
                when (prescription.foodTiming) {
                    com.example.whitelabel.data.FoodTiming.BEFORE_MEAL -> {
                        Text(
                            "🍽️ Take before meals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                    com.example.whitelabel.data.FoodTiming.DURING_MEAL -> {
                        Text(
                            "🍽️ Take with meals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                    com.example.whitelabel.data.FoodTiming.AFTER_MEAL -> {
                        Text(
                            "🍽️ Take after meals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                    com.example.whitelabel.data.FoodTiming.NEUTRAL -> {
                        // Don't show anything for neutral timing
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = BrightCoral
                        )
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenDetail,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "View Details",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
