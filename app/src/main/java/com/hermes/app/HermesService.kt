package com.hermes.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import java.io.*

class HermesService : Service() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    var onOutput: ((String) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService() = this@HermesService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        startHermes()
    }

    private fun buildNotification(): Notification {
        val channelId = "hermes_service"
        val channel = NotificationChannel(channelId, "Hermes", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(this, channelId)
            .setContentTitle("Hermes 运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun startHermes() {
        scope.launch {
            val filesDir = applicationContext.filesDir
            val pythonPath = File(filesDir, "usr/bin/python3").absolutePath
            val hermesPath = File(filesDir, "hermes/main.py").absolutePath
            val prefs = applicationContext.getSharedPreferences("hermes_prefs", MODE_PRIVATE)
            val apiKey = prefs.getString("api_key", "") ?: ""
            val baseUrl = prefs.getString("base_url", "https://api.openai.com/v1") ?: ""
            val model = prefs.getString("model", "gpt-4o") ?: ""
            val pb = ProcessBuilder(pythonPath, hermesPath)
            pb.environment()["OPENAI_API_KEY"] = apiKey
            pb.environment()["OPENAI_BASE_URL"] = baseUrl
            pb.environment()["HERMES_MODEL"] = model
            pb.environment()["HOME"] = filesDir.absolutePath
            pb.redirectErrorStream(true)
            process = pb.start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            val reader = BufferedReader(InputStreamReader(process!!.inputStream))
            var line: String?
            val buffer = StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                buffer.appendLine(line)
                if (line.isNullOrBlank() && buffer.isNotBlank()) {
                    onOutput?.invoke(buffer.toString().trim())
                    buffer.clear()
                }
            }
        }
    }

    fun sendInput(text: String) {
        scope.launch {
            writer?.write(text)
            writer?.newLine()
            writer?.flush()
        }
    }

    override fun onDestroy() {
        process?.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
