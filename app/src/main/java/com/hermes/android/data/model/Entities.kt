package com.hermes.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class MessageEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chatId: String,
    val role: String,
    val content: String,
    val toolCalls: String? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokens: Int = 0
)

@Serializable
data class MemoryEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val category: String = "general",
    val createdAt: Long = System.currentTimeMillis()
)
