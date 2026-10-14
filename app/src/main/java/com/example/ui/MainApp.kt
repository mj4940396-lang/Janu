package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceListeningOverlay
import com.example.ui.screens.WritingHubScreen
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun MainApp(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()

    val isListening by viewModel.speechManager.isListening.collectAsState()
    val recognizedText by viewModel.speechManager.recognizedText.collectAsState()
    val rmsLevel by viewModel.speechManager.rmsLevel.collectAsState()
    val voiceError by viewModel.speechManager.errorMessage.collectAsState()

    var showImagePromptDialog by remember { mutableStateOf(false) }
    var imagePromptInput by remember { mutableStateOf("") }

    // Theme calculation
    val darkTheme = when (uiState.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    // Urdu RTL support
    val layoutDirection = if (uiState.appLanguage == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr

    // Permission launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.speechManager.startListening(uiState.appLanguage) { spokenText ->
                if (spokenText.isNotBlank()) {
                    viewModel.startNewChat(spokenText)
                }
            }
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice input.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestVoice() {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.speechManager.startListening(uiState.appLanguage) { spokenText ->
                if (spokenText.isNotBlank()) {
                    viewModel.startNewChat(spokenText)
                }
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Show voice errors if any
    voiceError?.let { err ->
        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        viewModel.speechManager.clearError()
    }

    // Handle Back Press to return to Home
    if (uiState.currentScreen != AppScreen.HOME) {
        BackHandler {
            viewModel.navigateTo(AppScreen.HOME)
        }
    }

    // Repository reference for writing hub
    val appDatabase = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ChatRepository(appDatabase.chatDao()) }

    MyApplicationTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        // Bottom Navigation for phones
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.HOME,
                                onClick = { viewModel.navigateTo(AppScreen.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.CHAT,
                                onClick = { viewModel.navigateTo(AppScreen.CHAT) },
                                icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                                label = { Text("Chat", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.WRITER,
                                onClick = { viewModel.navigateTo(AppScreen.WRITER) },
                                icon = { Icon(Icons.Default.Edit, contentDescription = "Writing Hub") },
                                label = { Text("Writing", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.HISTORY,
                                onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.SETTINGS,
                                onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.currentScreen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    uiState = uiState,
                                    conversations = conversations,
                                    onStartNewChat = { prompt ->
                                        viewModel.startNewChat(prompt)
                                    },
                                    onSelectConversation = { convId ->
                                        viewModel.selectConversation(convId)
                                    },
                                    onNavigate = { screen ->
                                        viewModel.navigateTo(screen)
                                    },
                                    onSetLanguage = { lang ->
                                        viewModel.setLanguage(lang)
                                    },
                                    onRequestVoice = { requestVoice() },
                                    onOpenImageDialog = { showImagePromptDialog = true }
                                )
                            }
                            AppScreen.CHAT -> {
                                ChatScreen(
                                    uiState = uiState,
                                    onBack = { viewModel.navigateTo(AppScreen.HOME) },
                                    onSendMessage = { prompt ->
                                        viewModel.sendMessage(prompt)
                                    },
                                    onStopGeneration = {
                                        viewModel.stopGeneration()
                                    },
                                    onStartNewChat = {
                                        viewModel.startNewChat()
                                    },
                                    onInputChanged = { text ->
                                        viewModel.setInputPrompt(text)
                                    },
                                    onCopyMessage = { text ->
                                        viewModel.copyToClipboard(text)
                                    },
                                    onSpeakMessage = { msgId, text ->
                                        viewModel.speakMessage(msgId, text)
                                    },
                                    onRequestVoice = { requestVoice() },
                                    onOpenImageDialog = { showImagePromptDialog = true }
                                )
                            }
                            AppScreen.WRITER -> {
                                WritingHubScreen(
                                    uiState = uiState,
                                    repository = repository,
                                    onOpenInChat = { prompt ->
                                        viewModel.startNewChat(prompt)
                                    },
                                    onCopy = { text ->
                                        viewModel.copyToClipboard(text)
                                    },
                                    onSpeak = { text ->
                                        viewModel.speechManager.speak(text, uiState.appLanguage)
                                    }
                                )
                            }
                            AppScreen.HISTORY -> {
                                HistoryScreen(
                                    conversations = conversations,
                                    onSelectConversation = { convId ->
                                        viewModel.selectConversation(convId)
                                    },
                                    onDeleteConversation = { convId ->
                                        viewModel.deleteConversation(convId)
                                    },
                                    onClearAllHistory = {
                                        viewModel.clearAllHistory()
                                    },
                                    onStartNewChat = {
                                        viewModel.startNewChat()
                                    }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    uiState = uiState,
                                    onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                                    onSetLanguage = { lang -> viewModel.setLanguage(lang) },
                                    onSetModel = { model -> viewModel.setSelectedModel(model) },
                                    onSetAutoPlayTts = { enabled -> viewModel.setAutoPlayTts(enabled) },
                                    onSetSpeechRate = { rate -> viewModel.setSpeechRate(rate) },
                                    onSetSpeechPitch = { pitch -> viewModel.setSpeechPitch(pitch) },
                                    onTestVoice = {
                                        val testPhrase = when (uiState.appLanguage) {
                                            "ur" -> "السلام علیکم! میں جنید اے آئی اسسٹنٹ ہوں۔ آپ کی خدمت کے لیے حاضر ہوں۔"
                                            "roman_ur" -> "Hello! Main Junaid AI Assistant hoon. Main aapki madad ke liye tayyar hoon."
                                            else -> "Hello! I am Junaid AI Assistant. How can I help you today?"
                                        }
                                        viewModel.speechManager.speak(testPhrase, uiState.appLanguage)
                                    },
                                    onClearAllHistory = { viewModel.clearAllHistory() }
                                )
                            }
                        }

                        // Floating Voice Listening Overlay
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                VoiceListeningOverlay(
                                    isListening = isListening,
                                    rmsLevel = rmsLevel,
                                    partialText = recognizedText,
                                    onStop = {
                                        viewModel.speechManager.stopListening()
                                        if (recognizedText.isNotBlank()) {
                                            viewModel.startNewChat(recognizedText)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Image Prompt Creation Dialog
                if (showImagePromptDialog) {
                    AlertDialog(
                        onDismissRequest = { showImagePromptDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Image with AI", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Describe the image you want Junaid AI to generate using Gemini:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = imagePromptInput,
                                    onValueChange = { imagePromptInput = it },
                                    placeholder = {
                                        Text("e.g. A futuristic cyberpunk city in neon lights with flying cars", fontSize = 13.sp)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val prompt = imagePromptInput.trim()
                                    if (prompt.isNotBlank()) {
                                        showImagePromptDialog = false
                                        imagePromptInput = ""
                                        viewModel.generateImage(prompt)
                                    }
                                },
                                enabled = imagePromptInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                            ) {
                                Text("Generate", color = Color(0xFF003544), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImagePromptDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
