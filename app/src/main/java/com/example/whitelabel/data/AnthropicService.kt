package com.example.whitelabel.data

import android.net.Uri
import android.util.Log
import com.example.whitelabel.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    val model: String = "claude-3-5-haiku-20241022",
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
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AnthropicService", "Starting API call for text: $text")



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
                Log.d("AnthropicService", "Request created, making API call...")
                
                val response = client.newCall(request).execute()
                Log.d("AnthropicService", "Response received: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("AnthropicService", "Response body: $responseBody")
                    
                    if (responseBody.isNullOrBlank()) {
                        Log.e("AnthropicService", "Empty response body")
                        return@withContext LlmResult.Error("Empty response from API")
                    }
                    
                    try {
                        val anthropicResponse = gson.fromJson(responseBody, AnthropicResponse::class.java)
                        val content = anthropicResponse.content.firstOrNull()?.text ?: ""
                        Log.d("AnthropicService", "Parsed content: $content")
                        
                        // Extract JSON from the response
                        val jsonStart = content.indexOf('{')
                        val jsonEnd = content.lastIndexOf('}') + 1
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            val jsonContent = content.substring(jsonStart, jsonEnd)
                            Log.d("AnthropicService", "Extracted JSON: $jsonContent")
                            LlmResult.Success(jsonContent)
                        } else {
                            Log.e("AnthropicService", "No JSON found in response")
                            LlmResult.Error("No structured data found in response")
                        }
                    } catch (e: Exception) {
                        Log.e("AnthropicService", "Error parsing response: ${e.message}", e)
                        LlmResult.Error("Failed to parse API response: ${e.message ?: "Unknown parsing error"}")
                    }
                } else {
                    val errorBody = response.body?.string()
                    Log.e("AnthropicService", "API call failed: ${response.code}, body: $errorBody")
                    LlmResult.Error("API call failed: ${response.code} - ${errorBody ?: "No error details"}")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: e.javaClass.simpleName
                Log.e("AnthropicService", "Exception during API call: $errorMsg", e)
                LlmResult.Error("Network error: $errorMsg")
            }
        }
    }
    
    override suspend fun parseFromImage(uri: Uri): LlmResult {
        return withContext(Dispatchers.IO) {
            try {
                // For image processing, we'd need to convert the image to base64
                // For now, return an error suggesting to use text input
                LlmResult.Error("Image processing not yet implemented. Please type the prescription text.")
            } catch (e: Exception) {
                val errorMsg = e.message ?: e.javaClass.simpleName
                LlmResult.Error("Error processing image: $errorMsg")
            }
        }
    }
    
    private fun createRequest(prompt: String): Request {
        val apiKey = BuildConfig.ANTHROPIC_API_KEY
        Log.d("AnthropicService", "API Key length: ${apiKey.length}")
        
        if (apiKey.isBlank()) {
            throw IllegalStateException("ANTHROPIC_API_KEY not configured. Please set the environment variable or add it to local.properties")
        }
        
        val message = AnthropicMessage(
            role = "user",
            content = listOf(AnthropicContent(type = "text", text = prompt))
        )
        
        val request = AnthropicRequest(messages = listOf(message))
        val jsonBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
        
        Log.d("AnthropicService", "Request body: ${gson.toJson(request)}")
        
        return Request.Builder()
            .url(baseUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody)
            .build()
    }
}

