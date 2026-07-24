package com.hermes.android.ui.screens.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.chat.ChatEngine
import com.hermes.android.chat.ChatEvent
import com.hermes.android.chat.ChatEventType
import com.hermes.android.data.db.ChatRepository
import com.hermes.android.data.model.MessageEntity
import kotlinx.coroutines.launch

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val chatRepo = remember { ChatRepository(context) }
    val chatEngine = remember { ChatEngine(context) }
    val scope = rememberCoroutineScope()

    var currentChatId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    var toolCallInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Initialize or load chat
    LaunchedEffect(Unit) {
        val chats = chatRepo.getChats()
        currentChatId = chats.firstOrNull()?.id ?: chatRepo.createChat().id
        currentChatId?.let { messages = chatRepo.getMessages(it) }
    }

    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            listState.animateScrollToItem((messages.size + if (streamingContent.isNotEmpty()) 1 else 0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hermes") },
                actions = {
                    IconButton(onClick = {
                        val newChat = chatRepo.createChat()
                        currentChatId = newChat.id
                        messages = emptyList()
                        streamingContent = ""
                        error = null
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "新对话")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg)
                }
                if (streamingContent.isNotEmpty()) {
                    item {
                        StreamingBubble(streamingContent)
                    }
                }
                if (toolCallInfo != null) {
                    item {
                        ToolCallBubble(toolCallInfo!!.first, toolCallInfo!!.second)
                    }
                }
                if (error != null) {
                    item {
                        ErrorBubble(error!!)
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息…") },
                    maxLines = 4,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val chatId = currentChatId ?: chatRepo.createChat().id.also { currentChatId = it }
                            val userMsg = inputText
                            inputText = ""
                            isLoading = true
                            streamingContent = ""
                            error = null
                            scope.launch {
                                chatEngine.chat(chatId, userMsg).collect { event ->
                                    when (event.type) {
                                        ChatEventType.USER_MESSAGE -> {
                                            messages = chatRepo.getMessages(chatId)
                                        }
                                        ChatEventType.ASSISTANT_MESSAGE -> {
                                            streamingContent = event.content
                                        }
                                        ChatEventType.TOOL_CALL_START -> {
                                            toolCallInfo = event.toolName to (event.toolArgs ?: "")
                                        }
                                        ChatEventType.TOOL_CALL_END -> {
                                            toolCallInfo = null
                                            messages = chatRepo.getMessages(chatId)
                                        }
                                        ChatEventType.ASSISTANT_DONE -> {
                                            messages = chatRepo.getMessages(chatId)
                                            streamingContent = ""
                                            isLoading = false
                                        }
                                        ChatEventType.ERROR -> {
                                            error = event.error
                                            isLoading = false
                                            messages = chatRepo.getMessages(chatId)
                                        }
                                        ChatEventType.THINKING -> { /* could show typing indicator */ }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: MessageEntity) {
    val isUser = msg.role == "user"
    val isTool = msg.role == "tool"
    val alignment = when {
        isUser -> Alignment.End
        isTool -> Alignment.Center
        else -> Alignment.Start
    }
    val bgColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isTool -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isTool -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (isTool) {
            Text(
                text = "[Tool: ${msg.name}]",
                style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = msg.content,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun StreamingBubble(content: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Text(
            text = content,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ToolCallBubble(name: String, args: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "调用工具: $name",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun ErrorBubble(msg: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = msg,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp
        )
    }
}
