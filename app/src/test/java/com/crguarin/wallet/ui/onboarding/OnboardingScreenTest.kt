package com.crguarin.wallet.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.domain.model.AccountType
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

        composeRule.onNodeWithText("Continuar").performClick()
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
        composeRule.onNodeWithText("Anterior").performClick()
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

        composeRule.onNodeWithText("Comenzar").assertIsNotEnabled()
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

        composeRule.onNodeWithText("Comenzar").assertIsEnabled()
        composeRule.onNodeWithText("Comenzar").performClick()
        assertThat(completeClicked).isTrue()
    }

    private fun setScreen(
        uiState: OnboardingUiState,
        onAccountTypeChanged: (AccountType) -> Unit = {},
        onIncludeInTotalChanged: (Boolean) -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                OnboardingScreen(
                    uiState = uiState,
                    onLanguageSelected = {},
                    onNextStep = {},
                    onPreviousStep = {},
                    onAccountNameChanged = {},
                    onAccountTypeChanged = onAccountTypeChanged,
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = onIncludeInTotalChanged,
                    onComplete = onComplete,
                )
            }
        }
    }

    @Test
    fun `step 1 has continue in the bottom bar and no previous button`() {
        setScreen(OnboardingUiState(step = 1))

        composeRule.onNodeWithText("Continuar").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Anterior").assertDoesNotExist()
        composeRule.onNodeWithText("Comenzar").assertDoesNotExist()
    }

    @Test
    fun `step 1 marks the chosen language`() {
        setScreen(OnboardingUiState(step = 1, selectedLanguage = AppLanguage.SPANISH))

        composeRule.onNodeWithText("Español").assertIsSelected()
        composeRule.onNodeWithText("English").assertIsNotSelected()
    }

    @Test
    fun `step 2 has previous and start in the bottom bar`() {
        setScreen(OnboardingUiState(step = 2, accountName = "Efectivo"))

        composeRule.onNodeWithText("Anterior").assertIsDisplayed()
        composeRule.onNodeWithText("Comenzar").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertDoesNotExist()
    }

    @Test
    fun `step 2 account types show the chosen one and report taps`() {
        var chosen: AccountType? = null
        setScreen(
            OnboardingUiState(step = 2, accountName = "Efectivo", accountType = AccountType.CASH),
            onAccountTypeChanged = { chosen = it },
        )

        composeRule.onNodeWithText("Cuenta de ahorros").performScrollTo().assertIsNotSelected()
        composeRule.onNodeWithText("Cuenta de ahorros").performClick()

        assertThat(chosen).isEqualTo(AccountType.SAVINGS)
    }

    @Test
    fun `step 2 include in total switch reports the change`() {
        var included: Boolean? = null
        setScreen(
            OnboardingUiState(step = 2, accountName = "Efectivo", includeInTotal = true),
            onIncludeInTotalChanged = { included = it },
        )

        val includeSwitch = composeRule.onAllNodes(isToggleable()).onFirst()
        includeSwitch.performScrollTo()
        includeSwitch.performClick()

        assertThat(included).isFalse()
    }

    @Test
    fun `step 2 hides the start label while saving`() {
        setScreen(OnboardingUiState(step = 2, accountName = "Efectivo", isSaving = true))

        composeRule.onNodeWithText("Comenzar").assertDoesNotExist()
    }
}
