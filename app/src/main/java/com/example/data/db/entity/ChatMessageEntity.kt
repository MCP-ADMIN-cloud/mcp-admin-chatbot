package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val sender: String, // "USER", "ASSISTANT", "TOOL", "SYSTEM"
    val content: String,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT"
    val mediaName: String? = null,
    val toolCallJson: String? = null, // JSON object of tool call invocation
    val toolResultJson: String? = null, // JSON object of tool result output
    val tokensCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
