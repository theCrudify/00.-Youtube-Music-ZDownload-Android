package com.thecrudify.zdownload.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.thecrudify.zdownload.ui.screens.home.AppLanguage

val LocalLang = staticCompositionLocalOf { AppLanguage.ID }

/** Picks the Indonesian or English text for the current app language. */
@Composable
@ReadOnlyComposable
fun tr(id: String, en: String): String = if (LocalLang.current == AppLanguage.ID) id else en
