package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.entity.ChatMessageEntity
import com.example.data.db.entity.ChatSessionEntity
import com.example.data.db.entity.McpServerEntity
import com.example.data.db.entity.McpToolEntity
import com.example.data.db.entity.ProviderKeyEntity
import com.example.data.llm.LlmService
import com.example.data.llm.StreamChunk
import com.example.data.llm.ToolCallInfo
import com.example.data.mcp.McpClient
import com.example.data.mcp.McpRepository
import com.example.data.repository.ChatRepository
import com.example.data.secure.EncryptedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class AttachmentInfo(
    val uri: String,
    val type: String, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT"
    val name: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val encryptedStorage = EncryptedStorage(application)
    
    private val mcpClient = McpClient()
    val mcpRepository = McpRepository(db.mcpDao(), mcpClient)
    val chatRepository = ChatRepository(db.chatDao())
    val llmService = LlmService(encryptedStorage)

    val chatSessions: StateFlow<List<ChatSessionEntity>> = chatRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mcpServers: StateFlow<List<McpServerEntity>> = mcpRepository.allServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mcpTools: StateFlow<List<McpToolEntity>> = mcpRepository.allTools
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providerKeys: StateFlow<List<ProviderKeyEntity>> = db.providerDao().getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSessionEntity?>(null)
    val currentSession: StateFlow<ChatSessionEntity?> = _currentSession.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id ->
            if (id != null) chatRepository.getMessagesForSession(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isStreaming = MutableStateFlow(false)
    val textInput = MutableStateFlow("")
    val pendingAttachment = MutableStateFlow<AttachmentInfo?>(null)
    val activeContextTurns = MutableStateFlow(16)
    val maxContextTokens = MutableStateFlow(32000)

    private var streamJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            mcpRepository.initializeDefaultServersIfEmpty()
            initializeDefaultProvidersIfEmpty()

            chatSessions.collect { sessions ->
                if (_currentSessionId.value == null) {
                    if (sessions.isNotEmpty()) {
                        selectSession(sessions.first().id)
                    } else {
                        val newId = chatRepository.createNewSession(
                            title = "MCP Chatbot AI Assistant",
                            providerId = "gemini",
                            modelName = "gemini-2.5-flash"
                        )
                        selectSession(newId)
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch(Dispatchers.IO) {
            val session = chatRepository.getSessionById(sessionId)
            _currentSession.value = session
            if (session != null) {
                activeContextTurns.value = session.contextWindowSize
            }
        }
    }

    fun createNewSession(title: String = "New Conversation") {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = chatRepository.createNewSession(title = title)
            selectSession(newId)
        }
    }

    fun updateSessionSettings(
        providerId: String,
        modelName: String,
        systemPrompt: String,
        turnsLimit: Int
    ) {
        val s = _currentSession.value ?: return
        val updated = s.copy(
            providerId = providerId,
            modelName = modelName,
            systemPrompt = systemPrompt,
            contextWindowSize = turnsLimit
        )
        _currentSession.value = updated
        activeContextTurns.value = turnsLimit
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.updateSession(updated)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun sendMessage() {
        val content = textInput.value.trim()
        val attachment = pendingAttachment.value
        val sessionId = _currentSessionId.value ?: return

        if (content.isBlank() && attachment == null) return

        textInput.value = ""
        pendingAttachment.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val sessionBefore = chatRepository.getSessionById(sessionId)
            if (sessionBefore != null && (sessionBefore.title.startsWith("New Conversation") || sessionBefore.title.startsWith("New Chat"))) {
                val cleanTitle = if (content.length > 25) content.take(22) + "..." else content
                if (cleanTitle.isNotBlank()) {
                    chatRepository.updateSession(sessionBefore.copy(title = cleanTitle))
                    _currentSession.value = chatRepository.getSessionById(sessionId)
                }
            }

            chatRepository.addMessage(
                sessionId = sessionId,
                sender = "USER",
                content = if (content.isBlank() && attachment != null) "Sent ${attachment.type.lowercase()} attachment ${attachment.name}" else content,
                mediaUri = attachment?.uri,
                mediaType = attachment?.type,
                mediaName = attachment?.name
            )

            generateAssistantResponse(sessionId)
        }
    }

    fun togglePinSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = chatRepository.getSessionById(sessionId)
            if (session != null) {
                val updated = session.copy(isPinned = !session.isPinned)
                chatRepository.updateSession(updated)
                if (_currentSessionId.value == sessionId) {
                    _currentSession.value = updated
                }
            }
        }
    }

    private fun generateAssistantResponse(sessionId: String) {
        streamJob?.cancel()
        isStreaming.value = true

        streamJob = viewModelScope.launch(Dispatchers.IO) {
            val session = chatRepository.getSessionById(sessionId) ?: return@launch
            val messages = chatRepository.getMessagesListForSession(sessionId)
            val tools = mcpTools.value

            val contextMessages = messages.takeLast(session.contextWindowSize * 2)

            val assistantMsgId = chatRepository.addMessage(
                sessionId = sessionId,
                sender = "ASSISTANT",
                content = "Thinking..."
            )

            var accumulatedText = ""
            var currentToolCall: ToolCallInfo? = null

            llmService.generateStreamResponse(
                providerId = session.providerId,
                modelName = session.modelName,
                systemPrompt = session.systemPrompt,
                messages = contextMessages,
                availableTools = tools
            ).collect { chunk ->
                if (chunk.toolCallRequest != null) {
                    currentToolCall = chunk.toolCallRequest
                    val toolJson = JSONObject().apply {
                        put("toolName", currentToolCall!!.toolName)
                        put("arguments", currentToolCall!!.argumentsJson)
                        put("status", "EXECUTING")
                    }.toString()

                    chatRepository.updateMessageContent(
                        messageId = assistantMsgId,
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = "Calling MCP tool `${currentToolCall!!.toolName}`...",
                        toolCallJson = toolJson
                    )

                    val toolResult = mcpRepository.executeTool(
                        toolName = currentToolCall!!.toolName,
                        argumentsJson = currentToolCall!!.argumentsJson
                    )

                    val updatedToolJson = JSONObject().apply {
                        put("toolName", currentToolCall!!.toolName)
                        put("arguments", currentToolCall!!.argumentsJson)
                        put("status", if (toolResult.isError) "FAILED" else "SUCCESS")
                        put("result", toolResult.outputText)
                    }.toString()

                    val toolResultContent = "Tool Execution Complete:\n\n${toolResult.outputText}"

                    chatRepository.updateMessageContent(
                        messageId = assistantMsgId,
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = toolResultContent,
                        toolCallJson = updatedToolJson,
                        toolResultJson = toolResult.rawResultJson
                    )

                } else if (chunk.textDelta.isNotEmpty()) {
                    accumulatedText += chunk.textDelta
                    chatRepository.updateMessageContent(
                        messageId = assistantMsgId,
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = accumulatedText
                    )
                }

                if (chunk.isDone) {
                    isStreaming.value = false
                }
            }
        }
    }

    fun isProviderConfigured(providerId: String): Boolean {
        val userKey = encryptedStorage.getCredential(providerId)
        return !userKey.isNullOrBlank()
    }

    fun saveProviderKey(providerId: String, apiKey: String, defaultModel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            encryptedStorage.saveCredential(providerId, apiKey)
            val p = db.providerDao().getProviderById(providerId)
            if (p != null) {
                db.providerDao().updateProvider(
                    p.copy(
                        apiKey = if (apiKey.length > 8) apiKey.take(4) + "..." + apiKey.takeLast(4) else "Saved",
                        isEnabled = apiKey.isNotBlank(),
                        defaultModel = defaultModel,
                        lastVerifiedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun addMcpServer(
        name: String,
        url: String,
        transportType: String,
        authType: String,
        authKeyName: String?,
        authValue: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            mcpRepository.addServer(name, url, transportType, authType, authKeyName, authValue)
        }
    }

    fun syncServer(server: McpServerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            mcpRepository.syncServerTools(server)
        }
    }

    fun deleteMcpServer(serverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mcpRepository.deleteServer(serverId)
        }
    }

    private suspend fun initializeDefaultProvidersIfEmpty() {
        val providers = db.providerDao().getAllProviders().firstOrNull() ?: emptyList()
        if (providers.isEmpty()) {
            val defaultList = listOf(
                ProviderKeyEntity("gemini", "Google Gemini", "", false, "gemini-2.5-flash", "gemini-2.5-flash,gemini-2.5-pro,gemini-1.5-pro"),
                ProviderKeyEntity("openai", "OpenAI GPT", "", false, "gpt-4o", "gpt-4o,gpt-4o-mini,o3-mini"),
                ProviderKeyEntity("claude", "Anthropic Claude", "", false, "claude-3-5-sonnet-20241022", "claude-3-5-sonnet-20241022,claude-3-5-haiku-20241022"),
                ProviderKeyEntity("openrouter", "OpenRouter", "", false, "google/gemini-2.5-flash", "google/gemini-2.5-flash,anthropic/claude-3.5-sonnet,deepseek/deepseek-r1")
            )
            for (p in defaultList) {
                db.providerDao().insertProvider(p)
            }
        } else {
            // Self-healing migration for existing databases:
            // If Gemini is marked enabled but no key actually exists in storage, disable it.
            val geminiProvider = providers.find { it.providerId == "gemini" }
            if (geminiProvider != null && geminiProvider.isEnabled) {
                val hasKey = !encryptedStorage.getCredential("gemini").isNullOrBlank()
                if (!hasKey) {
                    db.providerDao().updateProvider(geminiProvider.copy(isEnabled = false, apiKey = ""))
                }
            }
        }
    }
}
