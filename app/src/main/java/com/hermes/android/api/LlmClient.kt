package com.hermes.android.api

import com.hermes.android.api.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class LlmClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
        isLenient = true
    }

    private val mediaType = "application/json".toMediaType()

    suspend fun chat(
        messages: List<ChatMessage>,
        temperature: Double? = null,
        maxTokens: Int? = null,
        tools: List<ToolDefinition>? = null
    ): ChatCompletionResponse {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = false,
            tools = tools
        )
        val body = json.encodeToString(request).toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            val error = try { json.decodeFromString<ApiError>(responseBody) } catch (_: Exception) { null }
            throw LlmException(response.code, error?.error?.message ?: responseBody)
        }
        return json.decodeFromString(responseBody)
    }

    suspend fun chatStream(
        messages: List<ChatMessage>,
        temperature: Double? = null,
        maxTokens: Int? = null,
        tools: List<ToolDefinition>? = null
    ): Flow<StreamChunk> = flow {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = true,
            tools = tools
        )
        val body = json.encodeToString(request).toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val error = try { json.decodeFromString<ApiError>(responseBody) } catch (_: Exception) { null }
            throw LlmException(response.code, error?.error?.message ?: responseBody)
        }

        val source = response.body?.source() ?: throw LlmException(-1, "Empty response body")
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ")
                if (data == "[DONE]") break
                try {
                    val chunk = json.decodeFromString<StreamChunk>(data)
                    emit(chunk)
                } catch (_: Exception) { }
            }
        }
    }.flowOn(Dispatchers.IO)
}

class LlmException(val code: Int, message: String) : Exception(message)
