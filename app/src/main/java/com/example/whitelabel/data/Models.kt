package com.example.whitelabel.data

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
