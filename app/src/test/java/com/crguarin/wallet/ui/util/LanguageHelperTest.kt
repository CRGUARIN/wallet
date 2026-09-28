package com.crguarin.wallet.ui.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguageHelperTest {
    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun `default language is system when empty`() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        assertThat(LanguageHelper.getCurrentLanguage()).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun `setLanguage updates current language for Spanish and English`() {
        val scenario =
            androidx.test.core.app.ActivityScenario
                .launch(com.crguarin.wallet.MainActivity::class.java)
        scenario.onActivity {
            LanguageHelper.setLanguage(AppLanguage.SPANISH)
            assertThat(LanguageHelper.getCurrentLanguage()).isEqualTo(AppLanguage.SPANISH)

            LanguageHelper.setLanguage(AppLanguage.ENGLISH)
            assertThat(LanguageHelper.getCurrentLanguage()).isEqualTo(AppLanguage.ENGLISH)

            LanguageHelper.setLanguage(AppLanguage.SYSTEM)
            assertThat(LanguageHelper.getCurrentLanguage()).isEqualTo(AppLanguage.SYSTEM)
        }
        scenario.close()
    }
}
