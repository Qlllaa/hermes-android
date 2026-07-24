package com.hermes.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ChatMessage(val role: String, val content: String)

class HermesViewModel(app: Application) : AndroidViewModel(app) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    private var service: HermesService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as HermesService.LocalBinder).getService()
            service?.onOutput = { line ->
                addMessage("assistant", line)
                _isRunning.value = false
            }
        }
        override fun onServiceDisconnected(name: ComponentName) { service = null }
    }
    init {
        val intent = Intent(app, HermesService::class.java)
        app.startForegroundService(intent)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    fun sendMessage(text: String) {
        addMessage("user", text)
        _isRunning.value = true
        service?.sendInput(text)
    }
    private fun addMessage(role: String, content: String) {
        _messages.value = _messages.value + ChatMessage(role, content)
    }
}
