package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.ui.util.AppLanguage
import com.crguarin.wallet.ui.util.AppLanguageController

class FakeAppLanguageController(
    private var language: AppLanguage = AppLanguage.SYSTEM,
) : AppLanguageController {
    val setCalls = mutableListOf<AppLanguage>()

    override fun current(): AppLanguage = language

    override fun set(language: AppLanguage) {
        setCalls += language
        this.language = language
    }
}
