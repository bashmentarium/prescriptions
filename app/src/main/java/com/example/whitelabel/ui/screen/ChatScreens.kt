package com.example.whitelabel.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.whitelabel.data.LlmService
import com.example.whitelabel.data.FakeLlmService
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onOpenChat: (String) -> Unit) {
    val items = remember { mutableStateListOf("Course 1", "Course 2") }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Treatment Courses") }) },
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

data class ChatMessage(val fromUser: Boolean, val text: String = "", val image: Uri? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(onOpenSchedule: () -> Unit) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val input = remember { mutableStateOf("") }
    val llm: LlmService = remember { FakeLlmService() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) messages.add(ChatMessage(true, image = uri))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chat") }) }
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
                    if (m.text.isNotBlank()) Text(m.text)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Filled.Image, contentDescription = null) }
                TextField(value = input.value, onValueChange = { input.value = it }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    if (input.value.isBlank()) return@Button
                    messages.add(ChatMessage(true, text = input.value))
                    messages.add(ChatMessage(false, text = "Proposed schedule parsed. Continue to set timing."))
                    input.value = ""
                }) { Text("Send") }
            }
            Button(onClick = onOpenSchedule, modifier = Modifier.padding(12.dp).fillMaxWidth()) { Text("Set Timing & Approve") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBuilderScreen(onConfirm: (startMillis: Long, earliest: Int, latest: Int, days: Int) -> Unit) {
    val startMillis = remember { mutableStateOf(nowAtMidnight()) }
    val earliest = remember { mutableStateOf(8 * 60) }
    val latest = remember { mutableStateOf(20 * 60) }
    val days = remember { mutableStateOf(7) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Schedule") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
