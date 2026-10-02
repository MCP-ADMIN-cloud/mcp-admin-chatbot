package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.db.entity.ChatMessageEntity
import com.example.data.llm.DEFAULT_MODEL_NAME
import com.example.data.llm.DEFAULT_PROVIDER_ID
import com.example.data.llm.LlmProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttachmentInfo
import com.example.ui.viewmodel.MainViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToMcpDashboard: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToToolsInspector: () -> Unit
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val inputText by viewModel.textInput.collectAsState()
    val pendingAttachment by viewModel.pendingAttachment.collectAsState()
    val activeContextTurns by viewModel.activeContextTurns.collectAsState()
    val maxContextTokens by viewModel.maxContextTokens.collectAsState()
    val providerKeys by viewModel.providerKeys.collectAsState()
    val fetchedModels by viewModel.fetchedModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()

    var showModelSelectorModal by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember { mutableStateOf(false) }
    var showContextDialog by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val totalEstimatedTokens = remember(messages) {
        messages.sumOf { it.tokensCount }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier
                                .clickable { showModelSelectorModal = true }
                                .testTag("select_model_header_chip")
                        ) {
                            Text(
                                text = currentSession?.title ?: "MCP Chatbot",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                ),
                                maxLines = 1
                            )

                            // Interactive Model Picker Chip
                            val currentProviderId = currentSession?.providerId ?: "gemini"
                            val isCurrentConfigured = remember(currentProviderId, providerKeys) {
                                viewModel.isProviderConfigured(currentProviderId)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCurrentConfigured) Color(0xFFE0E7FF) else Color(0xFFFEE2E2),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrentConfigured) Emerald600 else Rose600)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCurrentConfigured) {
                                            "${currentProviderId.uppercase()} • ${currentSession?.modelName ?: "flash"}"
                                        } else {
                                            "${currentProviderId.uppercase()} • NOT CONFIGURED ⚠️"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentConfigured) Indigo900 else Color(0xFF991B1B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Model",
                                        tint = if (isCurrentConfigured) Indigo600 else Color(0xFFDC2626),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("open_chat_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Chat Menu", tint = Slate800)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onNavigateToMcpDashboard,
                            modifier = Modifier.testTag("nav_mcp_dashboard_button")
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = "MCP Dashboard", tint = Indigo600)
                        }
                        IconButton(
                            onClick = onNavigateToProviders,
                            modifier = Modifier.testTag("nav_providers_button")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = "Providers", tint = Sky500)
                        }
                        IconButton(
                            onClick = onNavigateToToolsInspector,
                            modifier = Modifier.testTag("nav_tools_inspector_button")
                        ) {
                            Icon(Icons.Default.Construction, contentDescription = "Tools Inspector", tint = Violet600)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // Context Window Gauge Bar
                ContextGaugeBar(
                    tokensUsed = totalEstimatedTokens,
                    maxContextTokens = maxContextTokens,
                    contextTurnsLimit = activeContextTurns,
                    onOpenAdjustDialog = { showContextDialog = true },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Divider(color = Slate200, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val isConfigured = remember(currentSession, providerKeys) {
                    viewModel.isProviderConfigured(currentSession?.providerId ?: "gemini")
                }
                if (!isConfigured) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("warning_configure_llm_box"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "AI Provider Not Configured",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                    )
                                    Text(
                                        text = "Add an API key to enable live cloud capabilities.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB91C1C), fontSize = 11.sp)
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToProviders,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Configure", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    }
                }

                if (pendingAttachment != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0E7FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo600.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (pendingAttachment!!.type) {
                                        "IMAGE" -> Icons.Default.Image
                                        "VIDEO" -> Icons.Default.Videocam
                                        "AUDIO" -> Icons.Default.Mic
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = Indigo600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pendingAttachment!!.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo900
                                    )
                                )
                            }

                            IconButton(
                                onClick = { viewModel.pendingAttachment.value = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Attachment", tint = Rose600)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("attach_media_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach Media", tint = Indigo600)
                    }

                    IconButton(
                        onClick = { showVoiceRecorder = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("voice_input_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Rose600)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.textInput.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        placeholder = { Text("Chat with AI & MCP tools...", style = MaterialTheme.typography.bodyMedium.copy(color = Slate600)) },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Indigo600,
                            unfocusedBorderColor = Slate300,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    FloatingActionButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("send_message_fab"),
                        containerColor = Indigo600,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isStreaming) Icons.Default.Sync else Icons.Default.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.size <= 2) {
                item {
                    BrandedBanners()
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(message = msg)
            }
        }
    }

    // Interactive Model / Provider Selector Modal
    if (showModelSelectorModal) {
        AlertDialog(
            onDismissRequest = { showModelSelectorModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Indigo600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select AI Model Provider", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose which AI model handles messages and MCP tool executions for this chat:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val configuredProviders = remember(LlmProvider.entries) {
                        LlmProvider.entries.filter { viewModel.isProviderConfigured(it.id) }
                    }

                    LaunchedEffect(showModelSelectorModal) {
                        configuredProviders.forEach { provider ->
                            viewModel.loadModelsForProvider(provider.id)
                        }
                    }

                    if (configuredProviders.isEmpty()) {
                        Text(
                            text = "No model providers are configured yet. Please go to Providers screen to set up your API keys.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate800, fontWeight = FontWeight.Bold)
                        )
                    } else {
                        configuredProviders.forEach { provider ->
                            val isLoading = isFetchingModels[provider.id] ?: false
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentSession?.providerId == provider.id) Color(0xFFE0E7FF) else Slate100,
                                border = if (currentSession?.providerId == provider.id) androidx.compose.foundation.BorderStroke(1.5.dp, Indigo600) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = provider.displayName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Indigo900
                                            )
                                        )

                                        if (isLoading) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = Indigo600
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Loading API...",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate600)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Dynamic API List",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Emerald600, fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Dropdown Menu Selector
                                    var isDropdownExpanded by remember { mutableStateOf(false) }
                                    val models = fetchedModels[provider.id] ?: provider.supportedModels

                                    Box {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isDropdownExpanded = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isSelectedProvider = currentSession?.providerId == provider.id
                                                val activeModelName = if (isSelectedProvider) (currentSession?.modelName ?: provider.defaultModel) else provider.defaultModel
                                                Text(
                                                    text = activeModelName,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (isSelectedProvider) Indigo900 else Slate800,
                                                        fontWeight = if (isSelectedProvider) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Open Dropdown Menu",
                                                    tint = Slate600,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = isDropdownExpanded,
                                            onDismissRequest = { isDropdownExpanded = false },
                                            modifier = Modifier
                                                .background(Color.White)
                                        ) {
                                            models.forEach { model ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = model,
                                                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate900)
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.updateSessionSettings(
                                                            providerId = provider.id,
                                                            modelName = model,
                                                            systemPrompt = currentSession?.systemPrompt ?: "System prompt",
                                                            turnsLimit = currentSession?.contextWindowSize ?: 16
                                                        )
                                                        isDropdownExpanded = false
                                                        showModelSelectorModal = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelSelectorModal = false }) {
                    Text("Close", color = Indigo600, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Voice Recorder Modal
    if (showVoiceRecorder) {
        VoiceRecorderDialog(
            onDismiss = { showVoiceRecorder = false },
            onRecordingComplete = { audioName, text ->
                showVoiceRecorder = false
                viewModel.pendingAttachment.value = AttachmentInfo(
                    uri = "content://media/voice_note",
                    type = "AUDIO",
                    name = audioName
                )
                viewModel.textInput.value = text
            }
        )
    }

    // Attachment Chooser Sheet
    if (showAttachmentMenu) {
        AlertDialog(
            onDismissRequest = { showAttachmentMenu = false },
            title = { Text("Attach Media / Document", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.pendingAttachment.value = AttachmentInfo(
                                    uri = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600",
                                    type = "IMAGE",
                                    name = "Circuit_Board_Analysis.jpg"
                                )
                                showAttachmentMenu = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Indigo600)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sample Image Asset (.jpg)", style = MaterialTheme.typography.bodyMedium.copy(color = Slate900))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.pendingAttachment.value = AttachmentInfo(
                                    uri = "content://media/sample_video",
                                    type = "VIDEO",
                                    name = "MCP_SSE_Protocol_Demo.mp4"
                                )
                                showAttachmentMenu = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Sky500)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sample Video Clip (.mp4)", style = MaterialTheme.typography.bodyMedium.copy(color = Slate900))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.pendingAttachment.value = AttachmentInfo(
                                    uri = "content://media/sample_doc",
                                    type = "DOCUMENT",
                                    name = "Model_Context_Protocol_Specification_2026.pdf"
                                )
                                showAttachmentMenu = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Violet600)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sample PDF Document (.pdf)", style = MaterialTheme.typography.bodyMedium.copy(color = Slate900))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentMenu = false }) {
                    Text("Cancel", color = Slate600)
                }
            }
        )
    }

    // Context Window Adjustment Dialog
    if (showContextDialog) {
        var turns by remember { mutableFloatStateOf(activeContextTurns.toFloat()) }
        AlertDialog(
            onDismissRequest = { showContextDialog = false },
            title = { Text("Dynamic Context Window", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) },
            text = {
                Column {
                    Text(
                        text = "Adjust history message turns kept in LLM prompt context to save tokens and optimize streaming latency.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Active Turns Limit: ${turns.toInt()} messages",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Indigo600)
                    )

                    Slider(
                        value = turns,
                        onValueChange = { turns = it },
                        valueRange = 2f..50f,
                        steps = 24,
                        colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2 turns (Fast)", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
                        Text("50 turns (Deep context)", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContextDialog = false
                        viewModel.updateSessionSettings(
                            providerId = currentSession?.providerId ?: DEFAULT_PROVIDER_ID,
                            modelName = currentSession?.modelName ?: DEFAULT_MODEL_NAME,
                            systemPrompt = currentSession?.systemPrompt ?: "System prompt",
                            turnsLimit = turns.toInt()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Apply Window", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextDialog = false }) {
                    Text("Cancel", color = Slate600)
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val isUser = message.sender == "USER"
    val isSystem = message.sender == "SYSTEM"

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate200,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = if (isUser) "You" else "AI Engine",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) Indigo600 else Slate900
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${message.tokensCount} tokens",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Slate600
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) Indigo600 else Color.White,
            shadowElevation = 1.5.dp,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                if (message.mediaType != null) {
                    MediaAttachmentCard(
                        mediaUri = message.mediaUri,
                        mediaType = message.mediaType,
                        mediaName = message.mediaName
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (!message.toolCallJson.isNullOrBlank()) {
                    val toolName = remember(message.toolCallJson) {
                        try { JSONObject(message.toolCallJson).optString("toolName") } catch (e: Exception) { "MCP Tool" }
                    }
                    val args = remember(message.toolCallJson) {
                        try { JSONObject(message.toolCallJson).optString("arguments") } catch (e: Exception) { "{}" }
                    }
                    val status = remember(message.toolCallJson) {
                        try { JSONObject(message.toolCallJson).optString("status") } catch (e: Exception) { "SUCCESS" }
                    }

                    ToolExecutionCard(
                        toolName = toolName,
                        argumentsJson = args,
                        resultOutput = message.toolResultJson,
                        isExecuting = status == "EXECUTING",
                        isError = status == "FAILED"
                    )
                } else {
                    MarkdownText(text = message.content, isUser = isUser)
                }
            }
        }
    }
}

@Composable
fun BrandedBanners() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner 1: Create MCP servers on mcpadmin.cloud
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try { uriHandler.openUri("https://mcpadmin.cloud") } catch (e: Exception) {}
                }
                .testTag("banner_mcp_admin"),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "https://news.mcpadmin.cloud/wp-content/uploads/2026/10/Screenshot-2026-08-26-113140.png",
                    contentDescription = "MCP Admin Logo",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create MCP Servers Live",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        text = "Provision, host & manage compliant SSE/HTTP servers in 1-click on mcpadmin.cloud",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontSize = 11.sp, lineHeight = 14.sp)
                    )
                }
                Icon(Icons.Default.Launch, contentDescription = "Go", tint = Indigo600, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ==========================================
// Robust Native Jetpack Compose Markdown Parser
// ==========================================

sealed class MarkdownPart {
    data class Paragraph(val lines: List<String>) : MarkdownPart()
    data class CodeBlock(val code: String, val language: String) : MarkdownPart()
}

fun parseMarkdownBlocks(text: String): List<MarkdownPart> {
    val parts = mutableListOf<MarkdownPart>()
    val lines = text.split("\n")
    var inCodeBlock = false
    val codeContent = StringBuilder()
    var codeLang = ""
    val paragraphLines = mutableListOf<String>()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                parts.add(MarkdownPart.CodeBlock(codeContent.toString().trimEnd(), codeLang))
                codeContent.clear()
                codeLang = ""
                inCodeBlock = false
            } else {
                if (paragraphLines.isNotEmpty()) {
                    parts.add(MarkdownPart.Paragraph(paragraphLines.toList()))
                    paragraphLines.clear()
                }
                codeLang = line.replace("```", "").trim()
                inCodeBlock = true
            }
        } else {
            if (inCodeBlock) {
                codeContent.append(line).append("\n")
            } else {
                paragraphLines.add(line)
            }
        }
    }

    if (inCodeBlock) {
        parts.add(MarkdownPart.CodeBlock(codeContent.toString().trimEnd(), codeLang))
    } else if (paragraphLines.isNotEmpty()) {
        parts.add(MarkdownPart.Paragraph(paragraphLines.toList()))
    }

    return parts
}

fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val nextBold = text.indexOf("**", cursor)
            val nextCode = text.indexOf("`", cursor)

            if (nextBold != -1 && (nextCode == -1 || nextBold < nextCode)) {
                append(text.substring(cursor, nextBold))
                val endBold = text.indexOf("**", nextBold + 2)
                if (endBold != -1) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(nextBold + 2, endBold))
                    pop()
                    cursor = endBold + 2
                } else {
                    append("**")
                    cursor = nextBold + 2
                }
            } else if (nextCode != -1) {
                append(text.substring(cursor, nextCode))
                val endCode = text.indexOf("`", nextCode + 1)
                if (endCode != -1) {
                    pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1A000000),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(text.substring(nextCode + 1, endCode))
                    pop()
                    cursor = endCode + 1
                } else {
                    append("`")
                    cursor = nextCode + 1
                }
            } else {
                append(text.substring(cursor))
                break
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, isUser: Boolean) {
    val color = if (isUser) Color.White else Slate900
    val parts = remember(text) { parseMarkdownBlocks(text) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        parts.forEach { part ->
            when (part) {
                is MarkdownPart.CodeBlock -> {
                    CodeBlockCard(part.code, part.language)
                }
                is MarkdownPart.Paragraph -> {
                    part.lines.forEach { line ->
                        when {
                            line.startsWith("### ") -> {
                                Text(
                                    text = parseInlineMarkdown(line.substring(4)),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            line.startsWith("## ") -> {
                                Text(
                                    text = parseInlineMarkdown(line.substring(3)),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            line.startsWith("# ") -> {
                                Text(
                                    text = parseInlineMarkdown(line.substring(2)),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = color,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            line.startsWith("* ") || line.startsWith("- ") || line.startsWith("• ") -> {
                                val cleanLine = line.substring(2)
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = parseInlineMarkdown(cleanLine),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = color,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }
                            line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                                val dotIdx = line.indexOf(".")
                                val num = line.substring(0, dotIdx + 1)
                                val cleanLine = line.substring(dotIdx + 1).trim()
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                ) {
                                    Text(
                                        text = num,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = parseInlineMarkdown(cleanLine),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = color,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }
                            else -> {
                                if (line.isNotBlank()) {
                                    Text(
                                        text = parseInlineMarkdown(line),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = color,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(code: String, language: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E24),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E38))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                Row(
                    modifier = Modifier.clickable {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "COPY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFF8FAFC),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

