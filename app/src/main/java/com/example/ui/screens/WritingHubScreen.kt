package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ChatRepository
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

data class WritingTemplate(
    val title: String,
    val description: String,
    val promptPrefix: String,
    val placeholder: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WritingHubScreen(
    uiState: UiState,
    repository: ChatRepository,
    onOpenInChat: (String) -> Unit,
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedTone by remember { mutableStateOf("Professional") }
    var customNotes by remember { mutableStateOf("") }
    var generatedResult by remember { mutableStateOf("") }
    var isGeneratingWriting by remember { mutableStateOf(false) }

    val categories = listOf("Emails", "Messages", "Social Media", "Translation")

    val templates = remember(selectedCategoryIndex) {
        when (selectedCategoryIndex) {
            0 -> listOf(
                WritingTemplate(
                    "Work Follow-up Email",
                    "Polite reminder regarding a pending project, proposal, or meeting",
                    "Write a polite, concise work follow-up email about:",
                    "e.g. Sent client proposal yesterday, haven't heard back yet"
                ),
                WritingTemplate(
                    "Job Application Cover Note",
                    "Compelling introduction highlighting key qualifications",
                    "Write an impressive job application email/cover note for:",
                    "e.g. Applying for Senior Android Engineer role with 4 years Compose experience"
                ),
                WritingTemplate(
                    "Leave Request / Absence",
                    "Formal notice requesting leave or absence from duties",
                    "Write a formal leave application to manager for:",
                    "e.g. 2 days leave due to family wedding / sick leave"
                ),
                WritingTemplate(
                    "Polite Declining / Refusal",
                    "Diplomatic response declining an offer, meeting, or invitation",
                    "Draft a graceful, diplomatic email declining:",
                    "e.g. Cannot attend conference next week due to prior commitments"
                )
            )
            1 -> listOf(
                WritingTemplate(
                    "WhatsApp Announcement",
                    "Clean, formatted update for team, class, or community groups",
                    "Draft a structured WhatsApp broadcast message regarding:",
                    "e.g. Team meeting rescheduled to 3 PM tomorrow, bring slides"
                ),
                WritingTemplate(
                    "Friendly Catch-up",
                    "Warm, thoughtful message to reconnect with a friend or colleague",
                    "Write a warm and natural catch-up message to:",
                    "e.g. Old college friend whom I haven't spoken to in 6 months"
                ),
                WritingTemplate(
                    "Payment / Task Reminder",
                    "Courteous reminder for overdue task or invoice",
                    "Write a courteous reminder message about:",
                    "e.g. Monthly freelance invoice payment due"
                )
            )
            2 -> listOf(
                WritingTemplate(
                    "LinkedIn Thought Leadership",
                    "Engaging post with hook, insights, and call-to-action",
                    "Write an engaging LinkedIn post with hook and hashtags about:",
                    "e.g. Lessons learned launching an AI assistant app"
                ),
                WritingTemplate(
                    "Twitter / X Viral Thread",
                    "Punchy, informative thread with numbered tweets",
                    "Write a high-retention Twitter/X thread about:",
                    "e.g. 5 AI tools every developer must use in 2026"
                ),
                WritingTemplate(
                    "Instagram Caption",
                    "Catchy caption with emojis and relevant hashtags",
                    "Write an aesthetic Instagram caption for:",
                    "e.g. Working late at cozy cafe building new tech project"
                )
            )
            else -> listOf(
                WritingTemplate(
                    "English to Urdu (با محاورہ)",
                    "Natural, polished translation with cultural context",
                    "Translate this English text into elegant, grammatically perfect Urdu:",
                    "e.g. Enter English text to translate..."
                ),
                WritingTemplate(
                    "Urdu to English",
                    "Accurate and natural English translation",
                    "Translate this Urdu / Roman Urdu text into fluent English:",
                    "e.g. Enter Urdu or Roman Urdu text..."
                ),
                WritingTemplate(
                    "Grammar & Tone Polish",
                    "Fix grammatical mistakes and elevate vocabulary",
                    "Improve and polish the grammar, vocabulary, and flow of this text:",
                    "e.g. Enter text that needs proofreading and refinement..."
                )
            )
        }
    }

    var selectedTemplate by remember(selectedCategoryIndex) {
        mutableStateOf(templates.first())
    }

    val tones = listOf("Professional", "Friendly", "Casual", "Urgent", "Diplomatic", "Enthusiastic")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CyanPrimary, VioletAccent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Junaid Writing Assistant",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Craft professional emails, messages, posts, & translations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category Tab Row
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = {
                            selectedCategoryIndex = index
                            selectedTemplate = templates.first()
                        },
                        text = {
                            Text(
                                text = cat,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategoryIndex == index) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Template Selection Chips
        item {
            Text(
                text = "Choose Template",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { tmpl ->
                    FilterChip(
                        selected = selectedTemplate == tmpl,
                        onClick = { selectedTemplate = tmpl },
                        label = { Text(tmpl.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }
        }

        // Tone Selector
        item {
            Text(
                text = "Tone of Voice",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tones.forEach { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { selectedTone = tone },
                        label = { Text(tone, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VioletAccent.copy(alpha = 0.25f),
                            selectedLabelColor = VioletAccent
                        )
                    )
                }
            }
        }

        // Input Details Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectedTemplate.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedTemplate.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = { Text(selectedTemplate.placeholder, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("writing_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val notes = customNotes.ifBlank { selectedTemplate.placeholder }
                            val prompt = "${selectedTemplate.promptPrefix} '$notes'. Tone: $selectedTone. Format clearly with high-quality grammar."
                            isGeneratingWriting = true
                            generatedResult = ""

                            coroutineScope.launch {
                                val result = repository.generateContentOnce(
                                    prompt = prompt,
                                    language = uiState.appLanguage,
                                    model = uiState.selectedModel
                                )
                                isGeneratingWriting = false
                                result.onSuccess { text ->
                                    generatedResult = text
                                }.onFailure { err ->
                                    generatedResult = "Generation error: ${err.message}"
                                }
                            }
                        },
                        enabled = !isGeneratingWriting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_writing_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGeneratingWriting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Drafting with Gemini AI…", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF003544),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Draft with Junaid AI", color = Color(0xFF003544), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Generated Result Preview Card
        if (generatedResult.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Draft",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            )
                            Row {
                                IconButton(
                                    onClick = { onCopy(generatedResult) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy draft",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onSpeak(generatedResult) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Read aloud",
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FormattedText(
                            text = generatedResult,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onOpenInChat(
                                    "Here is my draft:\n\n$generatedResult\n\nPlease help me refine it further or make changes."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continue Refining in Chat",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
