package com.example.whitelabel.data

import android.net.Uri
import com.example.whitelabel.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContent>
)

data class AnthropicContent(
    val type: String,
    val text: String? = null,
    val source: AnthropicSource? = null
)

data class AnthropicSource(
    val type: String,
    val media_type: String,
    val data: String
)

data class AnthropicRequest(
    val model: String = "claude-3-5-sonnet-20241022",
    val max_tokens: Int = 1000,
    val messages: List<AnthropicMessage>
)

data class AnthropicResponse(
    val content: List<AnthropicContent>,
    val usage: AnthropicUsage
)

data class AnthropicUsage(
    val input_tokens: Int,
    val output_tokens: Int
)

class RealAnthropicService : LlmService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseUrl = "https://api.anthropic.com/v1/messages"
    
    override suspend fun parseFromText(text: String): LlmResult {
        return try {
            val prompt = """
                You are a medical prescription parser. Extract the following information from this prescription text and format it as JSON:
                
                {
                    "medications": [
                        {
                            "name": "medication name",
                            "dosage": "dosage amount and form",
                            "frequency": "how often to take",
                            "duration": "how long to take",
                            "instructions": "special instructions"
                        }
                    ],
                    "schedule": {
                        "times_per_day": number,
                        "preferred_times": ["morning", "afternoon", "evening"],
                        "with_food": boolean,
                        "duration_days": number
                    }
                }
                
                Prescription text: $text
                
                Respond with only the JSON, no additional text.
            """.trimIndent()
            
            val request = createRequest(prompt)
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val anthropicResponse = gson.fromJson(responseBody, AnthropicResponse::class.java)
                val content = anthropicResponse.content.firstOrNull()?.text ?: ""
                
                // Extract JSON from the response
                val jsonStart = content.indexOf('{')
                val jsonEnd = content.lastIndexOf('}') + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val jsonContent = content.substring(jsonStart, jsonEnd)
                    LlmResult.Success(jsonContent)
                } else {
                    LlmResult.Error("Failed to parse structured response")
                }
            } else {
                LlmResult.Error("API call failed: ${response.code}")
            }
        } catch (e: Exception) {
            LlmResult.Error("Error: ${e.message}")
        }
    }
    
    override suspend fun parseFromImage(uri: Uri): LlmResult {
        return try {
            // For image processing, we'd need to convert the image to base64
            // For now, return an error suggesting to use text input
            LlmResult.Error("Image processing not yet implemented. Please type the prescription text.")
        } catch (e: Exception) {
            LlmResult.Error("Error processing image: ${e.message}")
        }
    }
    
    private fun createRequest(prompt: String): Request {
        val apiKey = BuildConfig.ANTHROPIC_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("ANTHROPIC_API_KEY not configured. Please set the environment variable or add it to local.properties")
        }
        
        val message = AnthropicMessage(
            role = "user",
            content = listOf(AnthropicContent(type = "text", text = prompt))
        )
        
        val request = AnthropicRequest(messages = listOf(message))
        val jsonBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
        
        return Request.Builder()
            .url(baseUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody)
            .build()
    }
}

// Extension function to convert Uri to base64 (for future image support)
suspend fun Uri.toBase64(): String {
    // Implementation would read the image file and convert to base64
    // For now, return empty string
    return ""
}
