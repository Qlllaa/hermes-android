# Hermes Android

A native Android AI Agent app inspired by [Operit](https://github.com/AAswordman/Operit), with a fully customizable LLM backend (OpenAI-compatible API).

## Features

- **AI Chat** with streaming responses and tool calling
- **Custom API Key** — connect to any OpenAI-compatible endpoint (OpenAI, DeepSeek, Ollama, vLLM, LMStudio, etc.)
- **Tool System** — 8 built-in tools: web search, calculator, HTTP requests, file read/write, shell commands, clipboard, screenshot
- **Memory** — persistent memory across conversations
- **Material 3 UI** with Jetpack Compose, dynamic color support, light/dark theme
- **Foreground Service** to keep the chat running in background
- **Floating Window** (placeholder) and Accessibility Service (placeholder) for future automation
- **GitHub Actions CI** — automatic APK builds on push and releases on tags

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 Design
- OkHttp for API calls
- DataStore for preferences
- kotlinx.serialization for JSON
- No DI framework — lightweight manual dependency injection

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

Or push to GitHub — the CI workflow builds and uploads APKs automatically.

## Configuration

1. Open the app
2. Go to Settings
3. Enter your API Base URL (e.g. `https://api.openai.com`)
4. Enter your API Key
5. Enter model name (e.g. `gpt-4o`, `deepseek-chat`, `llama-3.1-70b`)
6. Save and start chatting

## Roadmap

- [ ] Streaming chat responses (SSE)
- [ ] Floating chat window
- [ ] Voice input (STT) and TTS
- [ ] Local terminal with full shell access
- [ ] File manager UI
- [ ] Accessibility-based UI automation (like Operit)
- [ ] MCP plugin support
- [ ] Character cards / personas
- [ ] Local model inference (llama.cpp / MNN)
- [ ] Workflow automation
- [ ] Deep search

## License

MIT
