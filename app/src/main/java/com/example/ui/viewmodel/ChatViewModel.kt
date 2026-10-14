package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.repository.ChatRepository
import com.example.voice.SpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CHAT,
    WRITER,
    HISTORY,
    SETTINGS
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val currentConversationId: String? = null,
    val currentConversationTitle: String = "New Chat",
    val messages: List<ChatMessageEntity> = emptyList(),
    val inputPrompt: String = "",
    val isGenerating: Boolean = false,
    val streamingContent: String = "",
    val errorMessage: String? = null,
    val currentSpeakingMessageId: String? = null,
    // Settings
    val appLanguage: String = "en", // "en", "ur", "roman_ur"
    val themeMode: String = "system", // "system", "dark", "light"
    val selectedModel: String = "gemini-3.5-flash",
    val autoPlayTts: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f
)

class ChatViewModel(
    application: Application,
    private val repository: ChatRepository,
    val speechManager: SpeechManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentJob: Job? = null
    private var messagesCollectJob: Job? = null

    init {
        // Set up SpeechManager speech rate and pitch
        speechManager.setSpeechRate(_uiState.value.speechRate)
        speechManager.setPitch(_uiState.value.speechPitch)
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun setInputPrompt(text: String) {
        _uiState.value = _uiState.value.copy(inputPrompt = text)
    }

    fun setLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(appLanguage = lang)
    }

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setSelectedModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun setAutoPlayTts(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPlayTts = enabled)
    }

    fun setSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
        speechManager.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(speechPitch = pitch)
        speechManager.setPitch(pitch)
    }

    fun startNewChat(initialPrompt: String? = null) {
        messagesCollectJob?.cancel()
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.CHAT,
            currentConversationId = null,
            currentConversationTitle = "New Chat",
            messages = emptyList(),
            streamingContent = "",
            isGenerating = false,
            errorMessage = null,
            inputPrompt = initialPrompt ?: ""
        )
        if (!initialPrompt.isNullOrBlank()) {
            sendMessage(initialPrompt)
        }
    }

    fun selectConversation(id: String) {
        messagesCollectJob?.cancel()
        viewModelScope.launch {
            val conv = repository.getConversation(id)
            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.CHAT,
                currentConversationId = id,
                currentConversationTitle = conv?.title ?: "Chat",
                streamingContent = "",
                isGenerating = false,
                errorMessage = null
            )
            observeMessages(id)
        }
    }

    private fun observeMessages(conversationId: String) {
        messagesCollectJob?.cancel()
        messagesCollectJob = viewModelScope.launch {
            repository.getMessages(conversationId).collect { msgList ->
                _uiState.value = _uiState.value.copy(messages = msgList)
            }
        }
    }

    fun sendMessage(promptText: String? = null) {
        val prompt = (promptText ?: _uiState.value.inputPrompt).trim()
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        // Clear input field immediately
        _uiState.value = _uiState.value.copy(inputPrompt = "", errorMessage = null)

        viewModelScope.launch {
            var conversationId = _uiState.value.currentConversationId
            if (conversationId == null) {
                val newConv = repository.createConversation(
                    title = prompt.take(40),
                    preview = prompt.take(100),
                    language = _uiState.value.appLanguage
                )
                conversationId = newConv.id
                _uiState.value = _uiState.value.copy(
                    currentConversationId = conversationId,
                    currentConversationTitle = newConv.title
                )
                observeMessages(conversationId)
            }

            // Save user message
            val userMsg = ChatMessageEntity(
                conversationId = conversationId,
                role = "user",
                content = prompt
            )
            repository.saveMessage(userMsg)

            // Start streaming AI response
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                streamingContent = ""
            )

            currentJob = launch {
                val responseBuilder = StringBuilder()
                try {
                    repository.streamGeminiResponse(
                        prompt = prompt,
                        history = _uiState.value.messages,
                        language = _uiState.value.appLanguage,
                        model = _uiState.value.selectedModel
                    ).collect { chunk ->
                        responseBuilder.append(chunk)
                        _uiState.value = _uiState.value.copy(
                            streamingContent = responseBuilder.toString()
                        )
                    }

                    val finalResponse = responseBuilder.toString()
                    if (finalResponse.isNotBlank()) {
                        val aiMsg = ChatMessageEntity(
                            conversationId = conversationId,
                            role = "model",
                            content = finalResponse
                        )
                        repository.saveMessage(aiMsg)

                        if (_uiState.value.autoPlayTts) {
                            speakMessage(aiMsg.id, finalResponse)
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = "Response generation stopped: ${e.message}"
                    if (responseBuilder.isNotBlank()) {
                        val partialMsg = ChatMessageEntity(
                            conversationId = conversationId,
                            role = "model",
                            content = responseBuilder.toString()
                        )
                        repository.saveMessage(partialMsg)
                    } else {
                        val aiMsg = ChatMessageEntity(
                            conversationId = conversationId,
                            role = "model",
                            content = errorMsg,
                            isError = true
                        )
                        repository.saveMessage(aiMsg)
                    }
                } finally {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        streamingContent = ""
                    )
                }
            }
        }
    }

    fun stopGeneration() {
        currentJob?.cancel()
        val partial = _uiState.value.streamingContent
        val conversationId = _uiState.value.currentConversationId

        if (partial.isNotBlank() && conversationId != null) {
            viewModelScope.launch {
                val partialMsg = ChatMessageEntity(
                    conversationId = conversationId,
                    role = "model",
                    content = "$partial\n\n*(Generation stopped by user)*"
                )
                repository.saveMessage(partialMsg)
            }
        }

        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            streamingContent = ""
        )
    }

    fun generateImage(prompt: String) {
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            var conversationId = _uiState.value.currentConversationId
            if (conversationId == null) {
                val newConv = repository.createConversation(
                    title = "Image: ${prompt.take(30)}",
                    preview = prompt.take(100),
                    language = _uiState.value.appLanguage
                )
                conversationId = newConv.id
                _uiState.value = _uiState.value.copy(
                    currentConversationId = conversationId,
                    currentConversationTitle = newConv.title,
                    currentScreen = AppScreen.CHAT
                )
                observeMessages(conversationId)
            } else {
                _uiState.value = _uiState.value.copy(currentScreen = AppScreen.CHAT)
            }

            // Save user prompt
            repository.saveMessage(
                ChatMessageEntity(
                    conversationId = conversationId,
                    role = "user",
                    content = "🎨 Generate Image: $prompt"
                )
            )

            _uiState.value = _uiState.value.copy(isGenerating = true, streamingContent = "Generating image with Gemini AI…")

            val result = repository.generateImageWithGemini(prompt)
            result.onSuccess { (desc, base64) ->
                repository.saveMessage(
                    ChatMessageEntity(
                        conversationId = conversationId,
                        role = "model",
                        content = desc,
                        imageUrl = base64
                    )
                )
            }.onFailure { err ->
                repository.saveMessage(
                    ChatMessageEntity(
                        conversationId = conversationId,
                        role = "model",
                        content = "Could not generate image: ${err.message}. Please verify your Gemini API key.",
                        isError = true
                    )
                )
            }

            _uiState.value = _uiState.value.copy(isGenerating = false, streamingContent = "")
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                startNewChat()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            startNewChat()
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Junaid AI", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun speakMessage(messageId: String, text: String) {
        if (_uiState.value.currentSpeakingMessageId == messageId && speechManager.isSpeaking.value) {
            speechManager.stopSpeaking()
            _uiState.value = _uiState.value.copy(currentSpeakingMessageId = null)
        } else {
            _uiState.value = _uiState.value.copy(currentSpeakingMessageId = messageId)
            speechManager.speak(text, _uiState.value.appLanguage)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}

class ChatViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = AppDatabase.getDatabase(application)
        val repository = ChatRepository(database.chatDao())
        val speechManager = SpeechManager(application)
        return ChatViewModel(application, repository, speechManager) as T
    }
}
