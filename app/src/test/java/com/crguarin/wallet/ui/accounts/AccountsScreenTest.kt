package com.crguarin.wallet.ui.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

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

    // The shortcuts row makes the list taller than the default 470 dp test screen: the lazy list would
    // not compose the archived row, so the test uses a phone-sized screen.
    @Test
    @Config(qualifiers = "es-w360dp-h800dp")
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

    private val cardAccount =
        Account(
            id = 7L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-250_000_00L),
            color = 0xFFFF6F00.toInt(),
            icon = "credit_card",
            sortOrder = 2,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private fun cardState(
        overview: CardOverview,
        today: LocalDate,
    ) = AccountsUiState(
        activeAccounts = listOf(AccountWithBalance(cardAccount, Money(-250_000_00L))),
        totalBalance = Money(-250_000_00L),
        cardOverviews = mapOf(7L to overview),
        today = today,
    )

    private fun showCardState(
        overview: CardOverview,
        today: LocalDate,
    ) {
        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = cardState(overview, today),
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }
    }

    @Test
    fun `card row shows the payment reminder in one status line without the debt label`() {
        val today = LocalDate.of(2026, 9, 28)
        showCardState(sampleCardOverview(accountId = 7L, today = today), today)

        composeRule.onNodeWithText("Pagar en 7 días").assertIsDisplayed()
        composeRule.onNodeWithText("Deuda").assertDoesNotExist()
        composeRule.onNodeWithText("Disponible", substring = true).assertDoesNotExist()
    }

    @Test
    fun `card row without a near payment shows the available credit`() {
        val today = LocalDate.of(2026, 9, 28)
        showCardState(sampleCardOverview(accountId = 7L, today = today, dueDate = LocalDate.of(2026, 11, 5)), today)

        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Pagar en", substring = true).assertDoesNotExist()
    }

    @Test
    fun `overdue card row shows the overdue status`() {
        val today = LocalDate.of(2026, 10, 10)
        showCardState(
            sampleCardOverview(
                accountId = 7L,
                today = today,
                paymentStatus = StatementPaymentStatus.OVERDUE,
            ),
            today,
        )

        composeRule.onNodeWithText("Pago vencido").assertIsDisplayed()
    }

    @Test
    fun `card with credit in favor has no debt label in its row`() {
        val today = LocalDate.of(2026, 9, 28)
        showCardState(
            sampleCardOverview(
                accountId = 7L,
                today = today,
                debt = Money(-50_000_00L),
                paymentStatus = null,
                pendingAmount = null,
                dueDate = null,
            ),
            today,
        )

        composeRule.onNodeWithText("Saldo a favor").assertDoesNotExist()
        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the three shortcuts sit side by side on one row`() {
        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = AccountsUiState(activeAccounts = listOf(AccountWithBalance(account1, Money.ZERO))),
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }

        val tops =
            listOf("Deudas", "Pagos recurrentes", "Categorías").map { label ->
                composeRule
                    .onNodeWithText(label)
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }
        assertThat(tops.distinct()).hasSize(1)
    }

    @Test
    fun `shortcuts to debts recurring payments and categories call their callbacks`() {
        var debtsClicks = 0
        var recurringClicks = 0
        var categoriesClicks = 0
        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState =
                        AccountsUiState(
                            activeAccounts = listOf(AccountWithBalance(account1, Money(50_000L))),
                            totalBalance = Money(50_000L),
                        ),
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                    onOpenCategories = { categoriesClicks++ },
                    onOpenRecurring = { recurringClicks++ },
                    onOpenDebts = { debtsClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Deudas").assertIsDisplayed().performClick()
        assertThat(debtsClicks).isEqualTo(1)
        assertThat(recurringClicks).isEqualTo(0)
        assertThat(categoriesClicks).isEqualTo(0)

        composeRule.onNodeWithText("Pagos recurrentes").assertIsDisplayed().performClick()
        assertThat(recurringClicks).isEqualTo(1)

        composeRule.onNodeWithText("Categorías").assertIsDisplayed().performClick()
        assertThat(categoriesClicks).isEqualTo(1)
        assertThat(debtsClicks).isEqualTo(1)
    }

    @Test
    fun `shortcuts sit right under the total balance and above the accounts`() {
        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState =
                        AccountsUiState(
                            activeAccounts = listOf(AccountWithBalance(account1, Money(50_000L))),
                            totalBalance = Money(50_000L),
                        ),
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }

        val totalTop =
            composeRule
                .onNodeWithText("Saldo total")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val shortcutsTop =
            composeRule
                .onNodeWithText("Deudas")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val accountTop =
            composeRule
                .onNodeWithText("Efectivo")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertThat(shortcutsTop).isGreaterThan(totalTop)
        assertThat(accountTop).isGreaterThan(shortcutsTop)
    }

    @Test
    fun `the empty state offers no shortcuts`() {
        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState = AccountsUiState(),
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }

        composeRule.onNodeWithText("Deudas").assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "es-w360dp-h800dp")
    fun `with a big font the balance of a row drops under the name`() {
        // A name that differs from the type label ("Efectivo"), so the name is found on its own.
        val named = account1.copy(name = "Caja menor")
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                WalletTheme {
                    AccountsScreen(
                        uiState =
                            AccountsUiState(
                                activeAccounts = listOf(AccountWithBalance(named, Money(1_234_500L))),
                                totalBalance = Money(1_234_500L),
                            ),
                        onAccountClick = {},
                        onNewAccountClick = {},
                        onToggleReordering = {},
                        onMoveAccountUp = {},
                        onMoveAccountDown = {},
                        onToggleArchivedExpanded = {},
                    )
                }
            }
        }

        val name = composeRule.onNodeWithText("Caja menor", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // The hero shows the same total: the balance of the row is the one under the name.
        val balances = composeRule.onAllNodesWithText("12.345", substring = true, useUnmergedTree = true).fetchSemanticsNodes()
        val rowBalance = balances.map { it.boundsInRoot }.first { it.top >= name.bottom }
        assertThat(rowBalance.left).isWithin(1f).of(name.left)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "es-w360dp-h800dp")
    fun `with the normal font the balance of a row sits at the end of the name line`() {
        val named = account1.copy(name = "Caja menor")
        composeRule.setContent {
            WalletTheme {
                AccountsScreen(
                    uiState =
                        AccountsUiState(
                            activeAccounts = listOf(AccountWithBalance(named, Money(1_234_500L))),
                            totalBalance = Money(1_234_500L),
                        ),
                    onAccountClick = {},
                    onNewAccountClick = {},
                    onToggleReordering = {},
                    onMoveAccountUp = {},
                    onMoveAccountDown = {},
                    onToggleArchivedExpanded = {},
                )
            }
        }

        val name = composeRule.onNodeWithText("Caja menor", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val balances = composeRule.onAllNodesWithText("12.345", substring = true, useUnmergedTree = true).fetchSemanticsNodes()
        val rowBalance = balances.map { it.boundsInRoot }.first { it.top >= name.top - 1f && it.left > name.right }
        assertThat(rowBalance.left).isGreaterThan(name.right)
    }
}
