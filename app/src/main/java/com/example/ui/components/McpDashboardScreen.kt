package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.db.entity.McpServerEntity
import com.example.data.db.entity.McpToolEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpDashboardScreen(
    viewModel: MainViewModel,
    onBackToChat: () -> Unit,
    onNavigateToToolsInspector: () -> Unit
) {
    val servers by viewModel.mcpServers.collectAsState()
    val tools by viewModel.mcpTools.collectAsState()

    var showAddServerDialog by remember { mutableStateOf(false) }
    var selectedServerForDetails by remember { mutableStateOf<McpServerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MCP Servers Dashboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Text("${servers.size} Connected Servers • ${tools.size} Offline Tools Cached", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToChat, modifier = Modifier.testTag("mcp_back_to_chat_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Chat", tint = Slate800)
                    }
                },
                actions = {
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = { scope.launch { viewModel.mcpRepository.syncAllServers() } },
                        modifier = Modifier.testTag("refresh_mcp_servers_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Connections", tint = Indigo600)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddServerDialog = true },
                containerColor = Indigo600,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_mcp_server_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add MCP Server")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Active Servers",
                    value = "${servers.count { it.status == "CONNECTED" }}/${servers.size}",
                    icon = Icons.Default.CloudDone,
                    iconTint = Emerald600,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Cached Tools",
                    value = "${tools.size}",
                    icon = Icons.Default.Construction,
                    iconTint = Indigo600,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Transport Mode",
                    value = "SSE / HTTP",
                    icon = Icons.Default.SwapHoriz,
                    iconTint = Sky500,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CONFIGURED MCP SERVERS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800, letterSpacing = 0.8.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No MCP servers added yet. Tap + to connect an SSE or HTTP server.", style = MaterialTheme.typography.bodyMedium.copy(color = Slate800))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(servers, key = { it.id }) { server ->
                        val serverTools = tools.filter { it.serverId == server.id }
                        McpServerCard(
                            server = server,
                            serverTools = serverTools,
                            onSync = { viewModel.syncServer(server) },
                            onDelete = { viewModel.deleteMcpServer(server.id) },
                            onInspect = { selectedServerForDetails = server }
                        )
                    }
                }
            }
        }
    }

    if (showAddServerDialog) {
        AddMcpServerDialog(
            onDismiss = { showAddServerDialog = false },
            onAddServer = { name, url, transport, authType, keyName, authVal ->
                viewModel.addMcpServer(name, url, transport, authType, keyName, authVal)
                showAddServerDialog = false
            }
        )
    }

    if (selectedServerForDetails != null) {
        val server = selectedServerForDetails!!
        val serverTools = tools.filter { it.serverId == server.id }
        AlertDialog(
            onDismissRequest = { selectedServerForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Indigo600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(server.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("URL: ${server.url}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Slate800))
                    Text("Transport: ${server.transportType} • Auth: ${server.authType}", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Slate200)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("EXPOSED TOOLS (${serverTools.size}):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo600))

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(serverTools) { tool ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(tool.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                    Text(tool.description, style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontSize = 11.sp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedServerForDetails = null }, colors = ButtonDefaults.buttonColors(containerColor = Indigo600)) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Slate600))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
        }
    }
}

@Composable
fun McpServerCard(
    server: McpServerEntity,
    serverTools: List<McpToolEntity>,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mcp_server_card_${server.id}"),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (server.status) {
                                    "CONNECTED" -> Emerald600
                                    "CONNECTING" -> Amber600
                                    else -> Rose600
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                }

                Row {
                    IconButton(onClick = onSync, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Tools", tint = Indigo600, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onInspect, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = "Inspect Server", tint = Sky500, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Server", tint = Rose600, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = server.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Slate800, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE0E7FF)
                ) {
                    Text(
                        text = "${server.transportType} • Auth: ${server.authType}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Indigo900, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${serverTools.size} Tools Cached",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate800, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAddServer: (name: String, url: String, transport: String, authType: String, keyName: String?, authVal: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var transport by remember { mutableStateOf("SSE") }
    var authType by remember { mutableStateOf("NONE") }
    var keyName by remember { mutableStateOf("Authorization") }
    var authVal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MCP Server", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name", color = Slate800) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_server_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Slate900, unfocusedTextColor = Slate900)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL (SSE / HTTP)", color = Slate800) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_server_url_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Slate900, unfocusedTextColor = Slate900)
                )

                Text("Transport Mode:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                Row {
                    FilterChip(
                        selected = transport == "SSE",
                        onClick = { transport = "SSE" },
                        label = { Text("SSE (Server-Sent Events)") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = transport == "HTTP",
                        onClick = { transport = "HTTP" },
                        label = { Text("HTTP JSON-RPC") }
                    )
                }

                Text("Authentication:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = authType == "NONE", onClick = { authType = "NONE" }, label = { Text("None") })
                    FilterChip(selected = authType == "BEARER", onClick = { authType = "BEARER" }, label = { Text("Bearer") })
                    FilterChip(selected = authType == "QUERY_PARAM", onClick = { authType = "QUERY_PARAM" }, label = { Text("Query Key") })
                }

                if (authType != "NONE") {
                    OutlinedTextField(
                        value = authVal,
                        onValueChange = { authVal = it },
                        label = { Text(if (authType == "BEARER") "Bearer Token" else "API Key Value", color = Slate800) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Slate900, unfocusedTextColor = Slate900)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onAddServer(name, url, transport, authType, keyName, authVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                modifier = Modifier.testTag("confirm_add_mcp_server_button")
            ) {
                Text("Connect Server", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
