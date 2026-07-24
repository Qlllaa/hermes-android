package com.hermes.android.chat

import android.content.Context
import com.hermes.android.api.LlmClient
import com.hermes.android.api.models.*
import com.hermes.android.data.db.ChatRepository
import com.hermes.android.data.db.MemoryRepository
import com.hermes.android.data.model.MessageEntity
import com.hermes.android.data.prefs.SettingsManager
import com.hermes.android.tools.ToolRegistry
import com.hermes.android.tools.builtin.BuiltinTools
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

import kotlinx.coroutines.flow.first

data class ChatEvent(
    val type: ChatEventType,
    val content: String = "",
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolResult: String? = null,
    val error: String? = null
)

enum class ChatEventType {
    USER_MESSAGE, ASSISTANT_MESSAGE, TOOL_CALL_START, TOOL_CALL_END, 
    ASSISTANT_DONE, ERROR, THINKING
}

class ChatEngine(private val context: Context) {
    private val chatRepo = ChatRepository(context)
    private val memoryRepo = MemoryRepository(context)
    private val json = Json { ignoreUnknownKeys = true }

    init {
        BuiltinTools.registerAll()
    }

    fun chat(
        chatId: String,
        userMessage: String
    ): Flow<ChatEvent> = flow {
        // Save user message
        chatRepo.addMessage(MessageEntity(chatId = chatId, role = "user", content = userMessage))
        emit(ChatEvent(ChatEventType.USER_MESSAGE, userMessage))

        // Get API config
        val apiConfig = SettingsManager.apiConfig(context)
        val apiConfigValue = apiConfig.first()
        val toolConfig = SettingsManager.toolConfig(context).first()

        if (apiConfigValue.apiKey.isBlank()) {
            emit(ChatEvent(ChatEventType.ERROR, error = "未配置 API Key，请先在设置中填写"))
            return@flow
        }

        val client = LlmClient(
            baseUrl = apiConfigValue.baseUrl,
            apiKey = apiConfigValue.apiKey,
            model = apiConfigValue.model
        )

        // Build messages
        val messages = mutableListOf<ChatMessage>()
        // System prompt with memory context
        val memories = memoryRepo.getAll().take(20).joinToString("\n") { "- ${it.content}" }
        val systemPrompt = buildString {
            append(apiConfigValue.systemPrompt)
            if (memories.isNotBlank()) {
                append("\n\n## Memory\nYou remember these facts about the user:\n")
                append(memories)
            }
        }
        messages.add(ChatMessage("system", systemPrompt))

        // Add history
        chatRepo.getMessages(chatId).forEach { msg ->
            val tc: List<ToolCall>? = if (!msg.toolCalls.isNullOrEmpty()) {
                try { json.decodeFromString<List<ToolCall>>(msg.toolCalls) } catch (_: Exception) { null }
            } else null
            messages.add(ChatMessage(
                role = msg.role,
                content = msg.content,
                toolCalls = tc,
                toolCallId = msg.toolCallId
            ))
        }

        // Get tool definitions
        val toolDefs = ToolRegistry.toDefinitions(toolConfig.enabledTools)
        var iterations = 0
        val maxIterations = toolConfig.maxToolIterations

        // LLM conversation loop with tool calling
        while (iterations <= maxIterations) {
            iterations++
            emit(ChatEvent(ChatEventType.THINKING))

            val response = try {
                client.chat(
                    messages = messages,
                    temperature = apiConfigValue.temperature,
                    maxTokens = apiConfigValue.maxTokens,
                    tools = toolDefs.ifEmpty { null }
                )
            } catch (e: LlmException) {
                emit(ChatEvent(ChatEventType.ERROR, error = e.message))
                return@flow
            } catch (e: Exception) {
                emit(ChatEvent(ChatEventType.ERROR, error = "请求失败: ${e.message}"))
                return@flow
            }

            val assistantMessage = response.choices.firstOrNull()?.message
                ?: run {
                    emit(ChatEvent(ChatEventType.ERROR, error = "空响应"))
                    return@flow
                }

            // If no tool calls, we're done
            val toolCalls = assistantMessage.toolCalls
            if (toolCalls.isNullOrEmpty()) {
                // Save and emit final message
                chatRepo.addMessage(MessageEntity(
                    chatId = chatId, role = "assistant", content = assistantMessage.content
                ))
                emit(ChatEvent(ChatEventType.ASSISTANT_MESSAGE, assistantMessage.content))
                emit(ChatEvent(ChatEventType.ASSISTANT_DONE))
                break
            }

            // Save assistant message with tool calls
            val toolCallsJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(ToolCall.serializer()), toolCalls
            )
            val assistantEntity = MessageEntity(
                chatId = chatId,
                role = "assistant",
                content = assistantMessage.content,
                toolCalls = toolCallsJson
            )
            chatRepo.addMessage(assistantEntity)
            messages.add(assistantMessage)

            // Execute each tool call
            if (toolConfig.autoExecuteTools) {
                for (tc in toolCalls) {
                    emit(ChatEvent(ChatEventType.TOOL_CALL_START, toolName = tc.function.name, toolArgs = tc.function.arguments))
                    
                    val tool = ToolRegistry.get(tc.function.name)
                    val result = if (tool != null) {
                        val argsMap = parseToolArgs(tc.function.arguments)
                        tool.execute(argsMap)
                    } else {
                        com.hermes.android.tools.ToolResult(false, "Unknown tool: ${tc.function.name}")
                    }

                    emit(ChatEvent(ChatEventType.TOOL_CALL_END, toolName = tc.function.name, toolResult = result.output))

                    val toolMessage = ChatMessage(
                        role = "tool",
                        content = result.output,
                        toolCallId = tc.id,
                        name = tc.function.name
                    )
                    messages.add(toolMessage)
                    chatRepo.addMessage(MessageEntity(
                        chatId = chatId,
                        role = "tool",
                        content = result.output,
                        toolCallId = tc.id,
                        name = tc.function.name
                    ))
                }
            } else {
                // Don't execute, just stop and ask user
                emit(ChatEvent(ChatEventType.ASSISTANT_MESSAGE, assistantMessage.content))
                emit(ChatEvent(ChatEventType.ASSISTANT_DONE))
                break
            }
        }

        if (iterations > maxIterations) {
            emit(ChatEvent(ChatEventType.ERROR, error = "达到最大工具调用次数 ($maxIterations)"))
        }
    }

    private fun parseToolArgs(argsJson: String): Map<String, String> {
        return try {
            val parsed = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(argsJson)
            parsed.mapValues { (_, v) -> 
                v.toString().trim('"').replace("\\\"", "\"").replace("\\n", "\n")
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
