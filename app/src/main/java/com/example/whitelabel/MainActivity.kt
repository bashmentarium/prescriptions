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
import com.example.whitelabel.ui.screen.ChatDetailScreen
import com.example.whitelabel.ui.screen.ChatListScreen
import com.example.whitelabel.ui.screen.ScheduleBuilderScreen
import com.example.whitelabel.ui.theme.WhitelabelTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhitelabelTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(onInsertEvents = { startMillis, earliest, days ->
                        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
                        var created = 0
                        repeat(days) { dayIndex ->
                            val eventCal = (cal.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_YEAR, dayIndex)
                                set(Calendar.HOUR_OF_DAY, earliest / 60)
                                set(Calendar.MINUTE, earliest % 60)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val start = eventCal.timeInMillis
                            val end = start + 30 * 60 * 1000
                            CalendarSync.insertEvent(
                                context = this,
                                title = "Medication",
                                description = "Take prescribed medications",
                                startMillis = start,
                                endMillis = end
                            )?.let { created++ }
                        }
                        Toast.makeText(this, "$created events added to Calendar", Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
    }
}

@Composable
fun AppNav(onInsertEvents: (startMillis: Long, earliestMinutes: Int, days: Int) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chats") {
        composable("chats") {
            ChatListScreen(onOpenChat = { id -> navController.navigate("chat/$id") })
        }
        composable("chat/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            ChatDetailScreen(onBack = { navController.popBackStack() }, onOpenSchedule = { navController.navigate("schedule") })
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
            ScheduleBuilderScreen(onBack = { navController.popBackStack() }, onConfirm = { start, earliest, _, days ->
                if (hasCalendarPermission.value) onInsertEvents(start, earliest, days)
                navController.popBackStack()
            })
        }
    }
}