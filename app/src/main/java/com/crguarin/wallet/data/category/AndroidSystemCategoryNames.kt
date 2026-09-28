package com.crguarin.wallet.data.category

import android.content.Context
import android.content.res.Configuration
import com.crguarin.wallet.core.resources.SystemCategoryStrings
import com.crguarin.wallet.domain.model.SystemCategoryNames
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSystemCategoryNames
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SystemCategoryNames {
        override fun namesFor(systemKey: String): Set<String> {
            val resId = SystemCategoryStrings.resIdFor(systemKey) ?: return emptySet()

            val esContext = createLocalizedContext(Locale.forLanguageTag("es"))
            val enContext = createLocalizedContext(Locale.forLanguageTag("en"))

            val esName = runCatching { esContext.getString(resId) }.getOrNull()
            val enName = runCatching { enContext.getString(resId) }.getOrNull()

            return listOfNotNull(esName, enName).toSet()
        }

        private fun createLocalizedContext(locale: Locale): Context {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
