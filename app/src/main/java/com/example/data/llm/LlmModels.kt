package com.example.data.llm

const val DEFAULT_PROVIDER_ID = "gemini"
const val DEFAULT_MODEL_NAME = "gemini-2.5-flash"

enum class LlmProvider(val id: String, val displayName: String, val defaultModel: String, val supportedModels: List<String>) {
    GEMINI("gemini", "Google Gemini", "gemini-2.5-flash", listOf("gemini-2.5-flash")),
    OPENAI("openai", "OpenAI GPT", "gpt-4o", listOf("gpt-4o")),
    CLAUDE("claude", "Anthropic Claude", "claude-3-5-sonnet-20241022", listOf("claude-3-5-sonnet-20241022")),
    OPENROUTER("openrouter", "OpenRouter", "google/gemini-2.5-flash", listOf("google/gemini-2.5-flash"))
}

data class StreamChunk(
    val textDelta: String = "",
    val toolCallRequest: ToolCallInfo? = null,
    val isDone: Boolean = false
)

data class ToolCallInfo(
    val callId: String,
    val toolName: String,
    val argumentsJson: String
)
