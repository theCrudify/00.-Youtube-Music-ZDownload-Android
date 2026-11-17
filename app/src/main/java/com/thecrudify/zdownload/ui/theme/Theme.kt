package com.thecrudify.zdownload.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.thecrudify.zdownload.data.prefs.ThemeMode

private val DarkMaterialColorScheme = darkColorScheme(
    primary = DarkAppColors.primary,
    onPrimary = DarkAppColors.onPrimary,
    secondary = DarkAppColors.secondary,
    onSecondary = DarkAppColors.onSecondary,
    background = DarkAppColors.background,
    onBackground = DarkAppColors.textPrimary,
    surface = DarkAppColors.surfaceElevated,
    onSurface = DarkAppColors.textPrimary,
    surfaceVariant = DarkAppColors.surfaceElevatedHover,
    onSurfaceVariant = DarkAppColors.textSecondary,
    outline = DarkAppColors.borderSubtle,
    error = DarkAppColors.error
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = LightAppColors.primary,
    onPrimary = LightAppColors.onPrimary,
    secondary = LightAppColors.secondary,
    onSecondary = LightAppColors.onSecondary,
    background = LightAppColors.background,
    onBackground = LightAppColors.textPrimary,
    surface = LightAppColors.surfaceElevated,
    onSurface = LightAppColors.textPrimary,
    surfaceVariant = LightAppColors.surfaceElevatedHover,
    onSurfaceVariant = LightAppColors.textSecondary,
    outline = LightAppColors.borderSubtle,
    error = LightAppColors.error
)

@Composable
fun ZDownloadTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val appColors = if (isDark) DarkAppColors else LightAppColors
    val materialColorScheme = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            window.navigationBarColor = appColors.navigationBackground.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
