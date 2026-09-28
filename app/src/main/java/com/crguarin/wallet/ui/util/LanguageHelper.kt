package com.crguarin.wallet.ui.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage {
    SYSTEM,
    SPANISH,
    ENGLISH,
}

object LanguageHelper {
    fun getCurrentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val tags = locales.toLanguageTags()
        return when {
            tags.startsWith("es") -> AppLanguage.SPANISH
            tags.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    fun setLanguage(language: AppLanguage) {
        val localeList =
            when (language) {
                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.SPANISH -> LocaleListCompat.forLanguageTags("es")
                AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
