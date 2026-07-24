package com.hermes.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE)
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var baseUrl by remember { mutableStateOf(prefs.getString("base_url", "https://api.openai.com/v1") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-4o") ?: "") }
    var systemPrompt by remember { mutableStateOf(prefs.getString("system_prompt", "") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF16213E))
            )
        },
        containerColor = Color(0xFF1A1A2E)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsField("API Key", apiKey, true) { apiKey = it }
            SettingsField("Base URL", baseUrl) { baseUrl = it }
            SettingsField("模型", model) { model = it }
            SettingsField("系统提示词", systemPrompt, maxLines = 5) { systemPrompt = it }
            Button(
                onClick = {
                    prefs.edit()
                        .putString("api_key", apiKey)
                        .putString("base_url", baseUrl)
                        .putString("model", model)
                        .putString("system_prompt", systemPrompt)
                        .apply()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B68EE))
            ) {
                Text("保存", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsField(label: String, value: String, isPassword: Boolean = false, maxLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = maxLines,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF7B68EE),
            unfocusedBorderColor = Color.Gray,
        )
    )
}
