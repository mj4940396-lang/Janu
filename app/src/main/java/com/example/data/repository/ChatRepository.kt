package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiImageConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GeminiResponse
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ChatRepository(private val chatDao: ChatDao) {

    val allConversations: Flow<List<ConversationEntity>> = chatDao.getAllConversations()

    fun getMessages(conversationId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForConversation(conversationId)
    }

    suspend fun getConversation(id: String): ConversationEntity? {
        return chatDao.getConversationById(id)
    }

    suspend fun createConversation(
        title: String,
        preview: String = "",
        language: String = "en"
    ): ConversationEntity {
        val conv = ConversationEntity(
            title = title.take(60),
            preview = preview.take(120),
            language = language
        )
        chatDao.insertConversation(conv)
        return conv
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        chatDao.updateConversation(conversation)
    }

    suspend fun saveMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
        // Also update conversation timestamp and preview
        chatDao.getConversationById(message.conversationId)?.let { conv ->
            chatDao.updateConversation(
                conv.copy(
                    updatedAt = System.currentTimeMillis(),
                    preview = message.content.take(120)
                )
            )
        }
    }

    suspend fun deleteConversation(id: String) {
        chatDao.deleteMessagesForConversation(id)
        chatDao.deleteConversation(id)
    }

    suspend fun clearAll() {
        chatDao.clearAllMessages()
        chatDao.clearAllConversations()
    }

    private fun getSystemInstruction(language: String): GeminiContent {
        val instructionText = when (language) {
            "ur" -> "آپ جنید اے آئی اسسٹنٹ (Junaid AI Assistant) ہیں، ایک انتہائی ذہین، باادب، دوستانہ اور باصلاحیت اے آئی معاون۔ صارفین کے ہر سوال کا مفصل، باوقار اور شستہ اردو میں جواب دیجئے۔ ریاضی، سائنس، کوڈنگ، شاعری، ترجمہ اور عام معلومات میں کمال مہارت سے رہنمائی فرمائیے۔"
            "roman_ur" -> "Aap Junaid AI Assistant hain, ek bohot hi intelligent, friendly aur polite AI assistant. Aap Roman Urdu (English alphabet me likhi hui Urdu) me aasan, saaf aur natural tareeqay se user ki mukammal rehnumai karte hain. Coding, writing, translations aur general questions ka behtareen jawab dete hain."
            else -> "You are Junaid AI Assistant, an advanced, highly intelligent, friendly, and articulate AI assistant. Provide helpful, accurate, well-structured, and polite responses. Use formatted markdown, bullet points, and code blocks where helpful."
        }
        return GeminiContent(
            parts = listOf(GeminiPart(text = instructionText))
        )
    }

    fun streamGeminiResponse(
        prompt: String,
        history: List<ChatMessageEntity>,
        language: String = "en",
        model: String = "gemini-3.5-flash"
    ): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit("API Key is not configured. Please add your GEMINI_API_KEY in the Secrets panel in AI Studio to enable real AI responses.")
            return@flow
        }

        // Build contents from previous messages
        val contents = mutableListOf<GeminiContent>()
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.role == "user") "user" else "model"
            if (msg.content.isNotBlank() && !msg.isError) {
                contents.add(
                    GeminiContent(
                        role = role,
                        parts = listOf(GeminiPart(text = msg.content))
                    )
                )
            }
        }
        // Add current prompt
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = prompt))
            )
        )

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = getSystemInstruction(language),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            )
        )

        val responseBody = try {
            RetrofitClient.service.streamGenerateContent(model, apiKey, request)
        } catch (e: Exception) {
            emit("Error connecting to Gemini API: ${e.localizedMessage ?: e.message}. Please check your internet connection or API quota.")
            return@flow
        }

        val adapter = RetrofitClient.moshi.adapter(GeminiResponse::class.java)

        BufferedReader(InputStreamReader(responseBody.byteStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val raw = line?.trim() ?: continue
                if (!raw.startsWith("data:")) continue
                val json = raw.removePrefix("data:").trim()
                if (json.isEmpty() || json == "[DONE]") continue
                try {
                    val geminiResponse = adapter.fromJson(json)
                    val textChunk = geminiResponse?.candidates
                        ?.firstOrNull()?.content?.parts
                        ?.firstOrNull()?.text
                    if (!textChunk.isNullOrEmpty()) {
                        emit(textChunk)
                    }
                } catch (_: Exception) {
                    // Skip malformed chunk
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateContentOnce(
        prompt: String,
        language: String = "en",
        model: String = "gemini-3.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("API Key is not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel.")
            )
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = getSystemInstruction(language),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            val error = response.error
            if (error != null) {
                return@withContext Result.failure(Exception(error.message ?: "Gemini API error"))
            }
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("No response received from Gemini."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateImageWithGemini(
        prompt: String
    ): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("API Key is not configured. Please add your GEMINI_API_KEY in Secrets.")
            )
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE"),
                imageConfig = GeminiImageConfig(aspectRatio = "1:1", imageSize = "1K")
            )
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-2.5-flash-image", apiKey, request)
            var foundImageBase64: String? = null
            var description = ""

            response.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
                if (part.inlineData != null) {
                    foundImageBase64 = part.inlineData.data
                }
                if (!part.text.isNullOrBlank()) {
                    description += part.text + " "
                }
            }

            if (foundImageBase64 != null) {
                Result.success(Pair(description.ifBlank { "Generated image for: $prompt" }, foundImageBase64))
            } else if (description.isNotBlank()) {
                Result.success(Pair(description, null))
            } else {
                Result.failure(Exception("Could not generate image. Please try a different prompt."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
