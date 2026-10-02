package com.example.data.mcp

import org.json.JSONObject

enum class TransportType {
    SSE,
    HTTP
}

enum class AuthType {
    NONE,
    BEARER,
    QUERY_PARAM,
    HEADER
}

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}

data class McpTool(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val serverId: String,
    val serverName: String
)

data class McpToolCallRequest(
    val callId: String,
    val toolName: String,
    val argumentsJson: String,
    val serverId: String? = null
)

data class McpToolCallResult(
    val callId: String,
    val toolName: String,
    val isError: Boolean,
    val outputText: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "IMAGE", "DOCUMENT", "JSON"
    val rawResultJson: String
)

data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
    val transportType: TransportType = TransportType.SSE,
    val authType: AuthType = AuthType.NONE,
    val authKeyName: String? = null,
    val authValue: String? = null,
    val isEnabled: Boolean = true
)
