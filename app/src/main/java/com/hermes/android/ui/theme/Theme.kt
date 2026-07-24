package com.hermes.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF3B1B9A),
    primaryContainer = Color(0xFF5633AC),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFF8FCCD0),
    onSecondary = Color(0xFF00373B),
    secondaryContainer = Color(0xFF1E4E52),
    onSecondaryContainer = Color(0xFFABE8EC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF1F1B24),
    onSurface = Color(0xFFE5E1E9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF20005E),
    secondary = Color(0xFF38696D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBCEDEF),
    onSecondaryContainer = Color(0xFF002022),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun HermesTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColors
        else -> LightColors
    }

    // Set status bar color
    (context as? Activity)?.window?.statusBarColor = colorScheme.background.toArgb()

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
