package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val providerId: String = "gemini", // "gemini", "openai", "claude", "openrouter"
    val modelName: String = "gemini-2.5-flash",
    val systemPrompt: String = "You are a helpful AI assistant connected to MCP (Model Context Protocol) servers. You can call tools dynamically and process text, code, images, audio, video, and documents.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val contextWindowSize: Int = 16, // max message turns kept in active context window
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String = "",
    val isPinned: Boolean = false
)
