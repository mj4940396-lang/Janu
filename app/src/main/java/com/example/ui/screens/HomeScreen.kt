package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ConversationEntity
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: UiState,
    conversations: List<ConversationEntity>,
    onStartNewChat: (String?) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onSetLanguage: (String) -> Unit,
    onRequestVoice: () -> Unit,
    onOpenImageDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var homePromptText by remember { mutableStateOf("") }

    val welcomeHeadline = when (uiState.appLanguage) {
        "ur" -> "خوش آمدید! میں جنید اے آئی اسسٹنٹ ہوں۔"
        "roman_ur" -> "Hello! Main Junaid AI Assistant hoon."
        else -> "Hello! I'm Junaid AI Assistant."
    }

    val welcomeSubheadline = when (uiState.appLanguage) {
        "ur" -> "آج میں آپ کی کیا مدد کر سکتا ہوں؟"
        "roman_ur" -> "Aaj main aap ki kya madad kar sakta hoon?"
        else -> "How can I help you today?"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Top Bar with Logo & Language Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(CyanPrimary, VioletAccent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.junaid_logo),
                            contentDescription = "Junaid AI Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Junaid AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gemini 3.5 Flash Online",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = EmeraldSuccess
                                )
                            )
                        }
                    }
                }

                // Language quick chip selector
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("en" to "EN", "ur" to "اردو", "roman_ur" to "Roman").forEach { (code, label) ->
                        val isSelected = uiState.appLanguage == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetLanguage(code) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }
        }

        // Hero Greeting Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(CyanPrimary.copy(alpha = 0.5f), VioletAccent.copy(alpha = 0.5f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Intelligent AI Companion",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = welcomeHeadline,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = welcomeSubheadline,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Direct Quick Message Input
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = homePromptText,
                        onValueChange = { homePromptText = it },
                        placeholder = {
                            Text(
                                text = when (uiState.appLanguage) {
                                    "ur" -> "کوئی بھی سوال یہاں لکھیے…"
                                    "roman_ur" -> "Kuch bhi puchiye ya mic dabayein…"
                                    else -> "Ask anything or tap mic…"
                                },
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_input_field"),
                        singleLine = false,
                        maxLines = 3
                    )

                    IconButton(
                        onClick = onRequestVoice,
                        modifier = Modifier.testTag("home_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = CyanPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (homePromptText.isNotBlank()) {
                                val text = homePromptText
                                homePromptText = ""
                                onStartNewChat(text)
                            }
                        },
                        enabled = homePromptText.isNotBlank(),
                        modifier = Modifier.testTag("home_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (homePromptText.isNotBlank()) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Quick Actions Grid Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap to launch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The 6 Quick Action Cards (2-column layout)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Ask Anything & Translate Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Ask Anything",
                        description = "Instant answers, science, math, or coding",
                        icon = Icons.Default.QuestionAnswer,
                        iconColor = CyanPrimary,
                        gradientColors = listOf(CyanPrimary, CyanDark),
                        onClick = { onStartNewChat(null) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Translate Text",
                        description = "English, Urdu, Roman Urdu & 50+ languages",
                        icon = Icons.Default.Translate,
                        iconColor = VioletAccent,
                        gradientColors = listOf(VioletAccent, Color(0xFF512DA8)),
                        onClick = {
                            onStartNewChat(
                                "Please help me translate text. Provide translations between English and Urdu with explanations and examples."
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Write a Message & Generate an Image
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Write a Message",
                        description = "Emails, letters, professional replies & captions",
                        icon = Icons.Default.Description,
                        iconColor = EmeraldSuccess,
                        gradientColors = listOf(EmeraldSuccess, Color(0xFF00796B)),
                        onClick = { onNavigate(AppScreen.WRITER) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Generate an Image",
                        description = "Visual concepts powered by Gemini AI",
                        icon = Icons.Default.Image,
                        iconColor = Color(0xFFFF6D00),
                        gradientColors = listOf(Color(0xFFFF9100), Color(0xFFDD2C00)),
                        onClick = onOpenImageDialog,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Start Voice Chat & Explain a Topic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Start Voice Chat",
                        description = "Hands-free speech-to-text and voice answers",
                        icon = Icons.Default.Mic,
                        iconColor = Color(0xFF00B0FF),
                        gradientColors = listOf(Color(0xFF00B0FF), Color(0xFF2979FF)),
                        onClick = onRequestVoice,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Explain a Topic",
                        description = "Breakdown complex ideas into simple terms",
                        icon = Icons.Default.Lightbulb,
                        iconColor = Color(0xFFFFD600),
                        gradientColors = listOf(Color(0xFFFFD600), Color(0xFFFF6D00)),
                        onClick = {
                            onStartNewChat(
                                "Explain Quantum Computing to me as if I'm 12 years old, using a fun and clear analogy."
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Conversations Section
        if (conversations.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Conversations",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = CyanPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.clickable { onNavigate(AppScreen.HISTORY) }
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(conversations.take(6)) { conv ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectConversation(conv.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = conv.preview.ifBlank { "Conversation history" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
