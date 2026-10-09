package com.crguarin.wallet.ui.accounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.ui.theme.WalletPalette
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
        // The save button lives in the bottom bar: it needs no scrolling.
        composeRule.onNodeWithText("Guardar").assertIsNotEnabled()
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
                "Cambiar el saldo inicial también cambia el saldo actual.",
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

        composeRule.onNodeWithText("Guardar").assertIsEnabled()
        composeRule.onNodeWithText("Guardar").performClick()
        assertThat(saveClicked).isTrue()
    }

    @Test
    fun `card type shows debt limit and day fields`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "RappiCard",
                            type = AccountType.CREDIT_CARD,
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

        composeRule.onNodeWithText("Tarjeta de crédito").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Deuda actual").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cupo total").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Día de corte").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Día límite de pago").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `card field errors are shown inline`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "RappiCard",
                            type = AccountType.CREDIT_CARD,
                            creditLimitErrorRes = R.string.card_error_limit_required,
                            statementDayErrorRes = R.string.card_error_day_invalid,
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

        composeRule.onNodeWithText("Ingresa el cupo de la tarjeta").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ingresa un día entre 1 y 31").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `type lock message is shown when the type cannot change`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "RappiCard",
                            type = AccountType.CREDIT_CARD,
                            isEditMode = true,
                            hasTransactions = true,
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

        composeRule
            .onNodeWithText("No se puede cambiar: tiene movimientos")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `transit card form asks for the fare and shows the colors used by other accounts`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "Cívica",
                            type = AccountType.TRANSIT_CARD,
                            icon = "directions_bus",
                            color = WalletPalette.colors[1],
                            usedColors = setOf(WalletPalette.colors[0], WalletPalette.colors[1]),
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

        composeRule
            .onNodeWithText("Cada viaje descuenta la tarifa")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Tarifa por viaje").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("Los colores tachados ya los usa otra cuenta")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `transit card negative balance switch shows its supporting text and reports changes`() {
        var negative: Boolean? = null
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "Cívica",
                            type = AccountType.TRANSIT_CARD,
                            icon = "directions_bus",
                            fareText = "3500",
                            initialBalanceText = "1000",
                        ),
                    onNavigateBack = {},
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = {},
                    onTransitBalanceNegativeChanged = { negative = it },
                )
            }
        }

        composeRule.onNodeWithText("Debo el viaje a crédito").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodes(isToggleable()).onFirst().performClick()

        assertThat(negative).isTrue()
    }

    @Test
    fun `transit card negative balance above the fare shows the credit error`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState =
                        AccountEditUiState(
                            name = "Cívica",
                            type = AccountType.TRANSIT_CARD,
                            icon = "directions_bus",
                            fareText = "3500",
                            initialBalanceText = "5000",
                            transitBalanceNegative = true,
                            amountErrorRes = R.string.error_transit_credit_too_large,
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

        composeRule
            .onNodeWithText("No puede superar la tarifa")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Escribe cuánto debes, hasta una tarifa").performScrollTo().assertIsDisplayed()
    }
}
