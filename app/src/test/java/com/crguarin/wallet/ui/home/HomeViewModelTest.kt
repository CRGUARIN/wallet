package com.crguarin.wallet.ui.home

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtsOverviewUseCase
import com.crguarin.wallet.domain.usecase.recurring.ObserveUpcomingPaymentsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val today = LocalDate.of(2026, 9, 28)

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var cardStatementRepository: FakeCardStatementRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var debtRepository: FakeDebtRepository
    private lateinit var timeProvider: FakeTimeProvider

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
    private val savings =
        Account(
            id = 2L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            initialBalance = Money(200_000L),
            color = 2,
            icon = "savings",
            sortOrder = 1,
            createdAt = baseInstant,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        cardStatementRepository = FakeCardStatementRepository()
        categoryRepository = FakeCategoryRepository()
        recurringRepository = FakeRecurringRepository()
        debtRepository = FakeDebtRepository()
        timeProvider = FakeTimeProvider(baseInstant)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        HomeViewModel(
            observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository),
            observeMonthSummaryUseCase = ObserveMonthSummaryUseCase(transactionRepository),
            observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
            transactionRepository = transactionRepository,
            timeProvider = timeProvider,
            observeCardOverviewsUseCase =
                ObserveCardOverviewsUseCase(
                    accountRepository,
                    ObserveCardOverviewUseCase(accountRepository, cardStatementRepository, timeProvider),
                ),
            observeUpcomingPaymentsUseCase =
                ObserveUpcomingPaymentsUseCase(
                    recurringRepository,
                    accountRepository,
                    categoryRepository,
                    ObserveCardOverviewsUseCase(
                        accountRepository,
                        ObserveCardOverviewUseCase(accountRepository, cardStatementRepository, timeProvider),
                    ),
                    timeProvider,
                ),
            observeDebtsOverviewUseCase = ObserveDebtsOverviewUseCase(debtRepository),
        )

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        date: LocalDate = today,
        accountId: Long = 1L,
        categoryId: Long? = 10L,
        createdAt: Instant = baseInstant,
    ) = Transaction(
        id = id,
        type = type,
        amount = Money(amount),
        accountId = accountId,
        categoryId = categoryId,
        date = date,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun TestScope.observeState(viewModel: HomeViewModel): HomeViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `combines total balance current month summary accounts and recent transactions`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(cash, savings),
                balanceMap = mapOf(1L to Money(100_000L), 2L to Money(200_000L)),
            )
            transactionRepository.setInitialData(
                listOf(
                    transaction(id = 1L, type = TransactionType.INCOME, amount = 500_000L, categoryId = 20L),
                    transaction(id = 2L, type = TransactionType.EXPENSE, amount = 120_000L),
                    transaction(id = 3L, type = TransactionType.TRANSFER, amount = 50_000L, categoryId = null),
                ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.totalBalance).isEqualTo(Money(300_000L))
            assertThat(state.monthSummary.income).isEqualTo(Money(500_000L))
            assertThat(state.monthSummary.expense).isEqualTo(Money(120_000L))
            assertThat(state.monthSummary.net).isEqualTo(Money(380_000L))
            assertThat(state.accounts).hasSize(2)
            assertThat(state.recentTransactions).hasSize(3)
        }

    @Test
    fun `recent transactions are limited to ten`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                (1L..12L).map { id ->
                    transaction(
                        id = id,
                        type = TransactionType.EXPENSE,
                        amount = 10_000L,
                        createdAt = Instant.ofEpochMilli(id),
                    )
                },
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recentTransactions).hasSize(HOME_RECENT_LIMIT)
        }

    @Test
    fun `transfer quick action is enabled only with at least two active accounts`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash))
            val single = observeState(createViewModel())
            advanceUntilIdle()
            assertThat(single.uiState.value.canTransfer).isFalse()

            accountRepository.saveAccount(savings)
            advanceUntilIdle()
            assertThat(single.uiState.value.canTransfer).isTrue()
        }

    @Test
    fun `month summary only counts the current month`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                listOf(
                    transaction(id = 1L, type = TransactionType.EXPENSE, amount = 1_000L, date = today),
                    transaction(id = 2L, type = TransactionType.EXPENSE, amount = 2_000L, date = today.minusMonths(1)),
                ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.monthSummary.expense).isEqualTo(Money(1_000L))
        }

    @Test
    fun `card overviews start empty before the domain provides them`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash, savings))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.cardOverviews).isEmpty()
        }

    @Test
    fun `card overviews expose debt and available of a real card`() =
        runTest(testDispatcher) {
            val card =
                cash.copy(
                    id = 7L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = Money(-250_000L),
                    icon = "credit_card",
                    sortOrder = 2,
                    creditLimit = Money(500_000L),
                    statementDay = 15,
                    paymentDueDay = 5,
                )
            accountRepository.setInitialData(listOf(cash, card))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val overview = viewModel.uiState.value.cardOverviews[7L]
            assertThat(overview).isNotNull()
            assertThat(overview?.debt).isEqualTo(Money(250_000L))
            assertThat(overview?.available).isEqualTo(Money(250_000L))
        }

    @Test
    fun `upcoming payments from the domain reach the home state`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash, savings))
            categoryRepository.setInitialData(
                listOf(
                    Category(id = 10L, name = "Suscripciones", type = CategoryType.EXPENSE, color = 1, icon = "repeat"),
                ),
            )
            recurringRepository.setInitialData(
                listOf(
                    RecurringPayment(
                        id = 5L,
                        name = "Netflix",
                        type = TransactionType.EXPENSE,
                        amount = Money(45_900L),
                        accountId = 1L,
                        categoryId = 10L,
                        frequency = RecurrenceFrequency.MONTHLY,
                        startDate = today,
                        nextDueDate = today,
                        createdAt = baseInstant,
                        updatedAt = baseInstant,
                    ),
                ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val upcoming = viewModel.uiState.value.upcomingPayments
            assertThat(upcoming.map { it.title }).containsExactly("Netflix")
            assertThat(upcoming.single().date).isEqualTo(today)
            assertThat(upcoming.single().accountName).isEqualTo("Efectivo")
        }

    @Test
    fun `active debts from the domain reach the home state`() =
        runTest(testDispatcher) {
            debtRepository.setInitialData(
                listOf(
                    testDebt(id = 1L, personName = "Ana", direction = DebtDirection.OWED_TO_ME, amount = Money(500_000L)),
                ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val debts = viewModel.uiState.value.debts
            assertThat(debts?.hasActive).isTrue()
            assertThat(debts?.totalOwedToMe).isEqualTo(Money(500_000L))
        }
}
