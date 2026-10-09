package com.crguarin.wallet.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtsOverviewUseCase
import com.crguarin.wallet.domain.usecase.recurring.ObserveUpcomingPaymentsUseCase
import com.crguarin.wallet.domain.usecase.transit.RegisterTripUseCase
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The trip shortcut end to end: Home with its real ViewModel, the snackbar and the undo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class HomeRouteTripTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var viewModel: HomeViewModel

    private val cash =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(100_000L),
            color = 1,
            icon = "wallet",
            sortOrder = 0,
            createdAt = baseInstant,
        )
    private val civica =
        cash.copy(
            id = 5L,
            name = "Cívica",
            type = AccountType.TRANSIT_CARD,
            initialBalance = Money(1_050_000L),
            icon = "directions_bus",
            sortOrder = 1,
            fare = Money(355_000L),
        )

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        val cardStatementRepository = FakeCardStatementRepository()
        val categoryRepository = FakeCategoryRepository()
        val timeProvider = FakeTimeProvider(baseInstant)
        val cardOverviews =
            ObserveCardOverviewsUseCase(
                accountRepository,
                ObserveCardOverviewUseCase(accountRepository, cardStatementRepository, timeProvider),
            )
        viewModel =
            HomeViewModel(
                observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository),
                observeMonthSummaryUseCase = ObserveMonthSummaryUseCase(transactionRepository),
                observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
                transactionRepository = transactionRepository,
                timeProvider = timeProvider,
                observeCardOverviewsUseCase = cardOverviews,
                observeUpcomingPaymentsUseCase =
                    ObserveUpcomingPaymentsUseCase(
                        FakeRecurringRepository(),
                        accountRepository,
                        categoryRepository,
                        cardOverviews,
                        timeProvider,
                    ),
                observeDebtsOverviewUseCase = ObserveDebtsOverviewUseCase(FakeDebtRepository()),
                registerTripUseCase =
                    RegisterTripUseCase(
                        accountRepository,
                        categoryRepository,
                        SaveTransactionUseCase(
                            transactionRepository,
                            accountRepository,
                            categoryRepository,
                            timeProvider,
                            CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
                            FakeTransactionRunner(),
                        ),
                        timeProvider,
                        FakeTransactionRunner(),
                    ),
                deleteTransactionUseCase =
                    DeleteTransactionUseCase(transactionRepository, cardStatementRepository, FakeTransactionRunner()),
            )
    }

    private fun showHome() {
        composeRule.setContent {
            WalletTheme {
                HomeRoute(
                    onAccountClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onQuickAction = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tripsRegistered(): Int =
        runBlocking {
            (1L..10L).count { transactionRepository.getTransaction(it) != null }
        }

    @Test
    fun `tapping trip registers it and offers to undo it in a snackbar`() {
        accountRepository.setInitialData(listOf(cash, civica))
        showHome()
        waitForText("Viaje")

        composeRule.onNodeWithText("Viaje").performClick()

        waitForText("Viaje registrado")
        composeRule.onNodeWithText("Viaje registrado").assertIsDisplayed()
        composeRule.onNodeWithText("Deshacer").assertIsDisplayed()
        assertThat(tripsRegistered()).isEqualTo(1)
    }

    @Test
    fun `undo in the snackbar removes the trip`() {
        accountRepository.setInitialData(listOf(cash, civica))
        showHome()
        waitForText("Viaje")
        composeRule.onNodeWithText("Viaje").performClick()
        waitForText("Deshacer")

        composeRule.onNodeWithText("Deshacer").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) { tripsRegistered() == 0 }
    }

    @Test
    fun `a trip owed on credit shows the error in the snackbar`() {
        accountRepository.setInitialData(
            accountList = listOf(cash, civica),
            balanceMap = mapOf(5L to Money(-1L)),
        )
        showHome()
        waitForText("Viaje")

        composeRule.onNodeWithText("Viaje").performClick()

        waitForText("Ya usaste el viaje a crédito. Recarga la tarjeta.")
        assertThat(tripsRegistered()).isEqualTo(0)
    }

    @Test
    fun `with two transit cards the sheet asks which one and then registers on it`() {
        accountRepository.setInitialData(listOf(cash, civica, civica.copy(id = 6L, name = "Tullave", sortOrder = 2)))
        showHome()
        waitForText("Viaje")

        composeRule.onNodeWithText("Viaje").performClick()
        waitForText("Elige la tarjeta")
        assertThat(tripsRegistered()).isEqualTo(0)
        // The names also appear in the account list behind the sheet; the sheet is the last one.
        composeRule.onAllNodesWithText("Tullave").onLast().performClick()

        waitForText("Viaje registrado")
        assertThat(transactionRepository.let { runBlocking { it.getTransaction(1L) } }?.accountId).isEqualTo(6L)
    }
}
