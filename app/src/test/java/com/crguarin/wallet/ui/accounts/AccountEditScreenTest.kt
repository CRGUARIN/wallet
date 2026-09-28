package com.crguarin.wallet.ui.accounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class AccountEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `displays new account title and disables save button when form is invalid`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState = AccountEditUiState(name = "", isEditMode = false),
                    onNavigateBack = {},
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Nueva cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `shows inline name error when name error is present`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "",
                            nameErrorRes = R.string.error_name_empty,
                        ),
                    onNavigateBack = {},
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = {},
                )
            }
        }

        composeRule.onNodeWithText("El nombre no puede estar vacío").assertIsDisplayed()
    }

    @Test
    fun `shows initial balance warning in edit mode when balance warning flag is set`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "Efectivo",
                            isEditMode = true,
                            originalInitialBalance =
                                com.crguarin.wallet.core.money
                                    .Money(100_000L),
                            initialBalanceText = "200000",
                        ),
                    onNavigateBack = {},
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Editar cuenta").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Modificar el saldo inicial afectará el saldo actual de la cuenta.",
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `enables save button when form is valid and clicking save triggers callback`() {
        var saveClicked = false

        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "Ahorros",
                        ),
                    onNavigateBack = {},
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = { saveClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Guardar").performClick()
        assertThat(saveClicked).isTrue()
    }
}
