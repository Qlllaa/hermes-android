package com.hermes.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager

class FloatingWindowService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    // TODO: Implement floating chat window with WindowManager
}

class HermesAccessibilityService : android.accessibilityservice.AccessibilityService() {
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
