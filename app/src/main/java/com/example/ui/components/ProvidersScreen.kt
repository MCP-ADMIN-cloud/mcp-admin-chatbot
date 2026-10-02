package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.ProviderKeyEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    viewModel: MainViewModel,
    onBackToChat: () -> Unit
) {
    val providers by viewModel.providerKeys.collectAsState()
    var editingProvider by remember { mutableStateOf<ProviderKeyEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Model Providers", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Text("Secure encrypted storage for LLM credentials", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToChat, modifier = Modifier.testTag("providers_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Chat", tint = Slate800)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE0F2FE),
                border = androidx.compose.foundation.BorderStroke(1.dp, Sky500.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Sky500, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All API Keys are encrypted in local device storage. If no key is provided, the chatbot uses the built-in Smart Assistant engine with full MCP tool support.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0369A1), lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SUPPORTED AI PROVIDERS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800, letterSpacing = 0.8.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(providers, key = { it.providerId }) { provider ->
                    ProviderCard(
                        provider = provider,
                        onEdit = { editingProvider = provider }
                    )
                }
            }
        }
    }

    if (editingProvider != null) {
        val p = editingProvider!!
        var apiKeyInput by remember { mutableStateOf(viewModel.encryptedStorage.getCredential(p.providerId) ?: "") }

        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("Configure ${p.providerName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key", color = Slate800) },
                        placeholder = { Text("Enter your ${p.providerName} API key") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("provider_key_input_${p.providerId}"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Slate900, unfocusedTextColor = Slate900)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveProviderKey(p.providerId, apiKeyInput, p.defaultModel)
                        editingProvider = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    modifier = Modifier.testTag("save_provider_key_button")
                ) {
                    Text("Save Key", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = null }) {
                    Text("Cancel", color = Slate600)
                }
            }
        )
    }
}

@Composable
fun ProviderCard(
    provider: ProviderKeyEntity,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.providerId}"),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (provider.providerId) {
                                "gemini" -> Color(0xFFE0E7FF)
                                "openai" -> Color(0xFFD1FAE5)
                                "claude" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFE0F2FE)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = when (provider.providerId) {
                            "gemini" -> Indigo600
                            "openai" -> Emerald600
                            "claude" -> Amber600
                            else -> Sky500
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.providerName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (provider.isEnabled) Emerald600 else Slate300)
                        )
                    }

                    Text(
                        text = "Default: ${provider.defaultModel}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontSize = 11.sp)
                    )
                }
            }

            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Indigo600),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (provider.isEnabled) "Configured" else "Setup Key", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo600))
            }
        }
    }
}
