package com.example.whitelabel.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
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

class RealAnthropicService(private val context: Context) : LlmService {
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
                    You are a medical prescription parser. Extract ONLY the medication information from this prescription text and format it as JSON:
                    
                    {
                        "medications": [
                            {
                                "name": "medication name",
                                "dosage": "dosage amount and form",
                                "frequency": "how often to take (e.g., 'twice daily', 'once a day', '3 times per day')",
                                "duration": "how long to take (e.g., '7 days', '2 weeks', 'until finished')",
                                "instructions": "special instructions including food timing (e.g., 'take with food', 'on empty stomach', 'before meals')"
                            }
                        ]
                    }
                    
                    IMPORTANT: 
                    - Extract ONLY medication information, do not include any schedule or timing information
                    - Each medication may have a different duration - extract the specific duration for each medication individually
                    - Include any food timing instructions in the instructions field (e.g., "take with food", "on empty stomach", "before meals")
                    - Be as specific as possible with frequency (e.g., "twice daily", "once every 8 hours", "3 times per day")
                    - If a medication doesn't specify a duration, use "until finished" or "as needed"
                    
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
                Log.d("AnthropicService", "Starting API call for image: $uri")
                
                // Convert image to base64
                val base64Image = convertImageToBase64(uri)
                if (base64Image == null) {
                    Log.e("AnthropicService", "Failed to convert image to base64")
                    return@withContext LlmResult.Error("Failed to process image")
                }
                
                val prompt = """
                    You are a medical prescription parser. Analyze this prescription image and extract ONLY the medication information, formatting it as JSON:
                    
                    {
                        "medications": [
                            {
                                "name": "medication name",
                                "dosage": "dosage amount and form",
                                "frequency": "how often to take (e.g., 'twice daily', 'once a day', '3 times per day')",
                                "duration": "how long to take (e.g., '7 days', '2 weeks', 'until finished')",
                                "instructions": "special instructions including food timing (e.g., 'take with food', 'on empty stomach', 'before meals')"
                            }
                        ]
                    }
                    
                    IMPORTANT: 
                    - Extract ONLY medication information, do not include any schedule or timing information
                    - Each medication may have a different duration - extract the specific duration for each medication individually
                    - Include any food timing instructions in the instructions field (e.g., "take with food", "on empty stomach", "before meals")
                    - Be as specific as possible with frequency (e.g., "twice daily", "once every 8 hours", "3 times per day")
                    - If a medication doesn't specify a duration, use "until finished" or "as needed"
                    
                    Respond with only the JSON, no additional text.
                """.trimIndent()
                
                val request = createImageRequest(prompt, base64Image)
                Log.d("AnthropicService", "Image request created, making API call...")
                
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
                Log.e("AnthropicService", "Exception during image API call: $errorMsg", e)
                LlmResult.Error("Network error: $errorMsg")
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
    
    private fun createImageRequest(prompt: String, base64Image: String): Request {
        val apiKey = BuildConfig.ANTHROPIC_API_KEY
        Log.d("AnthropicService", "API Key length: ${apiKey.length}")
        
        if (apiKey.isBlank()) {
            throw IllegalStateException("ANTHROPIC_API_KEY not configured. Please set the environment variable or add it to local.properties")
        }
        
        val message = AnthropicMessage(
            role = "user",
            content = listOf(
                AnthropicContent(
                    type = "text", 
                    text = prompt
                ),
                AnthropicContent(
                    type = "image",
                    source = AnthropicSource(
                        type = "base64",
                        media_type = "image/jpeg",
                        data = base64Image
                    )
                )
            )
        )
        
        val request = AnthropicRequest(messages = listOf(message))
        val jsonBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
        
        Log.d("AnthropicService", "Image request body: ${gson.toJson(request)}")
        
        return Request.Builder()
            .url(baseUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody)
            .build()
    }
    
    private fun convertImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("AnthropicService", "Could not open input stream for URI: $uri")
                return null
            }
            
            // Decode the image
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e("AnthropicService", "Could not decode bitmap from URI: $uri")
                return null
            }
            
            // Compress the image to reduce size
            val compressedBitmap = compressBitmap(bitmap)
            
            // Convert to base64
            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            outputStream.close()
            
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            Log.d("AnthropicService", "Image converted to base64, size: ${base64.length} characters")
            
            base64
        } catch (e: Exception) {
            Log.e("AnthropicService", "Error converting image to base64: ${e.message}", e)
            null
        }
    }
    
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val maxWidth = 1024
        val maxHeight = 1024
        
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

