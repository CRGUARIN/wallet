package com.crguarin.wallet.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.AppLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `step 1 displays welcome and language options, clicking language and continue triggers callbacks`() {
        var nextStepClicked = false
        var selectedLang: AppLanguage? = null

        composeRule.setContent {
            WalletTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = 1, selectedLanguage = AppLanguage.SYSTEM),
                    onLanguageSelected = { selectedLang = it },
                    onNextStep = { nextStepClicked = true },
                    onPreviousStep = {},
                    onAccountNameChanged = {},
                    onAccountTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("Bienvenido a Wallet").assertIsDisplayed()
        composeRule.onNodeWithText("English").performScrollTo().performClick()
        assertThat(selectedLang).isEqualTo(AppLanguage.ENGLISH)

        composeRule.onNodeWithText("Continuar").performScrollTo().performClick()
        assertThat(nextStepClicked).isTrue()
    }

    @Test
    fun `step 2 renders first account form and previous button triggers onPreviousStep`() {
        var previousStepClicked = false

        composeRule.setContent {
            WalletTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = 2, accountName = "Efectivo"),
                    onLanguageSelected = {},
                    onNextStep = {},
                    onPreviousStep = { previousStepClicked = true },
                    onAccountNameChanged = {},
                    onAccountTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("Crea tu primera cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performScrollTo().performClick()
        assertThat(previousStepClicked).isTrue()
    }

    @Test
    fun `step 2 with invalid account disables start button`() {
        composeRule.setContent {
            WalletTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = 2, accountName = ""),
                    onLanguageSelected = {},
                    onNextStep = {},
                    onPreviousStep = {},
                    onAccountNameChanged = {},
                    onAccountTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("Comenzar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `step 2 with valid account enables start button and clicking it calls onComplete`() {
        var completeClicked = false

        composeRule.setContent {
            WalletTheme {
                OnboardingScreen(
                    uiState =
                        OnboardingUiState(
                            step = 2,
                            accountName = "Efectivo",
                        ),
                    onLanguageSelected = {},
                    onNextStep = {},
                    onPreviousStep = {},
                    onAccountNameChanged = {},
                    onAccountTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onComplete = { completeClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Comenzar").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Comenzar").performClick()
        assertThat(completeClicked).isTrue()
    }
}
