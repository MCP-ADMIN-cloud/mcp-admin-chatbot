package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_keys")
data class ProviderKeyEntity(
    @PrimaryKey
    val providerId: String, // "openai", "gemini", "claude", "openrouter"
    val providerName: String,
    val apiKey: String,
    val isEnabled: Boolean = true,
    val defaultModel: String,
    val availableModels: String, // Comma separated list
    val customEndpoint: String? = null,
    val lastVerifiedAt: Long? = null
)
