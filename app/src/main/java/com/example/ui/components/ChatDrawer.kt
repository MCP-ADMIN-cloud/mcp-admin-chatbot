package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.entity.ChatSessionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawerContent(
    viewModel: MainViewModel,
    onCloseDrawer: () -> Unit,
    onNavigateToMcpDashboard: () -> Unit,
    onNavigateToProviders: () -> Unit
) {
    val sessions by viewModel.chatSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()

    var showSystemPromptModal by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier
            .width(320.dp)
            .testTag("chat_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = Indigo600, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Conversations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                }

                IconButton(onClick = onCloseDrawer) {
                    Icon(Icons.Default.Close, contentDescription = "Close Drawer", tint = Slate600)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // New Chat Button
            Button(
                onClick = {
                    viewModel.createNewSession("New Chat ${sessions.size + 1}")
                    onCloseDrawer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_new_chat_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Slate200)
            Spacer(modifier = Modifier.height(12.dp))

            // Sessions List
            Text(
                text = "RECENT CHATS (${sessions.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate600, letterSpacing = 0.8.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isSelected = session.id == currentSessionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectSession(session.id)
                                onCloseDrawer()
                            }
                            .testTag("chat_session_item_${session.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFE0E7FF) else Slate50,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Indigo600) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Indigo900 else Slate900
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = if (session.lastMessagePreview.isBlank()) "No messages yet" else session.lastMessagePreview,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp),
                                    maxLines = 1
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Dynamic Pin/Unpin action button
                                IconButton(
                                    onClick = { viewModel.togglePinSession(session.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin Chat",
                                        tint = if (session.isPinned) Indigo600 else Slate300,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (sessions.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Chat", tint = Rose600, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Slate200)
            Spacer(modifier = Modifier.height(12.dp))

            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            // Sidebar Zyven Technologies Banner: Image first, then Title, then Description
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable {
                        try { uriHandler.openUri("http://zyven-technologies.com") } catch (e: Exception) {}
                    }
                    .testTag("sidebar_zyven_tech_banner"),
                shape = RoundedCornerShape(10.dp),
                color = Slate50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                shadowElevation = 1.dp
            ) {
                Column {
                    AsyncImage(
                        model = "https://news.mcpadmin.cloud/wp-content/uploads/2026/10/Gemini_Generated_Image_9n5n8q9n5n8q9n5ns.png",
                        contentDescription = "Zyven Technologies Logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "About Zyven Technologies",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Discover innovative enterprise AI infrastructure and developer ecosystems at zyven-technologies.com",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontSize = 11.sp, lineHeight = 14.sp)
                        )
                    }
                }
            }

            // Navigation Links
            OutlinedButton(
                onClick = {
                    showSystemPromptModal = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Indigo600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat System Prompt", color = Slate900)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onCloseDrawer()
                        onNavigateToMcpDashboard()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MCP", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onCloseDrawer()
                        onNavigateToProviders()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Sky500, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keys", fontSize = 12.sp)
                }
            }
        }
    }

    if (showSystemPromptModal && currentSession != null) {
        var promptText by remember { mutableStateOf(currentSession!!.systemPrompt) }
        AlertDialog(
            onDismissRequest = { showSystemPromptModal = false },
            title = { Text("Custom System Instructions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("System Prompt") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSessionSettings(
                            providerId = currentSession!!.providerId,
                            modelName = currentSession!!.modelName,
                            systemPrompt = promptText,
                            turnsLimit = currentSession!!.contextWindowSize
                        )
                        showSystemPromptModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save Prompt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
