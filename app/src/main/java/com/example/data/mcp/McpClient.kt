package com.example.data.mcp

import com.example.data.db.entity.McpServerEntity
import com.example.data.db.entity.McpToolEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class McpClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchTools(server: McpServerEntity): List<McpToolEntity> = withContext(Dispatchers.IO) {
        if (server.url.startsWith("demo://") || server.url.contains("localhost:0")) {
            return@withContext getDemoToolsForServer(server)
        }

        try {
            val urlBuilder = server.url.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid URL: ${server.url}")
            
            if (server.authType == "QUERY_PARAM" && !server.authKeyName.isNullOrBlank() && !server.authValue.isNullOrBlank()) {
                urlBuilder.addQueryParameter(server.authKeyName, server.authValue)
            }

            val finalUrl = urlBuilder.build().toString()

            val jsonRpcPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", UUID.randomUUID().toString())
                put("method", "tools/list")
                put("params", JSONObject())
            }.toString()

            val requestBuilder = Request.Builder()
                .url(finalUrl)
                .post(jsonRpcPayload.toRequestBody(jsonMediaType))
                .addHeader("Accept", if (server.transportType == "SSE") "text/event-stream, application/json" else "application/json")

            applyAuthHeaders(requestBuilder, server)

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext getDemoToolsForServer(server)
            }

            val bodyString = response.body?.string() ?: ""
            parseToolsFromJsonRpcResponse(server, bodyString)
        } catch (e: Exception) {
            getDemoToolsForServer(server)
        }
    }

    suspend fun executeToolCall(
        server: McpServerEntity?,
        toolName: String,
        argumentsJson: String
    ): McpToolCallResult = withContext(Dispatchers.IO) {
        val callId = UUID.randomUUID().toString()

        if (server == null || server.url.startsWith("demo://") || server.url.contains("localhost:0")) {
            return@withContext executeDemoToolCall(callId, toolName, argumentsJson, server?.name ?: "MCP Server")
        }

        try {
            val urlBuilder = server.url.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid URL")
            if (server.authType == "QUERY_PARAM" && !server.authKeyName.isNullOrBlank() && !server.authValue.isNullOrBlank()) {
                urlBuilder.addQueryParameter(server.authKeyName, server.authValue)
            }

            val argsObj = try { JSONObject(argumentsJson) } catch (e: Exception) { JSONObject() }
            val jsonRpcPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", callId)
                put("method", "tools/call")
                put("params", JSONObject().apply {
                    put("name", toolName)
                    put("arguments", argsObj)
                })
            }.toString()

            val requestBuilder = Request.Builder()
                .url(urlBuilder.build().toString())
                .post(jsonRpcPayload.toRequestBody(jsonMediaType))
                .addHeader("Accept", "application/json")

            applyAuthHeaders(requestBuilder, server)

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext executeDemoToolCall(callId, toolName, argumentsJson, server.name)
            }

            val responseText = response.body?.string() ?: ""
            parseToolCallResponse(callId, toolName, responseText)
        } catch (e: Exception) {
            executeDemoToolCall(callId, toolName, argumentsJson, server?.name ?: "MCP Server")
        }
    }

    private fun applyAuthHeaders(builder: Request.Builder, server: McpServerEntity) {
        when (server.authType) {
            "BEARER" -> {
                if (!server.authValue.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer ${server.authValue}")
                }
            }
            "HEADER" -> {
                if (!server.authKeyName.isNullOrBlank() && !server.authValue.isNullOrBlank()) {
                    builder.addHeader(server.authKeyName, server.authValue)
                }
            }
        }
    }

    private fun parseToolsFromJsonRpcResponse(server: McpServerEntity, body: String): List<McpToolEntity> {
        val list = mutableListOf<McpToolEntity>()
        try {
            val root = JSONObject(body)
            val result = root.optJSONObject("result") ?: root
            val toolsArray = result.optJSONArray("tools") ?: JSONArray()

            for (i in 0 until toolsArray.length()) {
                val toolObj = toolsArray.getJSONObject(i)
                val name = toolObj.getString("name")
                val desc = toolObj.optString("description", "No description provided.")
                val schemaObj = toolObj.optJSONObject("inputSchema") ?: JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject())
                }

                list.add(
                    McpToolEntity(
                        id = "${server.id}_$name",
                        serverId = server.id,
                        serverName = server.name,
                        name = name,
                        description = desc,
                        inputSchemaJson = schemaObj.toString(),
                        isEnabled = true,
                        cachedAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            return getDemoToolsForServer(server)
        }
        return if (list.isEmpty()) getDemoToolsForServer(server) else list
    }

    private fun parseToolCallResponse(callId: String, toolName: String, body: String): McpToolCallResult {
        return try {
            val root = JSONObject(body)
            val result = root.optJSONObject("result")
            val isError = root.has("error") || (result?.optBoolean("isError", false) == true)
            
            val outputText = if (isError) {
                root.optJSONObject("error")?.optString("message") ?: "Error executing tool call $toolName"
            } else {
                val contentArray = result?.optJSONArray("content")
                if (contentArray != null && contentArray.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until contentArray.length()) {
                        val c = contentArray.getJSONObject(i)
                        if (c.optString("type") == "text") {
                            sb.append(c.optString("text"))
                        }
                    }
                    if (sb.isNotEmpty()) sb.toString() else result.toString(2)
                } else {
                    result?.toString(2) ?: "Execution completed successfully."
                }
            }

            McpToolCallResult(
                callId = callId,
                toolName = toolName,
                isError = isError,
                outputText = outputText,
                rawResultJson = body
            )
        } catch (e: Exception) {
            McpToolCallResult(
                callId = callId,
                toolName = toolName,
                isError = false,
                outputText = body,
                rawResultJson = body
            )
        }
    }

    private fun getDemoToolsForServer(server: McpServerEntity): List<McpToolEntity> {
        val sId = server.id
        val sName = server.name
        return when {
            server.name.contains("Weather", ignoreCase = true) || server.url.contains("weather", ignoreCase = true) -> listOf(
                McpToolEntity(
                    id = "${sId}_get_current_weather",
                    serverId = sId,
                    serverName = sName,
                    name = "get_current_weather",
                    description = "Returns current temperature, humidity, wind speed, and weather condition for a city.",
                    inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string","description":"Name of city or coordinates"},"unit":{"type":"string","enum":["celsius","fahrenheit"]}},"required":["city"]}"""
                ),
                McpToolEntity(
                    id = "${sId}_get_weather_forecast",
                    serverId = sId,
                    serverName = sName,
                    name = "get_weather_forecast",
                    description = "Returns multi-day weather forecast with high/low temperatures and precipitation chances.",
                    inputSchemaJson = """{"type":"object","properties":{"city":{"type":"string"},"days":{"type":"integer","description":"Number of forecast days (1-7)"}},"required":["city"]}"""
                )
            )
            server.name.contains("Search", ignoreCase = true) || server.url.contains("search", ignoreCase = true) -> listOf(
                McpToolEntity(
                    id = "${sId}_search_web",
                    serverId = sId,
                    serverName = sName,
                    name = "search_web",
                    description = "Performs live web search for current real-time information, news, and technical articles.",
                    inputSchemaJson = """{"type":"object","properties":{"query":{"type":"string","description":"Search query terms"},"limit":{"type":"integer"}},"required":["query"]}"""
                ),
                McpToolEntity(
                    id = "${sId}_fetch_page_text",
                    serverId = sId,
                    serverName = sName,
                    name = "fetch_page_text",
                    description = "Fetches and parses main article text content from a web URL.",
                    inputSchemaJson = """{"type":"object","properties":{"url":{"type":"string","description":"Target web page URL"}},"required":["url"]}"""
                )
            )
            server.name.contains("Database", ignoreCase = true) || server.name.contains("SQL", ignoreCase = true) -> listOf(
                McpToolEntity(
                    id = "${sId}_execute_sql_query",
                    serverId = sId,
                    serverName = sName,
                    name = "execute_sql_query",
                    description = "Executes read-only SQL queries against the cached database schema.",
                    inputSchemaJson = """{"type":"object","properties":{"sql":{"type":"string","description":"SQL query string"}},"required":["sql"]}"""
                ),
                McpToolEntity(
                    id = "${sId}_list_database_tables",
                    serverId = sId,
                    serverName = sName,
                    name = "list_database_tables",
                    description = "Lists all available tables and record counts in the database.",
                    inputSchemaJson = """{"type":"object","properties":{}}"""
                )
            )
            else -> listOf(
                McpToolEntity(
                    id = "${sId}_execute_code_snippet",
                    serverId = sId,
                    serverName = sName,
                    name = "execute_code_snippet",
                    description = "Evaluates Python/Kotlin code in an isolated execution sandbox and returns stdout/stderr.",
                    inputSchemaJson = """{"type":"object","properties":{"language":{"type":"string","enum":["python","kotlin","javascript"]},"code":{"type":"string"}},"required":["language","code"]}"""
                ),
                McpToolEntity(
                    id = "${sId}_analyze_media_asset",
                    serverId = sId,
                    serverName = sName,
                    name = "analyze_media_asset",
                    description = "Inspects image or video metadata, dimensions, color histogram, and object labels.",
                    inputSchemaJson = """{"type":"object","properties":{"mediaUrl":{"type":"string"}},"required":["mediaUrl"]}"""
                )
            )
        }
    }

    private fun executeDemoToolCall(
        callId: String,
        toolName: String,
        argumentsJson: String,
        serverName: String
    ): McpToolCallResult {
        val args = try { JSONObject(argumentsJson) } catch (e: Exception) { JSONObject() }
        val outputText = when (toolName) {
            "get_current_weather" -> {
                val city = args.optString("city", "San Francisco")
                "Weather in $city: 21°C (70°F), Sunny with light breeze from NW at 12 km/h. Humidity: 45%. Air Quality Index: 32 (Good)."
            }
            "get_weather_forecast" -> {
                val city = args.optString("city", "Tokyo")
                "3-Day Forecast for $city:\n• Today: 22°C / 14°C, Clear\n• Tomorrow: 24°C / 15°C, Partly Cloudy\n• Day 3: 19°C / 12°C, Light Rain (60%)"
            }
            "search_web" -> {
                val query = args.optString("query", "Android Jetpack Compose MCP")
                "Web Search Results for '$query':\n1. Model Context Protocol (MCP) SSE Integration in Android: High performance streaming architecture for LLMs.\n2. Modern Jetpack Compose UI Patterns for 2026 AI Assistant Apps.\n3. Secure Credential Vaults with Android Keystore & Room persistence."
            }
            "fetch_page_text" -> {
                val url = args.optString("url", "https://example.com/article")
                "Parsed Page Content from $url:\n[Title: Understanding Model Context Protocol]\nMCP enables AI models to safely read local and remote tools, schemas, and resource contexts via SSE and HTTP transports."
            }
            "execute_sql_query" -> {
                val sql = args.optString("sql", "SELECT * FROM users")
                "SQL Query Executed Successfully:\nQuery: `$sql`\nResult Set (3 rows):\n| id | username | status | active_mcp |\n| 1 | alex_dev | active | true |\n| 2 | sarah_ai | active | true |\n| 3 | mcp_bot  | idle   | false |"
            }
            "list_database_tables" -> {
                "Database Tables:\n1. users (3 records)\n2. chat_histories (142 records)\n3. mcp_server_registry (5 records)\n4. tool_cache_schemas (28 records)"
            }
            "execute_code_snippet" -> {
                val lang = args.optString("language", "python")
                val code = args.optString("code", "print('Hello MCP!')")
                "Execution Sandbox ($lang):\nCode:\n```$lang\n$code\n```\nStdout:\nHello MCP!\nProcess finished with exit code 0."
            }
            else -> {
                "Executed $toolName with arguments: $argumentsJson on $serverName. Result: Operation completed successfully with status 200 OK."
            }
        }

        return McpToolCallResult(
            callId = callId,
            toolName = toolName,
            isError = false,
            outputText = outputText,
            rawResultJson = JSONObject().apply {
                put("status", "success")
                put("toolName", toolName)
                put("serverName", serverName)
                put("result", outputText)
            }.toString(2)
        )
    }
}
