package com.crguarin.wallet.ui.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class AccountsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val account1 =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money.ZERO,
            color = 0xFF4CAF50.toInt(),
            icon = "wallet",
            sortOrder = 0,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private val account2 =
        Account(
            id = 2L,
            name = "Ahorros Bancolombia",
            type = AccountType.SAVINGS,
            initialBalance = Money(100_000L),
            color = 0xFF2196F3.toInt(),
            icon = "account_balance",
            sortOrder = 1,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private val archivedAccount =
        Account(
            id = 3L,
            name = "Cuenta Vieja",
            type = AccountType.CASH,
            initialBalance = Money.ZERO,
            color = 0xFF9E9E9E.toInt(),
            icon = "wallet",
            sortOrder = 2,
            isArchived = true,
            includeInTotal = false,
            createdAt = Instant.EPOCH,
        )

    @Test
    fun `displays active accounts and clicking account triggers onAccountClick`() {
        var clickedAccountId: Long? = null

        val state =
            AccountsUiState(
                activeAccounts =
                    listOf(
                        AccountWithBalance(account1, Money(50_000L)),
                        AccountWithBalance(account2, Money(200_000L)),
                    ),
                totalBalance = Money(250_000L),
            )

        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = state,
                    onAccountClick = { clickedAccountId = it },
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros Bancolombia").assertIsDisplayed()

        composeRule.onNodeWithText("Efectivo").performClick()
        assertThat(clickedAccountId).isEqualTo(1L)
    }

    @Test
    fun `archived accounts section expands and collapses`() {
        var toggleArchivedClicked = false

        var state by mutableStateOf(
            AccountsUiState(
                activeAccounts = emptyList(),
                archivedAccounts = listOf(AccountWithBalance(archivedAccount, Money.ZERO)),
                isArchivedExpanded = false,
            ),
        )

        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = state,
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {
                        toggleArchivedClicked = true
                        state = state.copy(isArchivedExpanded = !state.isArchivedExpanded)
                    },
                )
            }
        }

        // Header shows count
        composeRule.onNodeWithText("Archivadas (1)").assertIsDisplayed()
        // Archived account item should not be displayed when collapsed
        composeRule.onNodeWithText("Cuenta Vieja").assertDoesNotExist()

        // Click header to toggle
        composeRule.onNodeWithText("Archivadas (1)").performClick()
        assertThat(toggleArchivedClicked).isTrue()

        // When expanded, item is visible in the list
        composeRule.onNodeWithText("Cuenta Vieja").assertExists()
    }

    @Test
    fun `reordering mode displays reorder controls and handles move actions`() {
        var moveDownId: Long? = null
        var toggleReorderClicked = false

        val state =
            AccountsUiState(
                activeAccounts =
                    listOf(
                        AccountWithBalance(account1, Money.ZERO),
                        AccountWithBalance(account2, Money.ZERO),
                    ),
                isReordering = true,
            )

        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = state,
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = { toggleReorderClicked = true },
                    onMoveAccountUp = {},
                    onMoveAccountDown = { moveDownId = it },
                    onToggleArchivedExpanded = {},
                )
            }
        }

        // Done button is visible in top bar
        composeRule.onNodeWithText("Listo").assertIsDisplayed()

        // Move buttons are present
        composeRule.onAllNodesWithContentDescription("Bajar").onFirst().performClick()
        assertThat(moveDownId).isEqualTo(1L)

        composeRule.onNodeWithText("Listo").performClick()
        assertThat(toggleReorderClicked).isTrue()
    }

    @Test
    fun `empty state displays message and allows creating new account`() {
        var newAccountClicked = false

        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = AccountsUiState(),
                    onAccountClick = {},
                    onNewAccountClick = { newAccountClicked = true },
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }

        composeRule.onNodeWithText("Sin cuentas").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Nueva cuenta").onFirst().performClick()
        assertThat(newAccountClicked).isTrue()
    }
}
