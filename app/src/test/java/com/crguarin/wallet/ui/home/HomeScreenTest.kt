package com.crguarin.wallet.ui.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.accounts.sampleCardOverview
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val account =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(0),
            color = 0xFF66BB6A.toInt(),
            icon = "wallet",
            sortOrder = 0,
            createdAt = Instant.EPOCH,
        )

    private val savingsAccount = account.copy(id = 2L, name = "Ahorros", icon = "savings", sortOrder = 1)

    private fun details(
        id: Long,
        type: TransactionType,
        systemKey: String,
    ) = TransactionDetails(
        transaction =
            Transaction(
                id = id,
                type = type,
                amount = Money(4_550_000L),
                accountId = 1L,
                categoryId = 10L,
                date = LocalDate.of(2026, 9, 28),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        accountName = "Efectivo",
        accountColor = 0xFF66BB6A.toInt(),
        accountIcon = "wallet",
        categorySystemKey = systemKey,
        categoryColor = 0xFFEF5350.toInt(),
        categoryIcon = "restaurant",
    )

    private fun state(
        accounts: List<AccountWithBalance>,
        transactions: List<TransactionDetails>,
    ) = HomeUiState(
        isLoading = false,
        totalBalance = Money(1_250_000L),
        monthSummary = MonthSummary(income = Money(500_000L), expense = Money(230_000L)),
        accounts = accounts,
        recentTransactions = transactions,
    )

    @Test
    fun `shows total balance month summary accounts and recent movements`() {
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState =
                        state(
                            accounts =
                                listOf(
                                    AccountWithBalance(account, Money(450_000L)),
                                    AccountWithBalance(savingsAccount, Money(800_000L)),
                                ),
                            transactions =
                                listOf(
                                    details(1L, TransactionType.EXPENSE, "food"),
                                    details(2L, TransactionType.INCOME, "salary"),
                                ),
                        ),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                )
            }
        }

        composeRule.onNodeWithText("Saldo total").assertIsDisplayed()
        composeRule.onNodeWithText("$ 12.500").assertIsDisplayed()
        composeRule.onNodeWithText("Resumen del mes").assertIsDisplayed()
        composeRule.onAllNodesWithText("Efectivo").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertIsDisplayed()
        composeRule.onNodeWithText("Últimos movimientos").assertIsDisplayed()
        composeRule.onNodeWithText("Ver todos").assertIsDisplayed()
        composeRule.onNodeWithText("Ver reportes").assertIsDisplayed()
        composeRule.onNodeWithText("Alimentación").assertIsDisplayed()
        composeRule.onNodeWithText("Salario").assertIsDisplayed()
    }

    @Test
    fun `shows the transfer quick action only with at least two active accounts`() {
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState = state(accounts = listOf(AccountWithBalance(account, Money(450_000L))), transactions = emptyList()),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                )
            }
        }

        composeRule.onNodeWithText("Gasto").assertIsDisplayed()
        composeRule.onNodeWithText("Ingreso").assertIsDisplayed()
        composeRule.onNodeWithText("Transferencia").assertDoesNotExist()
    }

    @Test
    fun `shows a friendly empty state when there are no movements`() {
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState = state(accounts = emptyList(), transactions = emptyList()),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                )
            }
        }

        composeRule.onNodeWithText("Aún no hay movimientos").assertIsDisplayed()
    }

    @Test
    fun `shows a loading indicator instead of empty content while loading`() {
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState = HomeUiState(isLoading = true),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
        composeRule.onNodeWithText("Aún no hay movimientos").assertDoesNotExist()
        composeRule.onNodeWithText("Saldo total").assertDoesNotExist()
    }

    @Test
    fun `section headers are marked as headings`() {
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState = state(accounts = listOf(AccountWithBalance(account, Money(450_000L))), transactions = emptyList()),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                )
            }
        }

        composeRule
            .onNode(hasText("Últimos movimientos") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }

    @Test
    fun `the view reports button calls its callback`() {
        var reportsClicks = 0
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState = state(accounts = emptyList(), transactions = emptyList()),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = { reportsClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Ver reportes").performClick()

        assertThat(reportsClicks).isEqualTo(1)
    }

    @Test
    fun `home account row shows card debt available and reminder chip`() {
        val today = LocalDate.of(2026, 9, 28)
        val card =
            Account(
                id = 7L,
                name = "RappiCard",
                type = AccountType.CREDIT_CARD,
                initialBalance = Money(-250_000_00L),
                color = 0xFFFF6F00.toInt(),
                icon = "credit_card",
                sortOrder = 2,
                createdAt = Instant.EPOCH,
            )
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState =
                        HomeUiState(
                            isLoading = false,
                            totalBalance = Money(-250_000_00L),
                            monthSummary = MonthSummary(income = Money.ZERO, expense = Money.ZERO),
                            accounts = listOf(AccountWithBalance(card, Money(-250_000_00L))),
                            recentTransactions = emptyList(),
                            cardOverviews =
                                mapOf(
                                    7L to sampleCardOverview(accountId = 7L),
                                ),
                            today = today,
                        ),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                )
            }
        }

        composeRule.onNodeWithText("RappiCard").assertIsDisplayed()
        composeRule.onNodeWithText("Deuda").assertIsDisplayed()
        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Pagar antes del", substring = true).assertIsDisplayed()
    }
}
