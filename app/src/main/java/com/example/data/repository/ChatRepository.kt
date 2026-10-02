package com.example.data.repository

import com.example.data.db.dao.ChatDao
import com.example.data.db.entity.ChatMessageEntity
import com.example.data.db.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getMessagesListForSession(sessionId: String): List<ChatMessageEntity> {
        return chatDao.getMessagesListForSession(sessionId)
    }

    suspend fun getSessionById(sessionId: String): ChatSessionEntity? {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun createNewSession(
        title: String = "New Conversation",
        providerId: String = "gemini",
        modelName: String = "gemini-2.5-flash",
        systemPrompt: String = "You are a helpful AI assistant connected to MCP (Model Context Protocol) servers."
    ): String {
        val id = "chat_" + UUID.randomUUID().toString().take(12)
        val session = ChatSessionEntity(
            id = id,
            title = title,
            providerId = providerId,
            modelName = modelName,
            systemPrompt = systemPrompt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        
        val sysMsg = ChatMessageEntity(
            id = "msg_" + UUID.randomUUID().toString().take(12),
            sessionId = id,
            sender = "SYSTEM",
            content = "Chat session initialized with $providerId ($modelName). Ready for multi-modal MCP tool calling.",
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(sysMsg)

        return id
    }

    suspend fun updateSession(session: ChatSessionEntity) {
        chatDao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun addMessage(
        sessionId: String,
        sender: String,
        content: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        mediaName: String? = null,
        toolCallJson: String? = null,
        toolResultJson: String? = null
    ): String {
        val msgId = "msg_" + UUID.randomUUID().toString().take(12)
        val estimatedTokens = (content.length / 4) + 5
        val message = ChatMessageEntity(
            id = msgId,
            sessionId = sessionId,
            sender = sender,
            content = content,
            mediaUri = mediaUri,
            mediaType = mediaType,
            mediaName = mediaName,
            toolCallJson = toolCallJson,
            toolResultJson = toolResultJson,
            tokensCount = estimatedTokens,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(message)

        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val preview = if (content.length > 50) content.take(50) + "..." else content
            chatDao.updateSession(
                session.copy(
                    updatedAt = System.currentTimeMillis(),
                    lastMessagePreview = preview
                )
            )
        }
        return msgId
    }

    suspend fun updateMessageContent(
        messageId: String,
        sessionId: String,
        sender: String,
        content: String,
        toolCallJson: String? = null,
        toolResultJson: String? = null
    ) {
        val estimatedTokens = (content.length / 4) + 5
        val message = ChatMessageEntity(
            id = messageId,
            sessionId = sessionId,
            sender = sender,
            content = content,
            toolCallJson = toolCallJson,
            toolResultJson = toolResultJson,
            tokensCount = estimatedTokens,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(message)
    }

    suspend fun clearMessagesForSession(sessionId: String) {
        chatDao.clearMessagesForSession(sessionId)
    }
}
