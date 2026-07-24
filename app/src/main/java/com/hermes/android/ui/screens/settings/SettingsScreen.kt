package com.hermes.android.ui.screens.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.android.data.prefs.SettingsManager
import com.hermes.android.data.prefs.ApiConfig
import com.hermes.android.data.prefs.UiConfig
import com.hermes.android.data.prefs.ToolConfig
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val apiConfig by SettingsManager.apiConfig(context).collectAsState(initial = ApiConfig())
    val uiConfig by SettingsManager.uiConfig(context).collectAsState(initial = UiConfig())

    var baseUrl by remember(apiConfig.baseUrl) { mutableStateOf(apiConfig.baseUrl) }
    var apiKey by remember(apiConfig.apiKey) { mutableStateOf(apiConfig.apiKey) }
    var model by remember(apiConfig.model) { mutableStateOf(apiConfig.model) }
    var systemPrompt by remember(apiConfig.systemPrompt) { mutableStateOf(apiConfig.systemPrompt) }
    var temperature by remember(apiConfig.temperature) { mutableStateOf(apiConfig.temperature.toString()) }
    var maxTokens by remember(apiConfig.maxTokens) { mutableStateOf(apiConfig.maxTokens.toString()) }

    var themeMode by remember(uiConfig.themeMode) { mutableStateOf(uiConfig.themeMode) }
    var dynamicColor by remember(uiConfig.dynamicColor) { mutableStateOf(uiConfig.dynamicColor) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("API 配置", style = MaterialTheme.typography.headlineSmall)
        Text("配置 LLM 接口的密钥和地址，兼容 OpenAI 格式", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("API 地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("模型名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            label = { Text("系统提示词") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )
        OutlinedTextField(
            value = temperature,
            onValueChange = { temperature = it },
            label = { Text("温度 (0-2)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = maxTokens,
            onValueChange = { maxTokens = it },
            label = { Text("最大 Token 数") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row {
            Button(
                onClick = {
                    scope.launch {
                        SettingsManager.updateApiConfig(context, ApiConfig(
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model,
                            systemPrompt = systemPrompt,
                            temperature = temperature.toDoubleOrNull() ?: 0.7,
                            maxTokens = maxTokens.toIntOrNull() ?: 4096
                        ))
                    }
                }
            ) { Text("保存") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = {
                // Paste from clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                apiKey = clip
            }) { Text("粘贴 Key") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("外观", style = MaterialTheme.typography.headlineSmall)

        Text("主题模式", fontSize = 14.sp)
        Row {
            listOf("light" to "浅色", "dark" to "深色", "system" to "跟随系统").forEach { (mode, label) ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { themeMode = mode },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("动态取色", modifier = Modifier.weight(1f))
            Switch(checked = dynamicColor, onCheckedChange = { dynamicColor = it })
        }

        Button(onClick = {
            scope.launch {
                SettingsManager.updateUiConfig(context, UiConfig(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    fontSize = uiConfig.fontSize
                ))
            }
        }) { Text("保存外观设置") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("关于", style = MaterialTheme.typography.headlineSmall)
        Text("Hermes Android v1.0.0", fontSize = 14.sp)
        Text("开源 AI Agent · 兼容 OpenAI API", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(32.dp))
    }
}
