# MCP Chatbot 🌐🤖

> Chat with your Model Context Protocol (MCP) servers using cloud LLMs. An open-source, multi-modal, agentic Android client built with modern Jetpack Compose.

Developed & Maintained by **Zyven Technologies Pvt Ltd** as part of the **MCP Admin** product line.
<img width="1500" height="500" alt="Black and Yellow Typographic Profile Twitter Header" src="https://github.com/user-attachments/assets/62cd522a-7ee4-4a6a-a3e0-e8f075ad5380" />

---

<p align="center">
  <img src="https://news.mcpadmin.cloud/wp-content/uploads/2026/10/Gemini_Generated_Image_9n5n8q9n5n8q9n5ns.png" width="120" alt="Zyven Technologies Logo" style="border-radius: 20px;"/>
</p>

---

## 🌟 Key Features

### 🔄 Autonomous Multi-Turn Agent Loop (NEW)
- **Automatic Call-and-Response Loop**: The app's engine runs an automated reasoning loop. When an LLM issues an MCP tool call request, the client intercepts it, executes the tool on the target server, updates the conversation state, and **immediately fires a follow-up turn** to the LLM containing the tool's raw output.
- **Conversational Synthesis**: The LLM reads the result, processes the data, and streams a human-readable final answer back to the user, providing a truly autonomous agent experience.

### ✍️ Premium Markdown & Code Block Parser (NEW)
- **Clean Block Splitting**: Separates chat content into formatted paragraphs and multi-line code blocks.
- **Rich Elements**: Fully supports bold formatting (`**bold**`), inline code blocks (`` `code` ``), ordered/numbered lists, bullet points, and headers (`#`, `##`, `###`).
- **Interactive Code Cards**: Displays programming snippets inside a beautifully styled dark terminal card with a dynamic language header and a **functional "COPY" button** linked straight to the Android clipboard.

### 🛡️ Persistent Configuration & Sync (NEW)
- **Memory Across Sessions**: Your chosen LLM provider and model configuration are preserved persistently. It will never reset when you launch a new chat, switch views, or restart the application.
- **First-Run Configured Default**: On initial startup or when starting a blank chat, the app automatically maps and seeds the active LLM provider list and defaults to your first configured provider instead of falling back to unconfigured templates.

### 🔌 Real-Time MCP Protocol Support
- **Direct Transport Support**: Standard-compliant client for both Server-Sent Events (SSE) and HTTP JSON-RPC protocols.
- **Exposed Tools Registry**: Seamlessly queries MCP servers for active tools (`tools/list`) and executes actions dynamically (`tools/call`).
- **Offline Cached Schemas**: Automatically saves server tool lists and JSON schemas in a local Room SQLite cache so you can inspect schemas offline.
- **Connection Dashboard**: View server health gauges (Connected, Connecting, Error), ping endpoints, or register new ones.

### 🔒 Multi-Provider Secure Vault
- **Supported Backends**: Direct native stream processing for **Google Gemini**, **OpenAI**, **Anthropic Claude**, and **OpenRouter**.
- **Local Obfuscated Encryption**: Keeps user API credentials secure using transformation layers and obfuscated SharedPreferences storage.
- **Intelligent Fallback Engine**: If no key is set, the built-in Smart Assistant processes local voice, media, and document contexts on-device.

### 🧠 Advanced Chat Mechanics
- **Dynamic Context Window**: Slider adjustment (2 to 50 turns) to optimize latency, context limits, and token counts.
- **Real-Time Step View**: Displays real-time accordion logs for active tool invocations, arguments JSON, server execution states, and response renders.
- **Multi-Modal Playback**: Handles image lightbox viewer, audio notes with wave visualizer, interactive video players, and parsed document preview chips.

---

## ⚙️ Tech Stack & Architecture

- **UI Framework**: Jetpack Compose (Material Design 3) with dynamic color support
- **Asynchronous Flow**: Kotlin Coroutines & Reactive Flows
- **Local Persistence**: Room Database (SQLite) with KSP compilation
- **Network Engine**: OkHttp (SSE, REST, and JSON-RPC 2.0)
- **Image Loader**: Coil (multi-modal base64 and remote caching)

---

## 📦 Building and Running

### Prerequisites
- Android Studio Ladybug or later
- JDK 17+
- Android SDK 24+ (MinSDK 24, TargetSDK 36)

### Local Build Commands
To compile and assemble the final debug APK:
```bash
gradle assembleDebug
```

The compiled APK will be output in the root folder at:
`./MCP_Chatbot.apk`

---

## 🏢 Connected Ecosystem

1. **MCP Admin Hub**: Create and host instant, compliant MCP servers with 1-click deployments on [mcpadmin.cloud](https://mcpadmin.cloud).
2. **Zyven Technologies**: Discover enterprise-grade AI toolkits and developer ecosystems at [zyven-technologies.com](http://zyven-technologies.com).

---

## 📄 License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.
*(C) 2026 Zyven Technologies Pvt Ltd. All rights reserved.*
