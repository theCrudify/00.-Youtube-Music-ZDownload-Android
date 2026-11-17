package com.thecrudify.zdownload.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceElevatedHover: Color,
    val surfaceInput: Color,
    val playerBackground: Color,
    val miniPlayerBackground: Color,
    val navigationBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderHighlight: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accentBlue: Color,
    val accentCyan: Color,
    val accentViolet: Color,
    val accentAmber: Color,
    val accentEmerald: Color,
    val error: Color,
    val success: Color,
    val warning: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF07090E),
    surface = Color(0xFF0F121A),
    surfaceElevated = Color(0xFF161A26),
    surfaceElevatedHover = Color(0xFF1E2436),
    surfaceInput = Color(0xFF0A0C14),
    playerBackground = Color(0xFF07090E),
    miniPlayerBackground = Color(0xFF141824),
    navigationBackground = Color(0xFF0F121A),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    borderSubtle = Color(0x1FFFFFFF),
    borderHighlight = Color(0x33FFFFFF),
    primary = Color(0xFF06B6D4),
    onPrimary = Color(0xFF07090E),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color(0xFFF8FAFC),
    accentBlue = Color(0xFF3B82F6),
    accentCyan = Color(0xFF06B6D4),
    accentViolet = Color(0xFF8B5CF6),
    accentAmber = Color(0xFFF59E0B),
    accentEmerald = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B)
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceElevatedHover = Color(0xFFE2E8F0),
    surfaceInput = Color(0xFFFFFFFF),
    playerBackground = Color(0xFFF8FAFC),
    miniPlayerBackground = Color(0xFFFFFFFF),
    navigationBackground = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    borderSubtle = Color(0x1E000000),
    borderHighlight = Color(0x33000000),
    primary = Color(0xFF0891B2),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    accentBlue = Color(0xFF2563EB),
    accentCyan = Color(0xFF0891B2),
    accentViolet = Color(0xFF7C3AED),
    accentAmber = Color(0xFFD97706),
    accentEmerald = Color(0xFF059669),
    error = Color(0xFFDC2626),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

// Semantic tokens dynamically connected to the current theme
val BgBase: Color @Composable get() = LocalAppColors.current.background
val BgElevated: Color @Composable get() = LocalAppColors.current.surfaceElevated
val BgElevatedHover: Color @Composable get() = LocalAppColors.current.surfaceElevatedHover
val SurfaceInput: Color @Composable get() = LocalAppColors.current.surfaceInput
val BorderSubtle: Color @Composable get() = LocalAppColors.current.borderSubtle
val BorderHighlight: Color @Composable get() = LocalAppColors.current.borderHighlight

val AccentBlue: Color @Composable get() = LocalAppColors.current.accentBlue
val AccentCyan: Color @Composable get() = LocalAppColors.current.accentCyan
val AccentViolet: Color @Composable get() = LocalAppColors.current.accentViolet
val AccentAmber: Color @Composable get() = LocalAppColors.current.accentAmber
val AccentEmerald: Color @Composable get() = LocalAppColors.current.accentEmerald

val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val TextMuted: Color @Composable get() = LocalAppColors.current.textMuted
