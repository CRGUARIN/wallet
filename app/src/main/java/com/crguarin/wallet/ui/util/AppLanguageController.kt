package com.crguarin.wallet.ui.util

import javax.inject.Inject

/**
 * Reads and changes the per-app language. An interface so ViewModels can be tested without AppCompat.
 */
interface AppLanguageController {
    fun current(): AppLanguage

    fun set(language: AppLanguage)
}

class LanguageHelperController
    @Inject
    constructor() : AppLanguageController {
        override fun current(): AppLanguage = LanguageHelper.getCurrentLanguage()

        override fun set(language: AppLanguage) = LanguageHelper.setLanguage(language)
    }
