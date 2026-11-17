package com.thecrudify.zdownload.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage { ID, EN }

val LocalLang = staticCompositionLocalOf { AppLanguage.ID }

/** Picks the Indonesian or English text for the current app language. */
@Composable
@ReadOnlyComposable
fun tr(id: String, en: String): String = if (LocalLang.current == AppLanguage.ID) id else en

/**
 * Language for text built outside Compose (ViewModels, services, error messages).
 * ZDownloadApp keeps it in sync with the language setting.
 */
object AppText {
    @Volatile var lang: AppLanguage = AppLanguage.ID
}

/** Non-composable twin of [tr]: picks the Indonesian or English text for the current app language. */
fun t(id: String, en: String): String = if (AppText.lang == AppLanguage.ID) id else en
