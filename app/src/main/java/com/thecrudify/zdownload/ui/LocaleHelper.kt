package com.thecrudify.zdownload.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import java.util.Locale

class LocalizedActivityContext(
    base: Context,
    val activity: ComponentActivity
) : ContextWrapper(base),
    ActivityResultRegistryOwner by activity,
    SavedStateRegistryOwner by activity,
    OnBackPressedDispatcherOwner by activity {
    override val lifecycle get() = activity.lifecycle
}

object LocaleHelper {
    // The device language, untouched by our own Locale.setDefault calls (which made "follow system" stick to the last pick).
    private fun systemLocale(): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) android.content.res.Resources.getSystem().configuration.locales[0]
        else @Suppress("DEPRECATION") android.content.res.Resources.getSystem().configuration.locale

    fun getLocale(languageCode: String): Locale {
        return when (languageCode.trim().lowercase()) {
            "id", "in" -> Locale("in", "ID")
            "en" -> Locale.ENGLISH
            else -> systemLocale()
        }
    }

    fun applyLocale(activity: ComponentActivity, languageCode: String): Context {
        val targetLocale = if (languageCode.isBlank() || languageCode.equals("system", ignoreCase = true)) {
            systemLocale()
        } else {
            getLocale(languageCode)
        }
        Locale.setDefault(targetLocale)

        val config = Configuration(activity.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }
        val configuredContext = activity.createConfigurationContext(config)
        return LocalizedActivityContext(configuredContext, activity)
    }

    fun toAppLanguage(languageCode: String): AppLanguage {
        return when (languageCode.trim().lowercase()) {
            "id", "in" -> AppLanguage.ID
            "en" -> AppLanguage.EN
            else -> {
                val sysLang = systemLocale().language.lowercase()
                if (sysLang == "id" || sysLang == "in") AppLanguage.ID else AppLanguage.EN
            }
        }
    }
}
