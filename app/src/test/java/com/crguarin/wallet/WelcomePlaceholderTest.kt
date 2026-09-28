package com.crguarin.wallet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.ui.theme.WalletTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Prueba de humo: valida que Compose + Robolectric funcionan y que los textos salen de strings.xml.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WelcomePlaceholderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showPlaceholder() {
        composeRule.setContent { WalletTheme { WelcomePlaceholder() } }
    }

    @Test
    @Config(qualifiers = "es")
    fun showsSpanishWelcome() {
        showPlaceholder()
        composeRule.onNodeWithText("Bienvenido a Wallet").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun showsEnglishWelcome() {
        showPlaceholder()
        composeRule.onNodeWithText("Welcome to Wallet").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun fallsBackToSpanishForUnsupportedLanguage() {
        showPlaceholder()
        composeRule.onNodeWithText("Bienvenido a Wallet").assertIsDisplayed()
    }
}
