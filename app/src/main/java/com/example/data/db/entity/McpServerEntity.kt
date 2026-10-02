package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val transportType: String = "SSE", // "SSE" or "HTTP"
    val authType: String = "NONE", // "NONE", "BEARER", "QUERY_PARAM", "HEADER"
    val authKeyName: String? = null, // Header name or query param key name (e.g. "Authorization", "api_key")
    val authValue: String? = null, // Bearer token or API key
    val status: String = "CONNECTED", // "CONNECTED", "CONNECTING", "DISCONNECTED", "ERROR"
    val lastError: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)
