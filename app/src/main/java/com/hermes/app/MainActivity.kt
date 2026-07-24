package com.hermes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HermesTheme { HermesApp() } }
    }
}

@Composable
fun HermesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7B68EE),
            background = Color(0xFF1A1A2E),
            surface = Color(0xFF16213E),
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content
    )
}

@Composable
fun HermesApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") { ChatScreen(onSettingsClick = { navController.navigate("settings") }) }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onSettingsClick: () -> Unit) {
    val viewModel: HermesViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hermes", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF16213E)),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFF1A1A2E)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message -> MessageBubble(message) }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
            if (isRunning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF7B68EE))
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7B68EE),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF7B68EE),
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) { viewModel.sendMessage(input); input = "" }
                    },
                    enabled = !isRunning
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送", tint = Color(0xFF7B68EE))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) Color(0xFF7B68EE) else Color(0xFF16213E),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(text = message.content, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(10.dp))
        }
    }
}
