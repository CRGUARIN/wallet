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
import com.crguarin.wallet.domain.model.recurring.UpcomingKind
import com.crguarin.wallet.domain.model.recurring.UpcomingPayment
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

    private val upcomingToday = LocalDate.of(2026, 9, 28)

    private fun upcoming(
        kind: UpcomingKind,
        refId: Long,
        title: String,
        date: LocalDate = upcomingToday,
        amount: Money? = Money(4_590_000L),
    ) = UpcomingPayment(
        kind = kind,
        refId = refId,
        title = title,
        type = TransactionType.EXPENSE,
        date = date,
        amount = amount,
        accountName = "Efectivo",
    )

    private fun upcomingState(payments: List<UpcomingPayment>) =
        HomeUiState(
            isLoading = false,
            totalBalance = Money(1_250_000L),
            monthSummary = MonthSummary(income = Money(500_000L), expense = Money(230_000L)),
            accounts = listOf(AccountWithBalance(account, Money(450_000L))),
            recentTransactions = emptyList(),
            today = upcomingToday,
            upcomingPayments = payments,
        )

    private fun showUpcoming(
        payments: List<UpcomingPayment>,
        onUpcomingRecurringClick: (Long) -> Unit = {},
        onUpcomingPendingClick: () -> Unit = {},
        onUpcomingCardClick: (Long) -> Unit = {},
        onViewAllRecurring: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                HomeScreen(
                    uiState = upcomingState(payments),
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    onViewReports = {},
                    onUpcomingRecurringClick = onUpcomingRecurringClick,
                    onUpcomingPendingClick = onUpcomingPendingClick,
                    onUpcomingCardClick = onUpcomingCardClick,
                    onViewAllRecurring = onViewAllRecurring,
                )
            }
        }
    }

    @Test
    fun `upcoming card is hidden when there is nothing upcoming`() {
        showUpcoming(emptyList())

        composeRule.onNodeWithText("Próximos pagos").assertDoesNotExist()
    }

    @Test
    fun `upcoming card shows title amount timing and account`() {
        showUpcoming(
            listOf(
                upcoming(UpcomingKind.RECURRING, refId = 11L, title = "Netflix", date = upcomingToday),
                upcoming(
                    UpcomingKind.RECURRING,
                    refId = 12L,
                    title = "Arriendo",
                    date = upcomingToday.plusDays(1),
                ),
                upcoming(
                    UpcomingKind.RECURRING,
                    refId = 13L,
                    title = "Gimnasio",
                    date = upcomingToday.plusDays(4),
                ),
            ),
        )

        composeRule.onNodeWithText("Próximos pagos").assertIsDisplayed()
        composeRule.onNodeWithText("Netflix").assertIsDisplayed()
        composeRule.onNodeWithText("Hoy · Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Mañana · Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("En 4 días · Efectivo").assertIsDisplayed()
        // Every row carries the same amount, so the money text appears three times.
        composeRule.onAllNodesWithText("− $ 45.900").onFirst().assertIsDisplayed()
    }

    @Test
    fun `pending rows carry the pending label and cards can show an unknown amount`() {
        showUpcoming(
            listOf(
                upcoming(UpcomingKind.RECURRING_PENDING, refId = 21L, title = "Luz"),
                upcoming(
                    UpcomingKind.CARD_PAYMENT,
                    refId = 7L,
                    title = "RappiCard",
                    date = upcomingToday.plusDays(2),
                    amount = null,
                ),
            ),
        )

        composeRule.onNodeWithText("Por confirmar").assertIsDisplayed()
        composeRule.onNodeWithText("Pago de RappiCard").assertIsDisplayed()
        composeRule.onNodeWithText("Concilia el extracto").assertIsDisplayed()
        composeRule.onNodeWithText("En 2 días · Efectivo").assertIsDisplayed()
    }

    @Test
    fun `upcoming card shows at most five rows`() {
        showUpcoming((1L..6L).map { id -> upcoming(UpcomingKind.RECURRING, refId = id, title = "Pago $id") })

        composeRule.onNodeWithText("Pago 5").assertIsDisplayed()
        composeRule.onNodeWithText("Pago 6").assertDoesNotExist()
    }

    @Test
    fun `upcoming rows route to edit list account or the full list`() {
        var recurringId: Long? = null
        var pendingClicks = 0
        var cardId: Long? = null
        var viewAllClicks = 0
        showUpcoming(
            listOf(
                upcoming(UpcomingKind.RECURRING, refId = 11L, title = "Netflix"),
                upcoming(UpcomingKind.RECURRING_PENDING, refId = 21L, title = "Luz"),
                upcoming(UpcomingKind.CARD_PAYMENT, refId = 7L, title = "RappiCard"),
            ),
            onUpcomingRecurringClick = { recurringId = it },
            onUpcomingPendingClick = { pendingClicks++ },
            onUpcomingCardClick = { cardId = it },
            onViewAllRecurring = { viewAllClicks++ },
        )

        composeRule.onNodeWithText("Netflix").performClick()
        composeRule.onNodeWithText("Luz").performClick()
        composeRule.onNodeWithText("Pago de RappiCard").performClick()
        // The upcoming card is laid out before the recent movements, so its
        // "Ver todos" comes first.
        composeRule.onAllNodesWithText("Ver todos").onFirst().performClick()

        assertThat(recurringId).isEqualTo(11L)
        assertThat(pendingClicks).isEqualTo(1)
        assertThat(cardId).isEqualTo(7L)
        assertThat(viewAllClicks).isEqualTo(1)
    }
}
