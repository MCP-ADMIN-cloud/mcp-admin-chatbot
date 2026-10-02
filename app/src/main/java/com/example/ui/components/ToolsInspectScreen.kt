package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.McpToolEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsInspectScreen(
    viewModel: MainViewModel,
    onBackToChat: () -> Unit
) {
    val tools by viewModel.mcpTools.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredTools = remember(tools, searchQuery) {
        if (searchQuery.isBlank()) tools
        else tools.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.serverName.contains(searchQuery, ignoreCase = true)
        }
    }

    var selectedToolForTest by remember { mutableStateOf<McpToolEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MCP Tools Inspector", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Text("${tools.size} Offline Tools In Schema Vault", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToChat, modifier = Modifier.testTag("tools_inspector_back_button")) {
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_tools_input"),
                placeholder = { Text("Search tools by name, server, or description...", color = Slate600) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Indigo600) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Slate900, unfocusedTextColor = Slate900)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTools.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching tools found in offline cache.", style = MaterialTheme.typography.bodyMedium.copy(color = Slate800))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTools, key = { it.id }) { tool ->
                        ToolInspectorCard(
                            tool = tool,
                            onTestTool = { selectedToolForTest = tool }
                        )
                    }
                }
            }
        }
    }

    if (selectedToolForTest != null) {
        val tool = selectedToolForTest!!
        var testArgsInput by remember { mutableStateOf(generateSampleArgsForTool(tool.name)) }
        var testResultText by remember { mutableStateOf<String?>(null) }
        var isExecutingTest by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { selectedToolForTest = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Indigo600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Tool: ${tool.name}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Server: ${tool.serverName}", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Input Arguments (JSON):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    OutlinedTextField(
                        value = testArgsInput,
                        onValueChange = { testArgsInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        minLines = 3,
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Slate900, unfocusedTextColor = Slate900)
                    )

                    if (!testResultText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Result Output:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Emerald600))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald100.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = testResultText!!,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate900),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExecutingTest = true
                        scope.launch {
                            val res = viewModel.mcpRepository.executeTool(tool.name, testArgsInput)
                            testResultText = res.outputText
                            isExecutingTest = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    enabled = !isExecutingTest
                ) {
                    Text(if (isExecutingTest) "Executing..." else "Run Test", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedToolForTest = null }) {
                    Text("Close", color = Slate600)
                }
            }
        )
    }
}

@Composable
fun ToolInspectorCard(
    tool: McpToolEntity,
    onTestTool: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_inspector_card_${tool.name}"),
        shape = RoundedCornerShape(12.dp),
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0E7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = tool.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            text = "Server: ${tool.serverName}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 11.sp)
                        )
                    }
                }

                Row {
                    Button(
                        onClick = onTestTool,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontSize = 11.sp, color = Color.White)
                    }

                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand Schema", tint = Slate600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate800, lineHeight = 16.sp)
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Divider(color = Slate200)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("INPUT PARAMETER SCHEMA (JSON):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo600))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = tool.inputSchemaJson,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate900),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun generateSampleArgsForTool(toolName: String): String {
    return when (toolName) {
        "get_current_weather" -> """{"city":"Tokyo","unit":"celsius"}"""
        "search_web" -> """{"query":"Model Context Protocol 2026","limit":5}"""
        "execute_sql_query" -> """{"sql":"SELECT * FROM users;"}"""
        "execute_code_snippet" -> """{"language":"python","code":"print('MCP Test Standard Output')"}"""
        else -> """{}"""
    }
}
