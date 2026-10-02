# MCP Chatbot 🌐🤖

> Chat with your MCP servers using cloud LLMs. An open-source, multi-modal Android client built with modern Jetpack Compose.

Developed & Maintained by **Zyven Technologies Pvt Ltd** as part of the **MCP Admin** product line.

---

<p align="center">
  <img src="https://news.mcpadmin.cloud/wp-content/uploads/2026/10/Gemini_Generated_Image_9n5n8q9n5n8q9n5ns.png" width="120" alt="Zyven Technologies Logo" style="border-radius: 20px;"/>
</p>

---

## 🌟 Key Features

### 🔌 Real-Time MCP (Model Context Protocol) Integration
- **Direct Transport Support**: Standard-compliant client for both SSE (Server-Sent Events) and HTTP JSON-RPC protocols.
- **Exposed Tools Registry**: Seamlessly queries MCP servers for active tools (`tools/list`) and executes actions dynamically (`tools/call`).
- **Offline Cached Schemas**: Automatically saves server tool lists and JSON schemas in a local Room SQLite cache so you can inspect schemas offline.
- **Connection Dashboard**: View server health gauges (Connected, Connecting, Error), ping endpoints, or register new ones.

### 🛡️ Multi-Provider Secure Vault
- **Supported Backends**: Direct native stream processing for **Google Gemini**, **OpenAI**, **Anthropic Claude**, and **OpenRouter**.
- **Local Obfuscated Encryption**: Keeps user API credentials secure using transformation layers and obfuscated SharedPreferences storage.
- **Intelligent Fallback Engine**: If no key is set, the built-in Smart Assistant processes local voice, media, and document contexts on device.

### 🧠 Advanced Chat Mechanics
- **Dynamic Context Window**: Slider adjustment (2 to 50 turns) to optimize latency, context limits, and token counts.
- **Real-Time Step View**: Displays real-time accordion logs for active tool invocations, arguments JSON, server execution states, and response renders.
- **Multi-Modal Playback**: Handles image lightbox viewer, audio notes with wave visualizer, interactive video players, and parsed document preview chips.

---

## ⚙️ Tech Stack & Architecture

- **UI Framework**: Jetpack Compose (Material Design 3)
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
To compile and assemble the debug APK:
```bash
./gradlew assembleDebug
```

The compiled APK will be output in:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🏢 Connected Ecosystem

1. **MCP Admin Hub**: Create and host instant, compliant MCP servers with 1-click deployments on [mcpadmin.cloud](https://mcpadmin.cloud).
2. **Zyven Technologies**: Discover enterprise-grade AI toolkits and developer ecosystems at [zyven-technologies.com](http://zyven-technologies.com).

---

## 📄 License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.
*(C) 2026 Zyven Technologies Pvt Ltd. All rights reserved.*
