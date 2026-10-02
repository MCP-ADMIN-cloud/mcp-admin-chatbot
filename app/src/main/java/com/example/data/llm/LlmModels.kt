package com.example.data.llm

enum class LlmProvider(val id: String, val displayName: String, val defaultModel: String, val supportedModels: List<String>) {
    GEMINI("gemini", "Google Gemini", "gemini-2.5-flash", listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-pro", "gemini-1.5-flash")),
    OPENAI("openai", "OpenAI GPT", "gpt-4o", listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "gpt-4-turbo")),
    CLAUDE("claude", "Anthropic Claude", "claude-3-5-sonnet-20241022", listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")),
    OPENROUTER("openrouter", "OpenRouter", "google/gemini-2.5-flash", listOf("google/gemini-2.5-flash", "anthropic/claude-3.5-sonnet", "deepseek/deepseek-r1", "meta-llama/llama-3.3-70b-instruct"))
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
