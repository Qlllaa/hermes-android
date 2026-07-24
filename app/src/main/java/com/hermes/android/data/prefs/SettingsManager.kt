package com.hermes.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hermes_settings")

data class ApiConfig(
    val baseUrl: String = "https://api.openai.com",
    val apiKey: String = "",
    val model: String = "gpt-4o",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val systemPrompt: String = "You are a helpful AI assistant."
)

data class UiConfig(
    val themeMode: String = "system",
    val dynamicColor: Boolean = false,
    val fontSize: Float = 1.0f
)

data class ToolConfig(
    val enabledTools: Set<String> = defaultEnabledTools,
    val autoExecuteTools: Boolean = true,
    val maxToolIterations: Int = 10
)

object SettingsManager {
    private val KEY_BASE_URL = stringPreferencesKey("base_url")
    private val KEY_API_KEY = stringPreferencesKey("api_key")
    private val KEY_MODEL = stringPreferencesKey("model")
    private val KEY_TEMP = doublePreferencesKey("temperature")
    private val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
    private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    private val KEY_FONT_SIZE = floatPreferencesKey("font_size")
    private val KEY_ENABLED_TOOLS = stringSetPreferencesKey("enabled_tools")
    private val KEY_AUTO_EXECUTE = booleanPreferencesKey("auto_execute_tools")
    private val KEY_MAX_ITERATIONS = intPreferencesKey("max_tool_iterations")

    val defaultEnabledTools = setOf(
        "web_search", "calculator", "file_read", "file_write",
        "shell_command", "http_request", "screenshot", "clipboard"
    )

    fun apiConfig(context: Context): Flow<ApiConfig> = context.dataStore.data.map { p ->
        ApiConfig(
            baseUrl = p[KEY_BASE_URL] ?: "https://api.openai.com",
            apiKey = p[KEY_API_KEY] ?: "",
            model = p[KEY_MODEL] ?: "gpt-4o",
            temperature = p[KEY_TEMP] ?: 0.7,
            maxTokens = p[KEY_MAX_TOKENS] ?: 4096,
            systemPrompt = p[KEY_SYSTEM_PROMPT] ?: "You are a helpful AI assistant."
        )
    }

    fun uiConfig(context: Context): Flow<UiConfig> = context.dataStore.data.map { p ->
        UiConfig(
            themeMode = p[KEY_THEME_MODE] ?: "system",
            dynamicColor = p[KEY_DYNAMIC_COLOR] ?: false,
            fontSize = p[KEY_FONT_SIZE] ?: 1.0f
        )
    }

    fun toolConfig(context: Context): Flow<ToolConfig> = context.dataStore.data.map { p ->
        ToolConfig(
            enabledTools = p[KEY_ENABLED_TOOLS] ?: defaultEnabledTools,
            autoExecuteTools = p[KEY_AUTO_EXECUTE] ?: true,
            maxToolIterations = p[KEY_MAX_ITERATIONS] ?: 10
        )
    }

    suspend fun updateApiConfig(context: Context, config: ApiConfig) {
        context.dataStore.edit { p ->
            p[KEY_BASE_URL] = config.baseUrl
            p[KEY_API_KEY] = config.apiKey
            p[KEY_MODEL] = config.model
            p[KEY_TEMP] = config.temperature
            p[KEY_MAX_TOKENS] = config.maxTokens
            p[KEY_SYSTEM_PROMPT] = config.systemPrompt
        }
    }

    suspend fun updateUiConfig(context: Context, config: UiConfig) {
        context.dataStore.edit { p ->
            p[KEY_THEME_MODE] = config.themeMode
            p[KEY_DYNAMIC_COLOR] = config.dynamicColor
            p[KEY_FONT_SIZE] = config.fontSize
        }
    }

    suspend fun updateToolConfig(context: Context, config: ToolConfig) {
        context.dataStore.edit { p ->
            p[KEY_ENABLED_TOOLS] = config.enabledTools
            p[KEY_AUTO_EXECUTE] = config.autoExecuteTools
            p[KEY_MAX_ITERATIONS] = config.maxToolIterations
        }
    }
}
