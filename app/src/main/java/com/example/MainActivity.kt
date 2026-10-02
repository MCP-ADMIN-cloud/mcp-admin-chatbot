package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ui.components.ChatDrawerContent
import com.example.ui.components.ChatScreen
import com.example.ui.components.McpDashboardScreen
import com.example.ui.components.ProvidersScreen
import com.example.ui.components.ToolsInspectScreen
import com.example.ui.theme.McpTheme
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate900
import com.example.ui.theme.Indigo600
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            McpTheme {
                McpApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200) // 2.2 seconds splash delay
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Pluga MCP Logo
            AsyncImage(
                model = "https://assets.pluga.co/apps/icons/pluga_mcp/pluga_mcp-icon.svg",
                contentDescription = "MCP Chatbot Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MCP Chatbot",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Chat with your MCP servers using cloud LLMs",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Indigo600,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "PART OF MCP ADMIN PRODUCT LINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Indigo600,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "by Zyven Technologies Pvt Ltd",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun McpApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawerContent(
                viewModel = viewModel,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                },
                onNavigateToMcpDashboard = {
                    navController.navigate("mcp_dashboard")
                },
                onNavigateToProviders = {
                    navController.navigate("providers")
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("splash") {
                SplashScreen(onTimeout = {
                    navController.navigate("chat") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            composable("chat") {
                ChatScreen(
                    viewModel = viewModel,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onNavigateToMcpDashboard = {
                        navController.navigate("mcp_dashboard")
                    },
                    onNavigateToProviders = {
                        navController.navigate("providers")
                    },
                    onNavigateToToolsInspector = {
                        navController.navigate("tools_inspector")
                    }
                )
            }

            composable("mcp_dashboard") {
                McpDashboardScreen(
                    viewModel = viewModel,
                    onBackToChat = {
                        navController.popBackStack()
                    },
                    onNavigateToToolsInspector = {
                        navController.navigate("tools_inspector")
                    }
                )
            }

            composable("providers") {
                ProvidersScreen(
                    viewModel = viewModel,
                    onBackToChat = {
                        navController.popBackStack()
                    }
                )
            }

            composable("tools_inspector") {
                ToolsInspectScreen(
                    viewModel = viewModel,
                    onBackToChat = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
