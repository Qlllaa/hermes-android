package com.hermes.android.ui.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.android.data.prefs.SettingsManager
import com.hermes.android.data.prefs.ToolConfig
import com.hermes.android.tools.ToolRegistry
import com.hermes.android.tools.builtin.BuiltinTools
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toolConfig by SettingsManager.toolConfig(context).collectAsState(initial = ToolConfig())
    var enabledTools by remember { mutableStateOf(toolConfig.enabledTools) }

    LaunchedEffect(toolConfig) {
        enabledTools = toolConfig.enabledTools
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("工具管理", style = MaterialTheme.typography.headlineSmall)
        Text("启用或禁用 AI 可调用的工具", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(ToolRegistry.all()) { tool ->
                ListItem(
                    headlineContent = { Text(tool.name) },
                    supportingContent = { Text(tool.description, fontSize = 12.sp, maxLines = 2) },
                    trailingContent = {
                        Switch(
                            checked = tool.name in enabledTools,
                            onCheckedChange = { enabled ->
                                enabledTools = if (enabled) enabledTools + tool.name
                                               else enabledTools - tool.name
                                scope.launch {
                                    SettingsManager.updateToolConfig(context, 
                                        ToolConfig(enabledTools = enabledTools)
                                    )
                                }
                            }
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
