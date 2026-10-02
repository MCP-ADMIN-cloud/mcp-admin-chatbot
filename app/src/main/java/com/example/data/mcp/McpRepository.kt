package com.example.data.mcp

import com.example.data.db.dao.McpDao
import com.example.data.db.entity.McpServerEntity
import com.example.data.db.entity.McpToolEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class McpRepository(
    private val mcpDao: McpDao,
    private val mcpClient: McpClient
) {
    val allServers: Flow<List<McpServerEntity>> = mcpDao.getAllServers()
    val allTools: Flow<List<McpToolEntity>> = mcpDao.getAllTools()

    suspend fun initializeDefaultServersIfEmpty() {
        // Clear all hardcoded servers at startup as requested.
        // Users can easily add their own SSE or HTTP servers via the dashboard.
    }

    suspend fun addServer(
        name: String,
        url: String,
        transportType: String,
        authType: String,
        authKeyName: String?,
        authValue: String?
    ): String {
        val id = "server_" + UUID.randomUUID().toString().take(8)
        val server = McpServerEntity(
            id = id,
            name = name,
            url = url,
            transportType = transportType,
            authType = authType,
            authKeyName = authKeyName,
            authValue = authValue,
            status = "CONNECTING"
        )
        mcpDao.insertServer(server)
        syncServerTools(server)
        return id
    }

    suspend fun syncServerTools(server: McpServerEntity) {
        try {
            mcpDao.updateServerStatus(server.id, "CONNECTING", null)
            val tools = mcpClient.fetchTools(server)
            mcpDao.deleteToolsForServer(server.id)
            mcpDao.insertTools(tools)
            mcpDao.updateServerStatus(server.id, "CONNECTED", null)
        } catch (e: Exception) {
            mcpDao.updateServerStatus(server.id, "ERROR", e.message ?: "Failed to connect")
        }
    }

    suspend fun syncAllServers() {
        val servers = mcpDao.getActiveServers()
        for (s in servers) {
            syncServerTools(s)
        }
    }

    suspend fun deleteServer(serverId: String) {
        mcpDao.deleteToolsForServer(serverId)
        mcpDao.deleteServer(serverId)
    }

    suspend fun executeTool(
        toolName: String,
        argumentsJson: String
    ): McpToolCallResult {
        // Find which server provides this tool
        val tools = mcpDao.getActiveToolsList()
        val matchingTool = tools.find { it.name == toolName }
        val server = if (matchingTool != null) {
            mcpDao.getServerById(matchingTool.serverId)
        } else {
            null
        }

        return mcpClient.executeToolCall(server, toolName, argumentsJson)
    }
}
