package com.hermes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hermes.android.data.prefs.SettingsManager
import com.hermes.android.ui.theme.HermesTheme
import com.hermes.android.ui.screens.chat.ChatScreen
import com.hermes.android.ui.screens.settings.SettingsScreen
import com.hermes.android.ui.screens.tools.ToolsScreen
import com.hermes.android.ui.components.HermesBottomBar
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = SettingsManager(this)

        setContent {
            val themeMode = settingsManager.themeMode.collectAsState(initial = "system")
            val dynamicColor = settingsManager.dynamicColor.collectAsState(initial = false)

            HermesTheme(
                themeMode = themeMode.value,
                dynamicColor = dynamicColor.value
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Scaffold(
                        bottomBar = { HermesBottomBar(navController) }
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = "chat",
                            modifier = Modifier.padding(padding)
                        ) {
                            composable("chat") { ChatScreen() }
                            composable("tools") { ToolsScreen() }
                            composable("settings") { SettingsScreen() }
                        }
                    }
                }
            }
        }
    }
}
