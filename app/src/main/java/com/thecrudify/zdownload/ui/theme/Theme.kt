package com.thecrudify.zdownload.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.thecrudify.zdownload.data.prefs.ThemeMode

@Composable
fun ZDownloadTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        isDark -> DarkFallbackScheme
        else -> LightFallbackScheme
    }

    // Bars are transparent (enableEdgeToEdge); only their icon contrast follows the in-app theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
