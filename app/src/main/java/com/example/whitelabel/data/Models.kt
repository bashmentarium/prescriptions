package com.example.whitelabel.data

enum class FoodTiming {
    BEFORE_MEAL,
    DURING_MEAL,
    AFTER_MEAL,
    NEUTRAL
}

data class Medication(
    val name: String,
    val dosage: String,
    val frequencyPerDay: Int,
    val notes: String = ""
)

data class TreatmentCourse(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val medications: List<Medication>,
    val startDateMillis: Long,
    val earliestMinutes: Int,
    val latestMinutes: Int,
    val durationDays: Int
)

data class ParsedMedication(
    val name: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null
)

data class ParsedSchedule(
    val times_per_day: Int,
    val preferred_times: List<String>,
    val food_timing: FoodTiming,
    val duration_days: Int,
    val start_time_minutes: Int = 480, // Default 8:00 AM
    val end_time_minutes: Int = 1200,  // Default 8:00 PM
    val interval_days: Int = 1         // Default daily (1 = daily, 2 = every 2 days, etc.)
)

data class ParsedPrescription(
    val medications: List<ParsedMedication>,
    val schedule: ParsedSchedule? = null // Made optional since we'll aggregate it from medications
)

// New model for medications-only response from LLM
data class ParsedMedicationsOnly(
    val medications: List<ParsedMedication>
)

sealed interface LlmResult {
    data class Success(val normalizedSchedule: String) : LlmResult
    data class Error(val message: String) : LlmResult
}

interface LlmService {
    suspend fun parseFromText(text: String): LlmResult
    suspend fun parseFromImage(uri: android.net.Uri): LlmResult
}

class FakeLlmService : LlmService {
    override suspend fun parseFromText(text: String): LlmResult =
        LlmResult.Success("Parsed schedule for: $text")

    override suspend fun parseFromImage(uri: android.net.Uri): LlmResult =
        LlmResult.Success("Parsed schedule from image: $uri")
}

// Schedule aggregation utility functions
object ScheduleAggregator {
    
    /**
     * Aggregates schedule information from a list of medications
     * Uses medications as the source of truth for scheduling
     */
    fun aggregateScheduleFromMedications(medications: List<ParsedMedication>): ParsedSchedule {
        if (medications.isEmpty()) {
            return ParsedSchedule(
                times_per_day = 1,
                preferred_times = listOf("morning"),
                food_timing = FoodTiming.NEUTRAL,
                duration_days = 7
            )
        }
        
        // Extract frequency information from medications
        val frequencies = medications.mapNotNull { parseFrequency(it.frequency) }
        val timesPerDay = frequencies.maxOrNull() ?: 1
        
        // Extract duration information from medications
        val durations = medications.mapNotNull { parseDuration(it.duration) }
        val maxDuration = durations.maxOrNull() ?: 7
        
        // Determine preferred times based on frequency
        val preferredTimes = when (timesPerDay) {
            1 -> listOf("morning")
            2 -> listOf("morning", "evening")
            3 -> listOf("morning", "afternoon", "evening")
            else -> listOf("morning", "afternoon", "evening")
        }
        
        // Determine food timing from medication instructions
        val foodTiming = determineFoodTiming(medications)
        
        return ParsedSchedule(
            times_per_day = timesPerDay,
            preferred_times = preferredTimes,
            food_timing = foodTiming,
            duration_days = maxDuration,
            start_time_minutes = 480, // Default 8:00 AM
            end_time_minutes = 1200,  // Default 8:00 PM
            interval_days = 1         // Default daily
        )
    }
    
    private fun parseFrequency(frequency: String): Int? {
        val lowerFreq = frequency.lowercase()
        return when {
            lowerFreq.contains("once") || lowerFreq.contains("1x") -> 1
            lowerFreq.contains("twice") || lowerFreq.contains("2x") || lowerFreq.contains("two") -> 2
            lowerFreq.contains("three") || lowerFreq.contains("3x") || lowerFreq.contains("thrice") -> 3
            lowerFreq.contains("four") || lowerFreq.contains("4x") -> 4
            lowerFreq.contains("daily") || lowerFreq.contains("every day") -> 1
            lowerFreq.contains("bid") || lowerFreq.contains("b.i.d") -> 2
            lowerFreq.contains("tid") || lowerFreq.contains("t.i.d") -> 3
            lowerFreq.contains("qid") || lowerFreq.contains("q.i.d") -> 4
            else -> {
                // Try to extract number from frequency string
                val numberRegex = "\\d+".toRegex()
                numberRegex.find(lowerFreq)?.value?.toIntOrNull()
            }
        }
    }
    
    private fun parseDuration(duration: String): Int? {
        val lowerDur = duration.lowercase()
        return when {
            lowerDur.contains("day") -> {
                val numberRegex = "\\d+".toRegex()
                numberRegex.find(lowerDur)?.value?.toIntOrNull()
            }
            lowerDur.contains("week") -> {
                val numberRegex = "\\d+".toRegex()
                numberRegex.find(lowerDur)?.value?.toIntOrNull()?.times(7)
            }
            lowerDur.contains("month") -> {
                val numberRegex = "\\d+".toRegex()
                numberRegex.find(lowerDur)?.value?.toIntOrNull()?.times(30)
            }
            lowerDur.contains("until finished") || lowerDur.contains("as needed") -> 30 // Default 30 days
            else -> {
                // Try to extract number from duration string
                val numberRegex = "\\d+".toRegex()
                numberRegex.find(lowerDur)?.value?.toIntOrNull()
            }
        }
    }
    
    private fun determineFoodTiming(medications: List<ParsedMedication>): FoodTiming {
        val instructions = medications.mapNotNull { it.instructions }.joinToString(" ").lowercase()
        val dosage = medications.map { it.dosage }.joinToString(" ").lowercase()
        val combined = "$instructions $dosage"
        
        return when {
            combined.contains("before meal") || combined.contains("before food") || 
            combined.contains("on empty stomach") || combined.contains("ac") -> FoodTiming.BEFORE_MEAL
            combined.contains("with meal") || combined.contains("with food") || 
            combined.contains("during meal") || combined.contains("pc") -> FoodTiming.DURING_MEAL
            combined.contains("after meal") || combined.contains("after food") -> FoodTiming.AFTER_MEAL
            else -> FoodTiming.NEUTRAL
        }
    }
}

data class UserSettings(
    val earliestTimeMinutes: Int = 480, // Default 8:00 AM
    val latestTimeMinutes: Int = 1200,  // Default 8:00 PM
    val eventDurationMinutes: Int = 30, // Default 30 minutes
    val defaultCalendarId: Long = 1L,   // Default calendar ID
    val reminderMinutes: Int = 15,      // Default 15 minutes before
    val foodTimingDefault: FoodTiming = FoodTiming.NEUTRAL, // Default neutral food timing
    val preferredTimes: List<String> = listOf("morning", "evening") // Default preferred times
)
