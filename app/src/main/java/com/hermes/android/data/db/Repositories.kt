package com.hermes.android.data.db

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.hermes.android.data.model.*
import java.io.File

class ChatRepository(context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dataDir = File(context.filesDir, "chats").apply { mkdirs() }

    fun createChat(): ChatEntity {
        val chat = ChatEntity()
        saveChat(chat)
        return chat
    }

    fun getChats(): List<ChatEntity> {
        return dataDir.listFiles()?.mapNotNull { f ->
            try { json.decodeFromString<ChatEntity>(f.readText()) } catch (_: Exception) { null }
        }?.sortedByDescending { it.updatedAt } ?: emptyList()
    }

    fun getChat(chatId: String): ChatEntity? {
        val file = File(dataDir, "$chatId.json")
        return if (file.exists()) {
            try { json.decodeFromString(file.readText()) } catch (_: Exception) { null }
        } else null
    }

    fun saveChat(chat: ChatEntity) {
        File(dataDir, "${chat.id}.json").writeText(json.encodeToString(chat))
    }

    fun deleteChat(chatId: String) {
        File(dataDir, "$chatId.json").delete()
        File(dataDir, "${chatId}_messages.json").delete()
    }

    fun getMessages(chatId: String): List<MessageEntity> {
        val file = File(dataDir, "${chatId}_messages.json")
        return if (file.exists()) {
            try { json.decodeFromString<List<MessageEntity>>(file.readText()) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    fun addMessage(message: MessageEntity) {
        val messages = getMessages(message.chatId).toMutableList()
        messages.add(message)
        File(dataDir, "${message.chatId}_messages.json")
            .writeText(json.encodeToString(messages.toList()))
        // Update chat timestamp
        getChat(message.chatId)?.let { chat ->
            saveChat(chat.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateMessage(message: MessageEntity) {
        val messages = getMessages(message.chatId).toMutableList()
        val idx = messages.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            messages[idx] = message
            File(dataDir, "${message.chatId}_messages.json")
                .writeText(json.encodeToString(messages.toList()))
        }
    }

    fun clearMessages(chatId: String) {
        File(dataDir, "${chatId}_messages.json").delete()
    }
}

class MemoryRepository(context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dataDir = File(context.filesDir, "memories").apply { mkdirs() }
    private val indexFile = File(dataDir, "_index.json")

    fun getAll(): List<MemoryEntity> {
        return dataDir.listFiles()?.filter { it.name != "_index.json" }?.mapNotNull { f ->
            try { json.decodeFromString<MemoryEntity>(f.readText()) } catch (_: Exception) { null }
        }?.sortedByDescending { it.createdAt } ?: emptyList()
    }

    fun search(query: String): List<MemoryEntity> {
        return getAll().filter { 
            it.content.contains(query, ignoreCase = true) || 
            it.category.contains(query, ignoreCase = true) 
        }
    }

    fun add(content: String, category: String = "general"): MemoryEntity {
        val memory = MemoryEntity(content = content, category = category)
        File(dataDir, "${memory.id}.json").writeText(json.encodeToString(memory))
        return memory
    }

    fun delete(id: String) {
        File(dataDir, "$id.json").delete()
    }
}
